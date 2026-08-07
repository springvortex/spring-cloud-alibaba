package com.zjc.admin.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.servers.Server;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * SpringDoc OpenAPI 文档配置。
 *
 * <p>管理端模块当前为骨架，后续新增接口会自动收录到默认分组。
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

    @Value("${server.port}")
    private String port;

    /**
     * 管理端全量分组，匹配 {@code /admin/**} 下所有接口。
     */
    @Bean
    public GroupedOpenApi adminApi() {
        return GroupedOpenApi.builder()
                .group("01-管理端")
                .pathsToMatch("/**")
                .build();
    }

    /**
     * 文档元信息。
     */
    @Bean
    public OpenAPI openAPI() {
        Server server = new Server()
                .url("http://localhost:" + port)
                .description("本地开发环境");
        return new OpenAPI()
                .servers(List.of(server))
                .info(new Info()
                        .title("Service Admin API")
                        .description("管理端接口文档")
                        .version("v1.0.0")
                        .contact(new Contact()
                                .name("jiancai.zhong"))
                        .license(new License()
                                .name("Apache 2.0")));
    }
}
