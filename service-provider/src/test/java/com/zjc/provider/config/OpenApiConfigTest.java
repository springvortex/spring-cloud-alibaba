package com.zjc.provider.config;

import io.swagger.v3.oas.models.OpenAPI;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@link OpenApiConfig} 单元测试。
 *
 * <p>验证 Swagger 分组配置（group 名称、路径匹配）和文档元信息。
 *
 * @author jiancai.zhong
 */
@DisplayName("OpenAPI 文档配置")
class OpenApiConfigTest {

    private final OpenApiConfig config = new OpenApiConfig();

    /**
     * 验证文档元信息（标题、版本、联系人）配置正确。
     */
    @Test
    @DisplayName("openAPI: 文档元信息正确")
    void testOpenApiMetadata() {
        OpenAPI openAPI = config.openAPI();
        assertThat(openAPI.getInfo().getTitle()).isEqualTo("Service Provider API");
        assertThat(openAPI.getInfo().getVersion()).isEqualTo("v1.0.0");
        assertThat(openAPI.getInfo().getContact().getName()).isEqualTo("jiancai.zhong");
    }
}
