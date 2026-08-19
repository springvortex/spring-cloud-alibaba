package com.zjc.common.web;

import com.zjc.common.web.annotation.ApiVersion;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import org.springframework.boot.webmvc.autoconfigure.WebMvcAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Integration tests for the global API path convention.
 *
 * @author jiancai.zhong
 */
@DisplayName("统一 API 路径自动配置")
class ApiPathAutoConfigurationTest {

    private final WebApplicationContextRunner contextRunner = new WebApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(ApiPathAutoConfiguration.class, WebMvcAutoConfiguration.class))
            .withUserConfiguration(TestControllers.class)
            .withPropertyValues(
                    "spring.application.name=service-provider",
                    "zjc.api.prefix=/api",
                    "zjc.api.versions[0]=v1",
                    "zjc.api.versions[1]=v2",
                    "zjc.api.default-version=v1",
                    "zjc.api.base-packages[0]=com.zjc.common.web"
            );

    @Test
    @DisplayName("Controller 资源路径自动追加版本前缀")
    void testControllersReceiveGlobalPrefix() {
        contextRunner.run(context -> {
            assertThat(context).hasNotFailed();
            MockMvc mockMvc = MockMvcBuilders.webAppContextSetup(context.getSourceApplicationContext()).build();

            mockMvc.perform(get("/api/v1/provider/resource"))
                    .andExpect(status().isOk());
            mockMvc.perform(get("/api/v2/provider/resource"))
                    .andExpect(status().isOk());
            mockMvc.perform(get("/resource"))
                    .andExpect(status().isNotFound());
        });
    }

    @Test
    @DisplayName("Springdoc 按版本自动生成分组")
    @SuppressWarnings("unchecked")
    void testSpringdocGroupsAreGenerated() {
        contextRunner.run(context -> {
            List<GroupedOpenApi> groups = (List<GroupedOpenApi>) context
                    .getBean("apiVersionGroupedOpenApis", List.class);

            assertThat(groups)
                    .extracting(GroupedOpenApi::getGroup)
                    .containsExactly("v1-provider", "v2-provider");
            assertThat(groups.get(0).getPathsToMatch()).contains("/api/v1/provider/**");
            assertThat(groups.get(1).getPathsToMatch()).contains("/api/v2/provider/**");
            assertThat(context.getBean(GroupConsumer.class).groups()).hasSize(2);
        });
    }

    /**
     * 集成测试使用的控制器和分组消费者配置。
     *
     * @author jiancai.zhong
     */
    @org.springframework.context.annotation.Configuration
    static class TestControllers {

        @Bean
        VersionOneController versionOneController() {
            return new VersionOneController();
        }

        @Bean
        VersionTwoController versionTwoController() {
            return new VersionTwoController();
        }

        @Bean
        GroupConsumer groupConsumer(List<GroupedOpenApi> groups) {
            return new GroupConsumer(groups);
        }
    }

    /**
     * 用于验证 Springdoc 分组注入结果的消费者。
     *
     * @author jiancai.zhong
     */
    record GroupConsumer(List<GroupedOpenApi> groups) {
    }

    /**
     * 未标注版本的默认版本控制器。
     *
     * @author jiancai.zhong
     */
    @RestController
    static class VersionOneController {

        @GetMapping("/resource")
        String one() {
            return "v1";
        }
    }

    /**
     * 标注 v2 版本的控制器。
     *
     * @author jiancai.zhong
     */
    @RestController
    @ApiVersion("v2")
    static class VersionTwoController {

        @GetMapping("/resource")
        String two() {
            return "v2";
        }
    }
}
