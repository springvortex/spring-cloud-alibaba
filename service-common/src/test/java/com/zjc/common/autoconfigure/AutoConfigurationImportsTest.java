package com.zjc.common.autoconfigure;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 公共模块自动装配清单测试。
 *
 * @author jiancai.zhong
 */
@DisplayName("公共模块自动装配清单")
class AutoConfigurationImportsTest {

    @Test
    @DisplayName("Feign 降级工厂已注册为自动装配 Bean")
    void feignFallbackFactoriesAreAutoConfigured() throws IOException {
        List<String> imports;
        try (InputStream input = Thread.currentThread().getContextClassLoader()
                .getResourceAsStream("META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports")) {
            assertThat(input).isNotNull();
            imports = new String(input.readAllBytes(), StandardCharsets.UTF_8).lines()
                    .map(String::trim)
                    .toList();
        }

        assertThat(imports).contains(
                "com.zjc.common.api.mail.factory.MailFeignFallbackFactory",
                "com.zjc.common.api.user.factory.UserFeignFallbackFactory"
        );
    }
}
