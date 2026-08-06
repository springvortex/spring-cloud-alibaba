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
 * 商品表
 * </p>
 *
 * @author jiancai.zhong
 * @since 2026-08-06
 */
@Data
@TableName("t_goods")
public class Goods implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 商品主键
     */
    @TableId(value = "goods_id", type = IdType.AUTO)
    private Long goodsId;

    /**
     * 商品名称
     */
    @TableField("goods_name")
    private String goodsName;

    /**
     * 商品售价
     */
    @TableField("goods_price")
    private BigDecimal goodsPrice;

    /**
     * 库存数量
     */
    @TableField("stock")
    private Integer stock;

    /**
     * 状态 1上架 0下架
     */
    @TableField("status")
    private Byte status;

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
