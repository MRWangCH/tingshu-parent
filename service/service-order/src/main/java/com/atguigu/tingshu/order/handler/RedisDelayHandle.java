package com.atguigu.tingshu.order.handler;

import com.atguigu.tingshu.common.constant.KafkaConstant;
import com.atguigu.tingshu.order.service.OrderInfoService;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RBlockingDeque;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.management.RuntimeMBeanException;

@Slf4j
@Component
public class RedisDelayHandle {

    @Autowired
    private RedissonClient redissonClient;

    @Autowired
    private OrderInfoService orderInfoService;

    /**
     * 项目启动后自动执行方法：监听延迟消息
     */
    @PostConstruct
    public void listener() {
        //1 基于Redisson客户端获取阻塞队列
        RBlockingDeque<String> blockingDeque = redissonClient.getBlockingDeque(KafkaConstant.QUEUE_ORDER_CANCEL);
        //2 开启线程专门监听阻塞队列中到时消息执行业务
        new Thread(() -> {
            while (true) {
                String orderId = null;
                try {
                    orderId = blockingDeque.take();
                    orderInfoService.orderCancle(orderId);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
                log.info("监听到延迟消息内容：" + orderId);
            }
        }).start();
    }
}
