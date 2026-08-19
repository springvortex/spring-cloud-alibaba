package com.zjc.mail.controller;

import com.zjc.common.dto.MailLogDTO;
import com.zjc.common.dto.MailSendDTO;
import com.zjc.common.web.ApiResponse;
import com.zjc.mail.service.MailSendService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * 邮件发送 REST 接口。
 *
 * <p>其他微服务通过 Feign（{@link com.zjc.common.api.mail.MailFeignApi}）
 * 或网关直接调用本接口发送邮件，发件人和 SMTP 配置由邮件模块统一管理。
 *
 * @author jiancai.zhong
 */
@Tag(name = "邮件服务", description = "统一邮件发送")
@RestController
public class MailController {

    @Resource
    private MailSendService mailSendService;

    /**
     * 发送邮件，调用方只需提供收件人、主题、正文等业务字段。
     *
     * @param dto 邮件发送请求
     * @return 邮件发送记录，含主键和发送状态
     */
    @Operation(summary = "发送邮件")
    @PostMapping("/send")
    public ApiResponse<MailLogDTO> send(@Valid @RequestBody MailSendDTO dto) {
        return ApiResponse.success(mailSendService.send(dto));
    }
}
