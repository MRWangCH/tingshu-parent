package com.atguigu.tingshu.order.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.lang.Assert;
import cn.hutool.core.util.IdUtil;
import com.alibaba.fastjson.JSON;
import com.atguigu.tingshu.account.AccountFeignClient;
import com.atguigu.tingshu.album.AlbumFeignClient;
import com.atguigu.tingshu.common.constant.KafkaConstant;
import com.atguigu.tingshu.common.constant.RedisConstant;
import com.atguigu.tingshu.common.constant.SystemConstant;
import com.atguigu.tingshu.common.execption.GuiguException;
import com.atguigu.tingshu.common.result.Result;
import com.atguigu.tingshu.common.result.ResultCodeEnum;
import com.atguigu.tingshu.common.service.KafkaService;
import com.atguigu.tingshu.model.album.AlbumInfo;
import com.atguigu.tingshu.model.album.TrackInfo;
import com.atguigu.tingshu.model.order.OrderDerate;
import com.atguigu.tingshu.model.order.OrderDetail;
import com.atguigu.tingshu.model.order.OrderInfo;
import com.atguigu.tingshu.model.user.VipServiceConfig;
import com.atguigu.tingshu.order.helper.SignHelper;
import com.atguigu.tingshu.order.mapper.OrderDerateMapper;
import com.atguigu.tingshu.order.mapper.OrderDetailMapper;
import com.atguigu.tingshu.order.mapper.OrderInfoMapper;
import com.atguigu.tingshu.order.service.OrderInfoService;
import com.atguigu.tingshu.user.client.UserFeignClient;
import com.atguigu.tingshu.vo.account.AccountLockResultVo;
import com.atguigu.tingshu.vo.account.AccountLockVo;
import com.atguigu.tingshu.vo.order.OrderDerateVo;
import com.atguigu.tingshu.vo.order.OrderDetailVo;
import com.atguigu.tingshu.vo.order.OrderInfoVo;
import com.atguigu.tingshu.vo.order.TradeVo;
import com.atguigu.tingshu.vo.user.UserInfoVo;
import com.atguigu.tingshu.vo.user.UserPaidRecordVo;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.*;
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

    @Autowired
    private OrderDetailMapper orderDetailMapper;

    @Autowired
    private OrderDerateMapper orderDerateMapper;

    @Autowired
    private AccountFeignClient accountFeignClient;

    @Autowired
    private KafkaService kafkaService;


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
        //所有相乘计算保留2位小数
        MathContext mathContext = new MathContext(2, RoundingMode.HALF_UP);
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
                    orderAmount = originalAmount.multiply(albumInfo.getVipDiscount()).divide(new BigDecimal(10), 2, RoundingMode.HALF_UP);
                    derateAmount = originalAmount.subtract(orderAmount);
                }
            } else {
                if (albumInfo.getDiscount().intValue() != -1) {
                    //普通用户折扣
                    orderAmount = originalAmount.multiply(albumInfo.getDiscount()).divide(new BigDecimal("10"), 2, RoundingMode.HALF_UP);
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

    /**
     * 订单提交（余额付款）
     *
     * @param userId
     * @param orderInfoVo
     * @return
     */
    @Transactional(rollbackFor = Exception.class)
    @Override
    public Map<String, String> submitOrder(Long userId, OrderInfoVo orderInfoVo) {
        //1 请求数据验签，防止数据篡改
        //渲染结算页面时将payway排除，提交的vo中包含支付方式，将支付方式移除
        Map<String, Object> paramMap = BeanUtil.beanToMap(orderInfoVo);
        paramMap.remove("payWay");
        SignHelper.checkSign(paramMap);
        //2 验证流水号，防止订单重复提交
        //通过流水号key查询redis中正确的流水号，与前端页面传过来的流水号比较
        String tradeKey = RedisConstant.ORDER_TRADE_NO_PREFIX + userId;
        //2.2 跟用户提交流水号比对 比对成功后，将流水号删除（保证原子性-lua脚本）成功，将流水号删除
        String script = "if(redis.call('get', KEYS[1]) == ARGV[1]) then return redis.call('del', KEYS[1]) else return 0 end";
        DefaultRedisScript<Boolean> redisScript = new DefaultRedisScript<>();
        redisScript.setScriptText(script);
        redisScript.setResultType(Boolean.class);
        Boolean flag = (Boolean) redisTemplate.execute(redisScript, Arrays.asList(tradeKey), orderInfoVo.getTradeNo());
        if (!flag) {
            throw new GuiguException(ResultCodeEnum.ORDER_SUBMIT_REPEAT);
        }
        //3 保存订单以及订单明细，订单优惠明细
        OrderInfo orderInfo = this.saveOrder(userId, orderInfoVo);
        //4 TODO 处理余额付款 判断支付类型：支付方式：1101-微信 1102-支付宝 1103-账户余额，VIP，声音，专辑都支持余额付款，声音仅支持余额付款
        if (SystemConstant.ORDER_PAY_ACCOUNT.equals(orderInfoVo.getPayWay())) {
            try {
                //4.1 远程调用账户服务，以及锁定可用余额
                AccountLockVo accountLockVo = new AccountLockVo();
                accountLockVo.setOrderNo(orderInfo.getOrderNo());
                accountLockVo.setUserId(userId);
                accountLockVo.setAmount(orderInfoVo.getOrderAmount());
                accountLockVo.setContent(orderInfoVo.getOrderDetailVoList().get(0).getItemName());
                Result<AccountLockResultVo> lockResult = accountFeignClient.checkAndLock(accountLockVo);
                if (200 != lockResult.getCode() || lockResult.getData() == null) {
                    log.error("[订单服务]远程调用[账户服务]锁定可用余额失败：业务状态码：{}，信息：{}", lockResult.getCode(), lockResult.getMessage());
                    throw new GuiguException(lockResult.getCode(), lockResult.getMessage());
                }
                //4.2 锁定成功默认为扣减成功，采用异步mq完成账户扣减
                kafkaService.sendMessage(KafkaConstant.QUEUE_ACCOUNT_MINUS, orderInfo.getOrderNo());
                //4.3 修改订单状态：已支付
                orderInfo.setOrderStatus(SystemConstant.ORDER_STATUS_PAID);
                orderInfoMapper.updateById(orderInfo);
                //4.4 采用MQ处理用户购买记录
                UserPaidRecordVo userPaidRecordVo = new UserPaidRecordVo();
                userPaidRecordVo.setOrderNo(orderInfo.getOrderNo());
                userPaidRecordVo.setUserId(userId);
                userPaidRecordVo.setItemType(orderInfoVo.getItemType());
                List<Long> itemIdList = orderInfoVo.getOrderDetailVoList().stream().map(OrderDetailVo::getItemId).collect(Collectors.toList());
                userPaidRecordVo.setItemIdList(itemIdList);
                kafkaService.sendMessage(KafkaConstant.QUEUE_USER_PAY_RECORD, JSON.toJSONString(userPaidRecordVo));

            } catch (Exception e) {
                //4.5 以上有异常的话，采用MQ回滚
                //以上操作：锁定，扣减，购买记录等业务代码异常，则基于MQ消息进行回滚
                //5 利用Kafka消息解锁
                kafkaService.sendMessage(KafkaConstant.QUEUE_ACCOUNT_UNLOCK, orderInfo.getOrderNo());
                // TODO 利用Kafka消息完成购买记录回滚（删除） 达到事务最终一致性
                throw new RuntimeException(e);
            }

        }
        //5 封装订单编号到map
        Map<String, String> map = new HashMap<>();
        map.put("orderNo", orderInfo.getOrderNo());
        return map;
    }

    /**
     * 保存订单以及订单明细，订单优惠明细
     *
     * @param userId
     * @param orderInfoVo
     * @return
     */
    @Override
    public OrderInfo saveOrder(Long userId, OrderInfoVo orderInfoVo) {
        //1 将订单相关信息封装成订单对象，向订单表新增一条记录
        OrderInfo orderInfo = BeanUtil.copyProperties(orderInfoVo, OrderInfo.class);
        //1.1 剩余属性赋值 用户id 订单标题 订单编号 订单状态（未支付）
        orderInfo.setUserId(userId);
        orderInfo.setOrderStatus(SystemConstant.ORDER_STATUS_UNPAID);
        orderInfo.setOrderTitle(orderInfoVo.getOrderDetailVoList().get(0).getItemName());
        //订单编号形式：当天YYYYMMDD+分布式id生成策略：雪花算法
        String orderNo = DateUtil.today().replaceAll("-", "") + IdUtil.getSnowflakeNextId();
        orderInfo.setOrderNo(orderNo);
        orderInfoMapper.insert(orderInfo);
        Long orderInfoId = orderInfo.getId();

        //2 将提交的订单明细封装为订单明细集合，批量向订单明细表中新增若干条记录
        List<OrderDetailVo> orderDetailVoList = orderInfoVo.getOrderDetailVoList();
        if (CollectionUtil.isNotEmpty(orderDetailVoList)) {
            orderDetailVoList.stream().forEach(orderDetailVo -> {
                OrderDetail orderDetail = BeanUtil.copyProperties(orderDetailVo, OrderDetail.class);
                orderDetail.setOrderId(orderInfoId);
                orderDetailMapper.insert(orderDetail);
            });
        }

        //3 将提交的优惠明细封装为优惠明细集合，批量向优惠明细表中新增若干条记录
        List<OrderDerateVo> orderDerateVoList = orderInfoVo.getOrderDerateVoList();
        if (CollectionUtil.isNotEmpty(orderDerateVoList)) {
            orderDerateVoList.stream().forEach(orderDerateVo -> {
                OrderDerate orderDerate = BeanUtil.copyProperties(orderDerateVo, OrderDerate.class);
                orderDerate.setOrderId(orderInfoId);
                orderDerateMapper.insert(orderDerate);
            });
        }
        return orderInfo;
    }


    /**
     * 获取订单信息
     *
     * @param userId
     * @param orderNo
     * @return
     */
    @Override
    public OrderInfo getOrderInfo(Long userId, String orderNo) {
        //1 根据订单编号+用户id查询订单信息
        LambdaQueryWrapper<OrderInfo> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(OrderInfo::getOrderNo, orderNo);
        queryWrapper.eq(OrderInfo::getUserId, userId);
        OrderInfo orderInfo = orderInfoMapper.selectOne(queryWrapper);
        if (orderInfo != null) {
            //2 根据订单id查询订单信息
            LambdaQueryWrapper<OrderDetail> detailQueryWrapper = new LambdaQueryWrapper<>();
            detailQueryWrapper.eq(OrderDetail::getOrderId, orderInfo.getId());
            List<OrderDetail> orderDetailList = orderDetailMapper.selectList(detailQueryWrapper);
            orderInfo.setOrderDetailList(orderDetailList);
            orderInfo.setPayWay(this.getPayWayName(orderInfo.getPayWay()));
            orderInfo.setOrderStatusName(this.getOrderStatusName(orderInfo.getOrderStatus()));
            //3 根据订单id查询优惠信息
            LambdaQueryWrapper<OrderDerate> derateQueryWrapper = new LambdaQueryWrapper<>();
            derateQueryWrapper.eq(OrderDerate::getOrderId, orderInfo.getId());
            List<OrderDerate> orderDerateList = orderDerateMapper.selectList(derateQueryWrapper);
            orderInfo.setOrderDerateList(orderDerateList);
            return orderInfo;
        }
        return null;
    }

    /**
     * 根据支付方式编号得到支付类型
     *
     * @param payWay
     * @return
     */
    @Override
    public String getPayWayName(String payWay) {
        if (SystemConstant.ORDER_PAY_WAY_WEIXIN.equals(payWay)) {
            return "微信支付";
        } else if (SystemConstant.ORDER_PAY_ACCOUNT.equals(payWay)) {
            return "余额支付";
        } else if (SystemConstant.ORDER_PAY_WAY_ALIPAY.equals(payWay)) {
            return "支付宝支付";
        }
        return "";
    }

    /**
     * 根据订单状态编号得到订单状态
     * @param orderStatus
     * @return
     */
    @Override
    public String getOrderStatusName(String orderStatus) {
        if (SystemConstant.ORDER_STATUS_PAID.equals(orderStatus)) {
            return "已支付";
        } else if (SystemConstant.ORDER_STATUS_UNPAID.equals(orderStatus)) {
            return "未支付";
        } else if (SystemConstant.ORDER_STATUS_CANCEL.equals(orderStatus)) {
            return "取消支付";
        }
        return "";
    }
}
