package com.atguigu.tingshu.search.client;

import com.atguigu.tingshu.common.result.Result;
import com.atguigu.tingshu.search.client.impl.SearchDegradeFeignClient;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;

@FeignClient(value = "service-search", path = "api/search", fallback = SearchDegradeFeignClient.class)
public interface SearchFeignClient {

    /**
     * 更新所有分类下排行榜-定时任务，分布式任务调度框架
     * @return
     */
    @GetMapping("/albumInfo/updateLatelyAlbumRanking")
    public Result updateLatelyAlbumRanking();
}
