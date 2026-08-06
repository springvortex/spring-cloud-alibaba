package com.zjc.provider.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * <p>
 * 用户表
 * </p>
 *
 * @author jiancai.zhong
 * @since 2026-08-06
 */
@Data
@TableName("t_user")
public class User implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 用户主键
     */
    @TableId(value = "user_id", type = IdType.AUTO)
    private Long userId;

    /**
     * 登录账号
     */
    @TableField("username")
    private String username;

    /**
     * 昵称
     */
    @TableField("nick_name")
    private String nickName;

    /**
     * 手机号
     */
    @TableField("phone")
    private String phone;

    /**
     * 邮箱
     */
    @TableField("email")
    private String email;

    /**
     * 状态 1正常 0禁用
     */
    @TableField("status")
    private Byte status;

    /**
     * 性别 1男 2女
     */
    @TableField("gender")
    private Byte gender;

    /**
     * 逻辑删除 0未删 1已删
     */
    @TableField("is_deleted")
    private Byte isDeleted;

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
