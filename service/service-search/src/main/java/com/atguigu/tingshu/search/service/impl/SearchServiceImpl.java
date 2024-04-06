package com.atguigu.tingshu.search.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.lang.Assert;
import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.SortOrder;
import co.elastic.clients.elasticsearch._types.query_dsl.BoolQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.NestedQuery;
import co.elastic.clients.elasticsearch.core.SearchRequest;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.search.Hit;
import com.atguigu.tingshu.album.AlbumFeignClient;
import com.atguigu.tingshu.model.album.AlbumAttributeValue;
import com.atguigu.tingshu.model.album.AlbumInfo;
import com.atguigu.tingshu.model.album.BaseCategoryView;
import com.atguigu.tingshu.model.search.AlbumInfoIndex;
import com.atguigu.tingshu.model.search.AttributeValueIndex;
import com.atguigu.tingshu.query.search.AlbumIndexQuery;
import com.atguigu.tingshu.search.repository.AlbumInfoIndexRepository;
import com.atguigu.tingshu.search.service.SearchService;
import com.atguigu.tingshu.user.client.UserFeignClient;
import com.atguigu.tingshu.vo.search.AlbumInfoIndexVo;
import com.atguigu.tingshu.vo.search.AlbumSearchResponseVo;
import com.atguigu.tingshu.vo.user.UserInfoVo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.util.List;
import java.util.Map;
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

    @Autowired
    private ElasticsearchClient elasticsearchClient;

    private static final String index_name = "albuminfo";


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

        String albumInfo = "albuminfo";
        double hotScore = num1 * 0.1 + num2 * 0.5 + num3 * 0.4 + num4 * 0.4;
        albumInfoIndex.setHotScore(hotScore);
        //5 调用持久层新增文档
        albumInfoIndexRepository.save(albumInfoIndex);
    }

    /**
     * 下架专辑，该接口仅用于测试
     *
     * @param albumId
     * @return
     */
    @Override
    public void lowerAlbum(Long albumId) {
        albumInfoIndexRepository.deleteById(albumId);
    }


    /**
     * 根据关键字，分类id，属性/属性值检索专辑
     *
     * @param queryVo
     * @return
     */
    @Override
    public AlbumSearchResponseVo search(AlbumIndexQuery queryVo) {
        try {
            //1 创建一个检索的请求对象
            SearchRequest searchRequest = this.buildDSL(queryVo);

            //2 执行检索
            SearchResponse<AlbumInfoIndex> response = elasticsearchClient.search(searchRequest, AlbumInfoIndex.class);

            //3解析es响应数据
            return this.parseResult(response, queryVo);
        } catch (IOException e) {
            log.error("【搜索服务】检索专辑异常：{}", e);
            throw new RuntimeException();
        }
    }


    /**
     * 根据检索条件构建请求对象
     *
     * @param queryVo
     * @return
     */
    @Override
    public SearchRequest buildDSL(AlbumIndexQuery queryVo) {
        String keyword = queryVo.getKeyword();
        //1 创建检索请求构建器对象，指定索引库名称
        SearchRequest.Builder searchRequestBuilder = new SearchRequest.Builder();
        searchRequestBuilder.index(index_name);

        //2 设置查询条件
        BoolQuery.Builder allBoolQueryBuilder = new BoolQuery.Builder();
        //2.1 设置关键字查询 采用bool查询
        //2.1.1 创建关键字查询的bool查询对象
        if (StringUtils.hasText(keyword)) {
            BoolQuery.Builder keyWordBoolQueryBuilder = new BoolQuery.Builder();
            keyWordBoolQueryBuilder.should(s -> s.match(m -> m.field("albumTitle").query(keyword)));
            keyWordBoolQueryBuilder.should(s -> s.match(m -> m.field("albumIntro").query(keyword)));
            keyWordBoolQueryBuilder.should(s -> s.term(t -> t.field("announcerName").value(keyword)));
            //2.1.2 将关键字的查询对象放到最大bool查询中
            allBoolQueryBuilder.must(keyWordBoolQueryBuilder.build()._toQuery());
        }
        //2.2 设置1，2，3 级 分类数据过滤 使用filter不会对文档进行算分，并且进行缓存命中的数据
        if (queryVo.getCategory1Id() != null) {
            allBoolQueryBuilder.filter(f -> f.term(t -> t.field("category1Id").value(queryVo.getCategory1Id())));
        }
        if (queryVo.getCategory2Id() != null) {
            allBoolQueryBuilder.filter(f -> f.term(t -> t.field("category2Id").value(queryVo.getCategory2Id())));
        }
        if (queryVo.getCategory3Id() != null) {
            allBoolQueryBuilder.filter(f -> f.term(t -> t.field("category3Id").value(queryVo.getCategory3Id())));
        }
        //2.3 设置若干项属性值过滤条件
        List<String> attributeList = queryVo.getAttributeList();
        if (CollectionUtil.isNotEmpty(attributeList)) {
            for (String attribute : attributeList) {
                String[] split = attribute.split(":");
                if (split != null && split.length == 2) {
                    String attrId = split[0];
                    String attrValueId = split[1];
                    NestedQuery nestedQuery = NestedQuery.of(
                            o -> o.path("attributeValueIndexList")
                                    .query(q -> q.bool(
                                            b -> b.must(
                                                    m -> m.term(t -> t.field("attributeValueIndexList.attributeId").value(attrId))
                                            ).must(
                                                    m -> m.term(t -> t.field("attributeValueIndexList.valueId").value(attrValueId))
                                            )
                                    ))
                    );
                    allBoolQueryBuilder.filter(nestedQuery._toQuery());
                }
            }
        }

        searchRequestBuilder.query(allBoolQueryBuilder.build()._toQuery());

        //3 设置分页条件
        int from = (queryVo.getPageNo() - 1) * queryVo.getPageSize();
        searchRequestBuilder.from(from).size(queryVo.getPageSize());

        //4 设置高亮关键字
        if (StringUtils.hasText(keyword)) {
            searchRequestBuilder.highlight(h -> h.fields("albumTitle", hf -> hf.preTags("<font style = 'color:red'>").postTags("</font>")));
        }

        //5 设置排序
        if (StringUtils.hasText(queryVo.getOrder())) {
            //5.1 获取排序条件，按照冒号进行字符串分割
            String[] split = queryVo.getOrder().split(":");
            if (split != null && split.length == 2) {
                String orderFile = "";
                //5.2 获取排序字段，排序方式
                switch (split[0]) {
                    case "1":
                        orderFile = "hotScore";
                        break;
                    case "2":
                        orderFile = "playStatNum";
                        break;
                    case "3":
                        orderFile = "createTime";
                        break;
                }
                String finalOrderFile = orderFile;
                searchRequestBuilder.sort(s -> s.field(fs -> fs.field(finalOrderFile).order("asc".equals(split[1]) ? SortOrder.Asc : SortOrder.Desc)));

            }
        }

        //6 设置检索以及想响应es字段
        searchRequestBuilder.source(s -> s.filter(f -> f.excludes("attributeValueIndexList", "category1Id", "category2Id", "category3Id")));

        return searchRequestBuilder.build();
    }


    /**
     * 解析es响应结果，封装自定义结果
     *
     * @param response
     * @param queryVo
     * @return
     */
    @Override
    public AlbumSearchResponseVo parseResult(SearchResponse<AlbumInfoIndex> response, AlbumIndexQuery queryVo) {
        AlbumSearchResponseVo vo = new AlbumSearchResponseVo();
        //1 封装分页信息
        long total = response.hits().total().value();
        Integer pageSize = queryVo.getPageSize();
        long totalpage = total % pageSize == 0 ? total / pageSize : total / pageSize + 1;
        vo.setPageSize(pageSize);
        vo.setTotal(total);
        vo.setTotalPages(totalpage);
        vo.setPageNo(queryVo.getPageNo());
        //2 封装业务数据
        List<Hit<AlbumInfoIndex>> hits = response.hits().hits();
        if (CollectionUtil.isNotEmpty(hits)){
            List<AlbumInfoIndexVo> albumInfoIndexVoList = hits.stream().map(hit -> {
                //专辑对象
                AlbumInfoIndex albumInfoIndex = hit.source();
                //处理高亮
                Map<String, List<String>> highlightMap = hit.highlight();
                if (CollectionUtil.isNotEmpty(highlightMap)) {
                    if (highlightMap.containsKey("albumTitle")) {
                        //获取高亮关键字
                        String albumTitle = highlightMap.get("albumTitle").get(0);
                        albumInfoIndex.setAlbumTitle(albumTitle);
                    }
                }
                return BeanUtil.copyProperties(albumInfoIndex, AlbumInfoIndexVo.class);
            }).collect(Collectors.toList());

            vo.setList(albumInfoIndexVoList);
        }
        return vo;
    }
}
