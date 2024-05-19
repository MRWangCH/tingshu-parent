package com.atguigu.tingshu.account.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.atguigu.tingshu.account.mapper.UserAccountDetailMapper;
import com.atguigu.tingshu.account.mapper.UserAccountMapper;
import com.atguigu.tingshu.account.service.UserAccountService;
import com.atguigu.tingshu.common.constant.RedisConstant;
import com.atguigu.tingshu.common.constant.SystemConstant;
import com.atguigu.tingshu.common.execption.GuiguException;
import com.atguigu.tingshu.common.result.ResultCodeEnum;
import com.atguigu.tingshu.model.account.UserAccount;
import com.atguigu.tingshu.model.account.UserAccountDetail;
import com.atguigu.tingshu.vo.account.AccountLockResultVo;
import com.atguigu.tingshu.vo.account.AccountLockVo;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@SuppressWarnings({"all"})
public class UserAccountServiceImpl extends ServiceImpl<UserAccountMapper, UserAccount> implements UserAccountService {

    @Autowired
    private UserAccountMapper userAccountMapper;

    @Autowired
    private RedisTemplate redisTemplate;

    @Autowired
    private UserAccountDetailMapper userAccountDetailMapper;

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

    /**
     * 检查及锁定账户金额
     *
     * @param userId
     * @param accountLockVo
     * @return
     */
    @Transactional(rollbackFor = Exception.class)
    @Override
    public AccountLockResultVo checkAndLock(Long userId, AccountLockVo accountLockVo) {
        //1 幂等性处理：setnx避免同一订单多次对账户进行锁定，非首次处理查询redis得到锁定结果，将锁定结果返回
        String key = RedisConstant.ACCOUNT_MUTIPLE_CHECK + accountLockVo.getOrderNo();
        String lockResultKey = RedisConstant.ACCOUNT_CHECK_DATA + accountLockVo.getOrderNo();
        Boolean flag = redisTemplate.opsForValue().setIfAbsent(key, accountLockVo.getOrderNo(), 1, TimeUnit.HOURS);
        if (!flag) {
            //非首次调用该方法，尝试从redis中获取账户锁定结果，如果有结果则直接返回
            AccountLockResultVo accountLockResultVo = (AccountLockResultVo) redisTemplate.opsForValue().get(lockResultKey);
            if (accountLockResultVo == null) {
                //删除重复锁定key，抛出异常
                redisTemplate.delete(key);
                throw new GuiguException(ResultCodeEnum.ACCOUNT_LOCK_RESULT_NULL);
            }
            return accountLockResultVo;
        }
        //2 根据用户id+锁定金额对账户表余额查询是否充足，利用数据库锁机制避免并发时对账户多次锁定
        UserAccount userAccount = userAccountMapper.check(userId, accountLockVo.getAmount());
        if (userAccount == null) {
            throw new GuiguException(ResultCodeEnum.ACCOUNT_LESS);
        }
        //3 如果查询余额充足，执行锁定操作
        int count = userAccountMapper.lock(userId, accountLockVo.getAmount());
        if (count == 0) {
            //锁定失败
            redisTemplate.delete(key);
            throw new GuiguException(ResultCodeEnum.ACCOUNT_LOCK_RESULT_NULL);
        }
        //4 检查锁定都成功，将锁定结果存入redis，后续账户扣减，账户恢复都可从redis获取
        AccountLockResultVo accountLockResultVo = BeanUtil.copyProperties(accountLockVo, AccountLockResultVo.class);
        redisTemplate.opsForValue().set(lockResultKey, accountLockResultVo, 1, TimeUnit.HOURS);

        //5 新增账户变动日志
        this.saveUserAccountDetail(accountLockVo.getUserId(), "锁定" + accountLockVo.getContent(), SystemConstant.ACCOUNT_TRADE_TYPE_LOCK, accountLockVo.getAmount(), accountLockVo.getOrderNo());
        //5 将锁定结果返回
        return accountLockResultVo;
    }

    /**
     * 保存账户变动日志
     *
     * @param userId
     * @param title
     * @param tradeType
     * @param amount
     * @param orderNo
     */
    @Override
    public void saveUserAccountDetail(Long userId, String title, String tradeType, BigDecimal amount, String orderNo) {
        UserAccountDetail userAccountDetail = new UserAccountDetail();
        userAccountDetail.setUserId(userId);
        userAccountDetail.setTitle(title);
        userAccountDetail.setTradeType(tradeType);
        userAccountDetail.setAmount(amount);
        userAccountDetail.setOrderNo(orderNo);
        userAccountDetailMapper.insert(userAccountDetail);
    }


