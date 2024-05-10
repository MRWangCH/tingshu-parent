package com.atguigu.tingshu;

import com.atguigu.tingshu.common.constant.RedisConstant;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RBloomFilter;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication
@EnableDiscoveryClient
@EnableFeignClients
@Slf4j
public class ServiceAlbumApplication implements CommandLineRunner {

    public static void main(String[] args) {
        SpringApplication.run(ServiceAlbumApplication.class, args);
    }

    @Autowired
    private RedissonClient redissonClient;


    /**
     * springBoot项目启动完成后自动执行一次
     * @param args
     * @throws Exception
     */
    @Override
    public void run(String... args) throws Exception {
        //1 调用Redisson提供布隆过滤器api完成初始化
        RBloomFilter<Object> bloomFilter = redissonClient.getBloomFilter(RedisConstant.ALBUM_BLOOM_FILTER);
        bloomFilter.tryInit(500000, 0.3);
        log.info("[专辑服务]启动服务后初始化布隆过滤器");
    }
}
