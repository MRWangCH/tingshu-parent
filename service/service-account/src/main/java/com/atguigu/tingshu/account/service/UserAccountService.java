package com.atguigu.tingshu.account.service;

import com.atguigu.tingshu.model.account.UserAccount;
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
}