    /**
     * 账户余额扣减
     *
     * @param orderNo
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void accountMinus(String orderNo) {
        //1 幂等处理，避免同一订单多次扣减余额setnx，同一订单一小时内只能存入redis一次
        String key = "account:minus:" + orderNo;
        Boolean flag = redisTemplate.opsForValue().setIfAbsent(key, orderNo, 1, TimeUnit.HOURS);
        if (flag) {
            //第一次处理
            //2 完成账户扣减
            //2.1 根据订单编号构建锁定结果的key
            String lockResultKey = RedisConstant.ACCOUNT_CHECK_DATA + orderNo;
            //2.2 查询redis中锁定结果 得到订单对应：用户id+金额
            AccountLockResultVo accountLockResultVo = (AccountLockResultVo) redisTemplate.opsForValue().get(lockResultKey);
            if (accountLockResultVo == null) {
                //锁定结果失效，删除幂等的key
                redisTemplate.delete(key);
                throw new GuiguException(ResultCodeEnum.ACCOUNT_LOCK_RESULT_NULL);
            }
            int count = userAccountMapper.minus(accountLockResultVo.getUserId(), accountLockResultVo.getAmount());
            if (count == 0) {
                //余额扣减失败
                redisTemplate.delete(key);
                throw new GuiguException(ResultCodeEnum.ACCOUNT_LOCK_RESULT_NULL);
            }
            //3增加账户变动日志
            this.saveUserAccountDetail(accountLockResultVo.getUserId(), "扣减：" + accountLockResultVo.getContent(), SystemConstant.ACCOUNT_TRADE_TYPE_MINUS, accountLockResultVo.getAmount(), orderNo);
        }
    }

    /**
     * 账户余额解锁
     *
     * @param orderNo
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void accountUnlock(String orderNo) {
        //1 幂等性处理，避免多次解锁
        String key = "account:unlock:" + orderNo;
        Boolean flag = redisTemplate.opsForValue().setIfAbsent(key, orderNo, 1, TimeUnit.HOURS);
        if (flag) {
            //2 账户锁定金额恢复
            //2.1 获取redis中记录锁定结果
            String lockKeyResult = RedisConstant.ACCOUNT_CHECK_DATA + orderNo;
            AccountLockResultVo accountLockResultVo = (AccountLockResultVo) redisTemplate.opsForValue().get(lockKeyResult);
            if (accountLockResultVo == null) {
                //删除重复锁定key抛出异常
                redisTemplate.delete(key);
                throw new GuiguException(ResultCodeEnum.ACCOUNT_LOCK_RESULT_NULL);
            }
            //2.2 完成账户解锁
            int count = userAccountMapper.unlock(accountLockResultVo.getUserId(), accountLockResultVo.getAmount());
            if (count == 0) {
                //删除重复锁定key抛出异常
                redisTemplate.delete(key);
                throw new GuiguException(ResultCodeEnum.ACCOUNT_UNLOCK_ERROR);
            }
            //3 记录账户变更日志
            this.saveUserAccountDetail(accountLockResultVo.getUserId(), "恢复：" + accountLockResultVo.getContent(), SystemConstant.ACCOUNT_TRADE_TYPE_UNLOCK, accountLockResultVo.getAmount(), orderNo);
            //4 删除redis中锁定结果
            redisTemplate.delete(lockKeyResult);
        }
    }

    /**
     * 为指定账户充值金额
     *
     * @param userId
     * @param rechargeAmount
     */
    @Override
    public void add(Long userId, BigDecimal rechargeAmount) {
        userAccountMapper.add(userId, rechargeAmount);
    }

    /**
     * 分页获充值记录
     *
     * @param pageInfo
     * @param userId
     * @return
     */
    @Override
    public void getUserRechargePage(Page<UserAccountDetail> pageInfo, Long userId) {
        userAccountDetailMapper.getUserRechargePage(pageInfo, userId);
    }

    /**
     * 分页获消费记录
     *
     * @param pageInfo
     * @param userId
     * @return
     */
    @Override
    public void getUserConsumePage(Page<UserAccountDetail> pageInfo, Long userId) {
        userAccountDetailMapper.getUserConsumePage(pageInfo, userId);
    }
}
