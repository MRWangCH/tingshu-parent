package com.atguigu.tingshu.payment.service.impl;

import com.atguigu.tingshu.common.constant.SystemConstant;
import com.atguigu.tingshu.common.util.AuthContextHolder;
import com.atguigu.tingshu.model.order.OrderInfo;
import com.atguigu.tingshu.model.payment.PaymentInfo;
import com.atguigu.tingshu.order.client.OrderFeignClient;
import com.atguigu.tingshu.payment.config.WxPayV3Config;
import com.atguigu.tingshu.payment.service.PaymentInfoService;
import com.atguigu.tingshu.payment.service.WxPayService;
import com.atguigu.tingshu.payment.util.PayUtil;
import com.atguigu.tingshu.user.client.UserFeignClient;
import com.atguigu.tingshu.vo.user.UserInfoVo;
import com.wechat.pay.java.core.RSAAutoCertificateConfig;
import com.wechat.pay.java.core.notification.NotificationConfig;
import com.wechat.pay.java.core.notification.NotificationParser;
import com.wechat.pay.java.core.notification.RequestParam;
import com.wechat.pay.java.service.payments.jsapi.JsapiServiceExtension;
import com.wechat.pay.java.service.payments.jsapi.model.*;
import com.wechat.pay.java.service.payments.model.Transaction;
import jakarta.servlet.http.HttpServletRequest;
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

    @Autowired
    private RSAAutoCertificateConfig config;


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

    /**
     * 根据商户订单编号查询，查询微信支付状态
     *
     * @param orderNo
     * @return
     */
    @Override
    public Boolean queryPayStatus(String orderNo) {
        try {
            //1 构建微信交易结果查询对象-按照订单编号查询
            QueryOrderByOutTradeNoRequest request = new QueryOrderByOutTradeNoRequest();
            request.setMchid(wxPayV3Config.getMerchantId());
            request.setOutTradeNo(orderNo);
            //2 发送请求查询微信交易结果
            Transaction transaction = service.queryOrderByOutTradeNo(request);
            //3 解析结果得到支付状态
            if (transaction != null) {
                Transaction.TradeStateEnum tradeState = transaction.getTradeState();
                if (Transaction.TradeStateEnum.SUCCESS == tradeState) {
                    //用户支付成功
                    return true;
                }
            }
            return false;
        } catch (Exception e) {
            log.error("[支付]查询订单交易异常：{}", e);
            throw new RuntimeException(e);
        }
    }

    /**
     * 处理微信支付异步回调
     *
     * @param request
     * @return
     */
    @Override
    public Map<String, String> notifyTractionStatus(HttpServletRequest request) {
        try {
            //1 验证签名，验证数据是否被篡改 避免假通知
            String wechatPaySerial = request.getHeader("Wechatpay-Serial");  //签名
            String nonce = request.getHeader("Wechatpay-Nonce");  //签名中的随机数
            String timestamp = request.getHeader("Wechatpay-Timestamp"); //时间戳
            String signature = request.getHeader("Wechatpay-Signature"); //签名类型
            log.info("wechatPaySerial：{}", wechatPaySerial);
            log.info("nonce：{}", nonce);
            log.info("timestamp：{}", timestamp);
            log.info("signature：{}", signature);
            //1.1 构建RequestParam
            String requestBody = PayUtil.readData(request);
            RequestParam requestParam = new RequestParam.Builder()
                    .serialNumber(wechatPaySerial)
                    .nonce(nonce)
                    .signature(signature)
                    .timestamp(timestamp)
                    .body(requestBody)
                    .build();
            //1.2 NotificationParser 用于真正验签对象
            NotificationParser parser = new NotificationParser(config);
            //2 验签通过，获取支付结果
            Transaction transaction = parser.parse(requestParam, Transaction.class);
            if (transaction != null && transaction.getTradeState() == Transaction.TradeStateEnum.SUCCESS) {
                //3 根据支付结果处理后续：本地交易记录，订单，购买记录
                paymentInfoService.updatePaymentInfo(transaction.getOutTradeNo());
                Map<String, String> map = new HashMap<>();
                map.put("code", "SUCCESS");
                map.put("message", "SUCCESS");
                return map;
            }
        } catch (Exception e) {
            log.error("[支付]获取支付结果异常：{}", e);
            throw new RuntimeException(e);
        }
        Map<String, String> hashMap = new HashMap<>();
        hashMap.put("code", "FALL");
        hashMap.put("message", "FALL");
        return hashMap;
    }
}