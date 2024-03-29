package com.atguigu.tingshu.album.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollectionUtil;
import com.atguigu.tingshu.album.mapper.AlbumAttributeValueMapper;
import com.atguigu.tingshu.album.mapper.AlbumInfoMapper;
import com.atguigu.tingshu.album.mapper.AlbumStatMapper;
import com.atguigu.tingshu.album.mapper.TrackInfoMapper;
import com.atguigu.tingshu.album.service.AlbumInfoService;
import com.atguigu.tingshu.common.constant.SystemConstant;
import com.atguigu.tingshu.model.album.AlbumAttributeValue;
import com.atguigu.tingshu.model.album.AlbumInfo;
import com.atguigu.tingshu.model.album.AlbumStat;
import com.atguigu.tingshu.model.album.TrackInfo;
import com.atguigu.tingshu.query.album.AlbumInfoQuery;
import com.atguigu.tingshu.vo.album.AlbumAttributeValueVo;
import com.atguigu.tingshu.vo.album.AlbumInfoVo;
import com.atguigu.tingshu.vo.album.AlbumListVo;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
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

	@Autowired
	private TrackInfoMapper trackInfoMapper;

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

	/**
	 * 根据专辑id删除专辑
	 * 1 根据id删除专辑表
	 * 2 根据id删除统计表
	 * 3 根据id删除属性表
	 * 4 根据id删除声音表
	 * @param id 专辑id
	 */
	@Override
	@Transactional(rollbackFor = Exception.class)
	public void removeAlbumInfo(Long id) {
		//1 根据id删除专辑表
		albumInfoMapper.deleteById(id);
		//2 根据id删除统计表
		LambdaQueryWrapper<AlbumStat> wrapper = new LambdaQueryWrapper<>();
		wrapper.eq(AlbumStat::getAlbumId, id);
		albumStatMapper.delete(wrapper);

		//3 根据id删除属性表
		LambdaQueryWrapper<AlbumAttributeValue> wrapper1 = new LambdaQueryWrapper<>();
		wrapper1.eq(AlbumAttributeValue::getAlbumId, id);
		albumAttributeValueMapper.delete(wrapper1);

		//4 根据id删除声音表
		LambdaQueryWrapper<TrackInfo> wrapper2 = new LambdaQueryWrapper<>();
		wrapper2.eq(TrackInfo::getAlbumId, id);
		trackInfoMapper.delete(wrapper2);
	}

	/**
	 * 修改时根据专辑id查询数据的回写
	 * @param id
	 * @return
	 */
	@Override
	public AlbumInfo getAlbumInfo(Long id) {
		//1 根据主键查询专辑信息
		AlbumInfo albumInfo = albumInfoMapper.selectById(id);

		//2 根据专辑id查询属性列表
		LambdaQueryWrapper<AlbumAttributeValue> queryWrapper = new LambdaQueryWrapper<>();
		queryWrapper.eq(AlbumAttributeValue::getAlbumId, id);
		List<AlbumAttributeValue> albumAttributeValues = albumAttributeValueMapper.selectList(queryWrapper);
		albumInfo.setAlbumAttributeValueVoList(albumAttributeValues);
		return albumInfo;
	}

	/**
	 * 专辑修改
	 * 1 修改专辑信息
	 * 2 修改专辑属性（先删除旧属性，再新增）
	 * @param id 专辑id
	 * @param albumInfovo 修改后的专辑
	 * @return
	 */
	@Override
	@Transactional(rollbackFor = Exception.class)
	public void updateAlbumInfo(Long id, AlbumInfoVo albumInfovo) {
		//1 修改专辑信息
		//1.1 将vo转成po并且设置主键
		AlbumInfo albumInfo = BeanUtil.copyProperties(albumInfovo, AlbumInfo.class);
		albumInfo.setId(id);
		//1.2 修改
		albumInfoMapper.updateById(albumInfo);

		//2 修改专辑属性
		//2.1 根据专辑id条件删除专辑属性关系
		LambdaQueryWrapper<AlbumAttributeValue> lambdaQueryWrapper = new LambdaQueryWrapper<>();
		albumAttributeValueMapper.delete(lambdaQueryWrapper);
		//2.2 根据用户提交的专辑属性新增（关联专辑id）
		List<AlbumAttributeValueVo> lists = albumInfovo.getAlbumAttributeValueVoList();
		if (CollectionUtil.isNotEmpty(lists)){
			lists.forEach(list ->{
				//转为po对象，关联专辑
				AlbumAttributeValue albumAttributeValue = BeanUtil.copyProperties(list, AlbumAttributeValue.class);
				albumAttributeValue.setAlbumId(id);
				albumAttributeValueMapper.insert(albumAttributeValue);
			} );

		}
	}

	/**
	 * 查询当前登录用户的所有专辑列表
	 * @return
	 */
	@Override
	public List<AlbumInfo> getUserAlbumList(Long userId) {
		//1 构建查询条件 只需要专辑id，title
		LambdaQueryWrapper<AlbumInfo> queryWrapper = new LambdaQueryWrapper<>();
		//根据创建时间倒叙
		queryWrapper.eq(AlbumInfo::getUserId, userId)
					.select(AlbumInfo::getId, AlbumInfo::getAlbumTitle)//查询id，title两个字段
					.orderByDesc(AlbumInfo::getCreateTime)//根据创建时间倒序
					.last("limit 100");//查询100条
		//2 查询
		return albumInfoMapper.selectList(queryWrapper);
	}
}
