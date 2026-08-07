package com.zjc.provider.config;

import io.swagger.v3.oas.models.OpenAPI;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springdoc.core.models.GroupedOpenApi;

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
     * 验证用户管理分组的名称和路径匹配规则。
     */
    @Test
    @DisplayName("userApi: 用户管理分组匹配 /user/**")
    void testUserApiConfigured() {
        GroupedOpenApi api = config.userApi();
        assertThat(api.getGroup()).isEqualTo("01-用户管理");
        assertThat(api.getPathsToMatch()).contains("/user/**");
    }

    /**
     * 验证商品管理分组的名称和路径匹配规则。
     */
    @Test
    @DisplayName("goodsApi: 商品管理分组匹配 /goods/**")
    void testGoodsApiConfigured() {
        GroupedOpenApi api = config.goodsApi();
        assertThat(api.getGroup()).isEqualTo("02-商品管理");
        assertThat(api.getPathsToMatch()).contains("/goods/**");
    }

    /**
     * 验证订单管理分组的名称和路径匹配规则。
     */
    @Test
    @DisplayName("orderApi: 订单管理分组匹配 /order/**")
    void testOrderApiConfigured() {
        GroupedOpenApi api = config.orderApi();
        assertThat(api.getGroup()).isEqualTo("03-订单管理");
        assertThat(api.getPathsToMatch()).contains("/order/**");
    }

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
