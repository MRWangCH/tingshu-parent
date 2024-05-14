package com.atguigu.tingshu.account.api;

import com.atguigu.tingshu.account.service.UserAccountService;
import com.atguigu.tingshu.common.login.GuiGuLogin;
import com.atguigu.tingshu.common.result.Result;
import com.atguigu.tingshu.common.util.AuthContextHolder;
import com.atguigu.tingshu.vo.account.AccountLockResultVo;
import com.atguigu.tingshu.vo.account.AccountLockVo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

@Tag(name = "用户账户管理")
@RestController
@RequestMapping("api/account")
@SuppressWarnings({"all"})
public class UserAccountApiController {

    @Autowired
    private UserAccountService userAccountService;

    /**
     * 获取当前登录用户可用余额
     *
     * @return
     */
    @GuiGuLogin
    @Operation(summary = "获取当前登录用户可用余额")
    @GetMapping("/userAccount/getAvailableAmount")
    public Result<BigDecimal> getAvailableAmount() {
        Long userId = AuthContextHolder.getUserId();
        BigDecimal result = userAccountService.getAvailableAmount(userId);
        return Result.ok(result);
    }


    /**
     * 检查及锁定账户金额:验证账号余额是否充足，将部分金额锁定
     * @param accountLockVo
     * @return
     */
    @Operation(summary = "检查及锁定账户金额")
    @GuiGuLogin
    @PostMapping("/userAccount/checkAndLock")
    public Result<AccountLockResultVo> checkAndLock(@RequestBody AccountLockVo accountLockVo) {
        Long userId = AuthContextHolder.getUserId();
        AccountLockResultVo accountLockResultVo = userAccountService.checkAndLock(userId, accountLockVo);
        return Result.ok(accountLockResultVo);
    }
}

