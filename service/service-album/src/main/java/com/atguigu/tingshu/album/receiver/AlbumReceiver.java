package com.atguigu.tingshu.album.receiver;

import com.alibaba.fastjson.JSON;
import com.atguigu.tingshu.album.service.TrackInfoService;
import com.atguigu.tingshu.common.constant.KafkaConstant;
import com.atguigu.tingshu.common.constant.RedisConstant;
import com.atguigu.tingshu.vo.album.TrackStatMqVo;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.concurrent.TimeUnit;


@Slf4j
@Component
public class AlbumReceiver {

    @Autowired
    private RedisTemplate redisTemplate;

    @Autowired
    private TrackInfoService trackInfoService;

    /**
     * 监听更新声音统计的话题消息完成更新统计信息
     *
     * @param record
     */
    @KafkaListener(topics = KafkaConstant.QUEUE_TRACK_STAT_UPDATE)
    public void updateStat(ConsumerRecord<String, String> record) {
        String value = record.value();
        if (StringUtils.hasText(value)) {
            log.info("[专辑服务]，监听到更新统计信息消息 {}", value);
            //1 将收到的mq消息转成Java对象
            TrackStatMqVo mqVo = JSON.parseObject(value, TrackStatMqVo.class);
            //2 进行幂等处理，一个消息只能被处理一次（网络抖动，MQ服务器本身对同一消息进行多次投递）
            String key = mqVo.getBusinessNo();
            Boolean flag = redisTemplate.opsForValue().setIfAbsent(key, null, 1, TimeUnit.HOURS);
            if (flag) {
                //第一次处理该业务
                trackInfoService.updateStat(mqVo.getAlbumId(), mqVo.getTrackId(), mqVo.getStatType(), mqVo.getCount());
            }
        }
    }
}
