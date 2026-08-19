package com.zjc.common.web;

import com.zjc.common.web.annotation.ApiVersion;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@link ApiPathResolver} unit tests.
 *
 * @author jiancai.zhong
 */
@DisplayName("API 路径解析")
class ApiPathResolverTest {

    @Test
    @DisplayName("从服务名解析模块并生成标准前缀")
    void testResolverBuildsStandardPrefix() {
        ApiPathProperties properties = new ApiPathProperties();
        properties.setPrefix("/api/");
        properties.setVersions(List.of("v1", "v2"));
        properties.setDefaultVersion("v1");

        ApiPathResolver resolver = ApiPathResolver.of("service-provider", properties);

        assertThat(resolver.getModuleName()).isEqualTo("provider");
        assertThat(resolver.prefix("v1")).isEqualTo("/api/v1/provider");
        assertThat(resolver.prefix("v2")).isEqualTo("/api/v2/provider");
        assertThat(resolver.version(VersionOneController.class)).isEqualTo("v1");
        assertThat(resolver.version(VersionTwoController.class)).isEqualTo("v2");
    }

    @Test
    @DisplayName("按配置的应用包范围应用前缀")
    void testBasePackageFiltering() {
        ApiPathProperties properties = new ApiPathProperties();
        properties.setVersions(List.of("v1"));
        properties.setBasePackages(List.of("com.zjc.common.web"));

        ApiPathResolver resolver = ApiPathResolver.of("service-provider", properties);

        assertThat(resolver.appliesTo(VersionOneController.class)).isTrue();
        assertThat(resolver.appliesTo(String.class)).isFalse();
    }

    @RestController
    static class VersionOneController {
    }

    @RestController
    @ApiVersion("v2")
    static class VersionTwoController {
    }
}
