package com.atguigu.tingshu.album.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollectionUtil;
import com.atguigu.tingshu.album.mapper.AlbumAttributeValueMapper;
import com.atguigu.tingshu.album.mapper.AlbumInfoMapper;
import com.atguigu.tingshu.album.mapper.AlbumStatMapper;
import com.atguigu.tingshu.album.service.AlbumInfoService;
import com.atguigu.tingshu.common.constant.SystemConstant;
import com.atguigu.tingshu.model.album.AlbumAttributeValue;
import com.atguigu.tingshu.model.album.AlbumInfo;
import com.atguigu.tingshu.model.album.AlbumStat;
import com.atguigu.tingshu.query.album.AlbumInfoQuery;
import com.atguigu.tingshu.vo.album.AlbumAttributeValueVo;
import com.atguigu.tingshu.vo.album.AlbumInfoVo;
import com.atguigu.tingshu.vo.album.AlbumListVo;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@SuppressWarnings({"all"})
public class AlbumInfoServiceImpl extends ServiceImpl<AlbumInfoMapper, AlbumInfo> implements AlbumInfoService {

	@Autowired
	private AlbumInfoMapper albumInfoMapper;

	@Autowired
	private AlbumAttributeValueMapper albumAttributeValueMapper;

	@Autowired
	private AlbumStatMapper albumStatMapper;


	/**
	 * 新增专辑
	 * 1.向专辑信息表增加一条记录
	 * 2.向专辑属性表新增若干记录
	 * 3.向专辑统计表中新增4条记录
	 * @param albumInfoVo 专辑相关信息
	 * @param userId 用户id
	 */
	@Override
	@Transactional(rollbackFor = Exception.class) //Spring事务管理默认捕获RunTimeException才会事务回滚
	public void saveAlbumInfo(AlbumInfoVo albumInfoVo, Long userId) {
		//1.向专辑信息表增加一条记录
		//1.1将前端提交的vo对象转成po对象
		AlbumInfo albumInfo = BeanUtil.copyProperties(albumInfoVo, AlbumInfo.class);
		//1.2单独为部分属性赋值
		albumInfo.setUserId(userId);
		if (!SystemConstant.ALBUM_PAY_TYPE_FREE.equals(albumInfo.getPayType())){
			albumInfo.setTracksForFree(5);
		}
		//暂无审核写成通过
		albumInfo.setStatus(SystemConstant.ALBUM_STATUS_PASS);
		//1.3保存专辑，得到专辑id
		albumInfoMapper.insert(albumInfo);
		Long albumId = albumInfo.getId();
		//2.向专辑属性表新增若干记录
		List<AlbumAttributeValueVo> albumAttributeValueVoList = albumInfoVo.getAlbumAttributeValueVoList();
		if (CollectionUtil.isNotEmpty(albumAttributeValueVoList)){
			//遍历vo集合 将vo转成po
			albumAttributeValueVoList.forEach(albumAttributeValueVo -> {
				//关联专辑id
				AlbumAttributeValue albumAttributeValue = BeanUtil.copyProperties(albumAttributeValueVo, AlbumAttributeValue.class);
				albumAttributeValue.setAlbumId(albumId);
				//新增专辑属性
				albumAttributeValueMapper.insert(albumAttributeValue);
			});
		}
		//3.向专辑统计表中新增4条记录(播放数，订阅数，购买，评论数)
		this.saveAlbumStat(albumId, SystemConstant.ALBUM_STAT_PLAY);
		this.saveAlbumStat(albumId, SystemConstant.ALBUM_STAT_SUBSCRIBE);
		this.saveAlbumStat(albumId, SystemConstant.ALBUM_STAT_BUY);
		this.saveAlbumStat(albumId, SystemConstant.ALBUM_STAT_COMMENT);
	}

	/**
	 * @param albumId 专辑id
	 * @param statType 统计类型
	 */
	@Override
	public void saveAlbumStat(Long albumId, String statType) {
		AlbumStat albumStat = new AlbumStat();
		albumStat.setAlbumId(albumId);
		albumStat.setStatType(statType);
		albumStat.setStatNum(0);
		albumStatMapper.insert(albumStat);
	}

	/**
	 * 分页获取用户专辑列表
	 * @param pageInfo MP查询对象
	 * @param albumInfoQuery
	 * @return
	 */
	@Override
	public Page<AlbumListVo> getUserAlbumByPage(Page<AlbumListVo> pageInfo, AlbumInfoQuery albumInfoQuery) {
		return albumInfoMapper.getUserAlbumByPage(pageInfo, albumInfoQuery);
	}
}
