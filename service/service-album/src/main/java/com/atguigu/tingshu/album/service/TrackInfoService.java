package com.atguigu.tingshu.album.service;

import com.atguigu.tingshu.model.album.TrackInfo;
import com.atguigu.tingshu.query.album.TrackInfoQuery;
import com.atguigu.tingshu.vo.album.AlbumTrackListVo;
import com.atguigu.tingshu.vo.album.TrackInfoVo;
import com.atguigu.tingshu.vo.album.TrackListVo;
import com.atguigu.tingshu.vo.album.TrackStatVo;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import org.apache.ibatis.annotations.Param;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
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

    /**
     * 根据声音id删除声音信息
     * @param id
     * @return
     */
    void removeTrackInfo(Long id);

    /**
     * 根据登录情况查询专辑声音列表付费情况
     * @param pageInfo
     * @param userId
     * @param albumId
     * @return
     */
    Page<AlbumTrackListVo> getUserAlbumTrackPage(Page<AlbumTrackListVo> pageInfo, Long userId, Long albumId);

    /**
     * 消息队列Kafka更新专辑声音统计信息
     * @param albumId
     * @param trackId
     * @param statType
     * @param count
     */
    void updateStat(Long albumId, Long trackId, String statType, Integer count);

    /**
     * 根据声音id查询声音统计信息
     * @param trackId
     * @return
     */
    TrackStatVo getTrackStatVo(Long trackId);

    /**
     * 获取用户声音分集购买支付列表
     * @param userId
     * @param trackId
     * @return
     */
    List<Map<String, Object>> getUserTrackWaitPayList(Long userId, Long trackId);

    /**
     * 查询用户声音分集购买支付列表-用于渲染订单结算页
     * @param userId
     * @param trackId
     * @param trackCount
     * @return
     */
    List<TrackInfo> getWaitPayTrackInfoList(Long userId, Long trackId, Integer trackCount);
}
