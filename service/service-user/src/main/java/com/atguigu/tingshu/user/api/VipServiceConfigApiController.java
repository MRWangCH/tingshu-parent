package com.atguigu.tingshu.user.api;

import com.atguigu.tingshu.common.result.Result;
import com.atguigu.tingshu.model.user.VipServiceConfig;
import com.atguigu.tingshu.user.service.VipServiceConfigService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "VIP服务配置管理接口")
@RestController
@RequestMapping("api/user")
@SuppressWarnings({"all"})
public class VipServiceConfigApiController {

    @Autowired
    private VipServiceConfigService vipServiceConfigService;

    /**
     * 获取所有vip套餐类型
     * @return
     */
    @Operation(summary = "获取所有vip套餐类型")
    @GetMapping("/vipServiceConfig/findAll")
    public Result<List<VipServiceConfig>> getVipServiceConfig() {
        List<VipServiceConfig> vipServiceConfigs = vipServiceConfigService.list();
        return Result.ok(vipServiceConfigs);
    }

}

