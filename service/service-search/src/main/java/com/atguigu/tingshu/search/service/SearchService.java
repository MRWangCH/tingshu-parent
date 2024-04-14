package com.atguigu.tingshu.search.service;

import co.elastic.clients.elasticsearch.core.SearchRequest;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import com.atguigu.tingshu.model.search.AlbumInfoIndex;
import com.atguigu.tingshu.query.search.AlbumIndexQuery;
import com.atguigu.tingshu.vo.search.AlbumSearchResponseVo;

import java.util.List;
import java.util.Map;

public interface SearchService {


    /**
     * 上架专辑
     * @param albumId
     * @return
     */
    void upperAlbum(Long albumId);

    /**
     * 下架专辑，该接口仅用于测试
     * @param albumId
     * @return
     */
    void lowerAlbum(Long albumId);

    /**
     * 根据关键字，分类id，属性/属性值检索专辑
     * @param queryVo
     * @return
     */
    AlbumSearchResponseVo search(AlbumIndexQuery queryVo);

    /**
     * 根据检索条件构建请求对象
     * @param queryVo
     * @return
     */
    SearchRequest buildDSL(AlbumIndexQuery queryVo);

    /**
     * 解析es响应结果，封装自定义结果
     * @param response
     * @param queryVo
     * @return
     */
    AlbumSearchResponseVo parseResult(SearchResponse<AlbumInfoIndex> response, AlbumIndexQuery queryVo);

    /**
     * 查询当前三级分类下最热门的6个专辑列表
     * @param category1Id
     * @return
     */
    List<Map<String, Object>> getCategory3Top6Hot(Long category1Id);
}
