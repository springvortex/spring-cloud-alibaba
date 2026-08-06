package com.zjc.provider.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * SpringDoc OpenAPI 文档配置。
 *
 * <p>将接口按业务模块分组，Swagger UI 顶部下拉框可切换：
 * <ul>
 *   <li>用户管理：{@code /user/**}</li>
 *   <li>商品管理：{@code /goods/**}</li>
 *   <li>订单管理：{@code /order/**}</li>
 * </ul>
 *
 * <p>访问路径：
 * <ul>
 *   <li>Swagger UI：{@code /swagger-ui.html}</li>
 *   <li>OpenAPI JSON：{@code /v3/api-docs}</li>
 *   <li>分组 JSON：{@code /v3/api-docs/用户管理} 等</li>
 * </ul>
 *
 * @author jiancai.zhong
 */
@Configuration
public class OpenApiConfig {

    /**
     * 用户管理分组。
     */
    @Bean
    public GroupedOpenApi userApi() {
        return GroupedOpenApi.builder()
                .group("01-用户管理")
                .pathsToMatch("/user/**")
                .build();
    }

    /**
     * 商品管理分组。
     */
    @Bean
    public GroupedOpenApi goodsApi() {
        return GroupedOpenApi.builder()
                .group("02-商品管理")
                .pathsToMatch("/goods/**")
                .build();
    }

    /**
     * 订单管理分组。
     */
    @Bean
    public GroupedOpenApi orderApi() {
        return GroupedOpenApi.builder()
                .group("03-订单管理")
                .pathsToMatch("/order/**")
                .build();
    }

    /**
     * 文档元信息，所有分组共享。
     */
    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Service Provider API")
                        .description("服务提供者接口文档：用户、商品、订单管理")
                        .version("v1.0.0")
                        .contact(new Contact()
                                .name("jiancai.zhong"))
                        .license(new License()
                                .name("Apache 2.0")));
    }
}