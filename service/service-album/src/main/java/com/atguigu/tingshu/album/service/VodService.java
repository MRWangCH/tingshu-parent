package com.atguigu.tingshu.album.service;

import com.atguigu.tingshu.vo.album.TrackMediaInfoVo;

public interface VodService {

    /**
     * 根据云点播平台唯一标识获取媒体文件的详细信息
     * @param mediaFileId
     * @return
     */
    TrackMediaInfoVo getTrackMediaInfo(String mediaFileId);
}
