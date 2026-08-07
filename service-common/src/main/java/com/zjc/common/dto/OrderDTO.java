package com.zjc.common.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 订单公共DTO，Feign 跨服务调用传输对象。
 * 放在 common 模块，所有微服务可依赖。
 * 必须实现 Serializable，支持序列化。
 */
@Schema(description = "订单信息")
@Data
public class OrderDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 7163373472202107281L;

    @Schema(description = "订单ID")
    private Long orderId;

    @Schema(description = "订单编号")
    private String orderNo;

    @Schema(description = "下单用户ID")
    @NotNull(message = "下单用户ID不能为空")
    private Long userId;

    @Schema(description = "订单总金额")
    @NotNull(message = "订单总金额不能为空")
    @DecimalMin(value = "0", message = "订单总金额不能为负数")
    private BigDecimal totalAmount;

    @Schema(description = "实付金额")
    @DecimalMin(value = "0", message = "实付金额不能为负数")
    private BigDecimal payAmount;

    @Schema(description = "订单状态 0待支付 1已支付 2已发货 3已完成 4已取消")
    private Integer orderStatus;

    @Schema(description = "支付时间")
    private LocalDateTime payTime;

    @Schema(description = "下单时间")
    private LocalDateTime createTime;

    @Schema(description = "订单明细列表")
    private List<OrderDetailDTO> orderDetails;
}