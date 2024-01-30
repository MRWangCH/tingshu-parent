package com.atguigu.tingshu.album.service;

import com.atguigu.tingshu.model.album.TrackInfo;
import com.atguigu.tingshu.query.album.TrackInfoQuery;
import com.atguigu.tingshu.vo.album.TrackInfoVo;
import com.atguigu.tingshu.vo.album.TrackListVo;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import org.apache.ibatis.annotations.Param;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

public interface TrackInfoService extends IService<TrackInfo> {

    /***
     * 上传声音文件到腾讯云点播平台
     * @param file 上传的声音文件
     * @return
     */
    Map<String, String> uploadTrack(MultipartFile file);

    /**
     * 声音的保存
     * @param trackInfoVo
     * @return
     */
    void saveTrackInfo(Long userId, TrackInfoVo trackInfoVo);

    /**
     * 保存声音统计信息
     * @param id
     * @param statType
     */
    void saveTrackStat(Long id, String statType);

    /**
     * 当前用户声音列表页的分页查询
     * @param pageInfo mp分页对象
     * @param trackInfoQuery 查询条件
     * @return
     */
    Page<TrackListVo> getUserTrackPage(Page<TrackListVo> pageInfo, TrackInfoQuery trackInfoQuery);

    /**
     * 根据声音id修改声音
     * @param id
     * @param trackInfoVo
     * @return
     */
    void updateTrackInfo(Long id, TrackInfoVo trackInfoVo);
}
