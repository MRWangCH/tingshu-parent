package com.atguigu.tingshu.account.api;

import com.atguigu.tingshu.account.service.RechargeInfoService;
import com.atguigu.tingshu.common.login.GuiGuLogin;
import com.atguigu.tingshu.common.result.Result;
import com.atguigu.tingshu.model.account.RechargeInfo;
import com.atguigu.tingshu.vo.account.RechargeInfoVo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

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

    /**
     * 用户余额充值（保存充值记录）
     * @param rechargeInfoVo
     * @return
     */
    @GuiGuLogin
    @Operation(summary = "用户余额充值")
    @PostMapping("/rechargeInfo/submitRecharge")
    public Result<Map<String, String>> submitRecharge(@RequestBody RechargeInfoVo rechargeInfoVo) {
        Map<String, String> map = rechargeInfoService.submitRecharge(rechargeInfoVo);
        return Result.ok(map);
    }

}

