package com.zjc.mail.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * SpringDoc OpenAPI 文档配置。
 *
 * <p>API 版本分组由 {@code service-common} 根据统一路径前缀自动生成，
 * 本类只维护服务级文档元信息。
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
     * 文档元信息。
     *
     * @return OpenAPI 文档元信息
     */
    @Bean
    public OpenAPI openAPI() {
        Server server = new Server()
                .url("http://localhost:" + port)
                .description("本地开发环境");
        return new OpenAPI()
                .servers(List.of(server))
                .info(new Info()
                        .title("Service Mail API")
                        .description("邮件服务接口文档：统一邮件发送")
                        .version("v1.0.0")
                        .contact(new Contact()
                                .name("jiancai.zhong"))
                        .license(new License()
                                .name("Apache 2.0")));
    }
}
