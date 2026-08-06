package com.zjc.common.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 商品公共DTO，Feign 跨服务调用传输对象。
 * 放在 common 模块，所有微服务可依赖。
 * 必须实现 Serializable，支持序列化。
 */
@Schema(description = "商品信息")
@Data
public class GoodsDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 5312489017764153260L;

    @Schema(description = "商品ID")
    private Long goodsId;

    @Schema(description = "商品名称")
    private String goodsName;

    @Schema(description = "商品售价")
    private BigDecimal goodsPrice;

    @Schema(description = "库存数量")
    private Integer stock;

    @Schema(description = "状态 1上架 0下架")
    private Integer status;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;
}