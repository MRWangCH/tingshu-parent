package com.atguigu.tingshu.order.service;

import com.atguigu.tingshu.model.order.OrderInfo;
import com.atguigu.tingshu.vo.order.OrderInfoVo;
import com.atguigu.tingshu.vo.order.TradeVo;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.Map;

public interface OrderInfoService extends IService<OrderInfo> {


    /**
     * 订单结算页面渲染-数据汇总
     * @param tradeVo
     * @return
     */
    OrderInfoVo trade(Long userId, TradeVo tradeVo);

    /**
     * 订单提交（余额付款）
     * @param userId
     * @param orderInfoVo
     * @return
     */
    Map<String, String> submitOrder(Long userId, OrderInfoVo orderInfoVo);

    /**
     * 保存订单以及订单明细，订单优惠明细
     * @param userId
     * @param orderInfoVo
     * @return
     */
    OrderInfo saveOrder(Long userId, OrderInfoVo orderInfoVo);

    /**
     * 获取订单信息
     * @param userId
     * @param orderNo
     * @return
     */
    OrderInfo getOrderInfo(Long userId, String orderNo);

    /**
     * 根据支付方式编号得到支付类型
     * @param payWay
     * @return
     */
    String getPayWayName(String payWay);

    /**
     * 根据订单状态编号得到订单状态
     * @param orderStatus
     * @return
     */
    String getOrderStatusName(String orderStatus);
}
