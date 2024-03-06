package com.atguigu.tingshu.user.service;

import com.atguigu.tingshu.model.user.UserInfo;
import com.atguigu.tingshu.vo.user.UserInfoVo;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.Map;

public interface UserInfoService extends IService<UserInfo> {

    /**
     * 微信登录
     *
     * @param code
     * @return
     */
    Map<String, String> weiXinLogin(String code);

    /**
     * 获取当前登录用户
     * @param userId
     * @return
     */
    UserInfoVo getUserInfoVoByUserId(Long userId);

    /**
     * 修改用户信息
     * @param userInfoVo
     */
    void updateUser(UserInfoVo userInfoVo);
}
