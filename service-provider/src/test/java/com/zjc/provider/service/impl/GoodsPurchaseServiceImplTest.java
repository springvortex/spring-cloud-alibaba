package com.zjc.provider.service.impl;

import com.zjc.common.dto.GoodsPurchaseRequestDTO;
import com.zjc.common.dto.GoodsPurchaseResponseDTO;
import com.zjc.common.exception.BusinessException;
import com.zjc.provider.entity.Goods;
import com.zjc.provider.entity.Order;
import com.zjc.provider.entity.OrderDetail;
import com.zjc.provider.mapper.GoodsMapper;
import com.zjc.provider.service.OrderService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * {@link GoodsPurchaseServiceImpl} 分布式锁与购买事务测试。
 *
 * @author jiancai.zhong
 */
@DisplayName("商品购买服务")
@ExtendWith(MockitoExtension.class)
class GoodsPurchaseServiceImplTest {

    @Mock
    private RedissonClient redissonClient;

    @Mock
    private RLock lock;

    @Mock
    private TransactionTemplate transactionTemplate;

    @Mock
    private GoodsMapper goodsMapper;

    @Mock
    private OrderService orderService;

    @Mock
    private CacheManager cacheManager;

    @Mock
    private Cache goodsCache;

    @InjectMocks
    private GoodsPurchaseServiceImpl purchaseService;

    /**
     * 验证获取锁成功后的完整购买链路。
     */
    @Test
    @DisplayName("purchase: 获取商品锁后扣库存、创建订单并清理缓存")
    void purchaseSuccess() throws InterruptedException {
        GoodsPurchaseRequestDTO request = request(2);
        Goods goods = goods(10, "99.00");
        mockTransactionExecution();
        when(redissonClient.getLock(GoodsPurchaseServiceImpl.PURCHASE_LOCK_KEY_PREFIX + 1L))
                .thenReturn(lock);
        when(lock.tryLock(3, TimeUnit.SECONDS)).thenReturn(true);
        when(goodsMapper.selectById(1L)).thenReturn(goods);
        when(goodsMapper.decreaseStock(1L, 2)).thenReturn(1);
        doAnswer(invocation -> {
            Order order = invocation.getArgument(0);
            order.setOrderId(100L);
            return null;
        }).when(orderService).saveWithDetails(any(Order.class), any());
        when(cacheManager.getCache("provider:goods:id")).thenReturn(goodsCache);

        GoodsPurchaseResponseDTO response = purchaseService.purchase(1L, request);

        assertThat(response.getOrderId()).isEqualTo(100L);
        assertThat(response.getOrderNo()).isNotBlank();
        assertThat(response.getUserId()).isEqualTo(1L);
        assertThat(response.getGoodsId()).isEqualTo(1L);
        assertThat(response.getQuantity()).isEqualTo(2);
        assertThat(response.getPayAmount()).isEqualByComparingTo("198.00");
        assertThat(response.getRemainingStock()).isEqualTo(8);

        ArgumentCaptor<Order> orderCaptor = ArgumentCaptor.forClass(Order.class);
        ArgumentCaptor<List<OrderDetail>> detailsCaptor = ArgumentCaptor.captor();
        verify(orderService).saveWithDetails(orderCaptor.capture(), detailsCaptor.capture());
        assertThat(orderCaptor.getValue().getOrderStatus()).isZero();
        assertThat(orderCaptor.getValue().getTotalAmount()).isEqualByComparingTo("198.00");
        assertThat(detailsCaptor.getValue()).hasSize(1);
        assertThat(detailsCaptor.getValue().get(0).getGoodsNum()).isEqualTo(2);
        verify(goodsMapper).decreaseStock(1L, 2);
        verify(goodsCache).evict(1L);
        verify(lock).unlock();
    }

    /**
     * 验证锁等待超时时快速失败，不进入购买事务。
     */
    @Test
    @DisplayName("purchase: 锁等待超时返回业务繁忙且不访问数据库")
    void purchaseLockTimeout() throws InterruptedException {
        when(redissonClient.getLock(GoodsPurchaseServiceImpl.PURCHASE_LOCK_KEY_PREFIX + 1L))
                .thenReturn(lock);
        when(lock.tryLock(3, TimeUnit.SECONDS)).thenReturn(false);

        assertThatThrownBy(() -> purchaseService.purchase(1L, request(1)))
                .isInstanceOf(BusinessException.class)
                .hasMessage("当前购买人数过多，请稍后再试");

        verify(transactionTemplate, never()).execute(any());
        verify(goodsMapper, never()).selectById(1L);
        verify(lock, never()).unlock();
    }

