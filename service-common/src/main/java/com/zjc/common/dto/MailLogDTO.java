package com.zjc.common.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 邮件记录响应 DTO，邮件发送后返回给调用方。
 *
 * <p>包含邮件记录主键（雪花算法生成）和发送状态，调用方可据此查询发送结果。
 *
 * @author jiancai.zhong
 */
@Schema(description = "邮件发送记录")
@Data
public class MailLogDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 邮件记录主键（雪花算法）
     */
    @Schema(description = "邮件记录ID")
    private Long mailId;

    /**
     * 发件人邮箱（来自邮件模块配置，非调用方传入）
     */
    @Schema(description = "发件人邮箱")
    private String fromEmail;

    /**
     * 收件人邮箱
     */
    @Schema(description = "收件人邮箱")
    private String toEmails;

    /**
     * 抄送人邮箱
     */
    @Schema(description = "抄送人邮箱")
    private String ccEmails;

    /**
     * 密送人邮箱
     */
    @Schema(description = "密送人邮箱")
    private String bccEmails;

    /**
     * 邮件主题
     */
    @Schema(description = "邮件主题")
    private String subject;

    /**
     * 发送状态 0待发送 1成功 2失败
     */
    @Schema(description = "发送状态 0待发送 1成功 2失败")
    private Integer status;

    /**
     * 失败原因（发送失败时填充）
     */
    @Schema(description = "失败原因")
    private String errorMsg;

    /**
     * 创建时间
     */
    @Schema(description = "创建时间")
    private LocalDateTime createTime;
}
