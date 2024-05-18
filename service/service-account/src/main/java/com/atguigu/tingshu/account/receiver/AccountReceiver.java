package com.atguigu.tingshu.account.receiver;

import com.atguigu.tingshu.account.service.RechargeInfoService;
import com.atguigu.tingshu.account.service.UserAccountService;
import com.atguigu.tingshu.common.constant.KafkaConstant;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Slf4j
@Component
public class AccountReceiver {

    @Autowired
    private UserAccountService userAccountService;

    @Autowired
    private RechargeInfoService rechargeInfoService;

    /**
     * 监听新增账户的额消息完成账户余额初始化
     *
     * @param record
     */
    @KafkaListener(topics = KafkaConstant.QUEUE_USER_REGISTER)
    public void processInitAccount(ConsumerRecord<String, String> record) {
        String userId = record.value();
        if (StringUtils.hasText(userId)) {
            log.info("[账户服务]监听到初始化信息：{}", userId);
            userAccountService.saveUserAccount(Long.valueOf(userId));
        }
    }

    /**
     * 监听扣减账户金额消息，完成账户余额扣减
     *
     * @param record
     */
    @KafkaListener(topics = KafkaConstant.QUEUE_ACCOUNT_MINUS)
    public void accountMinus(ConsumerRecord<String, String> record) {
        String orderNo = record.value();
        if (StringUtils.hasText(orderNo)) {
            log.info("[账户服务]监听到扣减账户余额消息：{}", orderNo);
            userAccountService.accountMinus(orderNo);
        }
    }

    /**
     * 监听解锁账户金额消息，完成账户余额恢复
     *
     * @param record
     */
    @KafkaListener(topics = KafkaConstant.QUEUE_ACCOUNT_UNLOCK)
    public void accountUnlock(ConsumerRecord<String, String> record) {
        String orderNo = record.value();
        if (StringUtils.hasText(orderNo)) {
            log.info("[账户服务]监听到解锁账户金额消息：{}", orderNo);
            userAccountService.accountUnlock(orderNo);
        }
    }

    /**
     * 监听用户充值成功消息
     *
     * @param record
     */
    @KafkaListener(topics = KafkaConstant.QUEUE_RECHARGE_PAY_SUCCESS)
    public void rechargeSuccess(ConsumerRecord<String, String> record) {
        String orderNo = record.value();
        if (StringUtils.hasText(orderNo)) {
            log.info("[账户服务]监听到了充值成功的消息，订单编号：{}", orderNo);
            rechargeInfoService.rechargeSuccess(orderNo);
        }
    }
}
