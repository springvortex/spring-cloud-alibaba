package com.zjc.common.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 商品购买结果 DTO。
 *
 * @author jiancai.zhong
 */
@Schema(description = "商品购买结果")
@Data
public class GoodsPurchaseResponseDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 7844179458506903824L;

    @Schema(description = "订单ID")
    private Long orderId;

    @Schema(description = "订单编号")
    private String orderNo;

    @Schema(description = "购买用户ID")
    private Long userId;

    @Schema(description = "商品ID")
    private Long goodsId;

    @Schema(description = "商品名称快照")
    private String goodsName;

    @Schema(description = "购买数量")
    private Integer quantity;

    @Schema(description = "实付金额")
    private BigDecimal payAmount;

    @Schema(description = "购买后剩余库存")
    private Integer remainingStock;
}
