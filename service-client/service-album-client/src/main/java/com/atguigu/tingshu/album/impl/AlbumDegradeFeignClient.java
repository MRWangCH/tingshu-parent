package com.atguigu.tingshu.album.impl;


import com.atguigu.tingshu.album.AlbumFeignClient;
import com.atguigu.tingshu.common.result.Result;
import com.atguigu.tingshu.model.album.*;
import com.atguigu.tingshu.vo.album.AlbumStatVo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
public class AlbumDegradeFeignClient implements AlbumFeignClient {


    @Override
    public Result<AlbumInfo> getAlbumInfo(Long id) {
        log.error("[专辑模块Feign调用]getAlbumInfo异常");
        return null;
    }

    @Override
    public Result<BaseCategoryView> getCategoryViewBy3Id(Long category3Id) {
        log.error("[专辑模块Feign调用]getCategoryViewBy3Id异常");
        return null;
    }

    @Override
    public Result<List<BaseCategory3>> getTop7BaseCategory3(Long category1Id) {
        log.error("[专辑模块Feign调用]getTop7BaseCategory3异常");
        return null;
    }

    @Override
    public Result<AlbumStatVo> getAlbumStatVo(Long albumId) {
        log.error("[专辑模块Feign调用]getAlbumStatVo异常");
        return null;
    }

    @Override
    public Result<List<BaseCategory1>> findAllCategory1() {
        log.error("[专辑模块Feign调用]findAllCategory1异常");
        return null;
    }

    @Override
    public Result<List<TrackInfo>> getWaitPayTrackInfoList(Long trackId, Integer trackCount) {
        log.error("[专辑模块Feign调用]getWaitPayTrackInfoList异常");
        return null;
    }

    @Override
    public Result<TrackInfo> getTrackInfo(Long id) {
        log.error("[专辑模块Feign调用]getTrackInfo异常");
        return null;
    }
}
