package com.atguigu.tingshu.user.client.impl;


import com.atguigu.tingshu.common.result.Result;
import com.atguigu.tingshu.user.client.UserFeignClient;
import com.atguigu.tingshu.vo.user.UserInfoVo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Slf4j
@Component
public class UserDegradeFeignClient implements UserFeignClient {

    @Override
    public Result<UserInfoVo> getUserInfoVoByUserId(Long userId) {
        log.error("远程调用[用户服务getUserInfoVoByUserId方法服务降级]");
        return null;
    }

    @Override
    public Result<Map<Long, Integer>> userIsPaidTrackList(Long userId, Long albumId, List<Long> trackIdList) {
        log.error("远程调用[用户服务userIsPaidTrackList方法服务降级]");
        return null;
    }
}
