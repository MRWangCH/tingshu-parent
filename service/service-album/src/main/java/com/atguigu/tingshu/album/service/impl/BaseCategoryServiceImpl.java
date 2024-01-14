package com.atguigu.tingshu.album.service.impl;

import cn.hutool.core.collection.CollectionUtil;
import com.alibaba.fastjson.JSONObject;
import com.atguigu.tingshu.album.mapper.*;
import com.atguigu.tingshu.album.service.BaseCategoryService;
import com.atguigu.tingshu.model.album.BaseAttribute;
import com.atguigu.tingshu.model.album.BaseCategory1;
import com.atguigu.tingshu.model.album.BaseCategoryView;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@SuppressWarnings({"all"})
public class BaseCategoryServiceImpl extends ServiceImpl<BaseCategory1Mapper, BaseCategory1> implements BaseCategoryService {

	@Autowired
	private BaseCategory1Mapper baseCategory1Mapper;

	@Autowired
	private BaseCategory2Mapper baseCategory2Mapper;

	@Autowired
	private BaseCategory3Mapper baseCategory3Mapper;

	@Autowired
	private BaseCategoryViewMapper baseCategoryViewMapper;

	@Autowired
	private BaseAttributeMapper baseAttributeMapper;

	@Override
	public List<JSONObject> getBaseCategoryList() {
		List<JSONObject> listResult = new ArrayList<>();

		//1 查询视图所有记录
		List<BaseCategoryView> allCategoryList = baseCategoryViewMapper.selectList(null);
		//2 处理一级分类 封装所有一级分类jsonobject，将一级分类对象加入到list集合
		if (CollectionUtil.isNotEmpty(allCategoryList)){
			//2.1 采用stream中的分组api对集合中元素按照一级分类分组
			Map<Long, List<BaseCategoryView>> category1MapList = allCategoryList.stream().collect(Collectors.groupingBy(BaseCategoryView::getCategory1Id));
			//2.2 遍历map集合，每遍历一级处理一条一级分类数据
			for (Map.Entry<Long, List<BaseCategoryView>> entry1 : category1MapList.entrySet()) {
				Long category1Id = entry1.getKey();
				String category1Name = entry1.getValue().get(0).getCategory1Name();
				JSONObject category1 = new JSONObject();
				category1.put("categoryId", category1Id);
				category1.put("categoryName", category1Name);

				//一级分类下的二级分类为空
				Map<Long, List<BaseCategoryView>> category2MapList = entry1.getValue().stream().collect(Collectors.groupingBy(BaseCategoryView::getCategory2Id));
				List<Object> category2List = new ArrayList<>();
				//2.2 遍历map集合
				for (Map.Entry<Long, List<BaseCategoryView>> entry2 : category2MapList.entrySet()) {
					Long category2Id = entry2.getKey();
					String category2Name = entry2.getValue().get(0).getCategory2Name();
					JSONObject category2 = new JSONObject();
					category2.put("categoryId", category2Id);
					category2.put("categoryName", category2Name);
					category2List.add(category2);
					//4 在二级分类内部封装三级分类，将三级分类加入到二级分类的catagorychild中
					List<Object> category3List = new ArrayList<>();
					for (BaseCategoryView baseCategoryView : entry2.getValue()) {
						JSONObject category3 = new JSONObject();
						category3.put("categoryId", baseCategoryView.getCategory3Id());
						category3.put("categoryName", baseCategoryView.getCategory3Name());
						category3List.add(category3);
					}
					category2.put("categoryChild",category3List);

					category2List.add(category2);
				}

				//3 在一级分类内部封装二级分类，将二级分类加入到一级分类的catagorychild中
				category1.put("categoryChild",category2List);
				listResult.add(category1);

			}
		}
		return listResult;
	}

	/**
	 * 根据一级分类Id获取分类属性以及属性值
	 * @param category1Id
	 * @return
	 */
	@Override
	public List<BaseAttribute> getAttributeByCategoryId(Long category1Id) {
		//
		List<BaseAttribute> list = baseAttributeMapper.getAttributeByCategoryId(category1Id);
		return list;
	}
}
