package com.zjc.common.cache;

import com.zjc.common.dto.UserDTO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.cache.autoconfigure.CacheAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.cache.autoconfigure.RedisCacheManagerBuilderCustomizer;
import org.springframework.cache.CacheManager;
import org.springframework.cache.interceptor.CacheErrorHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.GenericJacksonJsonRedisSerializer;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Redis 缓存自动装配测试。
 *
 * @author jiancai.zhong
 */
@DisplayName("Redis 缓存自动装配")
class RedisCacheAutoConfigurationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(
                    CacheAutoConfiguration.class,
                    RedisCacheAutoConfiguration.class
            ))
            .withUserConfiguration(MockRedisConnectionFactoryConfiguration.class);

    @Test
    @DisplayName("默认装配 JSON 序列化、统一前缀和故障降级处理器")
    void testDefaultCacheInfrastructure() {
        contextRunner.run(context -> {
            assertThat(context).hasNotFailed();
            assertThat(context).hasSingleBean(GenericJacksonJsonRedisSerializer.class);
            assertThat(context).hasSingleBean(RedisCacheManagerBuilderCustomizer.class);
            assertThat(context).hasSingleBean(ResilientCacheErrorHandler.class);

            RedisCacheConfiguration configuration = context.getBean(RedisCacheConfiguration.class);
            assertThat(configuration.getKeyPrefixFor("provider:user:id"))
                    .isEqualTo("zjc:provider:user:id:");
            assertThat(context.getBean(RedisCacheAutoConfiguration.class).errorHandler())
                    .isInstanceOf(ResilientCacheErrorHandler.class);
        });
    }

    @Test
    @DisplayName("关闭公共配置时不装配 Redis 缓存基础设施")
    void testCacheInfrastructureCanBeDisabled() {
        contextRunner
                .withPropertyValues("zjc.cache.redis.enabled=false")
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context).doesNotHaveBean(GenericJacksonJsonRedisSerializer.class);
                    assertThat(context).doesNotHaveBean(RedisCacheConfiguration.class);
                    assertThat(context).doesNotHaveBean(ResilientCacheErrorHandler.class);
                });
    }

    @Test
    @DisplayName("与 Spring Boot Redis Cache 组合时生成 RedisCacheManager")
    void testRedisCacheManagerIsCreated() {
        contextRunner
                .withPropertyValues("spring.cache.type=redis")
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context.getBean(CacheManager.class)).isInstanceOf(RedisCacheManager.class);
                });
    }

    @Test
    @DisplayName("业务模块可以覆盖缓存故障降级处理器")
    void testCustomCacheErrorHandlerIsPreferred() {
        contextRunner
                .withUserConfiguration(CustomErrorHandlerConfiguration.class)
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context).hasSingleBean(CacheErrorHandler.class);
                    assertThat(context.getBean(CustomErrorHandlerConfiguration.CustomCacheErrorHandler.class))
                            .isNotNull();
                    assertThat(context.getBean(RedisCacheAutoConfiguration.class).errorHandler())
                            .isInstanceOf(CustomErrorHandlerConfiguration.CustomCacheErrorHandler.class);
                });
    }

    @Test
    @DisplayName("JSON 序列化保留 DTO 类型并拒绝项目外部多态类型")
    void testJsonSerializerTrustBoundary() {
        AtomicReference<GenericJacksonJsonRedisSerializer> serializerReference = new AtomicReference<>();
        contextRunner.run(context -> serializerReference.set(context.getBean(GenericJacksonJsonRedisSerializer.class)));
        GenericJacksonJsonRedisSerializer serializer = serializerReference.get();
        assertThat(serializer).isNotNull();

        UserDTO user = new UserDTO();
        user.setUserId(1L);
        user.setUsername("zhangsan");
        user.setCreateTime(LocalDateTime.of(2026, 8, 24, 10, 0));

        byte[] json = serializer.serialize(user);
        assertThat(new String(json, StandardCharsets.UTF_8)).contains("com.zjc.common.dto.UserDTO");
        assertThat(serializer.deserialize(json)).usingRecursiveComparison().isEqualTo(user);

        byte[] maliciousJson = "{\"@type\":\"java.lang.Thread\"}"
                .getBytes(StandardCharsets.UTF_8);
        assertThatThrownBy(() -> serializer.deserialize(maliciousJson)).isInstanceOf(Exception.class);
    }

    @Configuration
    static class CustomErrorHandlerConfiguration {

        @Bean
        CacheErrorHandler customCacheErrorHandler() {
            return new CustomCacheErrorHandler();
        }

        static class CustomCacheErrorHandler extends ResilientCacheErrorHandler {
        }
    }

    @Configuration
    static class MockRedisConnectionFactoryConfiguration {

        @Bean
        RedisConnectionFactory redisConnectionFactory() {
            return org.mockito.Mockito.mock(RedisConnectionFactory.class);
        }
    }
}
