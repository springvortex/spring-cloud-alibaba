package com.zjc.provider.service.impl;

import cn.hutool.core.util.IdUtil;
import com.zjc.common.constant.ApiResponseEnum;
import com.zjc.common.dto.GoodsPurchaseRequestDTO;
import com.zjc.common.dto.GoodsPurchaseResponseDTO;
import com.zjc.common.exception.BusinessException;
import com.zjc.provider.entity.Goods;
import com.zjc.provider.entity.Order;
import com.zjc.provider.entity.OrderDetail;
import com.zjc.provider.mapper.GoodsMapper;
import com.zjc.provider.service.GoodsPurchaseService;
import com.zjc.provider.service.OrderService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 商品购买服务实现。
 *
 * <p>Redisson 锁按商品维度拆分，数据库条件更新继续兜底，避免异常路径下超卖。
 *
 * @author jiancai.zhong
 */
@Slf4j
@Service
public class GoodsPurchaseServiceImpl implements GoodsPurchaseService {

    /**
     * 购买锁的 Redis key 前缀。
     */
    public static final String PURCHASE_LOCK_KEY_PREFIX = "zjc:provider:goods:purchase:lock:";

    /**
     * 购买锁最长等待时间；超过后快速失败，避免压测时请求无限堆积。
     */
    static final long LOCK_WAIT_SECONDS = 20;

    /**
     * 商品详情缓存名称，与查询侧 {@code @Cacheable} 保持一致。
     */
    private static final String GOODS_CACHE_NAME = "provider:goods:id";

    @Resource
    private RedissonClient redissonClient;

    @Resource
    private TransactionTemplate transactionTemplate;

    @Resource
    private GoodsMapper goodsMapper;

    @Resource
    private OrderService orderService;

    @Resource
    private CacheManager cacheManager;

    /**
     * 执行一次商品购买。
     *
     * <p>同一商品通过 Redisson 可重入锁串行执行；购买事务提交后清理商品详情缓存，
     * 最后安全释放商品锁。
     *
     * @param goodsId 商品 ID
     * @param request 购买请求
     * @return 订单编号、支付金额、购买数量与剩余库存
     * @throws BusinessException 商品不可购买、库存不足、锁等待超时或服务不可用
     */
    @Override
    public GoodsPurchaseResponseDTO purchase(Long goodsId, GoodsPurchaseRequestDTO request) {
        long startTime = System.nanoTime();
        log.debug("开始购买：goodsId={}, userId={}, quantity={}",
                goodsId, request.getUserId(), request.getQuantity());

        RLock lock = redissonClient.getLock(PURCHASE_LOCK_KEY_PREFIX + goodsId);
        boolean locked = false;
        try {
            long lockStartTime = System.nanoTime();
            locked = lock.tryLock(LOCK_WAIT_SECONDS, TimeUnit.SECONDS);
            if (!locked) {
                log.warn("获取商品购买锁超时：goodsId={}, waitSeconds={}", goodsId, LOCK_WAIT_SECONDS);
                throw new BusinessException(503, "当前购买人数过多，请稍后再试");
            }
            log.debug("获取商品购买锁成功：goodsId={}, waitMs={}", goodsId, elapsedMs(lockStartTime));

            GoodsPurchaseResponseDTO result;
            try {
                result = transactionTemplate.execute(status -> purchaseInTransaction(goodsId, request));
            } catch (BusinessException e) {
                throw e;
            } catch (RuntimeException e) {
                log.error("购买事务执行失败：goodsId={}, userId={}, quantity={}", goodsId, request.getUserId(), request.getQuantity(), e);
                throw e;
            }

            evictGoodsCache(goodsId);
            log.info("购买事务提交成功：goodsId={}, userId={}, orderNo={}, quantity={}, payAmount={}, " + "remainingStock={}, lockWaitMs={}, costMs={}",
                    result.getGoodsId(), result.getUserId(), result.getOrderNo(), result.getQuantity(),
                    result.getPayAmount(), result.getRemainingStock(), elapsedMs(lockStartTime),
                    elapsedMs(startTime));
            return result;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("获取商品购买锁被中断：goodsId={}", goodsId, e);
            throw new BusinessException(ApiResponseEnum.SERVICE_UNAVAILABLE);
        } finally {
            if (locked) {
                unlockQuietly(lock, goodsId);
            }
        }
    }

