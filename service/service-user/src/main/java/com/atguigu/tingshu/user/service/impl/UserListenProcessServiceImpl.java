package com.atguigu.tingshu.user.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.atguigu.tingshu.common.util.AuthContextHolder;
import com.atguigu.tingshu.common.util.MongoUtil;
import com.atguigu.tingshu.model.user.UserListenProcess;
import com.atguigu.tingshu.user.service.UserListenProcessService;
import com.atguigu.tingshu.vo.user.UserListenProcessVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Date;

@Service
@SuppressWarnings({"all"})
public class UserListenProcessServiceImpl implements UserListenProcessService {

    @Autowired
    private MongoTemplate mongoTemplate;

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
        return null;
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

        //4 TODO 基于Kafka异步通知声音/专辑的统计信息
    }
}
