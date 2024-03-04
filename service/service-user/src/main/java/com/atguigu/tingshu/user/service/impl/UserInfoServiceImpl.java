package com.atguigu.tingshu.user.service.impl;

import cn.binarywang.wx.miniapp.api.WxMaService;
import cn.binarywang.wx.miniapp.bean.WxMaJscode2SessionResult;
import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.IdUtil;
import com.atguigu.tingshu.common.constant.RedisConstant;
import com.atguigu.tingshu.model.user.UserInfo;
import com.atguigu.tingshu.user.mapper.UserInfoMapper;
import com.atguigu.tingshu.user.service.UserInfoService;
import com.atguigu.tingshu.vo.user.UserInfoVo;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import me.chanjar.weixin.common.error.WxErrorException;
import org.apache.catalina.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@SuppressWarnings({"all"})
public class UserInfoServiceImpl extends ServiceImpl<UserInfoMapper, UserInfo> implements UserInfoService {

	@Autowired
	private UserInfoMapper userInfoMapper;

	@Autowired
	private WxMaService wxMaService;

	@Autowired
	private RedisTemplate redisTemplate;

	/**
	 * 微信登录 采用微信JavaSDK
	 * @param code
	 * @return
	 */
	@Override
	public Map<String, String> weiXinLogin(String code) {
		try {
			//1 根据code调用微信SDK获取用户会话信息 得到微信用户的唯一标识
			WxMaJscode2SessionResult sessionInfo = wxMaService.getUserService().getSessionInfo(code);
			if (sessionInfo != null){
				String openid = sessionInfo.getOpenid();
				//String openid = odo3j4q2KskkbbW-krfE-cAxUnzU;
				//2 根据openId查询用户记录 odo3j4q2KskkbbW-krfE-cAxUnzU
				LambdaQueryWrapper<UserInfo> queryWrapper = new LambdaQueryWrapper<>();
				queryWrapper.eq(UserInfo::getWxOpenId, openid);
				UserInfo userInfo = userInfoMapper.selectOne(queryWrapper);
				//2.1 根据openid没有得到用户记录，新增用户记录 且采用MQ异步初始化账户信息
				if (userInfo == null){
					userInfo = new UserInfo();
					userInfo.setWxOpenId(openid);
					userInfo.setNickname("tingyou" + IdUtil.getSnowflake());
					userInfo.setAvatarUrl("https://oss.aliyuncs.com/aliyun_id_photo_bucket/default_handsome.jpg");
					userInfoMapper.insert(userInfo);
					//TODO 发送异步MQ消息通知账户微服务初始化当前用户余额信息

				}
				//2.2 根据openid得到用户记录

				//3 为登录的微信用户生成令牌存入redis
				String token = IdUtil.fastSimpleUUID();
				String loginKey = RedisConstant.USER_LOGIN_KEY_PREFIX+token;
				UserInfoVo userInfoVo = BeanUtil.copyProperties(userInfo, UserInfoVo.class);
				redisTemplate.opsForValue().set(loginKey, userInfoVo, RedisConstant.USER_LOGIN_KEY_TIMEOUT, TimeUnit.SECONDS);

				//4 相应令牌
				HashMap<String, String> mapResult = new HashMap<>();
				mapResult.put("token", token);
				return mapResult;
			}
			return null;
		} catch (WxErrorException e) {
			log.error("微信登录异常{}",e);
			throw new RuntimeException(e);
		}
	}
}
