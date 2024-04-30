package com.atguigu.tingshu.user.api;

import com.atguigu.tingshu.common.result.Result;
import com.atguigu.tingshu.user.service.UserInfoService;
import com.atguigu.tingshu.vo.user.UserInfoVo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Tag(name = "用户管理接口")
@RestController
@RequestMapping("api/user")
@SuppressWarnings({"all"})
public class UserInfoApiController {

	@Autowired
	private UserInfoService userInfoService;

	/**
	 * 根据用户ID查询用户信息
	 * @param userId
	 * @return
	 */
	@Operation(summary = "根据用户ID查询用户信息")
	@GetMapping("/userInfo/getUserInfoVo/{userId}")
	public Result<UserInfoVo> getUserInfoVoByUserId(@PathVariable Long userId){
		UserInfoVo userInfo = userInfoService.getUserInfoVoByUserId(userId);
		return Result.ok(userInfo);
	}

	/**
	 * 获取专辑声音列表某页中，用户对于声音的付费情况
	 * @param userId
	 * @param albumId
	 * @param trackIdList
	 * @return
	 */
	@Operation(summary = "获取专辑声音列表某页中，用户对于声音的付费情况")
	@PostMapping("/userInfo/userIsPaidTrack/{userId}/{albumId}")
	public Result<Map<Long, Integer>> userIsPaidTrackList(@PathVariable("userId") Long userId, @PathVariable("albumId") Long albumId, @RequestBody List<Long> trackIdList) {
		Map<Long, Integer> mapList = userInfoService.userIsPaidTrackList(userId, albumId, trackIdList);
		return Result.ok(mapList);
	}

}

