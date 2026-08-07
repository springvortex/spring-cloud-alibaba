package com.zjc.common.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 邮件发送请求 DTO，供其他微服务通过 Feign 调用邮件模块时使用。
 *
 * <p>调用方只需填写收件人、主题、正文等业务字段，发件人和 SMTP 配置由邮件模块统一管理。
 * 多个邮箱地址用英文逗号分隔，如 {@code "a@xx.com,b@xx.com"}。
 *
 * @author jiancai.zhong
 */
@Schema(description = "邮件发送请求")
@Data
public class MailSendDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 收件人邮箱，多个用英文逗号分隔。
     */
    @Schema(description = "收件人邮箱（多个逗号分隔）", example = "zhangsan@xx.com,lisi@xx.com")
    @NotBlank(message = "收件人不能为空")
    @Size(max = 2000, message = "收件人列表过长")
    private String toEmails;

    /**
     * 抄送人邮箱，多个用英文逗号分隔，可选。
     */
    @Schema(description = "抄送人邮箱（多个逗号分隔）", example = "wangwu@xx.com")
    @Size(max = 2000, message = "抄送人列表过长")
    private String ccEmails;

    /**
     * 密送人邮箱，多个用英文逗号分隔，可选。
     */
    @Schema(description = "密送人邮箱（多个逗号分隔）")
    @Size(max = 2000, message = "密送人列表过长")
    private String bccEmails;

    /**
     * 邮件主题。
     */
    @Schema(description = "邮件主题", example = "验证码通知")
    @NotBlank(message = "邮件主题不能为空")
    @Size(max = 500, message = "邮件主题长度不能超过500个字符")
    private String subject;

    /**
     * 邮件正文。
     */
    @Schema(description = "邮件正文", example = "您的验证码是 123456，5分钟内有效。")
    @NotBlank(message = "邮件正文不能为空")
    private String content;

    /**
     * 是否 HTML 格式，默认 false（纯文本）。
     */
    @Schema(description = "是否HTML格式", example = "false")
    private Boolean isHtml = false;
}
