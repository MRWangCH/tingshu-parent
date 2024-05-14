package com.atguigu.tingshu.order.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.lang.Assert;
import cn.hutool.core.util.IdUtil;
import com.atguigu.tingshu.album.AlbumFeignClient;
import com.atguigu.tingshu.common.constant.RedisConstant;
import com.atguigu.tingshu.common.constant.SystemConstant;
import com.atguigu.tingshu.common.execption.GuiguException;
import com.atguigu.tingshu.common.result.Result;
import com.atguigu.tingshu.common.result.ResultCodeEnum;
import com.atguigu.tingshu.model.album.AlbumInfo;
import com.atguigu.tingshu.model.album.TrackInfo;
import com.atguigu.tingshu.model.order.OrderInfo;
import com.atguigu.tingshu.model.user.VipServiceConfig;
import com.atguigu.tingshu.order.helper.SignHelper;
import com.atguigu.tingshu.order.mapper.OrderInfoMapper;
import com.atguigu.tingshu.order.service.OrderInfoService;
import com.atguigu.tingshu.user.client.UserFeignClient;
import com.atguigu.tingshu.vo.order.OrderDerateVo;
import com.atguigu.tingshu.vo.order.OrderDetailVo;
import com.atguigu.tingshu.vo.order.OrderInfoVo;
import com.atguigu.tingshu.vo.order.TradeVo;
import com.atguigu.tingshu.vo.user.UserInfoVo;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
@Service
@SuppressWarnings({"all"})
public class OrderInfoServiceImpl extends ServiceImpl<OrderInfoMapper, OrderInfo> implements OrderInfoService {

    @Autowired
    private OrderInfoMapper orderInfoMapper;

    @Autowired
    private UserFeignClient userFeignClient;

    @Autowired
    private RedisTemplate redisTemplate;

    @Autowired
    private AlbumFeignClient albumFeignClient;


