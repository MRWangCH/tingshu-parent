package com.atguigu.tingshu.user.api;

import com.atguigu.tingshu.common.login.GuiGuLogin;
import com.atguigu.tingshu.common.result.Result;
import com.atguigu.tingshu.common.util.AuthContextHolder;
import com.atguigu.tingshu.user.service.UserInfoService;
import com.atguigu.tingshu.vo.user.UserInfoVo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Tag(name = "微信授权登录接口")
@RestController
@RequestMapping("/api/user/wxLogin")
@Slf4j
public class WxLoginApiController {

    @Autowired
    private UserInfoService userInfoService;

    /**
     * 小程序微信用户登录
     *
     * @param code 用于获取用户的唯一标识
     * @return
     */
    @GetMapping("/wxLogin/{code}")
    public Result<Map<String, String>> weiXinLogin(@PathVariable String code) {
        Map<String, String> resultMap = userInfoService.weiXinLogin(code);
        return Result.ok(resultMap);
    }

    /**
     * 获取当前登录用户
     *
     * @return
     */
    @GuiGuLogin(required = true)
    @GetMapping("/getUserInfo")
    public Result<UserInfoVo> getUserInfo() {
        Long userId = AuthContextHolder.getUserId();
        UserInfoVo userInfoVo = userInfoService.getUserInfoVoByUserId(userId);
        return Result.ok(userInfoVo);
    }

    /**
     * 修改用户信息
     * @param userInfoVo
     * @return
     */
    @Operation(summary = "修改用户信息")
    @GuiGuLogin(required = true)
    @PostMapping("/updateUser")
    public Result updateUser(@RequestBody @Validated UserInfoVo userInfoVo){
        Long userId = AuthContextHolder.getUserId();
        userInfoVo.setId(userId);
        userInfoService.updateUser(userInfoVo);
        return Result.ok();
    }


}
