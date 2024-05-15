package com.atguigu.tingshu.account.service;

import com.atguigu.tingshu.model.account.UserAccount;
import com.atguigu.tingshu.vo.account.AccountLockResultVo;
import com.atguigu.tingshu.vo.account.AccountLockVo;
import com.baomidou.mybatisplus.extension.service.IService;

import java.math.BigDecimal;

public interface UserAccountService extends IService<UserAccount> {

    /**
     * 初始化账户余额
     *
     * @param userId
     */
    void saveUserAccount(Long userId);

    /**
     * 获取当前登录用户可用余额
     * @return
     */
    BigDecimal getAvailableAmount(Long userId);

    /**
     * 查询账户信息
     * @param userId
     * @return
     */
    UserAccount getUserAccount(Long userId);

    /**
     * 检查及锁定账户金额
     * @param userId
     * @param accountLockVo
     * @return
     */
    AccountLockResultVo checkAndLock(Long userId, AccountLockVo accountLockVo);

    /**
     * 保存账户变动日志
     * @param userId
     * @param title
     * @param tradeType
     * @param amount
     * @param orderNo
     */
    void saveUserAccountDetail(Long userId, String title, String tradeType, BigDecimal amount, String orderNo);

    /**
     * 账户余额扣减
     * @param orderNo
     */
    void accountMinus(String orderNo);

    /**
     * 账户余额解锁
     * @param orderNo
     */
    void accountUnlock(String orderNo);
}
