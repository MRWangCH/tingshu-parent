package com.atguigu.tingshu.album.api;

import com.alibaba.fastjson.JSONObject;
import com.atguigu.tingshu.album.service.BaseCategoryService;
import com.atguigu.tingshu.common.result.Result;
import com.atguigu.tingshu.model.album.BaseAttribute;
import com.atguigu.tingshu.model.album.BaseCategory1;
import com.atguigu.tingshu.model.album.BaseCategoryView;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;


@Tag(name = "分类管理")
@RestController
@RequestMapping(value="/api/album")
@SuppressWarnings({"all"})
public class BaseCategoryApiController {

	@Autowired
	private BaseCategoryService baseCategoryService;

	/**
	 * 查询所有分类（1、2、3级分类）
	 * @return
	 */
	@Operation(summary = "查询所有分类（1、2、3级分类）")
	@GetMapping("/category/getBaseCategoryList")
	public Result<List<JSONObject>> getBaseCategoryList() {
		List<JSONObject> list = baseCategoryService.getBaseCategoryList();
		return Result.ok(list);
	}

	/**
	 * 根据一级分类Id获取分类属性以及属性值
	 * @param category1Id
	 * @return
	 */
	@Operation(summary = "根据一级分类Id获取分类属性以及属性值")
	@GetMapping("/category/findAttribute/{category1Id}")
	public Result<List<BaseAttribute>> getAttribute(@PathVariable("category1Id") Long category1Id){
		List<BaseAttribute> list = baseCategoryService.getAttributeByCategoryId(category1Id);
		return Result.ok(list);
	}

	/**
	 * 根据三级分类id（视图的主键） 获取到分类信息
	 * @param category3Id
	 * @return
	 */
	@Operation(summary = "根据三级分类id 获取到分类信息")
	@GetMapping("/category/getCategoryView/{category3Id}")
	public Result<BaseCategoryView> getCategoryViewBy3Id(@PathVariable("category3Id") Long category3Id){
		BaseCategoryView baseCategoryView = baseCategoryService.getCategoryViewBy3Id(category3Id);
		return Result.ok(baseCategoryView);
	}
}

