package com.atguigu.tingshu.payment.api;

import com.atguigu.tingshu.common.login.GuiGuLogin;
import com.atguigu.tingshu.common.result.Result;
import com.atguigu.tingshu.payment.service.WxPayService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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

}
