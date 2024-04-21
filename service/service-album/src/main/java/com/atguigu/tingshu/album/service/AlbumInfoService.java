package com.atguigu.tingshu.album.service;

import com.atguigu.tingshu.model.album.AlbumInfo;
import com.atguigu.tingshu.query.album.AlbumInfoQuery;
import com.atguigu.tingshu.vo.album.AlbumInfoVo;
import com.atguigu.tingshu.vo.album.AlbumListVo;
import com.atguigu.tingshu.vo.album.AlbumStatVo;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

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

    /**
     * 根据专辑id删除专辑
     * @param id
     */
    void removeAlbumInfo(Long id);

    /**
     * 修改时根据专辑id查询数据的回写
     * @param id
     * @return
     */
    AlbumInfo getAlbumInfo(Long id);

    /**
     * 专辑修改
     * TODO 该接口必须登录才能访问
     * @param id 专辑id
     * @param albumInfovo 修改后的专辑
     * @return
     */
    void updateAlbumInfo(Long id, AlbumInfoVo albumInfovo);

    /**
     * 查询当前登录用户的所有专辑列表
     * TODO 该接口必须登录才能访问
     * @return
     */
    List<AlbumInfo> getUserAlbumList(Long userId);

    /**
     * 根据专辑ID获取专辑统计信息
     * @param albumId
     * @return
     */
    AlbumStatVo getAlbumStatVo(Long albumId);
}
