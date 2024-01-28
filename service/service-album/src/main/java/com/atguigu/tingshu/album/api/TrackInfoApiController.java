package com.atguigu.tingshu.album.api;

import com.atguigu.tingshu.album.service.TrackInfoService;
import com.atguigu.tingshu.common.result.Result;
import com.atguigu.tingshu.common.util.AuthContextHolder;
import com.atguigu.tingshu.model.album.TrackInfo;
import com.atguigu.tingshu.query.album.TrackInfoQuery;
import com.atguigu.tingshu.vo.album.TrackInfoVo;
import com.atguigu.tingshu.vo.album.TrackListVo;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@Tag(name = "声音管理")
@RestController
@RequestMapping("api/album")
@SuppressWarnings({"all"})
public class TrackInfoApiController {

	@Autowired
	private TrackInfoService trackInfoService;

	/***
	 * 上传声音文件到腾讯云点播平台
	 * @param file 上传的声音文件
	 * @return
	 */
	@Operation(summary = "上传声音文件到腾讯云点播平台")
	@PostMapping("/trackInfo/uploadTrack")
	public Result<Map<String, String>> uploadTrack(MultipartFile file){
		Map<String, String> map = trackInfoService.uploadTrack(file);
		return Result.ok(map);
	}

	/**
	 * 声音的保存
	 * TODO 该接口必须登录后才能访问
	 * @param trackInfoVo
	 * @return
	 */
	@Operation(summary = "声音的保存")
	@PostMapping("/trackInfo/saveTrackInfo")
	public Result saveTrackInfo(@RequestBody @Validated TrackInfoVo trackInfoVo){
		//1 获取用户id
		Long userId = AuthContextHolder.getUserId();
		//2 业务层保存
		trackInfoService.saveTrackInfo(userId, trackInfoVo);
		return Result.ok();
	}

	/**
	 * 当前用户声音列表页的分页查询
	 * 	TODO 该接口必须登录后才能访问
	 * @param page
	 * @param limit
	 * @param trackInfoQuery
	 * @return
	 */
	@Operation(summary = "当前用户声音列表页的分页查询")
	@PostMapping("/trackInfo/findUserTrackPage/{page}/{limit}")
	public Result<Page<TrackListVo>> getUserTrackPage(@PathVariable("page") int page, @PathVariable("limit") int limit,
													  @RequestBody TrackInfoQuery trackInfoQuery){
		// 1获取用户id
		Long userId = AuthContextHolder.getUserId();
		// 2 封装分页条件
		trackInfoQuery.setUserId(userId);
		// 3 业务层完成分页
		Page<TrackListVo> pageInfo = new Page<>(page, limit);
		pageInfo = trackInfoService.getUserTrackPage(pageInfo, trackInfoQuery);
		return Result.ok(pageInfo);
	}
}

