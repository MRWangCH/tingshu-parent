package com.atguigu.tingshu.order.receiver;

import com.atguigu.tingshu.common.constant.KafkaConstant;
import com.atguigu.tingshu.order.service.OrderInfoService;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Slf4j
@Component
public class OrderReceiver {

    @Autowired
    private OrderInfoService orderInfoService;

    /**
     * 监听在线修改的订单，修改订单状态
     *
     * @param record
     */
    @KafkaListener(topics = KafkaConstant.QUEUE_ORDER_PAY_SUCCESS)
    public void orderPaySuccess(ConsumerRecord<String, String> record) {
        String orderNo = record.value();
        if (StringUtils.hasText(orderNo)) {
            orderInfoService.orderPaySuccess(orderNo);
        }
    }
}


