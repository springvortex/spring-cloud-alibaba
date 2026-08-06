package com.zjc.provider.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zjc.common.dto.OrderDTO;
import com.zjc.common.dto.OrderDetailDTO;
import com.zjc.common.web.ApiResponse;
import com.zjc.provider.entity.Order;
import com.zjc.provider.entity.OrderDetail;
import com.zjc.provider.service.OrderDetailService;
import com.zjc.provider.service.OrderService;
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
@RestController
public class OrderController {

    @Resource
    private OrderService orderService;

    @Resource
    private OrderDetailService orderDetailService;

    /**
     * 根据ID查询单个订单，含全部商品明细。
     *
     * <p>先查主表订单，再按 orderId 关联查出明细列表，组装成聚合 DTO。
     * 逻辑删除的主表与明细记录都会被自动过滤。
     *
     * @param id 订单主键
     * @return 订单信息（含明细）；记录不存在时 data 为 null
     */
    @GetMapping("/order/{id}")
    public ApiResponse<OrderDTO> getOrder(@PathVariable("id") Long id) {
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

    /**
     * 查询全部有效订单。
     *
     * <p>仅返回主表信息，不含明细列表（明细在详情接口中按需获取）。
     * 逻辑删除的记录会被自动过滤。
     *
     * @return 订单列表，已按 DTO 过滤内部字段；无数据时返回空列表
     */
    @GetMapping("/order/list")
    public ApiResponse<List<OrderDTO>> list() {
        List<OrderDTO> list = orderService.list().stream().map(this::toDTO).toList();
        return ApiResponse.success(list);
    }

    /**
     * 分页查询有效订单。
     *
     * <p>仅返回主表信息，不含明细列表。依赖分页拦截器自动改写 SQL，
     * 返回带总数与分页信息的 {@link Page}。
     *
     * @param current 当前页码，从 1 开始，默认 1
     * @param size    每页条数，默认 10
     * @return 分页结果，records 已转为 DTO
     */
    @GetMapping("/order/page")
    public ApiResponse<Page<OrderDTO>> page(
            @RequestParam(value = "current", defaultValue = "1") long current,
            @RequestParam(value = "size", defaultValue = "10") long size) {
        Page<Order> page = orderService.page(new Page<>(current, size));
        Page<OrderDTO> result = new Page<>(page.getCurrent(), page.getSize(), page.getTotal());
        result.setRecords(page.getRecords().stream().map(this::toDTO).toList());
        return ApiResponse.success(result);
    }

    /**
     * 新增订单（仅主表）。
     *
     * <p>DTO 转实体后入库，数据库自增主键会回填到实体，
     * 因此返回的 DTO 含生成后的 orderId。当前仅保存主表，明细需单独维护。
     *
     * @param dto 订单信息
     * @return 含生成主键的完整订单信息
     */
    @PostMapping("/order")
    public ApiResponse<OrderDTO> add(@RequestBody OrderDTO dto) {
        Order order = new Order();
        BeanUtils.copyProperties(dto, order);
        orderService.save(order);
        return ApiResponse.success(toDTO(order));
    }

    /**
     * 根据ID修改订单。
     *
     * <p>基于 MyBatis-Plus 的 updateById，按 orderId 定位记录。
     * 默认只更新 DTO 中非 null 的字段，传 null 的字段保持原值不变。
     *
     * @param dto 待更新信息，必须携带 orderId
     * @return 无业务数据
     */
    @PutMapping("/order")
    public ApiResponse<Void> update(@RequestBody OrderDTO dto) {
        Order order = new Order();
        BeanUtils.copyProperties(dto, order);
        orderService.updateById(order);
        return ApiResponse.success();
    }

    /**
     * 根据ID删除订单。
     *
     * <p>执行逻辑删除：将 is_deleted 置为 1，而非物理删除记录。
     *
     * @param id 订单主键
     * @return 无业务数据
     */
    @DeleteMapping("/order/{id}")
    public ApiResponse<Void> delete(@PathVariable("id") Long id) {
        orderService.removeById(id);
        return ApiResponse.success();
    }

    /**
     * 订单主表实体转对外 DTO。
     *
     * <p>同名属性自动拷贝；isDeleted、updateTime 等内部字段因 DTO 不含，
     * 自然被忽略。明细列表不在此填充，仅在详情接口中组装。
     *
     * @param order 订单实体，允许为 null
     * @return 对外 DTO；入参为 null 时返回 null
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
     * 订单明细实体转对外 DTO。
     *
     * @param detail 明细实体，允许为 null
     * @return 对外 DTO；入参为 null 时返回 null
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