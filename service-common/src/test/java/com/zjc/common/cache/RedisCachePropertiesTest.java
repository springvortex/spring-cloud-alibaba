package com.zjc.common.cache;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.List;
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
        assertThat(properties.getAllowedSubTypes())
                .containsExactly(BigDecimal.class.getName());
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

    @Test
    @DisplayName("支持通过配置扩展 Redis 多态类型白名单")
    void testAllowedSubTypesOverride() {
        RedisCacheProperties properties = new RedisCacheProperties();
        properties.setAllowedSubTypes(List.of(
                BigDecimal.class.getName(),
                "java.math.BigInteger"
        ));

        assertThat(properties.getAllowedSubTypes()).containsExactly(
                BigDecimal.class.getName(),
                "java.math.BigInteger"
        );
    }
}
