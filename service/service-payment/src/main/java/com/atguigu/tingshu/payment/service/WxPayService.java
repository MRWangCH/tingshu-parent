package com.atguigu.tingshu.payment.service;

import java.util.Map;

public interface WxPayService {

    /**
     * 调用微信支付的api响应给小程序能够拉起微信支付页面的参数
     * @param paymentType
     * @param orderNo
     * @return
     */
    Map<String, String> createJsapiWxPayForm(String paymentType, String orderNo);
}
