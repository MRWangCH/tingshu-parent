package com.atguigu.tingshu.order.mapper;

import com.atguigu.tingshu.model.order.OrderInfo;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface OrderInfoMapper extends BaseMapper<OrderInfo> {

    /**
     * 订单分页列表
     * @param pageInfo
     * @param userId
     * @return
     */
    Page<OrderInfo> getUserOrderByPage(Page<OrderInfo> pageInfo,@Param("userId") Long userId);
}
