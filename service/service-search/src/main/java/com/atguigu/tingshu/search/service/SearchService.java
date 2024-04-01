package com.atguigu.tingshu.search.service;

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
}
