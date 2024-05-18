package com.atguigu.tingshu.account.service;

import com.atguigu.tingshu.model.account.RechargeInfo;
import com.atguigu.tingshu.vo.account.RechargeInfoVo;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.Map;

public interface RechargeInfoService extends IService<RechargeInfo> {

    /**
     * 根据订单号获取充值记录的信息
     *
     * @param orderNo
     * @return
     */
    RechargeInfo getRechargeInfoByOrderNo(String orderNo);


    /**
     * 用户余额充值（保存充值记录）
     *
     * @param rechargeInfoVo
     * @return
     */
    Map<String, String> submitRecharge(RechargeInfoVo rechargeInfoVo);
}
