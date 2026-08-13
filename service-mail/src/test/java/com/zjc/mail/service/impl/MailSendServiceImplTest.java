package com.zjc.mail.service.impl;

import com.zjc.common.dto.MailLogDTO;
import com.zjc.common.dto.MailSendDTO;
import com.zjc.mail.converter.MailLogConverter;
import com.zjc.mail.entity.MailLog;
import com.zjc.mail.service.MailLogService;
import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * {@link MailSendServiceImpl} 单元测试。
 *
 * <p>验证邮件发送的核心流程：先入库待发送记录、发送成功/失败更新状态。
 * 纯文本与 HTML 两种模式、抄送密送设置、非法邮箱地址拦截。
 *
 * @author jiancai.zhong
 */
@DisplayName("邮件发送服务")
@ExtendWith(MockitoExtension.class)
class MailSendServiceImplTest {

    @Mock
    private JavaMailSender mailSender;

    @Mock
    private MailLogService mailLogService;

    @Mock
    private MailLogConverter mailLogConverter;

    @InjectMocks
    private MailSendServiceImpl mailSendService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(mailSendService, "fromEmail", "noreply@xx.com");
        lenient().when(mailLogConverter.entityToDto(any(MailLog.class))).thenAnswer(inv -> {
            MailLog log = inv.getArgument(0);
            MailLogDTO dto = new MailLogDTO();
            dto.setStatus(log.getStatus());
            dto.setFromEmail(log.getFromEmail());
            dto.setErrorMsg(log.getErrorMsg());
            return dto;
        });
    }

    /**
     * 设置 mailSender.createMimeMessage() 的返回值，供 HTML 相关测试需要。
     */
    private void mockMimeMessage() {
        when(mailSender.createMimeMessage()).thenReturn(
                new MimeMessage(Session.getInstance(new Properties())));
    }

    /**
     * 验证发送纯文本邮件成功后，记录状态更新为成功。
     */
    @Test
    @DisplayName("send: 纯文本邮件发送成功")
    void testSendTextMailSuccess() {
        MailSendDTO dto = buildDto("a@xx.com", null, null, "测试", "内容", false);

        MailLogDTO result = mailSendService.send(dto);

        assertThat(result.getStatus()).isEqualTo(1);
        assertThat(result.getFromEmail()).isEqualTo("noreply@xx.com");
        verify(mailLogService).save(any(MailLog.class));
        verify(mailLogService).updateById(any(MailLog.class));
        verify(mailSender).send(any(SimpleMailMessage.class));
    }

    /**
     * 验证发送 HTML 邮件成功后，记录状态更新为成功。
     */
    @Test
    @DisplayName("send: HTML 邮件发送成功")
    void testSendHtmlMailSuccess() {
        mockMimeMessage();
        MailSendDTO dto = buildDto("a@xx.com", null, null, "测试", "<h1>内容</h1>", true);

        MailLogDTO result = mailSendService.send(dto);

        assertThat(result.getStatus()).isEqualTo(1);
        verify(mailSender).send(any(MimeMessage.class));
    }

    /**
     * 验证发送失败时记录状态更新为失败，并保存错误信息。
     */
    @Test
    @DisplayName("send: 发送失败时记录状态为失败并保存错误信息")
    void testSendMailFailure() {
        MailSendDTO dto = buildDto("a@xx.com", null, null, "测试", "内容", false);
        doThrow(new RuntimeException("SMTP 连接失败"))
                .when(mailSender).send(any(SimpleMailMessage.class));

        MailLogDTO result = mailSendService.send(dto);

        assertThat(result.getStatus()).isEqualTo(2);
        assertThat(result.getErrorMsg()).isEqualTo("SMTP 连接失败");
        verify(mailLogService).updateById(any(MailLog.class));
    }

    /**
     * 验证抄送和密送地址被正确传递给 SimpleMailMessage。
     */
    @Test
    @DisplayName("send: 抄送和密送正确设置")
    void testSendWithCcAndBcc() {
        MailSendDTO dto = buildDto("a@xx.com", "cc@xx.com", "bcc@xx.com", "测试", "内容", false);

        mailSendService.send(dto);

        ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(captor.capture());
        assertThat(captor.getValue().getCc()).contains("cc@xx.com");
        assertThat(captor.getValue().getBcc()).contains("bcc@xx.com");
    }

    /**
     * 验证多收件人（逗号分隔）获正确拆分。
     */
    @Test
    @DisplayName("send: 多收件人正确拆分")
    void testSendMultipleRecipients() {
        MailSendDTO dto = buildDto("a@xx.com,b@xx.com", null, null, "测试", "内容", false);

        mailSendService.send(dto);

        ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(captor.capture());
        assertThat(captor.getValue().getTo()).containsExactly("a@xx.com", "b@xx.com");
    }

    /**
     * 验证密送地址格式非法（如 Swagger 默认占位值 "string"）时直接抛出异常，不写库不发送。
     */
    @Test
    @DisplayName("send: 非法密送地址直接拦截")
    void testSendInvalidBccRejected() {
        MailSendDTO dto = buildDto("a@xx.com", null, "string", "测试", "内容", false);

        assertThatThrownBy(() -> mailSendService.send(dto))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("密送人邮箱格式不正确");

        verifyNoInteractions(mailLogService);
        verifyNoInteractions(mailSender);
    }

    /**
     * 验证收件人地址格式非法时直接抛出异常。
     */
    @Test
    @DisplayName("send: 非法收件人地址直接拦截")
    void testSendInvalidToRejected() {
        MailSendDTO dto = buildDto("not-an-email", null, null, "测试", "内容", false);

        assertThatThrownBy(() -> mailSendService.send(dto))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("收件人邮箱格式不正确");
    }

    /**
     * 构建测试用 MailSendDTO。
     */
    private MailSendDTO buildDto(String to, String cc, String bcc,
                                 String subject, String content, boolean isHtml) {
        MailSendDTO dto = new MailSendDTO();
        dto.setToEmails(to);
        dto.setCcEmails(cc);
        dto.setBccEmails(bcc);
        dto.setSubject(subject);
        dto.setContent(content);
        dto.setIsHtml(isHtml);
        return dto;
    }
}
