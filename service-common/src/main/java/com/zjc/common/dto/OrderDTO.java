package com.zjc.common.dto;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class OrderDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 7163373472202107281L;

    /**
     * 订单ID
     */
    private Long orderId;

    /**
     * 订单编号
     */
    private String orderNo;

    /**
     * 下单用户ID
     */
    private Long userId;

    /**
     * 订单总金额
     */
    private BigDecimal totalAmount;

    /**
     * 实付金额
     */
    private BigDecimal payAmount;

    /**
     * 订单状态 0待支付 1已支付 2已发货 3已完成 4已取消
     */
    private Integer orderStatus;

    /**
     * 支付时间
     */
    private LocalDateTime payTime;

    /**
     * 下单时间
     */
    private LocalDateTime createTime;

    /**
     * 订单明细列表
     */
    private List<OrderDetailDTO> orderDetails;
}