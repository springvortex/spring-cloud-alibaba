package com.zjc.provider.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * SpringDoc OpenAPI 文档配置。
 *
 * <p>定义接口文档的标题、描述、作者、版本等元信息。
 * 访问路径：
 * <ul>
 *   <li>Swagger UI：{@code /swagger-ui.html}（或 {@code /swagger-ui/index.html}）</li>
 *   <li>OpenAPI JSON：{@code /v3/api-docs}</li>
 * </ul>
 *
 * @author jiancai.zhong
 */
@Configuration
public class OpenApiConfig {

    /**
     * 文档元信息。
     *
     * @return OpenAPI 配置
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