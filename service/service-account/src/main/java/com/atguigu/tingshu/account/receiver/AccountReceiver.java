package com.atguigu.tingshu.account.receiver;

import com.atguigu.tingshu.account.service.UserAccountService;
import com.atguigu.tingshu.common.constant.KafkaConstant;
import io.micrometer.common.util.StringUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class AccountReceiver {

    @Autowired
    private UserAccountService userAccountService;

    /**
     * 监听新增账户的额消息完成账户余额初始化
     *
     * @param record
     */
    @KafkaListener(topics = KafkaConstant.QUEUE_USER_REGISTER)
    public void processInitAccount(ConsumerRecord<String, String> record) {
        String userId = record.value();
        if (StringUtils.isNotBlank(userId)) {
            log.info("[账户服务]监听到初始化信息：{}", userId);
            userAccountService.saveUserAccount(Long.valueOf(userId));
        }
    }

    /**
     * 监听扣减账户金额消息，完成账户余额扣减
     * @param record
     */
    @KafkaListener(topics = KafkaConstant.QUEUE_ACCOUNT_MINUS)
    public void accountMinus(ConsumerRecord<String, String> record) {
        String orderNo = record.value();
        if (StringUtils.isNotBlank(orderNo)) {
            log.info("[账户服务]监听到扣减账户余额消息：{}", orderNo);
            userAccountService.accountMinus(orderNo);
        }
    }

}
