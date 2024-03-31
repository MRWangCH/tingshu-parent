package com.atguigu.tingshu.common.config.thread;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

@Configuration
public class ThreadPoolConfig {

    /**
     * 注册自定义线程池
     *
     * @return
     */

    @Bean
    public ThreadPoolExecutor threadPoolExecutor() {
        //获取可用处理器对象
        int availableProcessors = Runtime.getRuntime().availableProcessors();
        availableProcessors = availableProcessors * 2;
        //1 创建一个线程池对象
        ThreadPoolExecutor threadPoolExecutor = new ThreadPoolExecutor(
                availableProcessors,//核心线程数
                availableProcessors,//最大线程数
                0,                  //非核心线程数空闲时间
                TimeUnit.SECONDS,//
                new ArrayBlockingQueue<>(200),
                Executors.defaultThreadFactory(), //创建线程工厂对象
                (r, t) -> {
                    //触发自动拒绝策略，要求任务必须执行
                    try {
                        Thread.sleep(200);
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                    //将被拒绝的任务再次尝试提交给线程执行
                    t.submit(r);
                }
        );
        //2 项目启动需要大量线程，提前将线程创建
        threadPoolExecutor.prestartAllCoreThreads();
        return threadPoolExecutor;
    }
}
