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
 * 订单主表
 * </p>
 *
 * @author jiancai.zhong
 * @since 2026-08-06
 */
@Data
@TableName("t_order")
public class Order implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 订单主键
     */
    @TableId(value = "order_id", type = IdType.AUTO)
    private Long orderId;

    /**
     * 订单编号
     */
    @TableField("order_no")
    private String orderNo;

    /**
     * 下单用户id
     */
    @TableField("user_id")
    private Long userId;

    /**
     * 订单总金额
     */
    @TableField("total_amount")
    private BigDecimal totalAmount;

    /**
     * 实付金额
     */
    @TableField("pay_amount")
    private BigDecimal payAmount;

    /**
     * 订单状态：0待支付 1已支付 2已发货 3已完成 4已取消
     */
    @TableField("order_status")
    private Byte orderStatus;

    /**
     * 支付时间，未支付为NULL
     */
    @TableField("pay_time")
    private LocalDateTime payTime;

    /**
     * 逻辑删除
     */
    @TableField("is_deleted")
    private Byte isDeleted;

    /**
     * 下单时间
     */
    @TableField("create_time")
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    @TableField("update_time")
    private LocalDateTime updateTime;
}
