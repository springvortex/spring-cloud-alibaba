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
 * 订单明细表实体，映射 t_order_detail。
 *
 * <p>记录订单中每个商品的成交快照（名称、单价、数量、小计）。
 * 通过 order_id 关联订单主表。
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
     * 明细主键，数据库自增
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 关联订单主表 order_id
     */
    @TableField("order_id")
    private Long orderId;

    /**
     * 订单编号（冗余，方便查询）
     */
    @TableField("order_no")
    private String orderNo;

    /**
     * 商品ID，关联 t_goods
     */
    @TableField("goods_id")
    private Long goodsId;

    /**
     * 商品名称（下单快照）
     */
    @TableField("goods_name")
    private String goodsName;

    /**
     * 成交单价（下单快照）
     */
    @TableField("goods_price")
    private BigDecimal goodsPrice;

    /**
     * 购买数量
     */
    @TableField("goods_num")
    private Integer goodsNum;

    /**
     * 小计金额
     */
    @TableField("sub_total")
    private BigDecimal subTotal;

    /**
     * 逻辑删除 0未删 1已删
     */
    @TableField("is_deleted")
    private Integer isDeleted;

    /**
     * 创建时间
     */
    @TableField("create_time")
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    @TableField("update_time")
    private LocalDateTime updateTime;
}