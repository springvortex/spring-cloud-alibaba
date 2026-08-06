package com.zjc.common.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 订单明细公共DTO，Feign 跨服务调用传输对象。
 * 放在 common 模块，所有微服务可依赖。
 * 必须实现 Serializable，支持序列化。
 */
@Schema(description = "订单明细信息")
@Data
public class OrderDetailDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 6420175538901248853L;

    @Schema(description = "明细ID")
    private Long id;

    @Schema(description = "商品ID")
    private Long goodsId;

    @Schema(description = "商品名称（下单快照）")
    private String goodsName;

    @Schema(description = "成交单价（下单快照）")
    private BigDecimal goodsPrice;

    @Schema(description = "购买数量")
    private Integer goodsNum;

    @Schema(description = "小计金额")
    private BigDecimal subTotal;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;
}