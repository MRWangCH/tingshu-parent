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
import com.atguigu.tingshu.vo.album.TrackInfoVo;
import com.atguigu.tingshu.vo.album.TrackMediaInfoVo;
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
import java.util.HashMap;
import java.util.Map;

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
}
