package com.zjc.common.dto;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class OrderDetailDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 6420175538901248853L;

    /**
     * 明细ID
     */
    private Long id;

    /**
     * 商品ID
     */
    private Long goodsId;

    /**
     * 商品名称（下单快照）
     */
    private String goodsName;

    /**
     * 成交单价（下单快照）
     */
    private BigDecimal goodsPrice;

    /**
     * 购买数量
     */
    private Integer goodsNum;

    /**
     * 小计金额
     */
    private BigDecimal subTotal;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;
}