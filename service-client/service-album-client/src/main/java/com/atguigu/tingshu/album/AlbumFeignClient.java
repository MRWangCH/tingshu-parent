package com.atguigu.tingshu.album;

import com.atguigu.tingshu.album.impl.AlbumDegradeFeignClient;
import com.atguigu.tingshu.common.result.Result;
import com.atguigu.tingshu.model.album.*;
import com.atguigu.tingshu.vo.album.AlbumStatVo;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.List;

/**
 * <p>
 * 专辑模块远程调用Feign接口
 * </p>
 *
 * @author atguigu
 */
@FeignClient(value = "service-album",path = "/api/album",fallback = AlbumDegradeFeignClient.class)
public interface AlbumFeignClient {

    /**
     * 根据专辑id查询专辑的属性列表
     * @param id 专辑id
     * @return
     */
    @GetMapping("/albumInfo/getAlbumInfo/{id}")
    public Result<AlbumInfo> getAlbumInfo(@PathVariable("id") Long id);

    /**
     * 根据三级分类id（视图的主键） 获取到分类信息
     * @param category3Id
     * @return
     */
    @GetMapping("/category/getCategoryView/{category3Id}")
    public Result<BaseCategoryView> getCategoryViewBy3Id(@PathVariable("category3Id") Long category3Id);

    /**
     * 根据一级分类id查询当前分类下前七个3级分类
     * @param category1Id
     * @return
     */
    @GetMapping("/category/findTopBaseCategory3/{category1Id}")
    public Result<List<BaseCategory3>> getTop7BaseCategory3(@PathVariable Long category1Id);

    /**
     * 根据专辑ID获取专辑统计信息
     * @param albumId
     * @return
     */
    @GetMapping("/albumInfo/getAlbumStatVo/{albumId}")
    public Result<AlbumStatVo> getAlbumStatVo(@PathVariable Long albumId);

    /**
     * 查询所有一级分类列表
     * @return
     */
    @GetMapping("/category/findAllCategory1")
    public Result<List<BaseCategory1>> findAllCategory1();

    /**
     * 查询用户声音分集购买支付列表-用于渲染订单结算页
     * @param trackId
     * @param trackCount
     * @return
     */
    @GetMapping("/trackInfo/findPaidTrackInfoList/{trackId}/{trackCount}")
    public Result<List<TrackInfo>> getWaitPayTrackInfoList(@PathVariable Long trackId, @PathVariable Integer trackCount);

    /**
     * 声音id查询声音信息
     * @param id
     * @return
     */
    @GetMapping("/trackInfo/getTrackInfo/{id}")
    public Result<TrackInfo> getTrackInfo(@PathVariable("id") Long id);
}
