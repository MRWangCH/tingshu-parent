package com.atguigu.tingshu.search.api;

import com.atguigu.tingshu.common.result.Result;
import com.atguigu.tingshu.query.search.AlbumIndexQuery;
import com.atguigu.tingshu.search.service.SearchService;
import com.atguigu.tingshu.vo.search.AlbumSearchResponseVo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Tag(name = "搜索专辑管理")
@RestController
@RequestMapping("api/search")
@SuppressWarnings({"all"})
public class SearchApiController {

    @Autowired
    private SearchService searchService;

    /**
     * 上架专辑，该接口仅用于测试
     * @param albumId
     * @return
     */
    @Operation(summary = "上架专辑，该接口仅用于测试")
    @GetMapping("/albumInfo/upperAlbum/{albumId}")
    public Result upperAlbum(@PathVariable Long albumId){
        searchService.upperAlbum(albumId);
        return Result.ok();
    }

    /**
     * 下架专辑，该接口仅用于测试
     * @param albumId
     * @return
     */
    @Operation(summary = "下架专辑，该接口仅用于测试")
    @GetMapping("/albumInfo/lowerAlbum/{albumId}")
    public Result lowerAlbum(@PathVariable Long albumId){
        searchService.lowerAlbum(albumId);
        return Result.ok();
    }

    /**
     * 根据关键字，分类id，属性/属性值检索专辑
     * @param queryVo
     * @return
     */
    @Operation(summary = "根据关键字，分类id，属性/属性值检索专辑")
    @PostMapping("/albumInfo")
    public Result<AlbumSearchResponseVo> search(@RequestBody AlbumIndexQuery queryVo) {
        AlbumSearchResponseVo vo = searchService.search(queryVo);
        return Result.ok(vo);
    }

    /**
     * 查询当前三级分类下最热门的6个专辑列表
     * @param category1Id
     * @return
     */
    @Operation(summary = "查询当前三级分类下最热门的6个专辑列表")
    @GetMapping("/albumInfo/channel/{category1Id}")
    public Result<List<Map<String, Object>>> getCategory3Top6Hot(@PathVariable Long category1Id){
        List<Map<String, Object>> list = searchService.getCategory3Top6Hot(category1Id);
        return Result.ok(list);
    }

    /**
     * 关键字自动补全
     * @param keyword
     * @return
     */
    @Operation(summary = "关键字自动补全")
    @GetMapping("/albumInfo/completeSuggest/{keyword}")
    public Result<List<String>> completeSuggest(@PathVariable String keyword) {
        List<String> list = searchService.completeSuggest(keyword);
        return Result.ok(list);
    }
}

