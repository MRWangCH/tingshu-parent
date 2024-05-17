package com.atguigu.tingshu.account.impl;


import com.atguigu.tingshu.account.AccountFeignClient;
import com.atguigu.tingshu.common.result.Result;
import com.atguigu.tingshu.model.account.RechargeInfo;
import com.atguigu.tingshu.vo.account.AccountLockResultVo;
import com.atguigu.tingshu.vo.account.AccountLockVo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class AccountDegradeFeignClient implements AccountFeignClient {

    @Override
    public Result<AccountLockResultVo> checkAndLock(AccountLockVo accountLockVo) {
        log.error("[账户服务]调用checkAndLock执行服务降级");
        return null;
    }

    @Override
    public Result<RechargeInfo> getRechargeInfoByOrderNo(String orderNo) {
        log.error("[账户服务]调用getRechargeInfoByOrderNo执行服务降级");
        return null;
    }
}
