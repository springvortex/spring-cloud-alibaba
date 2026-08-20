package com.zjc.provider.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.zjc.common.constant.ApiResponseEnum;
import com.zjc.common.exception.BusinessException;
import com.zjc.provider.entity.Order;
import com.zjc.provider.entity.OrderDetail;
import com.zjc.provider.service.OrderDetailService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * {@link OrderServiceImpl} 订单主表与明细的事务性写操作测试。
 */
@DisplayName("订单事务服务")
@ExtendWith(MockitoExtension.class)
class OrderServiceImplTest {

    @Mock
    private OrderDetailService orderDetailService;

    @Spy
    @InjectMocks
    private OrderServiceImpl orderService;

    @Test
    @DisplayName("saveWithDetails: 保存主表后补齐明细关联并批量保存")
    void testSaveWithDetails() {
        Order order = new Order();
        order.setOrderNo("NO001");
        OrderDetail detail = new OrderDetail();
        detail.setId(999L);
        detail.setOrderId(888L);
        detail.setOrderNo("OLD001");
        doAnswer(invocation -> {
            invocation.getArgument(0, Order.class).setOrderId(1L);
            return true;
        }).when(orderService).save(order);
        when(orderDetailService.saveBatch(List.of(detail))).thenReturn(true);

        orderService.saveWithDetails(order, List.of(detail));

        assertThat(detail.getId()).isNull();
        assertThat(detail.getOrderId()).isEqualTo(1L);
        assertThat(detail.getOrderNo()).isEqualTo("NO001");
        verify(orderDetailService).saveBatch(List.of(detail));
    }

    @Test
    @DisplayName("saveWithDetails: 无明细时不调用批量保存")
    void testSaveWithoutDetails() {
        Order order = new Order();
        order.setOrderNo("NO001");
        doReturn(true).when(orderService).save(order);

        orderService.saveWithDetails(order, List.of());

        verify(orderService).save(order);
        verify(orderDetailService, never()).saveBatch(anyList());
    }

    @Test
    @DisplayName("saveWithDetails: 明细保存失败时抛出业务异常")
    void testSaveDetailsFailure() {
        Order order = new Order();
        order.setOrderNo("NO001");
        OrderDetail detail = new OrderDetail();
        doAnswer(invocation -> {
            invocation.getArgument(0, Order.class).setOrderId(1L);
            return true;
        }).when(orderService).save(order);
        when(orderDetailService.saveBatch(List.of(detail))).thenReturn(false);

        assertThatThrownBy(() -> orderService.saveWithDetails(order, List.of(detail)))
                .isInstanceOf(BusinessException.class)
                .hasMessage(ApiResponseEnum.INTERNAL_ERROR.message());
    }

    @Test
    @DisplayName("removeWithDetails: 删除主表后删除关联明细")
    void testRemoveWithDetails() {
        doReturn(true).when(orderService).removeById(1L);
        when(orderDetailService.remove(any(LambdaQueryWrapper.class))).thenReturn(true);

        boolean removed = orderService.removeWithDetails(1L);

        assertThat(removed).isTrue();
        verify(orderDetailService).remove(any(LambdaQueryWrapper.class));
    }

    @Test
    @DisplayName("removeWithDetails: 订单不存在时不删除明细")
    void testRemoveMissingOrder() {
        doReturn(false).when(orderService).removeById(999L);

        boolean removed = orderService.removeWithDetails(999L);

        assertThat(removed).isFalse();
        verify(orderDetailService, never()).remove(any(LambdaQueryWrapper.class));
    }

    @Test
    @DisplayName("事务配置: 写操作对 Exception 回滚")
    void testTransactionalWriteMethods() throws NoSuchMethodException {
        Transactional save = OrderServiceImpl.class
                .getMethod("saveWithDetails", Order.class, List.class)
                .getAnnotation(Transactional.class);
        Transactional remove = OrderServiceImpl.class
                .getMethod("removeWithDetails", Long.class)
                .getAnnotation(Transactional.class);

        assertThat(save).isNotNull();
        assertThat(save.rollbackFor()).containsExactly(Exception.class);
        assertThat(remove).isNotNull();
        assertThat(remove.rollbackFor()).containsExactly(Exception.class);
    }
}
