package com.atguigu.tingshu.album;

import com.atguigu.tingshu.album.impl.AlbumDegradeFeignClient;
import com.atguigu.tingshu.common.result.Result;
import com.atguigu.tingshu.model.album.AlbumInfo;
import com.atguigu.tingshu.model.album.BaseCategoryView;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

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
}
