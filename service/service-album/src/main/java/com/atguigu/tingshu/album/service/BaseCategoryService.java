package com.atguigu.tingshu.album.service;

import com.alibaba.fastjson.JSONObject;
import com.atguigu.tingshu.model.album.BaseAttribute;
import com.atguigu.tingshu.model.album.BaseCategory1;
import com.atguigu.tingshu.model.album.BaseCategory3;
import com.atguigu.tingshu.model.album.BaseCategoryView;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

public interface BaseCategoryService extends IService<BaseCategory1> {

    /**
     * 查询所有分类（1、2、3级分类）
     * @return
     */
    List<JSONObject> getBaseCategoryList();

    /**
     * 根据一级分类Id获取分类属性以及属性值
     * @param category1Id
     * @return
     */
    List<BaseAttribute> getAttributeByCategoryId(Long category1Id);

    /**
     * 根据三级分类id（视图的主键） 获取到分类信息
     * @param category3Id
     * @return
     */
    BaseCategoryView getCategoryViewBy3Id(Long category3Id);

    /**
     * 根据一级分类id查询当前分类下前七个3级分类
     * @param category1Id
     * @return
     */
    List<BaseCategory3> getTop7BaseCategory3(Long category1Id);
}
