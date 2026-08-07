package com.zjc.provider.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zjc.common.dto.OrderDTO;
import com.zjc.common.web.ApiResponse;
import com.zjc.provider.entity.Order;
import com.zjc.provider.entity.OrderDetail;
import com.zjc.provider.service.OrderDetailService;
import com.zjc.provider.service.OrderService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * {@link OrderController} 单元测试。
 *
 * <p>重点覆盖 getById 的明细聚合逻辑（查询主表 + 关联明细表）
 * 以及 list/page 不含明细的行为差异。
 *
 * @author jiancai.zhong
 */
@DisplayName("订单管理 Controller")
@ExtendWith(MockitoExtension.class)
class OrderControllerTest {

    @Mock
    private OrderService orderService;

    @Mock
    private OrderDetailService orderDetailService;

    @InjectMocks
    private OrderController orderController;

    /**
     * 验证查询订单时主表和明细表的数据被聚合到同一个 DTO 中。
     */
    @Test
    @DisplayName("getOrder: 查询订单含明细聚合")
    void testGetOrderWithDetails() {
        Order order = new Order();
        order.setOrderId(1L);
        order.setOrderNo("NO001");
        order.setTotalAmount(new BigDecimal("100"));
        when(orderService.getById(1L)).thenReturn(order);

        OrderDetail detail = new OrderDetail();
        detail.setId(10L);
        detail.setOrderId(1L);
        detail.setGoodsName("iPhone");
        when(orderDetailService.list(any(LambdaQueryWrapper.class))).thenReturn(List.of(detail));

        ApiResponse<OrderDTO> resp = orderController.getOrder(1L);

        assertThat(resp.isSuccess()).isTrue();
        assertThat(resp.getData().getOrderNo()).isEqualTo("NO001");
        assertThat(resp.getData().getOrderDetails()).hasSize(1);
        assertThat(resp.getData().getOrderDetails().get(0).getGoodsName()).isEqualTo("iPhone");
        verify(orderDetailService).list(any(LambdaQueryWrapper.class));
    }

    /**
     * 验证订单不存在时返回 null data，且不会查询明细表。
     */
    @Test
    @DisplayName("getOrder: 订单不存在返回 null data")
    void testGetOrderNotFound() {
        when(orderService.getById(999L)).thenReturn(null);

        ApiResponse<OrderDTO> resp = orderController.getOrder(999L);

        assertThat(resp.isSuccess()).isTrue();
        assertThat(resp.getData()).isNull();
        verify(orderDetailService, never()).list(any(LambdaQueryWrapper.class));
    }

    /**
     * 验证列表查询只返回主表数据，orderDetails 为 null。
     */
    @Test
    @DisplayName("list: 查询订单列表(不含明细)")
    void testListReturnsList() {
        Order order = new Order();
        order.setOrderId(1L);
        order.setOrderNo("NO001");
        when(orderService.list()).thenReturn(List.of(order));

        ApiResponse<List<OrderDTO>> resp = orderController.list();

        assertThat(resp.getData()).hasSize(1);
        assertThat(resp.getData().get(0).getOrderNo()).isEqualTo("NO001");
        assertThat(resp.getData().get(0).getOrderDetails()).isNull();
    }

    /**
     * 验证无数据时返回空列表。
     */
    @Test
    @DisplayName("list: 空列表")
    void testListEmpty() {
        when(orderService.list()).thenReturn(Collections.emptyList());

        ApiResponse<List<OrderDTO>> resp = orderController.list();

        assertThat(resp.getData()).isEmpty();
    }

    /**
     * 验证分页查询只返回主表数据，不含明细。
     */
    @Test
    @DisplayName("page: 分页查询(不含明细)")
    void testPageReturnsPage() {
        Page<Order> page = new Page<>(1, 10, 1);
        Order order = new Order();
        order.setOrderId(1L);
        order.setOrderNo("NO001");
        page.setRecords(List.of(order));
        when(orderService.page(any(Page.class))).thenReturn(page);

        ApiResponse<Page<OrderDTO>> resp = orderController.page(1, 10);

        assertThat(resp.getData().getTotal()).isEqualTo(1);
        assertThat(resp.getData().getRecords().get(0).getOrderNo()).isEqualTo("NO001");
    }

    /**
     * 验证新增订单时只保存主表，返回回填的 DTO。
     */
    @Test
    @DisplayName("add: 新增订单(仅主表)")
    void testAddReturnsDto() {
        OrderDTO dto = new OrderDTO();
        dto.setOrderNo("NEW001");
        dto.setTotalAmount(new BigDecimal("200"));
        when(orderService.save(any(Order.class))).thenReturn(true);

        ApiResponse<OrderDTO> resp = orderController.add(dto);

        assertThat(resp.isSuccess()).isTrue();
        assertThat(resp.getData().getOrderNo()).isEqualTo("NEW001");
        verify(orderService).save(any(Order.class));
    }

    /**
     * 验证修改订单时调用 updateById。
     */
    @Test
    @DisplayName("update: 修改订单")
    void testUpdateSuccess() {
        OrderDTO dto = new OrderDTO();
        dto.setOrderId(1L);
        dto.setOrderNo("UPDATED");
        when(orderService.updateById(any(Order.class))).thenReturn(true);

        ApiResponse<Void> resp = orderController.update(dto);

        assertThat(resp.isSuccess()).isTrue();
        verify(orderService).updateById(any(Order.class));
    }

    /**
     * 验证删除订单时调用 removeById 实现逻辑删除。
     */
    @Test
    @DisplayName("delete: 逻辑删除订单")
    void testDeleteSuccess() {
        when(orderService.removeById(1L)).thenReturn(true);

        ApiResponse<Void> resp = orderController.delete(1L);

        assertThat(resp.isSuccess()).isTrue();
        verify(orderService).removeById(1L);
    }
}
