package com.atguigu.tingshu.user.service;

import com.atguigu.tingshu.common.result.Result;
import com.atguigu.tingshu.vo.user.UserListenProcessVo;

import java.math.BigDecimal;
import java.util.Map;

public interface UserListenProcessService {

    /**
     * 查询当前用户入参声音上次播放的进度
     * @param trackId
     * @return
     */
    BigDecimal getTrackBreakSecond(Long userId, Long trackId);

    /**
     * 更新声音播放进度
     * @param userListenProcessVo
     * @return
     */
    void updateListenProcess(UserListenProcessVo userListenProcessVo);

    /**
     * 获取登录用户上次播放专辑声音进度
     * @return
     */
    Map<String, Long> getLatelyTrack(Long userId);
}
