package com.atguigu.tingshu.order.api;

import com.atguigu.tingshu.common.login.GuiGuLogin;
import com.atguigu.tingshu.common.result.Result;
import com.atguigu.tingshu.common.util.AuthContextHolder;
import com.atguigu.tingshu.model.order.OrderInfo;
import com.atguigu.tingshu.order.service.OrderInfoService;
import com.atguigu.tingshu.vo.order.OrderInfoVo;
import com.atguigu.tingshu.vo.order.TradeVo;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Tag(name = "订单管理")
@RestController
@RequestMapping("api/order")
@SuppressWarnings({"all"})
public class OrderInfoApiController {

    @Autowired
    private OrderInfoService orderInfoService;

    /**
     * 订单结算页面渲染-数据汇总
     *
     * @param tradeVo
     * @return
     */
    @Operation(summary = "订单结算页面渲染-数据汇总")
    @GuiGuLogin
    @PostMapping("/orderInfo/trade")
    public Result<OrderInfoVo> trade(@RequestBody TradeVo tradeVo) {
        Long userId = AuthContextHolder.getUserId();
        OrderInfoVo orderInfoVo = orderInfoService.trade(userId, tradeVo);
        return Result.ok(orderInfoVo);
    }

    /**
     * 订单提交（余额付款）
     *
     * @param orderInfoVo
     * @return
     */
    @GuiGuLogin
    @Operation(summary = "订单提交（余额付款）")
    @PostMapping("/orderInfo/submitOrder")
    public Result<Map<String, String>> submitOrder(@RequestBody @Validated OrderInfoVo orderInfoVo) {
        Long userId = AuthContextHolder.getUserId();
        Map<String, String> map = orderInfoService.submitOrder(userId, orderInfoVo);
        return Result.ok(map);
    }

    /**
     * 查看指定订单信息
     *
     * @param orderNo
     * @return
     */
    @Operation(summary = "查看指定订单信息")
    @GuiGuLogin
    @GetMapping("/orderInfo/getOrderInfo/{orderNo}")
    public Result<OrderInfo> getOrderInfo(@PathVariable("orderNo") String orderNo) {
        Long userId = AuthContextHolder.getUserId();
        OrderInfo orderInfo = orderInfoService.getOrderInfo(userId, orderNo);
        return Result.ok(orderInfo);
    }

    /**
     * 分页获取用户订单列表
     *
     * @param page
     * @param limit
     * @return
     */
    @GuiGuLogin
    @Operation(summary = "分页获取用户订单列表")
    @GetMapping("/orderInfo/findUserPage/{page}/{limit}")
    public Result<Page<OrderInfo>> getUserOrderByPage(@PathVariable Long page, @PathVariable Long limit) {
        Long userId = AuthContextHolder.getUserId();
        Page<OrderInfo> pageInfo = new Page<>(page, limit);
        pageInfo= orderInfoService.getUserOrderByPage(userId, pageInfo);
        return Result.ok(pageInfo);
    }
}

