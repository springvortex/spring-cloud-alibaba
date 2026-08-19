package com.zjc.common.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 用户公共DTO，Feign 跨服务调用传输对象。
 * 放在 common 模块，所有微服务可依赖。
 * 必须实现 Serializable，支持序列化。
 *
 * @author jiancai.zhong
 */
@Schema(description = "用户信息")
@Data
public class UserDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = -3152972968568455762L;

    @Schema(description = "用户ID")
    private Long userId;

    @Schema(description = "登录账号", example = "zhangsan")
    @NotBlank(message = "登录账号不能为空")
    @Size(min = 3, max = 20, message = "账号长度需在3-20个字符之间")
    private String username;

    @Schema(description = "昵称", example = "张三")
    @Size(max = 50, message = "昵称长度不能超过50个字符")
    private String nickName;

    @Schema(description = "手机号", example = "13800138000")
    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式不正确")
    private String phone;

    @Schema(description = "邮箱", example = "zhangsan@example.com")
    @Email(message = "邮箱格式不正确")
    @Size(max = 100, message = "邮箱长度不能超过100个字符")
    private String email;

    @Schema(description = "状态 1正常 0禁用")
    private Integer status;

    @Schema(description = "性别 1男 2女")
    private Integer gender;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;
}
