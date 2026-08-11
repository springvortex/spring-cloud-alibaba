package com.zjc.consumer.config;

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
 * <p>将接口按业务模块分组，Swagger UI 顶部下拉框可切换：
 * <ul>
 *   <li>用户消费：{@code /consumer/**}</li>
 *   <li>Feign 测试：{@code /feign/**}</li>
 *   <li>配置测试：{@code /config}</li>
 * </ul>
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
     * 用户消费分组。
     *
     * @return 用户消费 API 分组配置
     */
    @Bean
    public GroupedOpenApi userConsumerApi() {
        return GroupedOpenApi.builder()
                .group("01-用户消费")
                .pathsToMatch("/consumer/**")
                .build();
    }

    /**
     * Feign 调用测试分组。
     *
     * @return Feign 调用测试 API 分组配置
     */
    @Bean
    public GroupedOpenApi feignTestApi() {
        return GroupedOpenApi.builder()
                .group("02-Feign测试")
                .pathsToMatch("/feign/**")
                .build();
    }

    /**
     * Nacos 配置测试分组。
     *
     * @return Nacos 配置测试 API 分组配置
     */
    @Bean
    public GroupedOpenApi configTestApi() {
        return GroupedOpenApi.builder()
                .group("03-配置测试")
                .pathsToMatch("/config")
                .build();
    }

    /**
     * 系统信息分组。
     *
     * @return 系统信息 API 分组配置
     */
    @Bean
    public GroupedOpenApi systemApi() {
        return GroupedOpenApi.builder()
                .group("04-系统信息")
                .pathsToMatch("/system/**")
                .build();
    }

    /**
     * 文档元信息，所有分组共享。
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
                        .title("Service Consumer API")
                        .description("服务消费者接口文档：Feign 远程调用、Nacos 配置动态刷新")
                        .version("v1.0.0")
                        .contact(new Contact()
                                .name("jiancai.zhong"))
                        .license(new License()
                                .name("Apache 2.0")));
    }
}
