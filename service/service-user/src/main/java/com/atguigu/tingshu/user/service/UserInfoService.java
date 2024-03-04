package com.atguigu.tingshu.user.service;

import com.atguigu.tingshu.model.user.UserInfo;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.Map;

public interface UserInfoService extends IService<UserInfo> {

    /**
     * 微信登录
     * @param code
     * @return
     */
    Map<String, String> weiXinLogin(String code);
}
