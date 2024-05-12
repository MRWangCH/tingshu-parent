package com.atguigu.tingshu.account.service.impl;

import com.atguigu.tingshu.account.mapper.UserAccountMapper;
import com.atguigu.tingshu.account.service.UserAccountService;
import com.atguigu.tingshu.model.account.UserAccount;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Slf4j
@Service
@SuppressWarnings({"all"})
public class UserAccountServiceImpl extends ServiceImpl<UserAccountMapper, UserAccount> implements UserAccountService {

    @Autowired
    private UserAccountMapper userAccountMapper;

    /**
     * 初始化账户余额
     *
     * @param valueOf
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveUserAccount(Long userId) {
        UserAccount userAccount = new UserAccount();
        userAccount.setUserId(userId);
        userAccountMapper.insert(userAccount);
    }

    /**
     * 获取当前登录用户可用余额
     *
     * @return
     */
    @Override
    public BigDecimal getAvailableAmount(Long userId) {
        //1 获取账户信息
        UserAccount userAccount = this.getUserAccount(userId);
        //2 获取可用余额
        if (userAccount != null) {
            BigDecimal availableAmount = userAccount.getAvailableAmount();
            return availableAmount;
        }
        return null;
    }

    /**
     * 查询账户信息
     *
     * @param userId
     * @return
     */
    @Override
    public UserAccount getUserAccount(Long userId) {
        LambdaQueryWrapper<UserAccount> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(UserAccount::getUserId, userId);
        return userAccountMapper.selectOne(queryWrapper);
    }
}
