package com.zjc.provider.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * <p>
 * 订单明细表
 * </p>
 *
 * @author jiancai.zhong
 * @since 2026-08-06
 */
@Data
@TableName("t_order_detail")
public class OrderDetail implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 明细主键
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 关联订单主表order_id
     */
    @TableField("order_id")
    private Long orderId;

    /**
     * 订单号冗余，方便查询
     */
    @TableField("order_no")
    private String orderNo;

    /**
     * 商品ID，关联t_goods
     */
    @TableField("goods_id")
    private Long goodsId;

    /**
     * 下单快照：商品名称
     */
    @TableField("goods_name")
    private String goodsName;

    /**
     * 下单快照：当时成交单价
     */
    @TableField("goods_price")
    private BigDecimal goodsPrice;

    /**
     * 购买数量
     */
    @TableField("goods_num")
    private Integer goodsNum;

    /**
     * 该商品小计金额
     */
    @TableField("sub_total")
    private BigDecimal subTotal;

    /**
     * 逻辑删除
     */
    @TableField("is_deleted")
    private Byte isDeleted;

    @TableField("create_time")
    private LocalDateTime createTime;

    @TableField("update_time")
    private LocalDateTime updateTime;
}
