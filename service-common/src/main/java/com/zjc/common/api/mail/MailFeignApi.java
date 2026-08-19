package com.zjc.common.api.mail;

import com.zjc.common.dto.MailLogDTO;
import com.zjc.common.dto.MailSendDTO;
import com.zjc.common.web.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * 邮件服务共享 Feign 客户端，远程调用 service-mail 的邮件发送接口。
 *
 * <p>放在 common 模块的 {@code api} 包下，所有依赖 common 的服务
 * 都能直接注入并复用这个 Feign 声明，无需各自重复定义。
 *
 * <p>{@code contextId = "mailFeignApi"} 用于在 OpenFeign 配置中隔离本客户端的
 * 超时、拦截器等子配置，避免与指向其他服务的客户端产生配置冲突。
 *
 * @author jiancai.zhong
 */
@FeignClient(value = "service-mail", contextId = "mailFeignApi")
public interface MailFeignApi {

    /**
     * 发送邮件，调用方只需提供收件人、主题、正文等业务字段。
     *
     * @param dto 邮件发送请求
     * @return 邮件发送记录，含主键和发送状态
     */
    @PostMapping("/send")
    ApiResponse<MailLogDTO> send(@Valid @RequestBody MailSendDTO dto);
}
