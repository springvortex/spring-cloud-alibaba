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
 * 商品公共DTO，Feign 跨服务调用传输对象。
 * 放在 common 模块，所有微服务可依赖。
 * 必须实现 Serializable，支持序列化。
 *
 * @author jiancai.zhong
 */
@Schema(description = "商品信息")
@Data
public class GoodsDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 5312489017764153260L;

    @Schema(description = "商品ID")
    private Long goodsId;

    @Schema(description = "商品名称", example = "iPhone 16")
    @NotBlank(message = "商品名称不能为空")
    @Size(max = 100, message = "商品名称长度不能超过100个字符")
    private String goodsName;

    @Schema(description = "商品售价", example = "6999.00")
    @NotNull(message = "商品售价不能为空")
    @DecimalMin(value = "0", message = "商品售价不能为负数")
    private BigDecimal goodsPrice;

    @Schema(description = "库存数量", example = "100")
    @Min(value = 0, message = "库存数量不能为负数")
    private Integer stock;

    @Schema(description = "状态 1上架 0下架")
    private Integer status;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;
}
