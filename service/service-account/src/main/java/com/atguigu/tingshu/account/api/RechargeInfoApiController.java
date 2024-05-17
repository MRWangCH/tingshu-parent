package com.atguigu.tingshu.account.api;

import com.atguigu.tingshu.account.service.RechargeInfoService;
import com.atguigu.tingshu.common.result.Result;
import com.atguigu.tingshu.model.account.RechargeInfo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "充值管理")
@RestController
@RequestMapping("api/account")
@SuppressWarnings({"all"})
public class RechargeInfoApiController {

    @Autowired
    private RechargeInfoService rechargeInfoService;


    /**
     * 根据订单号获取充值记录的信息
     *
     * @param orderNo
     * @return
     */
    @Operation(summary = "根据订单号获取充值记录的信息")
    @GetMapping("/rechargeInfo/getRechargeInfo/{orderNo}")
    public Result<RechargeInfo> getRechargeInfoByOrderNo(@PathVariable String orderNo) {
        RechargeInfo rechargeInfo = rechargeInfoService.getRechargeInfoByOrderNo(orderNo);
        return Result.ok(rechargeInfo);
    }
}

