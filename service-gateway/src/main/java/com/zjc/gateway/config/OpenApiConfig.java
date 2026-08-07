package com.zjc.gateway.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * SpringDoc OpenAPI 文档配置（WebFlux）。
 *
 * <p>网关模块基于 Spring Cloud Gateway（WebFlux），使用
 * {@code springdoc-openapi-starter-webflux-ui} 提供 Swagger UI。
 * 若需聚合下游各服务的 API 文档，可在 Nacos 配置中添加：
 * <pre>{@code
 * springdoc:
 *   swagger-ui:
 *     urls:
 *       - name: 服务提供者
 *         url: /service-provider/v3/api-docs
 *       - name: 服务消费者
 *         url: /service-consumer/v3/api-docs
 *       - name: 邮件服务
 *         url: /service-mail/v3/api-docs
 * }</pre>
 *
 * <p>访问路径：
 * <ul>
 *   <li>Swagger UI：{@code /swagger-ui.html}</li>
 *   <li>OpenAPI JSON：{@code /v3/api-docs}</li>
 * </ul>
 *
 * @author jiancai.zhong
 */
@Configuration
public class OpenApiConfig {

    /**
     * 文档元信息。
     */
    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Service Gateway API")
                        .description("网关接口文档：路由、聚合下游服务 API")
                        .version("v1.0.0")
                        .contact(new Contact()
                                .name("jiancai.zhong"))
                        .license(new License()
                                .name("Apache 2.0")));
    }
}
