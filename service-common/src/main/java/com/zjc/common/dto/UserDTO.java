package com.zjc.common.dto;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 用户公共DTO，Feign跨服务调用传输对象
 * 放在 common-model 模块，所有微服务可依赖
 * 必须实现 Serializable，支持序列化
 */
@Data
public class UserDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = -3152972968568455762L;

    /**
     * 用户id
     */
    private Long userId;

    /**
     * 账号
     */
    private String username;

    /**
     * 昵称
     */
    private String nickName;

    /**
     * 手机号
     */
    private String phone;

    /**
     * 邮箱
     */
    private String email;

    /**
     * 用户状态 0禁用 1正常
     */
    private Integer status;

    /**
     * 性别 0未知 1男 2女
     */
    private Integer gender;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;
}