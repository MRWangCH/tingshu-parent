package com.atguigu.tingshu.common.cache;

import cn.hutool.core.util.RandomUtil;
import com.atguigu.tingshu.common.constant.RedisConstant;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
@Aspect
@Component
public class GuiguCacheAspect {

    @Autowired
    private RedisTemplate redisTemplate;

    @Autowired
    private RedissonClient redissonClient;

    /**
     * 缓存自定义注解的切面类
     *
     * @param joinPoint
     * @param guiguCache
     */
    @SneakyThrows
    @Around("@annotation(guiguCache)")
    public Object cacheAspect(ProceedingJoinPoint joinPoint, GuiguCache guiguCache) {
        try {
            Object result = new Object();
            //1 优先从缓存中获取数据
            //1.1 动态构建缓存数据的key，注解中定义的前缀的值+方法参数
            String param = "none";
            Object[] args = joinPoint.getArgs();
            if (args == null && args.length > 0) {
                param = Arrays.asList(args).stream().map(arg -> arg.toString()).collect(Collectors.joining(":"));
            }
            String dataKey = guiguCache.prefix() + param;
            //1.2 查询缓存
            result = redisTemplate.opsForValue().get(dataKey);
            //1.3 命中缓存直接返回
            if (result != null) {
                return result;
            }

            //2 获取分布式锁
            //2.1 动态构建锁key
            String lockKey = guiguCache.prefix() + param + RedisConstant.CACHE_LOCK_SUFFIX;
            //2.2 创建锁对象
            RLock lock = redissonClient.getLock(lockKey);
            //2.3 获取锁对象
            lock.lock();
            try {
                //3 业务逻辑（目标方法）
                //3.1 避免阻塞中的线程查询数据库，在查询一次缓存
                result = redisTemplate.opsForValue().get(dataKey);
                if (result != null) {
                    return result;
                }
                //3.2 查库
                result = joinPoint.proceed();
                //3.3 查询不为空，将空数据加入缓存中，设置过期时间10分钟
                if (result == null) {
                    redisTemplate.opsForValue().set(dataKey, result, RedisConstant.ALBUM_TEMPORARY_TIMEOUT, TimeUnit.SECONDS);
                    return result;
                }
                //3.4 查询不为空，将空数据加入缓存中，设置过期时间1小时+随机值
                int ttl = RandomUtil.randomInt(500, 3600);
                redisTemplate.opsForValue().set(dataKey, result, RedisConstant.ALBUM_TIMEOUT + ttl, TimeUnit.SECONDS);
                return result;
            } finally {
                //4 释放锁
                lock.unlock();
            }
        } catch (Throwable e) {
            //5 Redis服务不可用，直接查库
            log.error("缓存服务异常：{}", e);
            //出现异常，查询数据库
            return joinPoint.proceed();
        }
    }
}
