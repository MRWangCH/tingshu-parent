package com.atguigu.tingshu.user.service.impl;

import cn.binarywang.wx.miniapp.api.WxMaService;
import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.util.IdUtil;
import com.atguigu.tingshu.album.AlbumFeignClient;
import com.atguigu.tingshu.common.constant.KafkaConstant;
import com.atguigu.tingshu.common.constant.RedisConstant;
import com.atguigu.tingshu.common.constant.SystemConstant;
import com.atguigu.tingshu.common.service.KafkaService;
import com.atguigu.tingshu.model.album.TrackInfo;
import com.atguigu.tingshu.model.user.UserInfo;
import com.atguigu.tingshu.model.user.UserPaidAlbum;
import com.atguigu.tingshu.model.user.UserPaidTrack;
import com.atguigu.tingshu.user.mapper.UserInfoMapper;
import com.atguigu.tingshu.user.mapper.UserPaidAlbumMapper;
import com.atguigu.tingshu.user.mapper.UserPaidTrackMapper;
import com.atguigu.tingshu.user.service.UserInfoService;
import com.atguigu.tingshu.vo.user.UserInfoVo;
import com.atguigu.tingshu.vo.user.UserPaidRecordVo;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

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

    @Autowired
    private KafkaService kafkaService;

    @Autowired
    private UserPaidAlbumMapper userPaidAlbumMapper;

    @Autowired
    private UserPaidTrackMapper userPaidTrackMapper;

    @Autowired
    private AlbumFeignClient albumFeignClient;

    /**
     * 微信登录 采用微信JavaSDK
     *
     * @param code
     * @return
     */
    @Override
    public Map<String, String> weiXinLogin(String code) {
        try {
            //1 根据code调用微信SDK获取用户会话信息 得到微信用户的唯一标识
//            WxMaJscode2SessionResult sessionInfo = wxMaService.getUserService().getSessionInfo(code);
//            if (sessionInfo != null) {
//                String openid = sessionInfo.getOpenid();
            String openid = "odo3j4q2KskkbbW-krfE-cAxUnzU";
            //2 根据openId查询用户记录 odo3j4q2KskkbbW-krfE-cAxUnzU
            LambdaQueryWrapper<UserInfo> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(UserInfo::getWxOpenId, openid);
            UserInfo userInfo = userInfoMapper.selectOne(queryWrapper);
            //2.1 根据openid没有得到用户记录，新增用户记录 且采用MQ异步初始化账户信息
            if (userInfo == null) {
                userInfo = new UserInfo();
                userInfo.setWxOpenId(openid);
                userInfo.setNickname("tingyou" + IdUtil.getSnowflake().nextId());
                userInfo.setAvatarUrl("https://oss.aliyuncs.com/aliyun_id_photo_bucket/default_handsome.jpg");
                userInfoMapper.insert(userInfo);
                //发送异步MQ消息通知账户微服务初始化当前用户余额信息
                kafkaService.sendMessage(KafkaConstant.QUEUE_USER_REGISTER, userInfo.getId().toString());
            }
            //2.2 根据openid得到用户记录

            //3 为登录的微信用户生成令牌存入redis
            String token = IdUtil.fastSimpleUUID();
            String loginKey = RedisConstant.USER_LOGIN_KEY_PREFIX + token;
            UserInfoVo userInfoVo = BeanUtil.copyProperties(userInfo, UserInfoVo.class);
            redisTemplate.opsForValue().set(loginKey, userInfoVo, RedisConstant.USER_LOGIN_KEY_TIMEOUT, TimeUnit.SECONDS);

            //4 相应令牌
            HashMap<String, String> mapResult = new HashMap<>();
            mapResult.put("token", token);
            return mapResult;
//            }
//            return null;
        } catch (Exception e) {
            log.error("微信登录异常{}", e);
            throw new RuntimeException(e);
        }
    }

    /**
     * 获取当前登录用户
     *
     * @param userId
     * @return
     */
    @Override
    public UserInfoVo getUserInfoVoByUserId(Long userId) {
        UserInfo userInfo = userInfoMapper.selectById(userId);
        return BeanUtil.copyProperties(userInfo, UserInfoVo.class);
    }

    /**
     * 修改用户信息
     *
     * @param userInfoVo
     */
    @Override
    public void updateUser(UserInfoVo userInfoVo) {
        //1 先删除缓存
        redisTemplate.delete("userInfo:" + userInfoVo.getId());
        //2 更新数据库
        UserInfo userInfo = new UserInfo();
        userInfo.setId(userInfoVo.getId());
        userInfo.setAvatarUrl(userInfoVo.getAvatarUrl());
        userInfo.setNickname(userInfoVo.getNickname());
        userInfoMapper.updateById(userInfo);
        try {
            //3 睡眠一段时间，确保并发情况下读的线程执行完毕
            Thread.sleep(500);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        //4 再次删除缓存
        redisTemplate.delete("userInfo:" + userInfoVo.getId());
    }

    /**
     * 获取专辑声音列表某页中，用户对于声音的付费情况
     *
     * @param userId
     * @param albumId
     * @param trackIdList 需要判断购买请求声音id集合（从用户查询专辑声音分页）
     * @return
     */
    @Override
    public Map<Long, Integer> userIsPaidTrackList(Long userId, Long albumId, List<Long> trackIdList) {
        //1 根据用户id+专辑id查询专辑购买记录 如果有记录 将trackList购买情况返回
        LambdaQueryWrapper<UserPaidAlbum> userPaidAlbumLambdaQueryWrapper = new LambdaQueryWrapper<>();
        userPaidAlbumLambdaQueryWrapper.eq(UserPaidAlbum::getUserId, userId);
        userPaidAlbumLambdaQueryWrapper.eq(UserPaidAlbum::getAlbumId, albumId);
        Long count = userPaidAlbumMapper.selectCount(userPaidAlbumLambdaQueryWrapper);
        if (count > 0) {
            //用户购买过该专辑
            Map<Long, Integer> map = new HashMap<>();
            for (Long trackId : trackIdList) {
                //将购买结果设置为已购买
                map.put(trackId, 1);
            }
            return map;
        }
        //2 根据用户id+声音列表查询声音购买集合记录，那些购买那些没购买
        LambdaQueryWrapper<UserPaidTrack> userPaidTrackLambdaQueryWrapper = new LambdaQueryWrapper<>();
        userPaidTrackLambdaQueryWrapper.eq(UserPaidTrack::getUserId, userId);
        userPaidTrackLambdaQueryWrapper.in(UserPaidTrack::getTrackId, trackIdList);
        List<UserPaidTrack> userPaidTrackList = userPaidTrackMapper.selectList(userPaidTrackLambdaQueryWrapper);
        if (CollectionUtil.isEmpty(userPaidTrackList)) {
            //专辑当前页包含的声音一个都没购买
            Map<Long, Integer> map = new HashMap<>();
            for (Long track : trackIdList) {
                map.put(track, 0);
            }
            return map;
        }

        //2.2 用户有购买过声音，判断哪些是购买哪些未购买
        //得到用户已购买的声音的id
        List<Long> userPaidTrackIdList = userPaidTrackList.stream().map(UserPaidTrack::getTrackId).collect(Collectors.toList());
        //遍历待检测的声音id列表，判断已购买的声音列表中是否包含声音id
        Map<Long, Integer> hashMap = new HashMap<>();
        for (Long trackId : trackIdList) {
            if (userPaidTrackIdList.contains(trackId)) {
                hashMap.put(trackId, 1);
            } else {
                hashMap.put(trackId, 0);
            }
        }
        return hashMap;
    }

    /**
     * 判断用户是否购买过指定专辑
     *
     * @param albumId
     * @return
     */
    @Override
    public Boolean isPaidAlbum(Long userId, Long albumId) {
        LambdaQueryWrapper<UserPaidAlbum> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(UserPaidAlbum::getUserId, userId);
        queryWrapper.eq(UserPaidAlbum::getAlbumId, albumId);
        Long count = userPaidAlbumMapper.selectCount(queryWrapper);
        return count > 0;
    }


    /**
     * 根据专辑id+用户ID获取用户已购买声音id列表
     *
     * @param albumId
     * @return
     */
    @Override
    public List<Long> getUserPaidTrackList(Long albumId, Long userId) {
        LambdaQueryWrapper<UserPaidTrack> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(UserPaidTrack::getUserId, userId);
        queryWrapper.eq(UserPaidTrack::getAlbumId, albumId);
        List<UserPaidTrack> userPaidTracks = userPaidTrackMapper.selectList(queryWrapper);
        if (CollectionUtil.isNotEmpty(userPaidTracks)) {
            //获取已购声音列表
            List<Long> userPaidTrackList = userPaidTracks.stream().map(UserPaidTrack::getTrackId).collect(Collectors.toList());
            return userPaidTrackList;
        }
        return null;
    }

    /**
     * 处理用户购买记录
     * 1 处理声音购买记录-根据订单编号避免重复增加购买记录
     * 2 处理专辑购买记录-根据订单编号避免重复增加购买记录
     * 3 处理会员购买记录
     * -根据订单编号避免重复购买记录
     * -修改用户表vip状态以及失效时间
     *
     * @param userPaidRecordVo
     */
    @Transactional(rollbackFor = Exception.class)
    @Override
    public void saveUserPayRecord(UserPaidRecordVo userPaidRecordVo) {
        //1 处理声音购买记录-根据订单编号避免重复增加购买记录
        if (SystemConstant.ORDER_ITEM_TYPE_TRACK.equals(userPaidRecordVo.getItemType())) {
            //1.1 根据订单编号避免重复增加购买记录
            LambdaQueryWrapper<UserPaidTrack> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(UserPaidTrack::getOrderNo, userPaidRecordVo.getOrderNo());
            Long count = userPaidTrackMapper.selectCount(queryWrapper);
            if (count > 0) {
                return;
            }
            //1.2 构建新增购买记录
            //1.3 远程调用专辑服务获取声音所属的专辑
            TrackInfo trackInfo = albumFeignClient.getTrackInfo(userPaidRecordVo.getItemIdList().get(0)).getData();
            userPaidRecordVo.getItemIdList().forEach(trackId -> {
                UserPaidTrack userPaidTrack = new UserPaidTrack();
                userPaidTrack.setOrderNo(userPaidRecordVo.getOrderNo());
                userPaidTrack.setUserId(userPaidRecordVo.getUserId());
                userPaidTrack.setAlbumId(trackInfo.getAlbumId());
                userPaidTrack.setTrackId(trackId);
                userPaidTrackMapper.insert(userPaidTrack);
            });
        }

        //2 处理专辑购买记录-根据订单编号避免重复增加购买记录
        //3 处理会员购买记录
        //3.1 根据订单编号避免重复购买记录
        //3.2 修改用户表vip状态以及失效时间
    }
}
