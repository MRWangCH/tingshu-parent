package com.atguigu.tingshu.account.service;

import com.atguigu.tingshu.model.account.UserAccount;
import com.atguigu.tingshu.model.account.UserAccountDetail;
import com.atguigu.tingshu.vo.account.AccountLockResultVo;
import com.atguigu.tingshu.vo.account.AccountLockVo;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import org.apache.ibatis.annotations.Param;

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

    /**
     * 为指定账户充值金额
     * @param userId
     * @param rechargeAmount
     */
    void add(Long userId, BigDecimal rechargeAmount);

    /**
     * 分页获取充值记录
     *
     * @param pageInfo
     * @param userId
     * @return
     */
    void getUserRechargePage(Page<UserAccountDetail> pageInfo, Long userId);

    /**
     * 分页获取消费记录
     *
     * @param pageInfo
     * @param userId
     * @return
     */
    void getUserConsumePage(Page<UserAccountDetail> pageInfo, Long userId);
}
