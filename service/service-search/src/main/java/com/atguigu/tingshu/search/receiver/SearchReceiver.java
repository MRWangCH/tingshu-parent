package com.atguigu.tingshu.search.receiver;

import com.atguigu.tingshu.common.constant.KafkaConstant;
import com.atguigu.tingshu.search.service.SearchService;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Slf4j
@Component
public class SearchReceiver {


    @Autowired
    private SearchService searchService;

    /**
     * 专辑上架消息
     * @param record
     */
    @KafkaListener(topics = KafkaConstant.QUEUE_ALBUM_UPPER)
    public void albumUpper(ConsumerRecord<String, String> record){
        String value = record.value();
        if (StringUtils.hasText(value)){
            log.info("[搜索服务]监听到了专辑上架消息：专辑id：{}", value);
            searchService.upperAlbum(Long.valueOf(value));
        }
    }

    /**
     * 专辑下架消息
     * @param record
     */
    @KafkaListener(topics = KafkaConstant.QUEUE_ALBUM_LOWER)
    public void albumLower(ConsumerRecord<String, String> record){
        String value = record.value();
        log.info("[搜索服务]监听到了专辑下架消息，专辑id：{}", value);
        searchService.lowerAlbum(Long.valueOf(value));
    }
}
