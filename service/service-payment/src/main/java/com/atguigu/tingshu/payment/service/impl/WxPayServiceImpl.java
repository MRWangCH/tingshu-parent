package com.atguigu.tingshu.payment.service.impl;

import com.atguigu.tingshu.common.util.AuthContextHolder;
import com.atguigu.tingshu.model.payment.PaymentInfo;
import com.atguigu.tingshu.payment.service.PaymentInfoService;
import com.atguigu.tingshu.payment.service.WxPayService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
@Slf4j
public class WxPayServiceImpl implements WxPayService {

    @Autowired
    private PaymentInfoService paymentInfoService;


    /**
     * 调用微信支付的api响应给小程序能够拉起微信支付页面的参数
     *
     * @param paymentType
     * @param orderNo
     * @return
     */
    @Override
    public Map<String, String> createJsapiWxPayForm(String paymentType, String orderNo) {
        //1 新增本地交易记录
        Long userId = AuthContextHolder.getUserId();
         PaymentInfo paymentInfo = paymentInfoService.savePaymentInfo(paymentType, orderNo, userId);
        //2 调用微信api 返回拉起微信支付页面参数
        return null;
    }
}
