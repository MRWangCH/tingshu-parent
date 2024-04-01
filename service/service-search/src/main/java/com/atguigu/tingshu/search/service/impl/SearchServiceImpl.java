package com.atguigu.tingshu.search.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.lang.Assert;
import com.atguigu.tingshu.album.AlbumFeignClient;
import com.atguigu.tingshu.common.result.Result;
import com.atguigu.tingshu.model.album.AlbumAttributeValue;
import com.atguigu.tingshu.model.album.AlbumInfo;
import com.atguigu.tingshu.model.album.BaseCategoryView;
import com.atguigu.tingshu.model.search.AlbumInfoIndex;
import com.atguigu.tingshu.model.search.AttributeValueIndex;
import com.atguigu.tingshu.search.repository.AlbumInfoIndexRepository;
import com.atguigu.tingshu.search.service.SearchService;
import com.atguigu.tingshu.user.client.UserFeignClient;
import com.atguigu.tingshu.vo.user.UserInfoVo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Random;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.stream.Collectors;


@Slf4j
@Service
@SuppressWarnings({"all"})
public class SearchServiceImpl implements SearchService {

    @Autowired
    private AlbumInfoIndexRepository albumInfoIndexRepository;

    @Autowired
    private AlbumFeignClient albumFeignClient;

    @Autowired
    private UserFeignClient userFeignClient;

    @Autowired
    private ThreadPoolExecutor threadPoolExecutor;

    /**
     * 上架专辑
     *
     * @param albumId
     * @return
     */
    @Override
    public void upperAlbum(Long albumId) {
        //1 新建索引库文档对象
        AlbumInfoIndex albumInfoIndex = new AlbumInfoIndex();

        //2 远程调用专辑服务获取专辑以及专辑属性列表信息，为索引库文档对象中的相关属性赋值
        CompletableFuture<AlbumInfo> albumInfoCompletableFuture = CompletableFuture.supplyAsync(() -> {
            AlbumInfo albumInfo = albumFeignClient.getAlbumInfo(albumId).getData();
            Assert.notNull(albumInfo, "专辑信息为空！");
            BeanUtil.copyProperties(albumInfo, albumInfoIndex);
            //2.2 处理专辑属性值列表
            List<AlbumAttributeValue> albumAttributeValueVoList = albumInfo.getAlbumAttributeValueVoList();
            if (CollectionUtil.isNotEmpty(albumAttributeValueVoList)) {
                //将List<albumAttributeValueVoList>转成List<attributeValueIndexList>
                List<AttributeValueIndex> collect = albumAttributeValueVoList.stream().map(a -> {
                    return BeanUtil.copyProperties(a, AttributeValueIndex.class);
                }).collect(Collectors.toList());
                albumInfoIndex.setAttributeValueIndexList(collect);
            }
            return albumInfo;
        }, threadPoolExecutor);


        //3 远程调用用户服务获取用户信息，为索引库文档对象中的相关属性赋值
        CompletableFuture<Void> userInfoComplatableFuture = albumInfoCompletableFuture.thenAcceptAsync(albumInfo -> {
            UserInfoVo userInfo = userFeignClient.getUserInfoVoByUserId(albumInfo.getUserId()).getData();
            Assert.notNull(userInfo, "主播信息不存在！");
            albumInfoIndex.setAnnouncerName(userInfo.getNickname());
        }, threadPoolExecutor);

        //4 远程调用专辑服务获取分类信息
        CompletableFuture<Void> categoryComplatableFuture = albumInfoCompletableFuture.thenAcceptAsync(albumInfo -> {
            BaseCategoryView categoryView = albumFeignClient.getCategoryViewBy3Id(albumInfo.getCategory3Id()).getData();
            Assert.notNull(categoryView, "分类信息不存在！");
            albumInfoIndex.setCategory1Id(categoryView.getCategory1Id());
            albumInfoIndex.setCategory2Id(categoryView.getCategory2Id());
        }, threadPoolExecutor);

        CompletableFuture.allOf(albumInfoCompletableFuture, userInfoComplatableFuture, categoryComplatableFuture).join();

        //4 手动随机生成统计值，为索引库文档对象中的相关属性赋值
        int num1 = new Random().nextInt(1000);
        int num2 = new Random().nextInt(400);
        int num3 = new Random().nextInt(500);
        int num4 = new Random().nextInt(800);
        albumInfoIndex.setPlayStatNum(num1);
        albumInfoIndex.setSubscribeStatNum(num2);
        albumInfoIndex.setBuyStatNum(num3);
        albumInfoIndex.setCommentStatNum(num4);
        double hotScore = num1 * 0.1 + num2 * 0.5 + num3 * 0.4 + num4 * 0.4;
        albumInfoIndex.setHotScore(hotScore);
        //5 调用持久层新增文档
        albumInfoIndexRepository.save(albumInfoIndex);
    }

    /**
     * 下架专辑，该接口仅用于测试
     * @param albumId
     * @return
     */
    @Override
    public void lowerAlbum(Long albumId) {
        albumInfoIndexRepository.deleteById(albumId);
    }
}
