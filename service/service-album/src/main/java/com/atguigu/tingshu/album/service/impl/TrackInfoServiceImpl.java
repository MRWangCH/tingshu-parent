package com.atguigu.tingshu.album.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.atguigu.tingshu.album.config.VodConstantProperties;
import com.atguigu.tingshu.album.mapper.AlbumInfoMapper;
import com.atguigu.tingshu.album.mapper.TrackInfoMapper;
import com.atguigu.tingshu.album.mapper.TrackStatMapper;
import com.atguigu.tingshu.album.service.TrackInfoService;
import com.atguigu.tingshu.album.service.VodService;
import com.atguigu.tingshu.common.constant.SystemConstant;
import com.atguigu.tingshu.common.util.UploadFileUtil;
import com.atguigu.tingshu.model.album.AlbumInfo;
import com.atguigu.tingshu.model.album.TrackInfo;
import com.atguigu.tingshu.model.album.TrackStat;
import com.atguigu.tingshu.query.album.TrackInfoQuery;
import com.atguigu.tingshu.user.client.UserFeignClient;
import com.atguigu.tingshu.vo.album.AlbumTrackListVo;
import com.atguigu.tingshu.vo.album.TrackInfoVo;
import com.atguigu.tingshu.vo.album.TrackListVo;
import com.atguigu.tingshu.vo.album.TrackMediaInfoVo;
import com.atguigu.tingshu.vo.user.UserInfoVo;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.qcloud.vod.VodUploadClient;
import com.qcloud.vod.model.VodUploadRequest;
import com.qcloud.vod.model.VodUploadResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@SuppressWarnings({"all"})
public class TrackInfoServiceImpl extends ServiceImpl<TrackInfoMapper, TrackInfo> implements TrackInfoService {

	@Autowired
	private TrackInfoMapper trackInfoMapper;

	@Autowired
	private VodConstantProperties props;

	@Autowired
	private AlbumInfoMapper albumInfoMapper;

	@Autowired
	private VodService vodService;

	@Autowired
	private TrackStatMapper trackStatMapper;

	@Autowired
	private UserFeignClient userFeignClient;

	/***
	 * 上传声音文件到腾讯云点播平台
	 * @param file 上传的声音文件
	 * @return
	 */
	@Override
	public Map<String, String> uploadTrack(MultipartFile file) {
		try {
			//将用户上传的临时文件保存到临时目录下
			String tempFilePath = UploadFileUtil.uploadTempPath(props.getTempPath(), file);
			//初始化一个上传客户端对象
			VodUploadClient client = new VodUploadClient(props.getSecretId(), props.getSecretKey());
			//构建上传请求对象
			VodUploadRequest request = new VodUploadRequest();
			request.setMediaFilePath(tempFilePath);
			VodUploadResponse response = client.upload(props.getRegion(), request);
			if (response!= null){
				//获取上传后文件地址
				String mediaUrl = response.getMediaUrl();
				//获取上传后文件唯一标识
				String fileId = response.getFileId();
				Map<String, String> map = new HashMap<>();
				map.put("mediaUrl", mediaUrl);
				map.put("mediaFileId", fileId);
				return map;
			}
			return null;
		} catch (Exception e) {
			log.error("云点播平台上传文件失败");
			throw new RuntimeException(e);
		}
	}

