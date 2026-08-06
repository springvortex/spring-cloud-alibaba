package com.zjc.provider.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zjc.common.dto.OrderDetailDTO;
import com.zjc.common.dto.OrderDTO;
import com.zjc.common.web.ApiResponse;
import com.zjc.provider.entity.Order;
import com.zjc.provider.entity.OrderDetail;
import com.zjc.provider.service.OrderDetailService;
import com.zjc.provider.service.OrderService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.springframework.beans.BeanUtils;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 订单管理 REST 接口。
 *
 * <p>对外统一返回 {@link OrderDTO}，{@link Order} 实体不直接暴露，
 * 避免数据库结构（逻辑删除字段、更新时间等）泄露到接口契约中。
 * 所有方法返回值包装在 {@link ApiResponse} 里，保证响应结构一致。
 *
 * <p>CRUD 直接复用 {@link OrderService}（继承 MyBatis-Plus 的 IService）
 * 自带的能力，无需在 service 层重复定义通用方法。
 * 订单详情查询（getById）会聚合明细表，一次性返回主表 + 明细列表。
 *
 * @author jiancai.zhong
 */
@Tag(name = "订单管理", description = "订单的增删改查，含明细聚合")
@RestController
public class OrderController {

    @Resource
    private OrderService orderService;

    @Resource
    private OrderDetailService orderDetailService;

    @Operation(summary = "根据ID查询单个订单（含明细）")
    @GetMapping("/order/{id}")
    public ApiResponse<OrderDTO> getOrder(
            @Parameter(description = "订单主键") @PathVariable("id") Long id) {
        Order order = orderService.getById(id);
        if (order == null) {
            return ApiResponse.success(null);
        }
        OrderDTO dto = toDTO(order);
        List<OrderDetail> details = orderDetailService.list(
                new LambdaQueryWrapper<OrderDetail>().eq(OrderDetail::getOrderId, id));
        dto.setOrderDetails(details.stream().map(this::toDTO).toList());
        return ApiResponse.success(dto);
    }

    @Operation(summary = "查询全部有效订单（不含明细）")
    @GetMapping("/order/list")
    public ApiResponse<List<OrderDTO>> list() {
        List<OrderDTO> list = orderService.list().stream().map(this::toDTO).toList();
        return ApiResponse.success(list);
    }

    @Operation(summary = "分页查询有效订单（不含明细）")
    @GetMapping("/order/page")
    public ApiResponse<Page<OrderDTO>> page(
            @Parameter(description = "当前页码，从1开始") @RequestParam(value = "current", defaultValue = "1") long current,
            @Parameter(description = "每页条数") @RequestParam(value = "size", defaultValue = "10") long size) {
        Page<Order> page = orderService.page(new Page<>(current, size));
        Page<OrderDTO> result = new Page<>(page.getCurrent(), page.getSize(), page.getTotal());
        result.setRecords(page.getRecords().stream().map(this::toDTO).toList());
        return ApiResponse.success(result);
    }

    @Operation(summary = "新增订单（仅主表）")
    @PostMapping("/order")
    public ApiResponse<OrderDTO> add(@Valid @RequestBody OrderDTO dto) {
        Order order = new Order();
        BeanUtils.copyProperties(dto, order);
        orderService.save(order);
        return ApiResponse.success(toDTO(order));
    }

    @Operation(summary = "根据ID修改订单")
    @PutMapping("/order")
    public ApiResponse<Void> update(@Valid @RequestBody OrderDTO dto) {
        Order order = new Order();
        BeanUtils.copyProperties(dto, order);
        orderService.updateById(order);
        return ApiResponse.success();
    }

    @Operation(summary = "根据ID删除订单（逻辑删除）")
    @DeleteMapping("/order/{id}")
    public ApiResponse<Void> delete(
            @Parameter(description = "订单主键") @PathVariable("id") Long id) {
        orderService.removeById(id);
        return ApiResponse.success();
    }

    /**
     * 订单主表 Entity 转 DTO
     */
    private OrderDTO toDTO(Order order) {
        if (order == null) {
            return null;
        }
        OrderDTO dto = new OrderDTO();
        BeanUtils.copyProperties(order, dto);
        return dto;
    }

    /**
     * 订单明细 Entity 转 DTO
     */
    private OrderDetailDTO toDTO(OrderDetail detail) {
        if (detail == null) {
            return null;
        }
        OrderDetailDTO dto = new OrderDetailDTO();
        BeanUtils.copyProperties(detail, dto);
        return dto;
    }
}