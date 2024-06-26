package com.atguigu.tingshu.search.service;

import co.elastic.clients.elasticsearch.core.SearchRequest;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import com.atguigu.tingshu.model.search.AlbumInfoIndex;
import com.atguigu.tingshu.model.search.SuggestIndex;
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

    /**
     * 将专辑标题存入提词索引库
     * @param albumInfoIndex
     */
    void saveSuggestDoc(AlbumInfoIndex albumInfoIndex);

    /**
     * 关键字自动补全
     * @param keyword
     * @return
     */
    List<String> completeSuggest(String keyword);

    /**
     * 解析提词响应结果
     * @param searchResponse
     * @param suggestName
     * @return
     */
    List<String> parseSuggestResult(SearchResponse<SuggestIndex> searchResponse, String suggestName);

    /**
     * 更新所有分类下排行榜，从es中获取不同分类下不同排行列表，存入到redis的hash结构中
     * @return
     */
    void updateLatelyAlbumRanking();

    /**
     * 获取reids中不同分类专辑排行榜
     * @param category1Id
     * @param dimension
     * @return
     */
    List<AlbumInfoIndex> getRankingList(Long category1Id, String dimension);
}