    /**
     * 订单结算页面渲染-数据汇总
     * 处理购买类型-vip会员
     * 处理购买类型-专辑
     * 处理购买类型-声音
     *
     * @return
     */
    @Override
    public OrderInfoVo trade(Long userId, TradeVo tradeVo) {
        //1 声明订单结算页中价格变量，订单明细，优惠信息变量-赋予初始值
        BigDecimal originalAmount = new BigDecimal("0.00");
        BigDecimal derateAmount = new BigDecimal("0.00");
        BigDecimal orderAmount = new BigDecimal("0.00");
        List<OrderDetailVo> orderDetailVoList = new ArrayList<>();
        List<OrderDerateVo> orderDerateVoList = new ArrayList<>();
        //2 远程调用用户微服务获取用户信息
        UserInfoVo userInfo = userFeignClient.getUserInfoVoByUserId(userId).getData();
        Assert.notNull(userInfo, "用户信息为空，请联系管理员");

        if (SystemConstant.ORDER_ITEM_TYPE_VIP.equals(tradeVo.getItemType())) {
            //3 处理购买类型-vip会员
            //3.1 远程调用用户服务-根据vip套餐id查询套餐信息
            VipServiceConfig vipServiceConfig = userFeignClient.getVipServiceConfig(tradeVo.getItemId()).getData();
            Assert.notNull(vipServiceConfig, "VIP套餐不存在，请联系管理员");
            //3.2 动态计算vip会员价格：原价、订单价、减免价
            originalAmount = vipServiceConfig.getPrice();
            orderAmount = vipServiceConfig.getDiscountPrice();
            derateAmount = originalAmount.subtract(orderAmount);

            //3.3 封装订单明细集合（vip套餐信息）
            OrderDetailVo orderDetailVo = new OrderDetailVo();
            orderDetailVo.setItemId(tradeVo.getItemId());
            orderDetailVo.setItemName(vipServiceConfig.getName());
            orderDetailVo.setItemUrl(vipServiceConfig.getImageUrl());
            //设置原价
            orderDetailVo.setItemPrice(originalAmount);
            orderDetailVoList.add(orderDetailVo);

            //3.4 封装订单优惠集合（vip优惠信息）
            OrderDerateVo orderDerateVo = new OrderDerateVo();
            orderDerateVo.setDerateType(SystemConstant.ORDER_DERATE_VIP_SERVICE_DISCOUNT);
            orderDerateVo.setDerateAmount(derateAmount);
            orderDerateVo.setRemarks("VIP折扣价" + derateAmount);
            orderDerateVoList.add(orderDerateVo);

        } else if (SystemConstant.ORDER_ITEM_TYPE_ALBUM.equals(tradeVo.getItemType())) {
            //4 处理购买类型-专辑
            //4.1 远程调用用户服务 判断用户是否购买该专辑
            Boolean ifBuy = userFeignClient.isPaidAlbum(tradeVo.getItemId()).getData();
            if (ifBuy) {
                throw new GuiguException(ResultCodeEnum.REPEAT_BUY_ERROR);
            }
            //4.2 远程调用专辑服务 获取专辑信息
            AlbumInfo albumInfo = albumFeignClient.getAlbumInfo(tradeVo.getItemId()).getData();
            Assert.notNull(albumInfo, "专辑不存在!");
            //4.3 动态计算专辑价格：原价、订单价、减免价
            orderAmount = originalAmount;
            originalAmount = albumInfo.getPrice();
            if (userInfo.getIsVip().intValue() == 1 && userInfo.getVipExpireTime().after(DateUtil.date())) {
                //是vip
                if (albumInfo.getVipDiscount().intValue() != -1) {
                    orderAmount = originalAmount.multiply(albumInfo.getVipDiscount()).divide(new BigDecimal(10));
                    derateAmount = originalAmount.subtract(orderAmount);
                }
            } else {
                if (albumInfo.getDiscount().intValue() != -1) {
                    //普通用户折扣
                    orderAmount = originalAmount.multiply(albumInfo.getDiscount()).divide(new BigDecimal("10"));
                    derateAmount = originalAmount.subtract(orderAmount);
                }

            }
            //4.4 封装订单明细集合（专辑信息）
            OrderDetailVo orderDetailVo = new OrderDetailVo();
            orderDetailVo.setItemId(tradeVo.getItemId());
            orderDetailVo.setItemName(albumInfo.getAlbumTitle());
            orderDetailVo.setItemUrl(albumInfo.getCoverUrl());
            orderDetailVo.setItemPrice(originalAmount);
            orderDetailVoList.add(orderDetailVo);

            //4.5 封装订单优惠集合（专辑信息）
            OrderDerateVo orderDerateVo = new OrderDerateVo();
            orderDerateVo.setDerateType(SystemConstant.ORDER_DERATE_ALBUM_DISCOUNT);
            orderDerateVo.setDerateAmount(derateAmount);
            orderDerateVo.setRemarks("专辑优惠：" + derateAmount);
            orderDerateVoList.add(orderDerateVo);


        } else if (SystemConstant.ORDER_ITEM_TYPE_TRACK.equals(tradeVo.getItemType())) {
            //5 处理购买类型-声音
            //5.1 远程调用专辑服务-选择声音id+数量得到待购声音列表（排除已购声音列表）
            List<TrackInfo> waitTrackInfoList = albumFeignClient.getWaitPayTrackInfoList(tradeVo.getItemId(), tradeVo.getTrackCount()).getData();
            //5.2 远程调用专辑服务获取专辑信息得到声音单价
            AlbumInfo albumInfo = albumFeignClient.getAlbumInfo(waitTrackInfoList.get(0).getAlbumId()).getData();
            BigDecimal albumInfoPrice = albumInfo.getPrice();
            //5.3 计算价格：原价，订单加，声音不支持折扣
            originalAmount = albumInfoPrice.multiply(BigDecimal.valueOf(waitTrackInfoList.size()));
            orderAmount = originalAmount;
            //5.4 遍历待购声音列表构建订单明细集合
            orderDetailVoList = waitTrackInfoList.stream().map(trackInfo -> {
                OrderDetailVo orderDetailVo = new OrderDetailVo();
                orderDetailVo.setItemId(trackInfo.getId());
                orderDetailVo.setItemName(trackInfo.getTrackTitle());
                orderDetailVo.setItemUrl(trackInfo.getCoverUrl());
                orderDetailVo.setItemPrice(albumInfoPrice);
                return orderDetailVo;
            }).collect(Collectors.toList());

        }

        //构建渲染订单结算所需对象OrderInfoVo：包含价格、订单明细表、优惠列表、其他属性
        OrderInfoVo orderInfoVo = new OrderInfoVo();
        //针对本次请求产生流水号，订单被多次重复提交。（提交后回退到页面继续提交）将流水号存入到redis设置5分钟过期时间
        String tradeKey = RedisConstant.ORDER_TRADE_NO_PREFIX + userId;
        String tradeNo = IdUtil.fastSimpleUUID();
        redisTemplate.opsForValue().set(tradeKey, tradeNo, 5, TimeUnit.MINUTES);
        orderInfoVo.setTradeNo(tradeNo);
        orderInfoVo.setItemType(tradeVo.getItemType());

        orderInfoVo.setOriginalAmount(originalAmount);
        orderInfoVo.setDerateAmount(derateAmount);
        orderInfoVo.setOrderAmount(orderAmount);

        orderInfoVo.setOrderDetailVoList(orderDetailVoList);
        orderInfoVo.setOrderDerateVoList(orderDerateVoList);
        //针对本次请求所有参数进行签名 --> md5（参数）= 签名值，防止数据传输过程中被篡改
        orderInfoVo.setTimestamp(DateUtil.current());
        Map<String, Object> map = BeanUtil.beanToMap(orderInfoVo, false, true);
        String sign = SignHelper.getSign(map);
        orderInfoVo.setSign(sign);


        return orderInfoVo;
    }
}
