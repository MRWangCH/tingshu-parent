package com.atguigu.tingshu.account.service;

import com.atguigu.tingshu.model.account.RechargeInfo;
import com.baomidou.mybatisplus.extension.service.IService;

public interface RechargeInfoService extends IService<RechargeInfo> {

    /**
     * 根据订单号获取充值记录的信息
     *
     * @param orderNo
     * @return
     */
    RechargeInfo getRechargeInfoByOrderNo(String orderNo);
}
