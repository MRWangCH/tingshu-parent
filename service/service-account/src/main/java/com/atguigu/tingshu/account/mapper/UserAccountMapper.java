package com.atguigu.tingshu.account.mapper;

import com.atguigu.tingshu.model.account.UserAccount;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.math.BigDecimal;

@Mapper
public interface UserAccountMapper extends BaseMapper<UserAccount> {

    /**
     * 查询可用余额是否充足
     * @param userId
     * @param amount
     * @return
     */
    UserAccount check(@Param("userId") Long userId, @Param("amount") BigDecimal amount);

    /**
     * 账户可用余额锁定
     * @param userId
     * @param amount
     * @return
     */
    int lock(@Param("userId") Long userId, @Param("amount") BigDecimal amount);

    /**
     * 账户扣减
     * @param userId
     * @param amount
     * @return
     */
    int minus(@Param("userId") Long userId, @Param("amount") BigDecimal amount);

    /**
     * 解锁账户
     * @param userId
     * @param amount
     * @return
     */
    int unlock(@Param("userId") Long userId, @Param("amount") BigDecimal amount);
    /**
     * 为指定账户充值金额
     * @param userId
     * @param rechargeAmount
     */
    void add(@Param("userId") Long userId,@Param("rechargeAmount") BigDecimal rechargeAmount);
}
