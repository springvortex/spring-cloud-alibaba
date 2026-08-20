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
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.validation.annotation.Validated;
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
 * <p>查询与修改复用 {@link OrderService}（继承 MyBatis-Plus 的 IService）
 * 自带的通用能力；新增和删除通过服务层事务方法维护主表与明细的一致性。
 * 订单详情查询（getById）会聚合明细表，一次性返回主表 + 明细列表。
 * Entity <-> DTO 转换使用 {@link OrderConverter}（MapStruct 编译期生成，零反射）。
 *
 * @author jiancai.zhong
 */
@Tag(name = "订单管理", description = "订单的增删改查，含明细聚合")
@RestController
@Validated
public class OrderController {

    @Resource
    private OrderService orderService;

    @Resource
    private OrderDetailService orderDetailService;

    @Resource
    private OrderConverter orderConverter;

    @Operation(summary = "根据ID查询单个订单（含明细）")
    @GetMapping("/order/{id}")
    public ApiResponse<OrderDTO> getOrder(
            @Parameter(description = "订单主键") @PathVariable("id") Long id) {
        Order order = orderService.getById(id);
        if (order == null) {
            return ApiResponse.success(null);
        }
        OrderDTO dto = orderConverter.entityToDto(order);
        List<OrderDetail> details = orderDetailService.list(
                new LambdaQueryWrapper<OrderDetail>().eq(OrderDetail::getOrderId, id));
        List<OrderDetailDTO> detailDTOs = details.stream().map(orderConverter::entityToDto).toList();
        dto.setOrderDetails(detailDTOs);
        return ApiResponse.success(dto);
    }

    @Operation(summary = "查询全部有效订单（不含明细）")
    @GetMapping("/order/list")
    public ApiResponse<List<OrderDTO>> list() {
        return ApiResponse.success(orderConverter.entityListToDtoList(orderService.list()));
    }

    @Operation(summary = "分页查询有效订单（不含明细）")
    @GetMapping("/order/page")
    public ApiResponse<Page<OrderDTO>> page(
            @Parameter(description = "当前页码，从1开始")
            @Min(value = 1, message = "当前页码必须从1开始")
            @RequestParam(value = "current", defaultValue = "1") long current,
            @Parameter(description = "每页条数，范围1-100")
            @Min(value = 1, message = "每页条数不能小于1")
            @Max(value = 100, message = "每页条数不能超过100")
            @RequestParam(value = "size", defaultValue = "10") long size) {
        Page<Order> page = orderService.page(new Page<>(current, size));
        Page<OrderDTO> result = new Page<>(page.getCurrent(), page.getSize(), page.getTotal());
        result.setRecords(orderConverter.entityListToDtoList(page.getRecords()));
        return ApiResponse.success(result);
    }

    @Operation(summary = "新增订单（主表与明细在同一事务保存）")
    @PostMapping("/order")
    public ApiResponse<OrderDTO> add(@Valid @RequestBody OrderDTO dto) {
        Order order = orderConverter.dtoToEntity(dto);
        List<OrderDetail> details = orderConverter.detailDtoListToEntityList(
                dto.getOrderDetails() == null ? List.of() : dto.getOrderDetails());
        orderService.saveWithDetails(order, details);

        OrderDTO result = orderConverter.entityToDto(order);
        result.setOrderDetails(details.stream().map(orderConverter::entityToDto).toList());
        return ApiResponse.success(result);
    }

    @Operation(summary = "根据ID修改订单")
    @PutMapping("/order")
    public ApiResponse<Void> update(@Valid @RequestBody OrderDTO dto) {
        boolean updated = orderService.updateById(orderConverter.dtoToEntity(dto));
        return updated ? ApiResponse.success() : ApiResponse.failure(ApiResponseEnum.NOT_FOUND);
    }

    @Operation(summary = "根据ID删除订单及明细（同一事务逻辑删除）")
    @DeleteMapping("/order/{id}")
    public ApiResponse<Void> delete(
            @Parameter(description = "订单主键") @PathVariable("id") Long id) {
        boolean removed = orderService.removeWithDetails(id);
        return removed ? ApiResponse.success() : ApiResponse.failure(ApiResponseEnum.NOT_FOUND);
    }
}
