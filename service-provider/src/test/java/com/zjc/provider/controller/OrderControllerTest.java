package com.zjc.provider.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zjc.common.constant.ApiResponseEnum;
import com.zjc.common.dto.OrderDTO;
import com.zjc.common.dto.OrderDetailDTO;
import com.zjc.common.web.ApiResponse;
import com.zjc.provider.converter.OrderConverter;
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
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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

    @Mock
    private OrderConverter orderConverter;

    @InjectMocks
    private OrderController orderController;

    @Test
    @DisplayName("getOrder: 查询订单含明细聚合")
    void testGetOrderWithDetails() {
        Order order = new Order();
        order.setOrderId(1L);
        order.setOrderNo("NO001");
        order.setTotalAmount(new BigDecimal("100"));
        OrderDTO orderDto = new OrderDTO();
        orderDto.setOrderNo("NO001");
        when(orderService.getById(1L)).thenReturn(order);
        when(orderConverter.entityToDto(order)).thenReturn(orderDto);

        OrderDetail detail = new OrderDetail();
        detail.setId(10L);
        detail.setOrderId(1L);
        detail.setGoodsName("iPhone");
        OrderDetailDTO detailDto = new OrderDetailDTO();
        detailDto.setGoodsName("iPhone");
        when(orderDetailService.list(any(LambdaQueryWrapper.class))).thenReturn(List.of(detail));
        when(orderConverter.entityToDto(detail)).thenReturn(detailDto);

        ApiResponse<OrderDTO> resp = orderController.getOrder(1L);

        assertThat(resp.isSuccess()).isTrue();
        assertThat(resp.getData().getOrderNo()).isEqualTo("NO001");
        assertThat(resp.getData().getOrderDetails()).hasSize(1);
        assertThat(resp.getData().getOrderDetails().get(0).getGoodsName()).isEqualTo("iPhone");
        verify(orderDetailService).list(any(LambdaQueryWrapper.class));
    }

    @Test
    @DisplayName("getOrder: 订单不存在返回 null data")
    void testGetOrderNotFound() {
        when(orderService.getById(999L)).thenReturn(null);

        ApiResponse<OrderDTO> resp = orderController.getOrder(999L);

        assertThat(resp.isSuccess()).isTrue();
        assertThat(resp.getData()).isNull();
        verify(orderDetailService, never()).list(any(LambdaQueryWrapper.class));
    }

    @Test
    @DisplayName("list: 查询订单列表(不含明细)")
    void testListReturnsList() {
        Order order = new Order();
        order.setOrderId(1L);
        order.setOrderNo("NO001");
        OrderDTO dto = new OrderDTO();
        dto.setOrderNo("NO001");
        when(orderService.list()).thenReturn(List.of(order));
        when(orderConverter.entityListToDtoList(List.of(order))).thenReturn(List.of(dto));

        ApiResponse<List<OrderDTO>> resp = orderController.list();

        assertThat(resp.getData()).hasSize(1);
        assertThat(resp.getData().get(0).getOrderNo()).isEqualTo("NO001");
        assertThat(resp.getData().get(0).getOrderDetails()).isNull();
    }

    @Test
    @DisplayName("list: 空列表")
    void testListEmpty() {
        when(orderService.list()).thenReturn(Collections.emptyList());
        when(orderConverter.entityListToDtoList(Collections.emptyList())).thenReturn(Collections.emptyList());

        ApiResponse<List<OrderDTO>> resp = orderController.list();

        assertThat(resp.getData()).isEmpty();
    }

    @Test
    @DisplayName("page: 分页查询(不含明细)")
    void testPageReturnsPage() {
        Page<Order> page = new Page<>(1, 10, 1);
        Order order = new Order();
        order.setOrderId(1L);
        order.setOrderNo("NO001");
        page.setRecords(List.of(order));
        OrderDTO dto = new OrderDTO();
        dto.setOrderNo("NO001");
        when(orderService.page(any(Page.class))).thenReturn(page);
        when(orderConverter.entityListToDtoList(List.of(order))).thenReturn(List.of(dto));

        ApiResponse<Page<OrderDTO>> resp = orderController.page(1, 10);

        assertThat(resp.getData().getTotal()).isEqualTo(1);
        assertThat(resp.getData().getRecords().get(0).getOrderNo()).isEqualTo("NO001");
    }

    @Test
    @DisplayName("add: 主表与明细同事务保存")
    void testAddWithDetailsReturnsDto() {
        OrderDTO dto = new OrderDTO();
        dto.setOrderNo("NEW001");
        dto.setTotalAmount(new BigDecimal("200"));
        OrderDetailDTO detailDto = new OrderDetailDTO();
        detailDto.setGoodsName("iPhone");
        dto.setOrderDetails(List.of(detailDto));
        Order entity = new Order();
        entity.setOrderNo("NEW001");
        OrderDetail detail = new OrderDetail();
        detail.setGoodsName("iPhone");
        OrderDTO resultDto = new OrderDTO();
        resultDto.setOrderNo("NEW001");
        OrderDetailDTO resultDetailDto = new OrderDetailDTO();
        resultDetailDto.setGoodsName("iPhone");
        when(orderConverter.dtoToEntity(dto)).thenReturn(entity);
        when(orderConverter.detailDtoListToEntityList(List.of(detailDto))).thenReturn(List.of(detail));
        when(orderConverter.entityToDto(entity)).thenReturn(resultDto);
        when(orderConverter.entityToDto(detail)).thenReturn(resultDetailDto);
        doAnswer(invocation -> {
            invocation.getArgument(0, Order.class).setOrderId(1L);
            return null;
        }).when(orderService).saveWithDetails(entity, List.of(detail));

        ApiResponse<OrderDTO> resp = orderController.add(dto);

        assertThat(resp.isSuccess()).isTrue();
        assertThat(resp.getData().getOrderNo()).isEqualTo("NEW001");
        assertThat(resp.getData().getOrderDetails()).containsExactly(resultDetailDto);
        verify(orderService).saveWithDetails(entity, List.of(detail));
    }

    @Test
    @DisplayName("add: 未传明细时保存空明细列表")
    void testAddWithoutDetailsReturnsDto() {
        OrderDTO dto = new OrderDTO();
        dto.setOrderNo("NEW001");
        dto.setTotalAmount(new BigDecimal("200"));
        Order entity = new Order();
        entity.setOrderNo("NEW001");
        OrderDTO resultDto = new OrderDTO();
        resultDto.setOrderNo("NEW001");
        when(orderConverter.dtoToEntity(dto)).thenReturn(entity);
        when(orderConverter.detailDtoListToEntityList(List.of())).thenReturn(List.of());
        when(orderConverter.entityToDto(entity)).thenReturn(resultDto);

        ApiResponse<OrderDTO> resp = orderController.add(dto);

        assertThat(resp.isSuccess()).isTrue();
        assertThat(resp.getData().getOrderDetails()).isEmpty();
        verify(orderService).saveWithDetails(entity, List.of());
    }

    @Test
    @DisplayName("update: 修改订单")
    void testUpdateSuccess() {
        OrderDTO dto = new OrderDTO();
        dto.setOrderId(1L);
        dto.setOrderNo("UPDATED");
        Order entity = new Order();
        entity.setOrderId(1L);
        when(orderConverter.dtoToEntity(dto)).thenReturn(entity);
        when(orderService.updateById(any(Order.class))).thenReturn(true);

        ApiResponse<Void> resp = orderController.update(dto);

        assertThat(resp.isSuccess()).isTrue();
        verify(orderService).updateById(any(Order.class));
    }

    @Test
    @DisplayName("update: 订单不存在返回资源不存在")
    void testUpdateNotFound() {
        OrderDTO dto = new OrderDTO();
        dto.setOrderId(999L);
        Order entity = new Order();
        entity.setOrderId(999L);
        when(orderConverter.dtoToEntity(dto)).thenReturn(entity);
        when(orderService.updateById(entity)).thenReturn(false);

        ApiResponse<Void> resp = orderController.update(dto);

        assertThat(resp.isSuccess()).isFalse();
        assertThat(resp.getCode()).isEqualTo(ApiResponseEnum.NOT_FOUND.code());
        assertThat(resp.getMessage()).isEqualTo(ApiResponseEnum.NOT_FOUND.message());
    }

    @Test
    @DisplayName("delete: 逻辑删除订单及明细")
    void testDeleteSuccess() {
        when(orderService.removeWithDetails(1L)).thenReturn(true);

        ApiResponse<Void> resp = orderController.delete(1L);

        assertThat(resp.isSuccess()).isTrue();
        verify(orderService).removeWithDetails(1L);
    }

    @Test
    @DisplayName("delete: 订单不存在返回资源不存在")
    void testDeleteNotFound() {
        when(orderService.removeWithDetails(999L)).thenReturn(false);

        ApiResponse<Void> resp = orderController.delete(999L);

        assertThat(resp.isSuccess()).isFalse();
        assertThat(resp.getCode()).isEqualTo(ApiResponseEnum.NOT_FOUND.code());
    }
}
