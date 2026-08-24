package com.zjc.provider.service.impl;

import com.zjc.common.dto.GoodsPurchaseRequestDTO;
import com.zjc.common.dto.GoodsPurchaseResponseDTO;
import com.zjc.common.exception.BusinessException;
import com.zjc.common.lock.DistributedLockFactory;
import com.zjc.common.lock.DistributedLockTemplate;
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
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.util.List;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * {@link GoodsPurchaseServiceImpl} 购买事务测试。
 *
 * @author jiancai.zhong
 */
@DisplayName("商品购买服务")
@ExtendWith(MockitoExtension.class)
class GoodsPurchaseServiceImplTest {

    /**
     * 购买锁等待超时提示。
     */
    private static final String LOCK_TIMEOUT_MESSAGE = "当前购买人数过多，请稍后再试";

    @Mock
    private DistributedLockFactory distributedLockFactory;

    @Mock
    private DistributedLockTemplate distributedLockTemplate;

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
     * 验证购买业务在分布式锁回调内扣库存、创建订单并清理缓存。
     */
    @Test
    @DisplayName("purchase: 在锁回调内扣库存、创建订单并清理缓存")
    void purchaseSuccess() {
        GoodsPurchaseRequestDTO request = request(2);
        Goods goods = goods(10, "99.00");
        mockTransactionExecution();
        mockLockExecution();
        when(goodsMapper.selectById(1L)).thenReturn(goods);
        when(goodsMapper.decreaseStock(1L, 2)).thenReturn(1);
        mockOrderSave();
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
    }

    /**
     * 验证锁模板抛出等待超时异常时不进入购买事务。
     */
    @Test
    @DisplayName("purchase: 锁等待超时不访问数据库")
    void purchaseLockTimeout() {
        when(distributedLockFactory.getTemplate()).thenReturn(distributedLockTemplate);
        when(distributedLockTemplate.execute(
                eq(GoodsPurchaseServiceImpl.PURCHASE_LOCK_KEY_PREFIX + 1L),
                eq(GoodsPurchaseServiceImpl.LOCK_WAIT),
                eq(LOCK_TIMEOUT_MESSAGE),
                any()))
                .thenThrow(new BusinessException(503, LOCK_TIMEOUT_MESSAGE));

        assertThatThrownBy(() -> purchaseService.purchase(1L, request(1)))
                .isInstanceOf(BusinessException.class)
                .hasMessage(LOCK_TIMEOUT_MESSAGE);

        verify(transactionTemplate, never()).execute(any());
        verify(goodsMapper, never()).selectById(1L);
    }

    /**
     * 验证库存不足时不落订单，业务异常从锁回调中原样传递。
     */
    @Test
    @DisplayName("purchase: 库存不足时不扣库存、不创建订单")
    void purchaseInsufficientStock() {
        mockTransactionExecution();
        mockLockExecution();
        when(goodsMapper.selectById(1L)).thenReturn(goods(1, "99.00"));

        assertThatThrownBy(() -> purchaseService.purchase(1L, request(2)))
                .isInstanceOf(BusinessException.class)
                .hasMessage("库存不足，请稍后再试");

        verify(goodsMapper, never()).decreaseStock(any(), anyInt());
        verify(orderService, never()).saveWithDetails(any(), any());
        verify(goodsCache, never()).evict(any());
    }

    /**
     * 验证缓存清理失败时记录日志，但不影响已提交的购买结果。
     */
    @Test
    @DisplayName("purchase: 缓存清理失败不掩盖购买成功")
    void purchaseCacheEvictFailureDoesNotMaskSuccess() {
        mockTransactionExecution();
        mockLockExecution();
        when(goodsMapper.selectById(1L)).thenReturn(goods(10, "99.00"));
        when(goodsMapper.decreaseStock(1L, 2)).thenReturn(1);
        mockOrderSave();
        when(cacheManager.getCache("provider:goods:id")).thenReturn(goodsCache);
        doThrow(new IllegalStateException("redis unavailable")).when(goodsCache).evict(1L);

        GoodsPurchaseResponseDTO response = purchaseService.purchase(1L, request(2));

        assertThat(response.getOrderId()).isEqualTo(100L);
        verify(goodsCache).evict(1L);
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
     * 模拟分布式锁模板直接执行业务回调。
     */
    @SuppressWarnings("unchecked")
    private void mockLockExecution() {
        when(distributedLockFactory.getTemplate()).thenReturn(distributedLockTemplate);
        when(distributedLockTemplate.execute(
                eq(GoodsPurchaseServiceImpl.PURCHASE_LOCK_KEY_PREFIX + 1L),
                eq(GoodsPurchaseServiceImpl.LOCK_WAIT),
                eq(LOCK_TIMEOUT_MESSAGE),
                any()))
                .thenAnswer(invocation -> {
                    Supplier<GoodsPurchaseResponseDTO> action = invocation.getArgument(3);
                    return action.get();
                });
    }

    /**
     * 模拟订单保存后回填订单 ID。
     */
    private void mockOrderSave() {
        doAnswer(invocation -> {
            Order order = invocation.getArgument(0);
            order.setOrderId(100L);
            return null;
        }).when(orderService).saveWithDetails(any(Order.class), any());
    }

    /**
     * 让事务模板直接执行回调，便于单元测试购买事务内部逻辑。
     */
    @SuppressWarnings("unchecked")
    private void mockTransactionExecution() {
        when(transactionTemplate.execute(any())).thenAnswer(invocation -> {
            TransactionCallback<GoodsPurchaseResponseDTO> callback = invocation.getArgument(0);
            return callback.doInTransaction(null);
        });
    }
}
