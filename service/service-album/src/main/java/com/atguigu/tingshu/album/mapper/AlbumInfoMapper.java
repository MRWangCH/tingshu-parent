package com.atguigu.tingshu.album.mapper;

import com.atguigu.tingshu.model.album.AlbumInfo;
import com.atguigu.tingshu.query.album.AlbumInfoQuery;
import com.atguigu.tingshu.vo.album.AlbumListVo;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface AlbumInfoMapper extends BaseMapper<AlbumInfo> {

    /**
     * 分页获取用户专辑列表
     * @param pageInfo
     * @param albumInfoQuery
     * @return
     */
    Page<AlbumListVo> getUserAlbumByPage(Page<AlbumListVo> pageInfo,@Param("vo") AlbumInfoQuery albumInfoQuery);
}
