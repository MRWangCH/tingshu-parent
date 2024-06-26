package com.atguigu.tingshu.account.service.impl;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.IdUtil;
import com.atguigu.tingshu.account.mapper.RechargeInfoMapper;
import com.atguigu.tingshu.account.service.RechargeInfoService;
import com.atguigu.tingshu.account.service.UserAccountService;
import com.atguigu.tingshu.common.constant.SystemConstant;
import com.atguigu.tingshu.common.util.AuthContextHolder;
import com.atguigu.tingshu.model.account.RechargeInfo;
import com.atguigu.tingshu.vo.account.RechargeInfoVo;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;

@Service
@SuppressWarnings({"all"})
public class RechargeInfoServiceImpl extends ServiceImpl<RechargeInfoMapper, RechargeInfo> implements RechargeInfoService {

    @Autowired
    private RechargeInfoMapper rechargeInfoMapper;

    @Autowired
    private UserAccountService userAccountService;

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

    /**
     * 用户余额充值（保存充值记录）
     *
     * @param rechargeInfoVo
     * @return
     */
    @Override
    public Map<String, String> submitRecharge(RechargeInfoVo rechargeInfoVo) {
        //1 为本地充值记录生成订单编号
        RechargeInfo rechargeInfo = new RechargeInfo();
        rechargeInfo.setUserId(AuthContextHolder.getUserId());
        rechargeInfo.setRechargeStatus(SystemConstant.ORDER_STATUS_UNPAID);
        rechargeInfo.setRechargeAmount(rechargeInfoVo.getAmount());
        rechargeInfo.setPayWay(rechargeInfo.getPayWay());
        String orderNo = "CZ" + DateUtil.today().replaceAll("-", "") + IdUtil.getSnowflakeNextId();
        rechargeInfo.setOrderNo(orderNo);
        rechargeInfoMapper.insert(rechargeInfo);
        //2 延迟队列延迟关闭充值记录
        Map<String, String> map = new HashMap<>();
        map.put("orderNo", orderNo);
        return map;
    }

    /**
     * 处理充值成功消息
     *
     * @param orderNo
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void rechargeSuccess(String orderNo) {
        //1 根据充值订单编号查询充值记录状态
        RechargeInfo rechargeInfo = rechargeInfoMapper.selectOne(new LambdaQueryWrapper<RechargeInfo>().eq(RechargeInfo::getOrderNo, orderNo));
        if (rechargeInfo != null && SystemConstant.ORDER_STATUS_UNPAID.equals(rechargeInfo.getRechargeStatus())) {
			//只处理未支付情况
            rechargeInfo.setRechargeStatus(SystemConstant.ORDER_STATUS_PAID);
            rechargeInfoMapper.updateById(rechargeInfo);
            //修改账户总金额可用余额
            userAccountService.add(rechargeInfo.getUserId(), rechargeInfo.getRechargeAmount());
            //新增账户变更记录
            userAccountService.saveUserAccountDetail(rechargeInfo.getUserId(), "充值", SystemConstant.ACCOUNT_TRADE_TYPE_DEPOSIT, rechargeInfo.getRechargeAmount(), orderNo);
        }
    }
}