    /**
     * 验证库存不足时不落订单，并正常释放商品锁。
     */
    @Test
    @DisplayName("purchase: 库存不足时不扣库存、不创建订单并释放锁")
    void purchaseInsufficientStock() throws InterruptedException {
        mockTransactionExecution();
        when(redissonClient.getLock(GoodsPurchaseServiceImpl.PURCHASE_LOCK_KEY_PREFIX + 1L))
                .thenReturn(lock);
        when(lock.tryLock(3, TimeUnit.SECONDS)).thenReturn(true);
        when(goodsMapper.selectById(1L)).thenReturn(goods(1, "99.00"));

        assertThatThrownBy(() -> purchaseService.purchase(1L, request(2)))
                .isInstanceOf(BusinessException.class)
                .hasMessage("库存不足，请稍后再试");

        verify(goodsMapper, never()).decreaseStock(any(), anyInt());
        verify(orderService, never()).saveWithDetails(any(), any());
        verify(goodsCache, never()).evict(any());
        verify(lock).unlock();
    }

    /**
     * 验证锁在解锁前异常丢失时，不掩盖已经成功的购买结果。
     */
    @Test
    @DisplayName("purchase: 解锁时锁已丢失则静默处理且不掩盖业务结果")
    void purchaseUnlockSilentlyWhenLockLost() throws InterruptedException {
        mockTransactionExecution();
        when(redissonClient.getLock(GoodsPurchaseServiceImpl.PURCHASE_LOCK_KEY_PREFIX + 1L))
                .thenReturn(lock);
        when(lock.tryLock(3, TimeUnit.SECONDS)).thenReturn(true);
        when(goodsMapper.selectById(1L)).thenReturn(goods(10, "99.00"));
        when(goodsMapper.decreaseStock(1L, 2)).thenReturn(1);
        doAnswer(invocation -> {
            Order order = invocation.getArgument(0);
            order.setOrderId(100L);
            return null;
        }).when(orderService).saveWithDetails(any(Order.class), any());
        when(cacheManager.getCache("provider:goods:id")).thenReturn(goodsCache);
        doThrow(new IllegalMonitorStateException("lock lost")).when(lock).unlock();

        GoodsPurchaseResponseDTO response = purchaseService.purchase(1L, request(2));

        assertThat(response.getOrderId()).isEqualTo(100L);
        verify(goodsCache).evict(1L);
        verify(lock).unlock();
    }

    /**
     * 验证缓存清理失败时记录日志，但不影响已提交的购买结果。
     */
    @Test
    @DisplayName("purchase: 缓存清理失败不掩盖购买成功")
    void purchaseCacheEvictFailureDoesNotMaskSuccess() throws InterruptedException {
        mockTransactionExecution();
        when(redissonClient.getLock(GoodsPurchaseServiceImpl.PURCHASE_LOCK_KEY_PREFIX + 1L))
                .thenReturn(lock);
        when(lock.tryLock(3, TimeUnit.SECONDS)).thenReturn(true);
        when(goodsMapper.selectById(1L)).thenReturn(goods(10, "99.00"));
        when(goodsMapper.decreaseStock(1L, 2)).thenReturn(1);
        doAnswer(invocation -> {
            Order order = invocation.getArgument(0);
            order.setOrderId(100L);
            return null;
        }).when(orderService).saveWithDetails(any(Order.class), any());
        when(cacheManager.getCache("provider:goods:id")).thenReturn(goodsCache);
        doThrow(new IllegalStateException("redis unavailable")).when(goodsCache).evict(1L);

        GoodsPurchaseResponseDTO response = purchaseService.purchase(1L, request(2));

        assertThat(response.getOrderId()).isEqualTo(100L);
        verify(goodsCache).evict(1L);
        verify(lock).unlock();
    }

    /**
     * 构造指定数量的购买请求。
     *
     * @param quantity 购买数量
     * @return 购买请求
     */
    private static GoodsPurchaseRequestDTO request(int quantity) {
        GoodsPurchaseRequestDTO request = new GoodsPurchaseRequestDTO();
        request.setUserId(1L);
        request.setQuantity(quantity);
        return request;
    }

    /**
     * 构造上架商品测试数据。
     *
     * @param stock 库存
     * @param price 单价
     * @return 商品实体
     */
    private static Goods goods(int stock, String price) {
        Goods goods = new Goods();
        goods.setGoodsId(1L);
        goods.setGoodsName("iPhone");
        goods.setGoodsPrice(new BigDecimal(price));
        goods.setStock(stock);
        goods.setStatus(1);
        return goods;
    }

    /**
     * 让事务模板直接执行回调，便于单元测试购买事务内部逻辑。
     */
    private void mockTransactionExecution() {
        when(transactionTemplate.execute(any())).thenAnswer(invocation -> {
            TransactionCallback<GoodsPurchaseResponseDTO> callback = invocation.getArgument(0);
            return callback.doInTransaction(null);
        });
    }
}
