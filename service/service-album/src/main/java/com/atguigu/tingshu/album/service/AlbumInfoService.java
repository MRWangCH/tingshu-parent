package com.atguigu.tingshu.album.service;

import com.atguigu.tingshu.model.album.AlbumInfo;
import com.atguigu.tingshu.query.album.AlbumInfoQuery;
import com.atguigu.tingshu.vo.album.AlbumInfoVo;
import com.atguigu.tingshu.vo.album.AlbumListVo;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;

public interface AlbumInfoService extends IService<AlbumInfo> {

    /**
     * 新增专辑
     * @param albumInfoVo 专辑相关信息
     * @param userId 用户id
     */
    void saveAlbumInfo(AlbumInfoVo albumInfoVo, Long userId);

    /**
     * 新增专辑统计信息
     * @param albumId 专辑id
     * @param statType 统计类型
     */
    void saveAlbumStat(Long albumId, String statType);

    /**
     * 分页获取用户专辑列表
     * @param pageInfo MP查询对象
     * @param albumInfoQuery
     * @return
     */
    Page<AlbumListVo> getUserAlbumByPage(Page<AlbumListVo> pageInfo, AlbumInfoQuery albumInfoQuery);
}
