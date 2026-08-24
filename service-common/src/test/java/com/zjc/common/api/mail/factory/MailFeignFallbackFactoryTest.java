package com.zjc.common.api.mail.factory;

import com.zjc.common.api.mail.MailFeignApi;
import com.zjc.common.constant.ApiResponseEnum;
import com.zjc.common.dto.MailLogDTO;
import com.zjc.common.dto.MailSendDTO;
import com.zjc.common.web.ApiResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@link MailFeignFallbackFactory} 单元测试。
 *
 * @author jiancai.zhong
 */
@DisplayName("邮件 Feign 降级工厂")
class MailFeignFallbackFactoryTest {

    private final MailFeignFallbackFactory factory = new MailFeignFallbackFactory();

    @Test
    @DisplayName("sendMail 降级: 返回业务繁忙失败响应")
    void testFallbackSendMailReturnsServiceUnavailable() {
        MailFeignApi fallback = factory.create(new RuntimeException("mail service down"));
        MailSendDTO request = new MailSendDTO();

        ApiResponse<MailLogDTO> resp = fallback.send(request);

        assertThat(resp.isSuccess()).isFalse();
        assertThat(resp.getCode()).isEqualTo(ApiResponseEnum.SERVICE_UNAVAILABLE.code());
        assertThat(resp.getMessage()).isEqualTo("业务繁忙，请稍后再试");
        assertThat(resp.getData()).isNull();
    }
}
