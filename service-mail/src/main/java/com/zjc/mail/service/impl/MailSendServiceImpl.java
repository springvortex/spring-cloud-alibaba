package com.zjc.mail.service.impl;

import com.zjc.common.dto.MailLogDTO;
import com.zjc.common.dto.MailSendDTO;
import com.zjc.mail.converter.MailLogConverter;
import com.zjc.mail.entity.MailLog;
import com.zjc.mail.service.MailLogService;
import com.zjc.mail.service.MailSendService;
import jakarta.annotation.Resource;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.util.regex.Pattern;

/**
 * 邮件发送服务实现。
 *
 * <p>发件人地址从配置 {@code spring.mail.username} 读取，
 * SMTP 连接参数也在邮件模块环境 Profile 中统一管理。
 * 发送流程：先入库一条待发送记录 -> 发送邮件 -> 更新状态为成功/失败。
 *
 * @author jiancai.zhong
 */
@Slf4j
@Service
public class MailSendServiceImpl implements MailSendService {

    /**
     * 邮箱地址正则，校验 local@domain 基本格式。
     */
    private static final Pattern EMAIL_PATTERN =
            Pattern.compile("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$");

    /**
     * 待发送
     */
    private static final int STATUS_PENDING = 0;

    /**
     * 发送成功
     */
    private static final int STATUS_SUCCESS = 1;

    /**
     * 发送失败
     */
    private static final int STATUS_FAILURE = 2;

    @Resource
    private JavaMailSender mailSender;

    @Resource
    private MailLogService mailLogService;

    @Resource
    private MailLogConverter mailLogConverter;

    /**
     * 发件人邮箱，来自环境 Profile 的 spring.mail.username
     */
    @Value("${spring.mail.username}")
    private String fromEmail;

    @Override
    public MailLogDTO send(MailSendDTO dto) {
        // 0. 校验收件人/抄送/密送地址格式，非法地址直接拦截，不写库不发送
        String[] toArray = parseAndValidate(dto.getToEmails(), "收件人");
        String[] ccArray = parseAndValidate(dto.getCcEmails(), "抄送人");
        String[] bccArray = parseAndValidate(dto.getBccEmails(), "密送人");

        // 1. 先入库一条待发送记录
        MailLog mailLog = buildMailLog(dto);
        mailLog.setStatus(STATUS_PENDING);
        mailLogService.save(mailLog);

        // 2. 发送邮件
        try {
            if (Boolean.TRUE.equals(dto.getIsHtml())) {
                sendHtmlMail(dto, toArray, ccArray, bccArray);
            } else {
                sendTextMail(dto, toArray, ccArray, bccArray);
            }
            mailLog.setStatus(STATUS_SUCCESS);
            log.info("邮件发送成功，mailId={}, to={}", mailLog.getMailId(), dto.getToEmails());
        } catch (Exception e) {
            mailLog.setStatus(STATUS_FAILURE);
            mailLog.setErrorMsg(e.getMessage());
            log.error("邮件发送失败，mailId={}, to={}", mailLog.getMailId(), dto.getToEmails(), e);
        }

        // 3. 更新记录状态
        mailLogService.updateById(mailLog);
        return mailLogConverter.entityToDto(mailLog);
    }

    /**
     * 发送纯文本邮件。
     *
     * @param dto 邮件请求
     * @param to  已校验的收件人数组
     * @param cc  已校验的抄送人数组，可为 null
     * @param bcc 已校验的密送人数组，可为 null
     */
    private void sendTextMail(MailSendDTO dto, String[] to, String[] cc, String[] bcc) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromEmail);
        message.setTo(to);
        message.setSubject(dto.getSubject());
        message.setText(dto.getContent());
        setCcAndBcc(message, cc, bcc);
        mailSender.send(message);
    }

    /**
     * 发送 HTML 格式邮件。
     *
     * @param dto 邮件请求
     * @param to  已校验的收件人数组
     * @param cc  已校验的抄送人数组，可为 null
     * @param bcc 已校验的密送人数组，可为 null
     */
    private void sendHtmlMail(MailSendDTO dto, String[] to, String[] cc, String[] bcc) throws MessagingException {
        MimeMessage mimeMessage = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true);
        helper.setFrom(fromEmail);
        helper.setTo(to);
        helper.setSubject(dto.getSubject());
        helper.setText(dto.getContent(), true);
        setCcAndBcc(helper, cc, bcc);
        mailSender.send(mimeMessage);
    }

    /**
     * 为 SimpleMailMessage 设置抄送和密送（仅当非空时设置）。
     *
     * @param message 邮件消息
     * @param cc      已校验的抄送人数组，可为 {@code null}
     * @param bcc     已校验的密送人数组，可为 {@code null}
     */
    private void setCcAndBcc(SimpleMailMessage message, String[] cc, String[] bcc) {
        if (cc != null && cc.length > 0) {
            message.setCc(cc);
        }
        if (bcc != null && bcc.length > 0) {
            message.setBcc(bcc);
        }
    }

    /**
     * 为 MimeMessageHelper 设置抄送和密送（仅当非空时设置）。
     *
     * @param helper MIME 消息助手
     * @param cc     已校验的抄送人数组，可为 {@code null}
     * @param bcc    已校验的密送人数组，可为 {@code null}
     * @throws MessagingException 设置抄送/密送时发生错误
     */
    private void setCcAndBcc(MimeMessageHelper helper, String[] cc, String[] bcc) throws MessagingException {
        if (cc != null && cc.length > 0) {
            helper.setCc(cc);
        }
        if (bcc != null && bcc.length > 0) {
            helper.setBcc(bcc);
        }
    }

    /**
     * 解析逗号分隔的邮箱列表，逐个校验格式，返回清洗后的数组。
     *
     * <p>对于可选字段（抄送、密送），传入 {@code null} 或空白时返回 {@code null}，表示不设置。
     * 对于必填字段（收件人），空值会抛出异常。
     * 每个地址会 trim 后做基本格式校验（local@domain），非法地址立即拦截。
     *
     * @param raw       原始逗号分隔字符串
     * @param fieldName 字段中文名，用于异常提示
     * @return 清洗后的邮箱数组，空白输入返回 {@code null}
     * @throws IllegalArgumentException 当存在格式非法的邮箱地址时
     */
    private String[] parseAndValidate(String raw, String fieldName) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        String[] emails = raw.split(",");
        for (int i = 0; i < emails.length; i++) {
            emails[i] = emails[i].trim();
            if (!EMAIL_PATTERN.matcher(emails[i]).matches()) {
                throw new IllegalArgumentException(
                        fieldName + "邮箱格式不正确：" + emails[i]);
            }
        }
        return emails;
    }

    /**
     * 将 MailSendDTO 构建为 MailLog 实体。
     *
     * @param dto 邮件发送请求
     * @return 待持久化的邮件记录实体
     */
    private MailLog buildMailLog(MailSendDTO dto) {
        MailLog mailLog = new MailLog();
        mailLog.setFromEmail(fromEmail);
        mailLog.setToEmails(dto.getToEmails());
        mailLog.setCcEmails(dto.getCcEmails());
        mailLog.setBccEmails(dto.getBccEmails());
        mailLog.setSubject(dto.getSubject());
        mailLog.setContent(dto.getContent());
        mailLog.setIsHtml(Boolean.TRUE.equals(dto.getIsHtml()) ? 1 : 0);
        return mailLog;
    }
}
