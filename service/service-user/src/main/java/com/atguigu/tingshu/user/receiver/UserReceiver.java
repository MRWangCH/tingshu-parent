package com.atguigu.tingshu.user.receiver;

import com.alibaba.fastjson.JSON;
import com.atguigu.tingshu.common.constant.KafkaConstant;
import com.atguigu.tingshu.user.service.UserInfoService;
import com.atguigu.tingshu.vo.user.UserPaidRecordVo;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Slf4j
@Component
public class UserReceiver {

    @Autowired
    private UserInfoService userInfoService;

    /**
     * 处理用户的购买记录，声音，vip，专辑
     * @param record
     */
    @KafkaListener(topics = KafkaConstant.QUEUE_USER_PAY_RECORD)
    public void saveUserPayRecord(ConsumerRecord<String, String> record) {
        String userPayRecordStr = record.value();
        if (StringUtils.hasText(userPayRecordStr)) {
            log.info("[用户服务]监听处理购买记录：{}", userPayRecordStr);
            UserPaidRecordVo userPaidRecordVo = JSON.parseObject(userPayRecordStr, UserPaidRecordVo.class);
            userInfoService.saveUserPayRecord(userPaidRecordVo);
        }
    }
}
