package com.zjc.common.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 用户公共DTO，Feign 跨服务调用传输对象。
 * 放在 common 模块，所有微服务可依赖。
 * 必须实现 Serializable，支持序列化。
 */
@Schema(description = "用户信息")
@Data
public class UserDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = -3152972968568455762L;

    @Schema(description = "用户ID")
    private Long userId;

    @Schema(description = "登录账号")
    private String username;

    @Schema(description = "昵称")
    private String nickName;

    @Schema(description = "手机号")
    private String phone;

    @Schema(description = "邮箱")
    private String email;

    @Schema(description = "状态 1正常 0禁用")
    private Integer status;

    @Schema(description = "性别 1男 2女")
    private Integer gender;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;
}