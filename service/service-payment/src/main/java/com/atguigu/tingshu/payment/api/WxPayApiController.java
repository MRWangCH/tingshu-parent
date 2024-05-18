package com.atguigu.tingshu.payment.api;

import com.atguigu.tingshu.common.login.GuiGuLogin;
import com.atguigu.tingshu.common.result.Result;
import com.atguigu.tingshu.payment.service.WxPayService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Tag(name = "微信支付接口")
@RestController
@RequestMapping("api/payment")
@Slf4j
public class WxPayApiController {

    @Autowired
    private WxPayService wxPayService;


    /**
     * 微信支付
     *
     * @param paymentType 支付类型：1301-订单 1302-充值
     * @param orderNo     订单号
     * @return
     */
    @GuiGuLogin
    @Operation(summary = "微信下单")
    @PostMapping("/wxPay/createJsapi/{paymentType}/{orderNo}")
    public Result<Map<String, String>> createJsapi(@PathVariable String paymentType, @PathVariable String orderNo) {
        Map<String, String> wxPayResult = wxPayService.createJsapiWxPayForm(paymentType, orderNo);
        return Result.ok(wxPayResult);
    }

    /**
     * 根据商户订单编号查询，查询微信支付状态
     *
     * @param orderNo
     * @return
     */
    @Operation(summary = "根据商户订单编号查询，查询微信支付状态")
    @GetMapping("/wxPay/queryPayStatus/{orderNo}")
    public Result<Boolean> queryPayStatus(@PathVariable String orderNo) {
        Boolean flag = wxPayService.queryPayStatus(orderNo);
        return Result.ok(flag);
    }

    /**
     * 微信支付异步通知接口
     * @param request
     * @return
     */
    @Operation(summary = "微信支付异步通知接口")
    @PostMapping("/wxpay/notify")
    public Map<String, String> notify(HttpServletRequest request) {
        Map<String, String> map = wxPayService.notifyTractionStatus(request);
        return map;
    }

}
