package com.atguigu.tingshu.album.service;

import com.atguigu.tingshu.model.album.TrackInfo;
import com.baomidou.mybatisplus.extension.service.IService;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

public interface TrackInfoService extends IService<TrackInfo> {

    /***
     * 上传声音文件到腾讯云点播平台
     * @param file 上传的声音文件
     * @return
     */
    Map<String, String> uploadTrack(MultipartFile file);
}
