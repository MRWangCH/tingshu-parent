package com.atguigu.tingshu.account.service;

import com.atguigu.tingshu.model.account.UserAccount;
import com.baomidou.mybatisplus.extension.service.IService;

public interface UserAccountService extends IService<UserAccount> {

    /**
     * 初始化账户余额
     *
     * @param userId
     */
    void saveUserAccount(Long userId);
}
