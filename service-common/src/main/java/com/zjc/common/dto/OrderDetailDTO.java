package com.zjc.common.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 订单明细公共DTO，Feign 跨服务调用传输对象。
 * 放在 common 模块，所有微服务可依赖。
 * 必须实现 Serializable，支持序列化。
 *
 * @author jiancai.zhong
 */
@Schema(description = "订单明细信息")
@Data
public class OrderDetailDTO implements Serializable {

    /**
     * 金额字段的非负下限，供校验注解引用
     */
    private static final String AMOUNT_MIN = "0";

    @Serial
    private static final long serialVersionUID = 6420175538901248853L;

    @Schema(description = "明细ID")
    private Long id;

    @Schema(description = "商品ID")
    @NotNull(message = "订单明细商品ID不能为空")
    private Long goodsId;

    @Schema(description = "商品名称（下单快照）")
    @NotBlank(message = "订单明细商品名称不能为空")
    @Size(max = 100, message = "订单明细商品名称不能超过100个字符")
    private String goodsName;

    @Schema(description = "成交单价（下单快照）")
    @NotNull(message = "订单明细成交单价不能为空")
    @DecimalMin(value = AMOUNT_MIN, message = "订单明细成交单价不能为负数")
    private BigDecimal goodsPrice;

    @Schema(description = "购买数量")
    @NotNull(message = "订单明细购买数量不能为空")
    @Min(value = 1, message = "订单明细购买数量至少为1")
    private Integer goodsNum;

    @Schema(description = "小计金额")
    @NotNull(message = "订单明细小计金额不能为空")
    @DecimalMin(value = AMOUNT_MIN, message = "订单明细小计金额不能为负数")
    private BigDecimal subTotal;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;
}
