package com.zjc.common.web;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * {@link ApiPathProperties} unit tests.
 *
 * @author jiancai.zhong
 */
@DisplayName("API 路径配置")
class ApiPathPropertiesTest {

    @Test
    @DisplayName("默认版本为第一个配置版本")
    void testDefaultVersionIsFirstVersion() {
        ApiPathProperties properties = new ApiPathProperties();
        properties.setVersions(List.of("v1", "v2"));

        properties.validate();

        assertThat(properties.effectiveDefaultVersion()).isEqualTo("v1");
        assertThat(properties.normalizedVersions()).containsExactly("v1", "v2");
    }

    @Test
    @DisplayName("默认版本必须是已配置版本")
    void testDefaultVersionMustBeConfigured() {
        ApiPathProperties properties = new ApiPathProperties();
        properties.setVersions(List.of("v1"));
        properties.setDefaultVersion("v3");

        assertThatThrownBy(properties::validate)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("zjc.api.default-version");
    }

    @Test
    @DisplayName("至少需要配置一个版本")
    void testVersionIsRequired() {
        ApiPathProperties properties = new ApiPathProperties();
        properties.setVersions(List.of(" ", ""));

        assertThatThrownBy(properties::validate)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("zjc.api.versions");
    }
}
