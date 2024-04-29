package com.atguigu.tingshu.album.mapper;

import com.atguigu.tingshu.model.album.TrackInfo;
import com.atguigu.tingshu.query.album.TrackInfoQuery;
import com.atguigu.tingshu.vo.album.AlbumTrackListVo;
import com.atguigu.tingshu.vo.album.TrackListVo;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface TrackInfoMapper extends BaseMapper<TrackInfo> {


    /**
     * 当前用户声音列表页的分页查询
     * @param pageInfo mp分页对象
     * @param trackInfoQuery 查询条件
     * @return
     */
    Page<TrackListVo> getUserTrackPage(Page<TrackListVo> pageInfo, @Param("vo") TrackInfoQuery trackInfoQuery);

    /**
     * 修改专辑的声音排序
     * @param albumId
     * @param orderNum
     */
    @Update("update track_info set order_num = order_num -1 where album_id = #{albumId} and order_num > #{orderNum} and is_deleted = '0'")
    void updateTrackNum(@Param("albumId") Long albumId, @Param("orderNum") Integer orderNum);

    /**
     * 根据专辑id分页查询声音列表
     * @param pageInfo
     * @param albumId
     * @return
     */
    Page<AlbumTrackListVo> getUserAlbumTrackPage(Page<AlbumTrackListVo> pageInfo, @Param("albumId") Long albumId);
}
