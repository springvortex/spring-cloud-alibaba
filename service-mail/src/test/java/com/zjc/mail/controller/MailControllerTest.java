package com.zjc.mail.controller;

import com.zjc.common.dto.MailLogDTO;
import com.zjc.common.dto.MailSendDTO;
import com.zjc.common.web.ApiResponse;
import com.zjc.mail.service.MailSendService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * {@link MailController} 单元测试。
 *
 * <p>验证 Controller 正确委托给 {@link MailSendService}。
 *
 * @author jiancai.zhong
 */
@DisplayName("邮件服务 Controller")
@ExtendWith(MockitoExtension.class)
class MailControllerTest {

    @Mock
    private MailSendService mailSendService;

    @InjectMocks
    private MailController mailController;

    /**
     * 验证 send 接口正确委托给 MailSendService 并返回结果。
     */
    @Test
    @DisplayName("send: 委托给 MailSendService")
    void testSendDelegates() {
        MailSendDTO dto = new MailSendDTO();
        dto.setToEmails("a@xx.com");
        dto.setSubject("测试");
        dto.setContent("内容");
        MailLogDTO logDTO = new MailLogDTO();
        logDTO.setMailId(1L);
        logDTO.setStatus(1);
        when(mailSendService.send(any(MailSendDTO.class))).thenReturn(logDTO);

        ApiResponse<MailLogDTO> resp = mailController.send(dto);

        assertThat(resp.isSuccess()).isTrue();
        assertThat(resp.getData().getMailId()).isEqualTo(1L);
        assertThat(resp.getData().getStatus()).isEqualTo(1);
        verify(mailSendService).send(any(MailSendDTO.class));
    }
}
