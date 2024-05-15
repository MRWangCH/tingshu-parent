package com.atguigu.tingshu.account;

import com.atguigu.tingshu.account.impl.AccountDegradeFeignClient;
import com.atguigu.tingshu.common.result.Result;
import com.atguigu.tingshu.common.util.AuthContextHolder;
import com.atguigu.tingshu.vo.account.AccountLockResultVo;
import com.atguigu.tingshu.vo.account.AccountLockVo;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * <p>
 * 账号模块远程调用API接口
 * </p>
 *
 * @author atguigu
 */
@FeignClient(value = "service-account", path = "api/account", fallback = AccountDegradeFeignClient.class)
public interface AccountFeignClient {

    /**
     * 检查及锁定账户金额:验证账号余额是否充足，将部分金额锁定
     * @param accountLockVo
     * @return
     */
    @PostMapping("/userAccount/checkAndLock")
    public Result<AccountLockResultVo> checkAndLock(@RequestBody AccountLockVo accountLockVo);
}