	/**
	 * 声音的保存
	 * TODO 该接口必须登录后才能访问
	 * 1 新增声音记录
	 * 2 更新专辑
	 * 3 初始化声音统计记录
	 * @param trackInfoVo
	 * @return
	 */
	@Override
	@Transactional(rollbackFor = Exception.class)
	public void saveTrackInfo(Long userId, TrackInfoVo trackInfoVo) {
		//1 新增声音记录
		//1.1 声音vo拷贝到声音po对象
		TrackInfo trackInfo = BeanUtil.copyProperties(trackInfoVo, TrackInfo.class);
		//1.2 设置用户id
		trackInfo.setUserId(userId);
		//1.3 设置状态
		trackInfo.setStatus(SystemConstant.ALBUM_STATUS_PASS);
		//1.4设置声音序号
		trackInfo.setSource(SystemConstant.TRACK_SOURCE_USER);
		//1.5设置声音序号 根据专辑得到已有声音数量
		AlbumInfo albumInfo = albumInfoMapper.selectById(trackInfo.getAlbumId());
		trackInfo.setOrderNum(albumInfo.getIncludeTrackCount() + 1);
		//1.6设置声音文件相关的信息，大小时长类型 从点播平台获取
		TrackMediaInfoVo mediaInfoVo = vodService.getTrackMediaInfo(trackInfoVo.getMediaFileId());
		if (mediaInfoVo != null){
			trackInfo.setMediaDuration(BigDecimal.valueOf(mediaInfoVo.getDuration()));
			trackInfo.setMediaSize(mediaInfoVo.getSize());
			trackInfo.setMediaType(mediaInfoVo.getType());
		}
		//保存
		trackInfoMapper.insert(trackInfo);
		//2 更新专辑
		albumInfo.setIncludeTrackCount(albumInfo.getIncludeTrackCount() + 1);
		albumInfoMapper.updateById(albumInfo);
		//3 初始化声音统计记录
		this.saveTrackStat(trackInfo.getId(), SystemConstant.TRACK_STAT_PLAY);
		this.saveTrackStat(trackInfo.getId(), SystemConstant.TRACK_STAT_COLLECT);
		this.saveTrackStat(trackInfo.getId(), SystemConstant.TRACK_STAT_PRAISE);
		this.saveTrackStat(trackInfo.getId(), SystemConstant.TRACK_STAT_COMMENT);
	}

	/**
	 * 保存声音统计信息
	 * @param id
	 * @param statType
	 */
	@Override
	public void saveTrackStat(Long id, String statType) {
		TrackStat trackStat = new TrackStat();
		trackStat.setTrackId(id);
		trackStat.setStatType(statType);
		trackStat.setStatNum(0);
		trackStatMapper.insert(trackStat);
	}

	/**
	 * 当前用户声音列表页的分页查询
	 * @param pageInfo mp分页对象
	 * @param trackInfoQuery 查询条件
	 * @return
	 */
	@Override
	public Page<TrackListVo> getUserTrackPage(Page<TrackListVo> pageInfo, TrackInfoQuery trackInfoQuery) {
		return trackInfoMapper.getUserTrackPage(pageInfo, trackInfoQuery);
	}

	/**
	 * 根据声音id修改声音
	 * @param id
	 * @param trackInfoVo
	 * @return
	 */
	@Override
	@Transactional(rollbackFor = Exception.class)
	public void updateTrackInfo(Long id, TrackInfoVo trackInfoVo) {
		//1 获取更新前声音唯一信息
		TrackInfo trackInfo = trackInfoMapper.selectById(id);
		//数据库中的数据
		String mediaFileId = trackInfo.getMediaFileId();
		BeanUtil.copyProperties(trackInfoVo, trackInfo);
		//2 调用腾讯云点播平台获取声音信息 时长，大小，类型
		if (!mediaFileId.equals(trackInfo.getMediaFileId())){
			TrackMediaInfoVo trackMediaInfo = vodService.getTrackMediaInfo(trackInfo.getMediaFileId());
			trackInfo.setMediaType(trackMediaInfo.getType());
			trackInfo.setMediaDuration(BigDecimal.valueOf(trackMediaInfo.getDuration()));
			trackInfo.setMediaSize(trackMediaInfo.getSize());
		}
		//3 持久层更新
		trackInfoMapper.updateById(trackInfo);
	}

	/**
	 * 根据声音id删除声音信息
	 * @param id
	 * @return
	 */
	@Override
	@Transactional(rollbackFor = Exception.class)
	public void removeTrackInfo(Long id) {
		//1 更新声音表中的序号（更新当前记录以后的序号）
		TrackInfo trackInfo = trackInfoMapper.selectById(id);
		Integer orderNum = trackInfo.getOrderNum();
		//更新当前声音以后的声音的序号，-1   update track_info set order_num = order_num -1 where album_id = ? and track_num > orderNum
		trackInfoMapper.updateTrackNum(trackInfo.getAlbumId(),orderNum);
		//2 删除声音表记录
		trackInfoMapper.deleteById(id);
		//3 删除统计表记录
		LambdaQueryWrapper<TrackStat> queryWrapper = new LambdaQueryWrapper<>();
		queryWrapper.eq(TrackStat::getTrackId, id);
		trackStatMapper.delete(queryWrapper);
		//4 修改专辑表统计数
		AlbumInfo albumInfo = albumInfoMapper.selectById(trackInfo.getAlbumId());
		albumInfo.setIncludeTrackCount(albumInfo.getIncludeTrackCount() - 1);
		albumInfoMapper.updateById(albumInfo);
		//5 删除云点播平台记录
		vodService.deleteTrackMedia(trackInfo.getMediaFileId());
	}


