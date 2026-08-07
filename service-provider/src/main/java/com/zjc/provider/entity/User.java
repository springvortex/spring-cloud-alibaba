package com.zjc.provider.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 用户表实体，映射 t_user。
 *
 * <p>仅 provider 模块内部使用，对外传输请用
 * {@link com.zjc.common.dto.UserDTO}，避免暴露逻辑删除等内部字段。
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
     * 用户主键，数据库自增
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
    private Integer status;

    /**
     * 性别 1男 2女
     */
    @TableField("gender")
    private Integer gender;

    /**
     * 逻辑删除 0未删 1已删
     */
    @TableField("is_deleted")
    private Integer isDeleted;

    /**
     * 创建时间
     */
    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    @TableField(value = "update_time", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}