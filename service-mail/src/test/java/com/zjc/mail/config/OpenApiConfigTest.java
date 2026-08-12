package com.zjc.mail.config;

import io.swagger.v3.oas.models.OpenAPI;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springdoc.core.models.GroupedOpenApi;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@link OpenApiConfig} 单元测试。
 *
 * <p>验证 Swagger 分组配置和文档元信息。
 *
 * @author jiancai.zhong
 */
@DisplayName("OpenAPI 文档配置")
class OpenApiConfigTest {

    private final OpenApiConfig config = new OpenApiConfig();

    @Test
    @DisplayName("mailApi: 邮件服务分组匹配 /mail/**")
    void testMailApiConfigured() {
        GroupedOpenApi api = config.mailApi();
        assertThat(api.getGroup()).isEqualTo("01-邮件服务");
        assertThat(api.getPathsToMatch()).contains("/mail/**");
    }

    @Test
    @DisplayName("openAPI: 文档元信息正确")
    void testOpenApiMetadata() {
        OpenAPI openAPI = config.openAPI();
        assertThat(openAPI.getInfo().getTitle()).isEqualTo("Service Mail API");
        assertThat(openAPI.getInfo().getVersion()).isEqualTo("v1.0.0");
        assertThat(openAPI.getInfo().getContact().getName()).isEqualTo("jiancai.zhong");
    }
}
