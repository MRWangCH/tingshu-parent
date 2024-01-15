package com.atguigu.tingshu.album.config;

import io.minio.MinioClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MinIoConfig {

    @Autowired
    private MinioConstantProperties props;

    @Bean
    public MinioClient minioClient(){
        return MinioClient.builder()
                .endpoint(props.getEndpointUrl()) //操作minion地址，端口9000
                .credentials(props.getAccessKey(), props.getSecreKey())
                .build();
    }
}
