package com.atguigu.tingshu.common.config.redis;

import lombok.Data;
import org.apache.commons.lang3.StringUtils;
import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.redisson.config.SingleServerConfig;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * redisson配置信息
 */
@Data
@Configuration
@ConfigurationProperties("spring.data.redis")
public class RedissonConfig {

    private String host;

    private String password;

    private String port;

    private int timeout = 3000;
    private static String ADDRESS_PREFIX = "redis://";

    /**
     * 自动装配
     */
    @Bean
    RedissonClient redissonSingle() {
        Config config = new Config();

        if (StringUtils.isBlank(host)) {
            throw new RuntimeException("host is  empty");
        }
        SingleServerConfig serverConfig = config
                //.useSentinelServers() 哨兵
                //.useClusterServers() Cluster集群
                //.useMasterSlaveServers() 主从
                .useSingleServer()
                .setAddress(ADDRESS_PREFIX + this.host + ":" + port)
                .setTimeout(this.timeout);
        if (StringUtils.isNotBlank(this.password)) {
            serverConfig.setPassword(this.password);
        }
        return Redisson.create(config);
    }
}