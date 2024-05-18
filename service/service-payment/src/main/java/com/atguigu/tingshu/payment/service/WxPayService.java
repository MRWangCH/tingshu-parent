package com.atguigu.tingshu.payment.service;

import jakarta.servlet.http.HttpServletRequest;

import java.util.Map;

public interface WxPayService {

    /**
     * 调用微信支付的api响应给小程序能够拉起微信支付页面的参数
     * @param paymentType
     * @param orderNo
     * @return
     */
    Map<String, String> createJsapiWxPayForm(String paymentType, String orderNo);

    /**
     * 根据商户订单编号查询，查询微信支付状态
     *
     * @param orderNo
     * @return
     */
    Boolean queryPayStatus(String orderNo);

    /**
     * 处理微信支付异步回调
     * @param request
     * @return
     */
    Map<String, String> notifyTractionStatus(HttpServletRequest request);
}
