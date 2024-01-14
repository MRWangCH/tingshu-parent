package com.atguigu.tingshu.album.mapper;

import com.atguigu.tingshu.model.album.BaseAttribute;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface BaseAttributeMapper extends BaseMapper<BaseAttribute> {


    /**
     * 根据一级分类Id获取分类属性以及属性值
     * @param category1Id
     * @return
     */
    List<BaseAttribute> getAttributeByCategoryId(@Param("category1Id") Long category1Id);
}
