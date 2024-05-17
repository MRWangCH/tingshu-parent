package com.atguigu.tingshu.payment.service.impl;

import com.atguigu.tingshu.common.constant.SystemConstant;
import com.atguigu.tingshu.common.util.AuthContextHolder;
import com.atguigu.tingshu.model.order.OrderInfo;
import com.atguigu.tingshu.model.payment.PaymentInfo;
import com.atguigu.tingshu.order.client.OrderFeignClient;
import com.atguigu.tingshu.payment.config.WxPayV3Config;
import com.atguigu.tingshu.payment.service.PaymentInfoService;
import com.atguigu.tingshu.payment.service.WxPayService;
import com.atguigu.tingshu.user.client.UserFeignClient;
import com.atguigu.tingshu.vo.user.UserInfoVo;
import com.wechat.pay.java.service.payments.jsapi.JsapiServiceExtension;
import com.wechat.pay.java.service.payments.jsapi.model.Amount;
import com.wechat.pay.java.service.payments.jsapi.model.Payer;
import com.wechat.pay.java.service.payments.jsapi.model.PrepayRequest;
import com.wechat.pay.java.service.payments.jsapi.model.PrepayWithRequestPaymentResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
@Slf4j
public class WxPayServiceImpl implements WxPayService {

    @Autowired
    private PaymentInfoService paymentInfoService;

    @Autowired
    private OrderFeignClient orderFeignClient;

    @Autowired
    private JsapiServiceExtension service;

    @Autowired
    private WxPayV3Config wxPayV3Config;

    @Autowired
    private UserFeignClient userFeignClient;


    /**
     * 调用微信支付的api响应给小程序能够拉起微信支付页面的参数
     *
     * @param paymentType
     * @param orderNo
     * @return
     */
    @Override
    public Map<String, String> createJsapiWxPayForm(String paymentType, String orderNo) {
        try {
            //1 根据订单编号再次查询订单信息 得到订单支付状态，只有未支付
            //2 新增本地交易记录
            Long userId = AuthContextHolder.getUserId();
            PaymentInfo paymentInfo = paymentInfoService.savePaymentInfo(paymentType, orderNo, userId);
            //3 调用微信api 返回拉起微信支付页面参数
            //3.1 调用预交易请求对象 单位：分
            PrepayRequest request = new PrepayRequest();
            Amount amount = new Amount();
            amount.setTotal(1); //预交易订单金额 1分
            request.setAmount(amount);
            request.setAppid(wxPayV3Config.getAppid());
            request.setMchid(wxPayV3Config.getMerchantId());
            request.setDescription(paymentInfo.getContent());
            request.setNotifyUrl(wxPayV3Config.getNotifyUrl());
            request.setOutTradeNo(orderNo);
            UserInfoVo userInfoVo = userFeignClient.getUserInfoVoByUserId(userId).getData();
            Payer payer = new Payer();
            payer.setOpenid(userInfoVo.getWxOpenId());
            request.setPayer(payer);
            PrepayWithRequestPaymentResponse paymentResponse = service.prepayWithRequestPayment(request);
            if (paymentResponse != null) {
                Map<String, String> map = new HashMap<>();
                map.put("timeStamp", paymentResponse.getTimeStamp());
                map.put("package", paymentResponse.getPackageVal());
                map.put("paySign", paymentResponse.getPaySign());
                map.put("signType", paymentResponse.getSignType());
                map.put("nonceStr", paymentResponse.getNonceStr());
                return map;
            }
        } catch (Exception e) {
            log.error("[微信支付]微信预支付创建失败：{}", e);
            throw new RuntimeException(e);
        }
        return null;
    }
}