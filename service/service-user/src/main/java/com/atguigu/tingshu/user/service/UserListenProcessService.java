package com.atguigu.tingshu.user.service;

import com.atguigu.tingshu.vo.user.UserListenProcessVo;

import java.math.BigDecimal;

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
}
