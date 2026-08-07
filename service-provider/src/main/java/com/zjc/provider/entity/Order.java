package com.zjc.provider.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
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
 * 订单主表实体，映射 t_order。
 *
 * <p>仅 provider 模块内部使用，对外传输请用
 * {@link com.zjc.common.dto.OrderDTO}，避免暴露逻辑删除等内部字段。
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
     * 订单主键，数据库自增
     */
    @TableId(value = "order_id", type = IdType.AUTO)
    private Long orderId;

    /**
     * 订单编号
     */
    @TableField("order_no")
    private String orderNo;

    /**
     * 下单用户ID
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
     * 订单状态 0待支付 1已支付 2已发货 3已完成 4已取消
     */
    @TableField("order_status")
    private Integer orderStatus;

    /**
     * 支付时间，未支付时为 null
     */
    @TableField("pay_time")
    private LocalDateTime payTime;

    /**
     * 逻辑删除 0未删 1已删
     */
    @TableField("is_deleted")
    private Integer isDeleted;

    /**
     * 下单时间
     */
    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    @TableField(value = "update_time", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}