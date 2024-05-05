package com.atguigu.tingshu.user.api;

import com.atguigu.tingshu.common.login.GuiGuLogin;
import com.atguigu.tingshu.common.result.Result;
import com.atguigu.tingshu.common.util.AuthContextHolder;
import com.atguigu.tingshu.user.service.UserListenProcessService;
import com.atguigu.tingshu.vo.user.UserListenProcessVo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

@Tag(name = "用户声音播放进度管理接口")
@RestController
@RequestMapping("api/user")
@SuppressWarnings({"all"})
public class UserListenProcessApiController {

	@Autowired
	private UserListenProcessService userListenProcessService;

	/**
	 * 查询当前用户入参声音上次播放的进度
	 * @param trackId
	 * @return
	 */
	@GuiGuLogin
	@Operation(summary = "查询当前用户入参声音上次播放的进度")
	@GetMapping("/userListenProcess/getTrackBreakSecond/{trackId}")
	public Result<BigDecimal> getTrackBreakSecond(@PathVariable Long trackId) {
		Long userId = AuthContextHolder.getUserId();
		BigDecimal breakSecond = userListenProcessService.getTrackBreakSecond(userId, trackId);
		return Result.ok(breakSecond);
	}

	/**
	 * 更新声音播放进度
	 * @param userListenProcessVo
	 * @return
	 */
	@GuiGuLogin
	@Operation(summary = "更新声音播放进度")
	@PostMapping("/userListenProcess/updateListenProcess")
	public Result updateListenProcess(@RequestBody UserListenProcessVo userListenProcessVo) {
		userListenProcessService.updateListenProcess(userListenProcessVo);
		return Result.ok();
	}
}

