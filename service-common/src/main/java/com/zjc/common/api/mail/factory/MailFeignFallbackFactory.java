package com.zjc.common.api.mail.factory;

import com.zjc.common.api.mail.MailFeignApi;
import com.zjc.common.constant.ApiResponseEnum;
import com.zjc.common.dto.MailLogDTO;
import com.zjc.common.web.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

/**
 * {@link MailFeignApi} 的降级工厂。
 *
 * <p>邮件发送是有副作用的操作，降级时不能伪装成发送成功。这里统一返回
 * 服务不可用的失败响应，具体异常只写入日志，用于后续排查。
 *
 * @author jiancai.zhong
 */
@Slf4j
@Component
public class MailFeignFallbackFactory implements FallbackFactory<MailFeignApi> {

    /**
     * 降级时的对外提示，不暴露 SMTP、下游地址等基础设施细节。
     */
    private static final String FALLBACK_MESSAGE = "业务繁忙，请稍后再试";

    /**
     * 创建降级代理对象，远程调用失败时由 Feign 自动回调。
     *
     * @param cause 远程调用失败原因（超时、连接拒绝、服务不可用等）
     * @return 降级代理，返回服务不可用的失败响应而非抛异常
     */
    @Override
    public MailFeignApi create(Throwable cause) {
        log.error("调用 service-mail 邮件接口失败，触发降级", cause);

        return dto -> {
            log.warn("sendMail 降级");
            return ApiResponse.failure(ApiResponseEnum.SERVICE_UNAVAILABLE.code(), FALLBACK_MESSAGE);
        };
    }
}
