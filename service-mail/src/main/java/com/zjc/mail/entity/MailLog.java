package com.zjc.mail.entity;

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
 * 邮件发送记录实体，映射 t_mail_log。
 *
 * <p>仅 mail 模块内部使用，对外传输请用
 * {@link com.zjc.common.dto.MailLogDTO}，避免暴露逻辑删除等内部字段。
 * 主键使用雪花算法（{@link IdType#ASSIGN_ID}），分布式下全局唯一。
 *
 * @author jiancai.zhong
 */
@Data
@TableName("t_mail_log")
public class MailLog implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 邮件记录主键，雪花算法生成
     */
    @TableId(value = "mail_id", type = IdType.ASSIGN_ID)
    private Long mailId;

    /**
     * 发件人邮箱
     */
    @TableField("from_email")
    private String fromEmail;

    /**
     * 收件人邮箱，多个逗号分隔
     */
    @TableField("to_emails")
    private String toEmails;

    /**
     * 抄送人邮箱，多个逗号分隔
     */
    @TableField("cc_emails")
    private String ccEmails;

    /**
     * 密送人邮箱，多个逗号分隔
     */
    @TableField("bcc_emails")
    private String bccEmails;

    /**
     * 邮件主题
     */
    @TableField("subject")
    private String subject;

    /**
     * 邮件正文
     */
    @TableField("content")
    private String content;

    /**
     * 是否 HTML 1是 0否
     */
    @TableField("is_html")
    private Integer isHtml;

    /**
     * 发送状态 0待发送 1成功 2失败
     */
    @TableField("status")
    private Integer status;

    /**
     * 失败原因
     */
    @TableField("error_msg")
    private String errorMsg;

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
