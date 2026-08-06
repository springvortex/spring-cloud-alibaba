package com.zjc.common.dto;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class GoodsDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 5312489017764153260L;

    /**
     * 商品ID
     */
    private Long goodsId;

    /**
     * 商品名称
     */
    private String goodsName;

    /**
     * 商品售价
     */
    private BigDecimal goodsPrice;

    /**
     * 库存数量
     */
    private Integer stock;

    /**
     * 状态 1上架 0下架
     */
    private Integer status;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;
}