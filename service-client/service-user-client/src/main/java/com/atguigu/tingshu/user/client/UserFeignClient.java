package com.atguigu.tingshu.user.client;

import com.atguigu.tingshu.common.result.Result;
import com.atguigu.tingshu.model.user.VipServiceConfig;
import com.atguigu.tingshu.user.client.impl.UserDegradeFeignClient;
import com.atguigu.tingshu.vo.user.UserInfoVo;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;
import java.util.Map;

/**
 * <p>
 * 产品列表API接口
 * </p>
 *
 * @author atguigu
 */
@FeignClient(value = "service-user", path = "api/user",fallback = UserDegradeFeignClient.class)
public interface UserFeignClient {
    /**
     * 根据用户ID查询用户信息
     * @param userId
     * @return
     */
    @GetMapping("/userInfo/getUserInfoVo/{userId}")
    public Result<UserInfoVo> getUserInfoVoByUserId(@PathVariable Long userId);

    /**
     * 获取专辑声音列表某页中，用户对于声音的付费情况
     * @param userId
     * @param albumId
     * @param trackIdList
     * @return
     */
    @PostMapping("/userInfo/userIsPaidTrack/{userId}/{albumId}")
    public Result<Map<Long, Integer>> userIsPaidTrackList(@PathVariable("userId") Long userId, @PathVariable("albumId") Long albumId, @RequestBody List<Long> trackIdList);

    /**
     * 是否购买过此专辑
     * @param albumId
     * @return
     */
    @GetMapping("/userInfo/isPaidAlbum/{albumId}")
    public Result<Boolean> isPaidAlbum(@PathVariable Long albumId);


    /**
     * 根据主键id查询vip套餐详情
     * @param id
     * @return
     */
    @GetMapping("/vipServiceConfig/getVipServiceConfig/{id}")
    public Result<VipServiceConfig> getVipServiceConfig(@PathVariable Long id);

    /**
     * 根据专辑id+用户ID获取用户已购买声音id列表
     * @param albumId
     * @return
     */
    @GetMapping("/userInfo/findUserPaidTrackList/{albumId}")
    public Result<List<Long>> getUserPaidTrackList(@PathVariable Long albumId);

    /**
     * 更新vip到期失效状态
     * @return
     */
    @GetMapping("updateVipExpireStatus")
    public Result updateVipExpireStatus();
}
