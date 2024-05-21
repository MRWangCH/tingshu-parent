package com.atguigu.tingshu.dispatch.job;

import com.atguigu.tingshu.search.client.SearchFeignClient;
import com.xxl.job.core.biz.model.ReturnT;
import com.xxl.job.core.handler.annotation.XxlJob;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class DispatchHandler {


    @Autowired
    private SearchFeignClient searchFeignClient;

    /**
     * 定时执行热门专辑的更新
     */
    @XxlJob("updateHotAlbumJob")
    public void updateHotAlbumJob() {
        searchFeignClient.updateLatelyAlbumRanking();
        log.info("定时执行热门专辑的更新");
    }


    /**
     * 定时执行用户vip状态的更新
     * @return
     */
    @XxlJob("updateUserVIPStatusJob")
    public ReturnT updateUserVIPStatusJob() {
        try {
            log.info("定时执行用户vip状态的更新");
            return ReturnT.SUCCESS;
        } catch (Exception e) {
            return ReturnT.FAIL;
        }
    }
}