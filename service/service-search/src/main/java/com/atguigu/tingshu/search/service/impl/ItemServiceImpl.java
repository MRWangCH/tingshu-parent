package com.atguigu.tingshu.search.service.impl;

import cn.hutool.core.lang.Assert;
import com.atguigu.tingshu.album.AlbumFeignClient;
import com.atguigu.tingshu.common.constant.RedisConstant;
import com.atguigu.tingshu.common.execption.GuiguException;
import com.atguigu.tingshu.common.result.ResultCodeEnum;
import com.atguigu.tingshu.model.album.AlbumInfo;
import com.atguigu.tingshu.model.album.BaseCategoryView;
import com.atguigu.tingshu.search.service.ItemService;
import com.atguigu.tingshu.user.client.UserFeignClient;
import com.atguigu.tingshu.vo.album.AlbumStatVo;
import com.atguigu.tingshu.vo.user.UserInfoVo;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RBloomFilter;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadPoolExecutor;

@Slf4j
@Service
@SuppressWarnings({"all"})
public class ItemServiceImpl implements ItemService {

    @Autowired
    private AlbumFeignClient albumFeignClient;

    @Autowired
    private UserFeignClient userFeignClient;

    @Autowired
    private ThreadPoolExecutor threadPoolExecutor;

    @Autowired
    private RedissonClient redissonClient;

    /**
     * 根据专辑id查询专辑详情
     * 汇总渲染详情页所需的4种数据
     * 当前专辑信息
     * 统计信息
     * 分类信息
     * 主播信息
     *
     * @param albumId
     * @return
     */
    @Override
    public Map<String, Object> getItem(Long albumId) {
        //0 先查询布隆过滤器中是否包含专辑id
        RBloomFilter<Long> bloomFilter = redissonClient.getBloomFilter(RedisConstant.ALBUM_BLOOM_FILTER);
        if (!bloomFilter.contains(albumId)) {
            throw new GuiguException(ResultCodeEnum.ARGUMENT_VALID_ERROR);
        }

        Map<String, Object> mapResult = new ConcurrentHashMap<>();
        //1 调用专辑服务获取专辑基本信息
        CompletableFuture<AlbumInfo> albumInfoCompletableFuture = CompletableFuture.supplyAsync(() -> {
            AlbumInfo albumInfo = albumFeignClient.getAlbumInfo(albumId).getData();
            Assert.notNull(albumInfo, "未获取到专辑信息！");
            mapResult.put("albumInfo", albumInfo);
            return albumInfo;
        }, threadPoolExecutor);

        //2 获取专辑统计信息
        CompletableFuture<Void> albumStatVoCompletableFuture = CompletableFuture.runAsync(() -> {
            AlbumStatVo albumStatVo = albumFeignClient.getAlbumStatVo(albumId).getData();
            Assert.notNull(albumStatVo, "未获取到专辑统计信息！");
            mapResult.put("albumStatVo", albumStatVo);
        }, threadPoolExecutor);

        //3 获取分类信息
        CompletableFuture<Void> baseCategoryViewCompletableFuture = albumInfoCompletableFuture.thenAcceptAsync(albumInfo -> {
            BaseCategoryView baseCategoryView = albumFeignClient.getCategoryViewBy3Id(albumInfo.getCategory3Id()).getData();
            Assert.notNull(baseCategoryView, "未获取到专辑分类信息！");
            mapResult.put("baseCategoryView", baseCategoryView);
        }, threadPoolExecutor);

        //4 获取主播信息
        CompletableFuture<Void> userInfoVoCompletableFuture = albumInfoCompletableFuture.thenAcceptAsync(albumInfo -> {
            UserInfoVo userInfoVo = userFeignClient.getUserInfoVoByUserId(albumInfo.getUserId()).getData();
            Assert.notNull(userInfoVo, "未获取到专辑主播信息！");
            mapResult.put("announcer", userInfoVo);
        }, threadPoolExecutor);

        CompletableFuture.allOf(albumInfoCompletableFuture, albumStatVoCompletableFuture, baseCategoryViewCompletableFuture, userInfoVoCompletableFuture).join();
        return mapResult;
    }
}
