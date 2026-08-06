package com.zjc.common.dto;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

@Data
public class UserDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = -3152972968568455762L;

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 登录账号
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
     * 状态 1正常 0禁用
     */
    private Integer status;

    /**
     * 性别 1男 2女
     */
    private Integer gender;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;
}