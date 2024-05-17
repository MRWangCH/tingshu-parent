package com.atguigu.tingshu.payment.service.impl;

import com.atguigu.tingshu.account.AccountFeignClient;
import com.atguigu.tingshu.common.constant.SystemConstant;
import com.atguigu.tingshu.model.payment.PaymentInfo;
import com.atguigu.tingshu.order.client.OrderFeignClient;
import com.atguigu.tingshu.payment.mapper.PaymentInfoMapper;
import com.atguigu.tingshu.payment.service.PaymentInfoService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
@SuppressWarnings({"all"})
public class PaymentInfoServiceImpl extends ServiceImpl<PaymentInfoMapper, PaymentInfo> implements PaymentInfoService {

    @Autowired
    private OrderFeignClient orderFeignClient;

    @Autowired
    private AccountFeignClient accountFeignClient;


    /**
     * 保存本地交易记录
     *
     * @param paymentType
     * @param orderNo
     * @param userId
     * @return
     */
    @Override
    public PaymentInfo savePaymentInfo(String paymentType, String orderNo, Long userId) {
        //1 根据订单号查询本地交易记录，如果存在直接返回
        LambdaQueryWrapper<PaymentInfo> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(PaymentInfo::getOrderNo, orderNo);
        PaymentInfo paymentInfo = this.getOne(queryWrapper);
        if (paymentInfo != null) {
            return paymentInfo;
        }
        //2 构建本集交易记录保存-设置每个属性赋值
        paymentInfo = new PaymentInfo();
        paymentInfo.setUserId(userId);
        paymentInfo.setPaymentType(paymentType);
        paymentInfo.setOrderNo(orderNo);
        paymentInfo.setPayWay(SystemConstant.ORDER_PAY_WAY_WEIXIN);
        paymentInfo.setPaymentStatus(SystemConstant.PAYMENT_STATUS_UNPAID);
        if (SystemConstant.PAYMENT_TYPE_ORDER.equals(paymentType)) {
            //2.1 判断支付类型-处理订单-查询订单信息得到金额
            //远程调用订单服务
            paymentInfo.setAmount();
            paymentInfo.setContent();
        } else if (SystemConstant.PAYMENT_TYPE_RECHARGE.equals(paymentType)) {
            //2.2 判断支付类型-处理充值-查询充值信息得到金额
            //远程调用账户服务
            paymentInfo.setAmount();
            paymentInfo.setContent();
        }

        //支付平台的交易编号-得到支付平台回调结果
        //paymentInfo.setOutTradeNo();
        //paymentInfo.setCallbackTime();
        //paymentInfo.setCallbackContent();
        this.save(paymentInfo);
        return paymentInfo;
    }
}
