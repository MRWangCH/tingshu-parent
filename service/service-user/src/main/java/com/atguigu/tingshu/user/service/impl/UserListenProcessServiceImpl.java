package com.atguigu.tingshu.user.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.IdUtil;
import com.alibaba.fastjson.JSON;
import com.atguigu.tingshu.common.constant.KafkaConstant;
import com.atguigu.tingshu.common.constant.RedisConstant;
import com.atguigu.tingshu.common.constant.SystemConstant;
import com.atguigu.tingshu.common.service.KafkaService;
import com.atguigu.tingshu.common.util.AuthContextHolder;
import com.atguigu.tingshu.common.util.MongoUtil;
import com.atguigu.tingshu.model.user.UserListenProcess;
import com.atguigu.tingshu.user.service.UserListenProcessService;
import com.atguigu.tingshu.vo.album.TrackStatMqVo;
import com.atguigu.tingshu.vo.user.UserListenProcessVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Date;
import java.util.concurrent.TimeUnit;

@Service
@SuppressWarnings({"all"})
public class UserListenProcessServiceImpl implements UserListenProcessService {

    @Autowired
    private MongoTemplate mongoTemplate;

    @Autowired
    private RedisTemplate redisTemplate;

    @Autowired
    private KafkaService kafkaService;

    /**
     * 查询当前用户入参声音上次播放的进度
     *
     * @param trackId
     * @return
     */
    @Override
    public BigDecimal getTrackBreakSecond(Long userId, Long trackId) {
        //1 封装查询参数：用户id+声音id
        Query query = new Query();
        query.addCriteria(Criteria.where("userId").is(userId).and("trackId").is(trackId));
        //2 动态获取当前用户集合名称
        String collectionName = MongoUtil.getCollectionName(MongoUtil.MongoCollectionEnum.USER_LISTEN_PROCESS, userId);
        // 执行查询MongoDB中存入用户声音播放进度
        UserListenProcess userListenProcess = mongoTemplate.findOne(query, UserListenProcess.class, collectionName);
        if (userListenProcess != null) {
            return userListenProcess.getBreakSecond();
        }
        return new BigDecimal("0.00");
    }

    /**
     * 更新声音播放进度
     *
     * @param userListenProcessVo
     * @return
     */
    @Override
    public void updateListenProcess(UserListenProcessVo userListenProcessVo) {
        Long userId = AuthContextHolder.getUserId();
        //1 根据用户id+声音id查询播放进度
        Query query = new Query(Criteria.where("userId").is(userId).and("trackId").is(userListenProcessVo.getTrackId()));
        String collectionName = MongoUtil.getCollectionName(MongoUtil.MongoCollectionEnum.USER_LISTEN_PROCESS, userId);
        UserListenProcess listenProcess = mongoTemplate.findOne(query, UserListenProcess.class, collectionName);
        if (listenProcess != null) {
            //2 如果进度存在，直接修改播放进度秒数
            listenProcess.setUpdateTime(new Date());
            listenProcess.setBreakSecond(userListenProcessVo.getBreakSecond());
            mongoTemplate.save(listenProcess, collectionName);
        } else {
            //3 如果进度不存在，新增播放进度文档到MongoDB
            listenProcess = new UserListenProcess();
            BeanUtil.copyProperties(userListenProcessVo, listenProcess);
            listenProcess.setUserId(userId);
            listenProcess.setIsShow(1);
            listenProcess.setCreateTime(new Date());
            listenProcess.setUpdateTime(new Date());
            mongoTemplate.save(listenProcess, collectionName);
        }

        //4 基于Kafka异步通知声音/专辑的统计信息
        //4.1 避免用户恶意刷播放量，一天同一用户对同一声音只能统计一次
        String key = RedisConstant.USER_TRACK_REPEAT_STAT_PREFIX + userId + ":" + userListenProcessVo.getTrackId();
        Boolean flag = redisTemplate.opsForValue().setIfAbsent(key, userId, 24, TimeUnit.HOURS);
        //4.2 封装更新统计信息的MQ对象，将发送到”声音统计“的topic中，由专辑微服务作为消费者监听更新声音专辑统计信息
        if (flag) {
            TrackStatMqVo mqVo = new TrackStatMqVo();
            //为了避免mq服务器对同一个消息进行多次投递，产生业务标识
            mqVo.setBusinessNo(RedisConstant.BUSINESS_PREFIX + IdUtil.fastSimpleUUID());
            mqVo.setStatType(SystemConstant.TRACK_STAT_PLAY);
            mqVo.setAlbumId(userListenProcessVo.getAlbumId());
            mqVo.setTrackId(userListenProcessVo.getTrackId());
            mqVo.setCount(1);
            kafkaService.sendMessage(KafkaConstant.QUEUE_TRACK_STAT_UPDATE, JSON.toJSONString(mqVo));
        }
    }
}
