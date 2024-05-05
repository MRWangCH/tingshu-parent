package com.atguigu.tingshu.user.service.impl;

import com.atguigu.tingshu.common.util.MongoUtil;
import com.atguigu.tingshu.model.user.UserListenProcess;
import com.atguigu.tingshu.user.service.UserListenProcessService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

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
        if (userListenProcess != null ) {
            return userListenProcess.getBreakSecond();
        }
        return null;
    }
}
