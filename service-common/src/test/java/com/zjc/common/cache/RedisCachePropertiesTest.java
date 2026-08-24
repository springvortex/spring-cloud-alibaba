package com.zjc.common.cache;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Redis 缓存配置属性测试。
 *
 * @author jiancai.zhong
 */
@DisplayName("Redis 缓存配置属性")
class RedisCachePropertiesTest {

    @Test
    @DisplayName("默认开启缓存并使用统一前缀和 30 分钟 TTL")
    void testDefaultProperties() {
        RedisCacheProperties properties = new RedisCacheProperties();

        assertThat(properties.isEnabled()).isTrue();
        assertThat(properties.getKeyPrefix()).isEqualTo("zjc:");
        assertThat(properties.getDefaultTtl()).isEqualTo(Duration.ofMinutes(30));
        assertThat(properties.getCacheTtls()).isEmpty();
    }

    @Test
    @DisplayName("支持按 cacheName 覆盖 TTL")
    void testCacheTtlOverride() {
        RedisCacheProperties properties = new RedisCacheProperties();
        properties.setDefaultTtl(Duration.ofMinutes(5));
        properties.setCacheTtls(Map.of(
                "provider:user:id", Duration.ofMinutes(1),
                "provider:goods:id", Duration.ofMinutes(2)
        ));

        assertThat(properties.getDefaultTtl()).isEqualTo(Duration.ofMinutes(5));
        assertThat(properties.getCacheTtls())
                .containsEntry("provider:user:id", Duration.ofMinutes(1))
                .containsEntry("provider:goods:id", Duration.ofMinutes(2));
    }
}
