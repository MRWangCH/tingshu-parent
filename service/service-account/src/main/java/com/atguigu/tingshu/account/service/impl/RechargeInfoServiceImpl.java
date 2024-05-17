package com.atguigu.tingshu.account.service.impl;

import com.atguigu.tingshu.account.mapper.RechargeInfoMapper;
import com.atguigu.tingshu.account.service.RechargeInfoService;
import com.atguigu.tingshu.model.account.RechargeInfo;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
@SuppressWarnings({"all"})
public class RechargeInfoServiceImpl extends ServiceImpl<RechargeInfoMapper, RechargeInfo> implements RechargeInfoService {

	@Autowired
	private RechargeInfoMapper rechargeInfoMapper;

	/**
	 * 根据订单号获取充值记录的信息
	 *
	 * @param orderNo
	 * @return
	 */
	@Override
	public RechargeInfo getRechargeInfoByOrderNo(String orderNo) {
		LambdaQueryWrapper<RechargeInfo> queryWrapper = new LambdaQueryWrapper<>();
		queryWrapper.eq(RechargeInfo::getOrderNo, orderNo);
		RechargeInfo rechargeInfo = rechargeInfoMapper.selectOne(queryWrapper);
		return rechargeInfo;
	}
}
