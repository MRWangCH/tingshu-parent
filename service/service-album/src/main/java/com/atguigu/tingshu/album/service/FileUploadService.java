package com.atguigu.tingshu.album.service;

import org.springframework.web.multipart.MultipartFile;

public interface FileUploadService {
    /**
     * 文件上传地址
     * @param file
     * @return 文件在线地址
     */
    String uploadImage(MultipartFile file);
}
