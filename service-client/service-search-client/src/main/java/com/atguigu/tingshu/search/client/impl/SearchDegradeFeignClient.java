package com.atguigu.tingshu.search.client.impl;

import com.atguigu.tingshu.common.result.Result;
import com.atguigu.tingshu.search.client.SearchFeignClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class SearchDegradeFeignClient implements SearchFeignClient {
    @Override
    public Result updateLatelyAlbumRanking() {
        log.error("[搜索服务]远程调用updateLatelyAlbumRanking异常");
        return null;
    }
}
