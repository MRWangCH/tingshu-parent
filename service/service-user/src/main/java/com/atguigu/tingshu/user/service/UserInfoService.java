package com.atguigu.tingshu.user.service;

import com.atguigu.tingshu.model.user.UserInfo;
import com.atguigu.tingshu.vo.user.UserInfoVo;
import com.atguigu.tingshu.vo.user.UserPaidRecordVo;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;
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

    /**
     * 获取专辑声音列表某页中，用户对于声音的付费情况
     * @param userId
     * @param albumId
     * @param trackIdList
     * @return
     */
    Map<Long, Integer> userIsPaidTrackList(Long userId, Long albumId, List<Long> trackIdList);

    /**
     * 是否购买过此专辑
     * @param albumId
     * @return
     */
    Boolean isPaidAlbum(Long userId, Long albumId);


    /**
     * 根据专辑id+用户ID获取用户已购买声音id列表
     * @param albumId
     * @return
     */
    List<Long> getUserPaidTrackList(Long albumId, Long userId);

    /**
     * 处理用户购买记录
     * @param userPaidRecordVo
     */
    void saveUserPayRecord(UserPaidRecordVo userPaidRecordVo);

    /**
     * 更新vip到期失效状态
     * @return
     */
    void updateVipExpireStatus();
}
