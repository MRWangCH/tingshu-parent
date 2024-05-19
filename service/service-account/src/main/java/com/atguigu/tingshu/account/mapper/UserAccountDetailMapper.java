package com.atguigu.tingshu.account.mapper;

import com.atguigu.tingshu.model.account.UserAccountDetail;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface UserAccountDetailMapper extends BaseMapper<UserAccountDetail> {

    /**
     * 查询充值记录
     * @param pageInfo
     * @param userId
     */
    Page<UserAccountDetail> getUserRechargePage(Page<UserAccountDetail> pageInfo, Long userId);

    /**
     * 查询消费记录
     * @param pageInfo
     * @param userId
     */
    Page<UserAccountDetail>  getUserConsumePage(Page<UserAccountDetail> pageInfo, Long userId);
}
