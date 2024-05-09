package com.atguigu.tingshu.album.service.impl;

import cn.hutool.core.collection.CollectionUtil;
import com.alibaba.fastjson.JSONObject;
import com.atguigu.tingshu.album.mapper.*;
import com.atguigu.tingshu.album.service.BaseCategoryService;
import com.atguigu.tingshu.common.cache.GuiguCache;
import com.atguigu.tingshu.model.album.*;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
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

	/**
	 * 根据三级分类id（视图的主键） 获取到分类信息
	 * @param category3Id
	 * @return
	 */
	@GuiguCache(prefix = "categoryViewBy3Id:")
	@Override
	public BaseCategoryView getCategoryViewBy3Id(Long category3Id) {
		return baseCategoryViewMapper.selectById(category3Id);
	}

	/**
	 * 根据一级分类id查询当前分类下前七个3级分类
	 * @param category1Id
	 * @return
	 */
	@Override
	public List<BaseCategory3> getTop7BaseCategory3(Long category1Id) {
		//1 根据1级分类id查询2级分类id集合
		LambdaQueryWrapper<BaseCategory2> queryWrapper = new LambdaQueryWrapper<>();
		queryWrapper.eq(BaseCategory2::getCategory1Id, category1Id);
		queryWrapper.select(BaseCategory2::getId);
		List<BaseCategory2> baseCategory2List = baseCategory2Mapper.selectList(queryWrapper);
		//2 根据2级分类id查询3级分类列表
		if (CollectionUtil.isNotEmpty(baseCategory2List)) {
			List<Long> category2IdList = baseCategory2List.stream().map(m -> m.getId()).collect(Collectors.toList());
			LambdaQueryWrapper<BaseCategory3> lambdaQueryWrapper = new LambdaQueryWrapper<>();
			lambdaQueryWrapper.in(BaseCategory3::getCategory2Id,category2IdList);
			lambdaQueryWrapper.last("limit 7");
			lambdaQueryWrapper.orderByAsc(BaseCategory3::getId);
			List<BaseCategory3> baseCategory3List = baseCategory3Mapper.selectList(lambdaQueryWrapper);
			return baseCategory3List;
		}
		return null;
	}

	/**
	 * 根据一级分类id查询当前分类包含所有二级分类以及二级分类下的三级分类列表
	 * @param category1Id
	 * @return
	 */
	@Override
	public JSONObject getCategoryListByCategory1Id(Long category1Id) {
		//1 根据1级分类id查询到以及分类的信息
		BaseCategory1 baseCategory1 = baseCategory1Mapper.selectById(category1Id);
		//1.1 构建一级分类的对象
		JSONObject jsonObject = new JSONObject();
		jsonObject.put("categoryId", baseCategory1.getId());
		jsonObject.put("categoryName", baseCategory1.getName());
		//2 一级分类下的二级分类列表
		List<BaseCategoryView> category1List = baseCategoryViewMapper.selectList(new LambdaQueryWrapper<BaseCategoryView>().eq(BaseCategoryView::getCategory1Id, category1Id));
		if (CollectionUtil.isNotEmpty(category1List)){
			//2.1 根据二级分类id分组得到map
			Map<Long, List<BaseCategoryView>> category2Map = category1List.stream().collect(Collectors.groupingBy(BaseCategoryView::getCategory2Id));
			ArrayList<JSONObject> categroy2List = new ArrayList<>();
			for (Map.Entry<Long, List<BaseCategoryView>> entry2 : category2Map.entrySet()) {
				Long entry2Id = entry2.getKey();
				String category2Name = entry2.getValue().get(0).getCategory2Name();
				JSONObject jsonObject2 = new JSONObject();
				jsonObject2.put("categoryId", entry2Id);
				jsonObject2.put("categoryName", category2Name);
				categroy2List.add(jsonObject2);
				//3 处理二级分类下三级分类列表
				ArrayList<JSONObject> categroy3List = new ArrayList<>();
				for (BaseCategoryView baseCategoryView : entry2.getValue()) {
					JSONObject jsonObject3 = new JSONObject();
					jsonObject3.put("categoryId", baseCategoryView.getCategory3Id());
					jsonObject3.put("categoryName", baseCategoryView.getCategory3Name());
					categroy3List.add(jsonObject3);
				}
				jsonObject2.put("categoryChild", categroy3List);
			}
			jsonObject.put("categoryChild", categroy2List);
		}
		return jsonObject;
	}
}
