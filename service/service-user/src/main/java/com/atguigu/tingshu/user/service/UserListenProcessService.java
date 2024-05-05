package com.atguigu.tingshu.user.service;

import java.math.BigDecimal;

public interface UserListenProcessService {

    /**
     * 查询当前用户入参声音上次播放的进度
     * @param trackId
     * @return
     */
    BigDecimal getTrackBreakSecond(Long userId, Long trackId);
}
