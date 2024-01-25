package com.atguigu.tingshu.album.api;

import com.atguigu.tingshu.album.service.AlbumInfoService;
import com.atguigu.tingshu.common.result.Result;
import com.atguigu.tingshu.common.util.AuthContextHolder;
import com.atguigu.tingshu.query.album.AlbumInfoQuery;
import com.atguigu.tingshu.vo.album.AlbumInfoVo;
import com.atguigu.tingshu.vo.album.AlbumListVo;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@Tag(name = "专辑管理")
@RestController
@RequestMapping("api/album")
@SuppressWarnings({"all"})
public class AlbumInfoApiController {

	@Autowired
	private AlbumInfoService albumInfoService;


	/**
	 * TODO 该接口必须登录才能访问
	 * 创作者新增专辑
	 *
	 * @return
	 */
	@Operation(summary = "新增专辑")
	@PostMapping("/albumInfo/saveAlbumInfo")
	public Result saveAlbumInfo(@RequestBody @Validated AlbumInfoVo albumInfoVo){
		Long userId = AuthContextHolder.getUserId();
		albumInfoService.saveAlbumInfo(albumInfoVo, userId);
		return Result.ok();
	}

	/**
	 * 分页查询当前用户专辑列表
	 * TODO 该接口必须登录才能访问
	 * @param page
	 * @param limit
	 * @param albumInfoQuery
	 * @return
	 */
	@Operation(summary = "分页查询当前用户专辑列表")
	@PostMapping("/albumInfo/findUserAlbumPage/{page}/{limit}")
	public Result<Page<AlbumListVo>> getUserAlbumByPage(@PathVariable int page, @PathVariable int limit, @RequestBody AlbumInfoQuery albumInfoQuery){
		//1.封装用户id查询条件
		Long userId = AuthContextHolder.getUserId();
		albumInfoQuery.setUserId(userId);
		//2.调用业务层完成分页查询
		//2.1 构建业务层或者持久层进行分页查询所需分页对象，两个参数，页码和页大小
		Page<AlbumListVo> pageInfo = new Page<>(page, limit);
		//2.2 剩余分页属性总条数，总页数，当前记录数
		pageInfo = albumInfoService.getUserAlbumByPage(pageInfo, albumInfoQuery);
		return Result.ok(pageInfo);
	}

	/**
	 * 根据专辑id删除专辑
	 * @param id 专辑id
	 * @return
	 */
	@Operation(summary = "根据专辑id删除专辑")
	@DeleteMapping("/albumInfo/removeAlbumInfo/{id}")
	public Result removeAlbumInfo(@PathVariable ("id") Long id){
		albumInfoService.removeAlbumInfo(id);
		return Result.ok();
	}
}