	/**
	 * 查询专辑声音列表
	 * @param pageInfo
	 * @param userId
	 * @param albumId
	 * @return
	 */
	@Override
	public Page<AlbumTrackListVo> getUserAlbumTrackPage(Page<AlbumTrackListVo> pageInfo, Long userId, Long albumId) {
		//1 根据专辑id分页查询声音列表-包含声音统计信息
		pageInfo = trackInfoMapper.getUserAlbumTrackPage(pageInfo, albumId);
		List<AlbumTrackListVo> trackList = pageInfo.getRecords();
		//对当前声音付费标识业务处理 找出声音需要付费的情况处理
		//2 根据专辑id查询专辑信息 得到专辑付费类型以及专辑免费试听集数（VIP免费，付费）
		AlbumInfo albumInfo = albumInfoMapper.selectById(albumId);
		//2.1 获取专辑付费类型: 0101-免费、0102-vip免费、0103-付费
		String payType = albumInfo.getPayType();

		//3 用户未登录
		if (userId == null) {
			// 专辑付费类型为vip免费或者付费
			if (!SystemConstant.ALBUM_PAY_TYPE_FREE.equals(payType)) {
				//将当前页中声音列表获取到，找出非试听的声音列表，为非试听声音设置付费标识
				trackList.stream().filter(trackInfo -> {
					return trackInfo.getOrderNum() > albumInfo.getTracksForFree();
				}).collect(Collectors.toList()).stream().forEach(trackInfo -> {
					//非试听设置付费标识为true
					trackInfo.setIsShowPaidMark(true);
				});
			}
		} else {
			//4 用户已登录 登录用户为普通用户或者vip过期 未购买专辑或者部分声音付费标识设置为true；付费的专辑未购买声音，将声音付费标识设置为true
			//4.1 远程调用用户微服务获取用户是否为vip
			UserInfoVo userInfo = userFeignClient.getUserInfoVoByUserId(userId).getData();
			//声明变量是否需要购买
			Boolean isPaid = false;
			if (SystemConstant.ALBUM_PAY_TYPE_VIPFREE.equals(payType)) {
				//vip免费 --> 普通用户或者过期 查看用户是否购买过专辑或者声音
				if (userInfo.getIsVip().intValue() == 0) {
					//普通用户
					isPaid = true;
				}
				if (userInfo.getIsVip().intValue() == 1 && userInfo.getVipExpireTime().before(new Date())) {
					//vip会员过期，会员到期时间在当前时间之前， 会有延迟任务更新会员过期时间
					isPaid = true;
				}
			} else if (SystemConstant.ALBUM_PAY_TYPE_REQUIRE.equals(payType)) {
				//必须购买 --> 普通用户或者vip查看用户是否购买过专辑或者声音
				isPaid = true;
			}
			//5 统一处理需要购买情况，如果未购买专辑或者声音，将声音付费标识设置为true
			if (isPaid) {
				//5.1 得到当前页中声音列表id
				List<AlbumTrackListVo> trackListVoList = trackList.stream().filter(trackInfo -> {
					//将试听的过滤掉
					return trackInfo.getOrderNum() > albumInfo.getTracksForFree();
				}).collect(Collectors.toList());

				List<Long> trackIdList = trackListVoList.stream().map(AlbumTrackListVo::getTrackId).collect(Collectors.toList());

				//5.2 远程调用用户微服务查询当前页中声音列表购买情况Map
				Map<Long, Integer> buyStatusMap = userFeignClient.userIsPaidTrackList(userId, albumId, trackIdList).getData();
				//5.3 当前页中声音未购买 将指定声音付费标识设置为true
				for (AlbumTrackListVo albumTrackListVo : trackListVoList) {
					//获取声音购买结果
					Integer isBuy = buyStatusMap.get(albumTrackListVo.getTrackId());
					if (isBuy == 0) {
						//当前用户未购买该专辑或者声音
						albumTrackListVo.setIsShowPaidMark(true);
					}
				}
			}
		}
		return pageInfo;
	}
}
