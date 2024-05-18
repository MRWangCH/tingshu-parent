package com.atguigu.tingshu.payment.service.impl;

import com.atguigu.tingshu.account.AccountFeignClient;
import com.atguigu.tingshu.common.constant.KafkaConstant;
import com.atguigu.tingshu.common.constant.SystemConstant;
import com.atguigu.tingshu.common.execption.GuiguException;
import com.atguigu.tingshu.common.result.ResultCodeEnum;
import com.atguigu.tingshu.common.service.KafkaService;
import com.atguigu.tingshu.model.account.RechargeInfo;
import com.atguigu.tingshu.model.order.OrderInfo;
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

    @Autowired
    private KafkaService kafkaService;


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
        paymentInfo = new PaymentInfo();
        if (SystemConstant.PAYMENT_TYPE_ORDER.equals(paymentType)) {
            //2.1 判断支付类型-处理订单-查询订单信息得到金额
            //远程调用订单服务
            OrderInfo orderInfo = orderFeignClient.getOrderInfo(orderNo).getData();
            if (!SystemConstant.ORDER_STATUS_UNPAID.equals(orderInfo.getOrderStatus())) {
                throw new GuiguException(ResultCodeEnum.ARGUMENT_VALID_ERROR);
            }
            paymentInfo.setAmount(orderInfo.getOrderAmount());
            paymentInfo.setContent(orderInfo.getOrderTitle());
        } else if (SystemConstant.PAYMENT_TYPE_RECHARGE.equals(paymentType)) {
            //2.2 判断支付类型-处理充值-查询充值信息得到金额
            //远程调用账户服务
            RechargeInfo rechargeInfo = accountFeignClient.getRechargeInfoByOrderNo(orderNo).getData();
            if (!SystemConstant.ORDER_STATUS_UNPAID.equals(rechargeInfo.getRechargeStatus())) {
                throw new GuiguException(ResultCodeEnum.ARGUMENT_VALID_ERROR);
            }
            paymentInfo.setAmount(rechargeInfo.getRechargeAmount());
            paymentInfo.setContent("充值");
        }
        //2 构建本集交易记录保存-设置每个属性赋值
        paymentInfo.setUserId(userId);
        paymentInfo.setPaymentType(paymentType);
        paymentInfo.setOrderNo(orderNo);
        paymentInfo.setPayWay(SystemConstant.ORDER_PAY_WAY_WEIXIN);
        paymentInfo.setPaymentStatus(SystemConstant.PAYMENT_STATUS_UNPAID);

        //支付平台的交易编号-得到支付平台回调结果
        //paymentInfo.setOutTradeNo();
        //paymentInfo.setCallbackTime();
        //paymentInfo.setCallbackContent();
        this.save(paymentInfo);
        return paymentInfo;
    }

    /**
     * 更新本地交易记录的状态：改为支付成功，基于MQ更新订单状态，充值购买记录
     *
     * @param orderNo
     */
    @Override
    public void updatePaymentInfo(String orderNo) {
        //1 根据订单编号查询本地记录，如果本地交易记录已支付说明更新过不处理
        LambdaQueryWrapper<PaymentInfo> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(PaymentInfo::getOrderNo, orderNo);
        PaymentInfo paymentInfo = this.getOne(queryWrapper);
        if (paymentInfo != null && SystemConstant.PAYMENT_STATUS_PAID.equals(paymentInfo.getPaymentStatus())) {
            return;
        }
        //更新本地交易记录的状态
        paymentInfo.setPaymentStatus(SystemConstant.PAYMENT_STATUS_PAID);
        this.updateById(paymentInfo);

        //2 利用MQ更新订单支付状态，充值记录状态
        //2.1 本地交易记录获取支付类型：1301-订单，1302-充值 动态得到目标话题名称
        String topic = paymentInfo.getPaymentStatus().equals(SystemConstant.PAYMENT_TYPE_ORDER) ? KafkaConstant.QUEUE_ORDER_PAY_SUCCESS : KafkaConstant.QUEUE_RECHARGE_PAY_SUCCESS;
        //2.2 发送消息到指定话题
        kafkaService.sendMessage(topic, orderNo);
    }
}