    /**
     * 在当前事务内完成商品校验、扣库存和订单落库。
     *
     * <p>库存扣减使用数据库条件更新，即使锁异常失效也不会把库存扣成负数。
     *
     * @param goodsId 商品 ID
     * @param request 购买请求
     * @return 购买结果
     * @throws BusinessException 商品不存在、已下架或库存不足
     */
    private GoodsPurchaseResponseDTO purchaseInTransaction(Long goodsId, GoodsPurchaseRequestDTO request) {
        Goods goods = goodsMapper.selectById(goodsId);
        if (goods == null) {
            log.warn("商品不存在，拒绝购买：goodsId={}", goodsId);
            throw new BusinessException(ApiResponseEnum.NOT_FOUND);
        }
        if (!Integer.valueOf(1).equals(goods.getStatus())) {
            log.warn("商品未上架，拒绝购买：goodsId={}, status={}", goodsId, goods.getStatus());
            throw new BusinessException(ApiResponseEnum.NOT_FOUND);
        }

        int quantity = request.getQuantity();
        if (goods.getStock() == null || goods.getStock() < quantity) {
            log.info("商品库存不足，拒绝购买：goodsId={}, quantity={}, currentStock={}",
                    goodsId, quantity, goods.getStock());
            throw new BusinessException("库存不足，请稍后再试");
        }
        if (goodsMapper.decreaseStock(goodsId, quantity) != 1) {
            log.warn("数据库原子扣减库存未命中：goodsId={}, quantity={}, currentStock={}",
                    goodsId, quantity, goods.getStock());
            throw new BusinessException("库存不足，请稍后再试");
        }

        BigDecimal payAmount = goods.getGoodsPrice().multiply(BigDecimal.valueOf(quantity));
        Order order = buildOrder(request.getUserId(), payAmount);
        OrderDetail detail = buildOrderDetail(goods, quantity, payAmount);
        orderService.saveWithDetails(order, List.of(detail));

        GoodsPurchaseResponseDTO response = new GoodsPurchaseResponseDTO();
        response.setOrderId(order.getOrderId());
        response.setOrderNo(order.getOrderNo());
        response.setUserId(order.getUserId());
        response.setGoodsId(goods.getGoodsId());
        response.setGoodsName(goods.getGoodsName());
        response.setQuantity(quantity);
        response.setPayAmount(payAmount);
        response.setRemainingStock(goods.getStock() - quantity);
        return response;
    }

    /**
     * 构建待支付订单主表记录。
     *
     * @param userId    购买用户 ID
     * @param payAmount 订单支付金额
     * @return 待保存的订单实体
     */
    private Order buildOrder(Long userId, BigDecimal payAmount) {
        Order order = new Order();
        order.setOrderNo(IdUtil.getSnowflakeNextIdStr());
        order.setUserId(userId);
        order.setTotalAmount(payAmount);
        order.setPayAmount(payAmount);
        order.setOrderStatus(0);
        return order;
    }

    /**
     * 构建订单明细快照，保留购买时的商品名称和价格。
     *
     * @param goods     商品快照
     * @param quantity  购买数量
     * @param payAmount 明细小计金额
     * @return 待保存的订单明细实体
     */
    private OrderDetail buildOrderDetail(Goods goods, int quantity, BigDecimal payAmount) {
        OrderDetail detail = new OrderDetail();
        detail.setGoodsId(goods.getGoodsId());
        detail.setGoodsName(goods.getGoodsName());
        detail.setGoodsPrice(goods.getGoodsPrice());
        detail.setGoodsNum(quantity);
        detail.setSubTotal(payAmount);
        return detail;
    }

    /**
     * 清理商品详情缓存，避免购买后继续读到旧库存。
     *
     * @param goodsId 商品 ID
     */
    private void evictGoodsCache(Long goodsId) {
        Cache cache = cacheManager.getCache(GOODS_CACHE_NAME);
        if (cache == null) {
            log.warn("未找到商品详情缓存，跳过清理：cache={}, goodsId={}", GOODS_CACHE_NAME, goodsId);
            return;
        }

        try {
            cache.evict(goodsId);
            log.debug("商品详情缓存清理成功：cache={}, goodsId={}", GOODS_CACHE_NAME, goodsId);
        } catch (RuntimeException e) {
            log.error("商品详情缓存清理失败，旧库存可能保留至 TTL 到期：cache={}, goodsId={}",
                    GOODS_CACHE_NAME, goodsId, e);
        }
    }

    /**
     * 释放当前请求获取到的商品锁。
     *
     * <p>看门狗或 Redis 异常导致锁在解锁前丢失时，Redisson 会抛出
     * {@link IllegalMonitorStateException}；此时业务事务已经完成，不应让解锁异常
     * 掩盖原始购买结果。
     *
     * @param lock    商品购买锁
     * @param goodsId 商品 ID
     */
    private void unlockQuietly(RLock lock, Long goodsId) {
        try {
            lock.unlock();
        } catch (IllegalMonitorStateException ignored) {
            log.warn("商品购买锁在解锁前已丢失：goodsId={}", goodsId);
        }
    }

    /**
     * 计算耗时，供压测日志观察锁等待和整体购买耗时。
     *
     * @param startTime 开始时间纳秒
     * @return 耗时毫秒
     */
    private static long elapsedMs(long startTime) {
        return Math.round((System.nanoTime() - startTime) / 1_000_000.0);
    }
}
