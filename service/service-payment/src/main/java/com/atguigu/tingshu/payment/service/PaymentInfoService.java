package com.atguigu.tingshu.payment.service;

import com.atguigu.tingshu.model.payment.PaymentInfo;
import com.baomidou.mybatisplus.extension.service.IService;

public interface PaymentInfoService extends IService<PaymentInfo> {

    /**
     * 保存本地交易记录
     * @param paymentType
     * @param orderNo
     * @param userId
     * @return
     */
    PaymentInfo savePaymentInfo(String paymentType, String orderNo, Long userId);

    /**
     * 更新本地交易记录的状态：改为支付成功
     * @param orderNo
     */
    void updatePaymentInfo(String orderNo);
}
