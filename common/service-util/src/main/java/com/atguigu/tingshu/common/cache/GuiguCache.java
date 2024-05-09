package com.atguigu.tingshu.common.cache;


import java.lang.annotation.*;

@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Inherited
@Documented
public @interface GuiguCache {
    /**
     * 注解属性：缓存数据前缀，分布式锁key的前缀
     */
    String prefix() default "cache";
}
