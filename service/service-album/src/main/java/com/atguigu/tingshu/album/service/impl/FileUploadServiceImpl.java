package com.atguigu.tingshu.album.service.impl;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.IdUtil;
import com.atguigu.tingshu.album.config.MinIoConfig;
import com.atguigu.tingshu.album.config.MinioConstantProperties;
import com.atguigu.tingshu.album.service.FileUploadService;
import com.atguigu.tingshu.common.execption.GuiguException;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.errors.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

@Slf4j
@Service
public class FileUploadServiceImpl implements FileUploadService {

    @Autowired
    private MinioClient minioClient;

    @Autowired
    private MinioConstantProperties props;

    /**
     * 文件上传地址
     * @param file
     * @return 文件在线地址
     */
    @Override
    public String uploadImage(MultipartFile file) {
        try {
            //1 校验上传的图片是否为图片
            BufferedImage read = ImageIO.read(file.getInputStream());
            if (read == null){
                throw new RuntimeException("图片格式非法");
            }
            //2 生成minio存储的唯一文件名称 日期+uuid+后缀
            String folderName = DateUtil.today();
            String fileName = IdUtil.randomUUID();
            String extName = FileUtil.extName(file.getOriginalFilename());
            //对象名称
            String objectName = "/" + folderName + "/" + fileName + "." + extName;
            //3 调用上传文件方法
            minioClient.putObject(
                    PutObjectArgs.builder().bucket(props.getBucketName()).object(objectName).stream(
                            file.getInputStream(), file.getSize(), -1
                    ).contentType(file.getContentType())
                    .build());

            //4 拼接文件在线地址
            return props.getEndpointUrl() + "/" + props.getBucketName() + objectName;
        } catch (Exception e) {
            log.error("[专辑服务]文件上传失败：{}",e);
            throw new RuntimeException(e);
        }
    }
}
