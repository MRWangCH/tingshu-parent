package com.atguigu.tingshu.album.mapper;

import com.atguigu.tingshu.model.album.TrackInfo;
import com.atguigu.tingshu.query.album.TrackInfoQuery;
import com.atguigu.tingshu.vo.album.TrackListVo;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface TrackInfoMapper extends BaseMapper<TrackInfo> {


    /**
     * 当前用户声音列表页的分页查询
     * @param pageInfo mp分页对象
     * @param trackInfoQuery 查询条件
     * @return
     */
    Page<TrackListVo> getUserTrackPage(Page<TrackListVo> pageInfo, @Param("vo") TrackInfoQuery trackInfoQuery);
}
