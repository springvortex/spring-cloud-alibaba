package com.zjc.provider.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.redisson.config.SingleServerConfig;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Redisson configuration tests.
 */
@DisplayName("Redisson 配置")
class RedissonConfigurationTest {

    @Test
    @DisplayName("复用 Spring Data Redis 连接并构建单机客户端配置")
    void buildsSingleServerConfigFromSpringRedisProperties() {
        RedissonConfiguration.RedisConnectionProperties redis =
                new RedissonConfiguration.RedisConnectionProperties();
        redis.setHost("129.204.226.206");
        redis.setPort(6379);
        redis.setUsername("provider");
        redis.setPassword("secret");
        redis.setDatabase(2);
        redis.setConnectTimeout(Duration.ofSeconds(3));
        redis.setTimeout(Duration.ofSeconds(4));

        RedissonConfiguration.RedissonProperties redisson =
                new RedissonConfiguration.RedissonProperties();
        redisson.setLockWatchdogTimeout(Duration.ofSeconds(45));

        Config config = RedissonConfiguration.redissonConfig(redis, redisson);
        SingleServerConfig server = config.useSingleServer();

        assertThat(config.isLazyInitialization()).isTrue();
        assertThat(config.getUsername()).isEqualTo("provider");
        assertThat(config.getPassword()).isEqualTo("secret");
        assertThat(server.getAddress()).isEqualTo("redis://129.204.226.206:6379");
        assertThat(server.getDatabase()).isEqualTo(2);
        assertThat(server.getConnectTimeout()).isEqualTo(3000);
        assertThat(server.getTimeout()).isEqualTo(4000);
        assertThat(config.getLockWatchdogTimeout()).isEqualTo(45000);
    }

    @Test
    @DisplayName("配置属性可装配客户端且关闭开关时不创建 Bean")
    void bindsConfigurationPropertiesAndRespectsSwitch() {
        new ApplicationContextRunner()
                .withUserConfiguration(RedissonConfiguration.class)
                .withPropertyValues(
                        "zjc.redisson.enabled=true",
                        "spring.data.redis.host=127.0.0.1",
                        "spring.data.redis.port=6379",
                        "spring.data.redis.password=secret",
                        "spring.data.redis.connect-timeout=3s",
                        "spring.data.redis.timeout=4s")
                .run(context -> {
                    assertThat(context).hasSingleBean(RedissonClient.class);
                    Config config = context.getBean(RedissonClient.class).getConfig();
                    assertThat(config.useSingleServer().getAddress()).isEqualTo("redis://127.0.0.1:6379");
                    assertThat(config.getPassword()).isEqualTo("secret");
                });

        new ApplicationContextRunner()
                .withUserConfiguration(RedissonConfiguration.class)
                .withPropertyValues("zjc.redisson.enabled=false")
                .run(context -> assertThat(context).doesNotHaveBean(RedissonClient.class));
    }

    @Test
    @DisplayName("未配置认证信息时不向 Redisson 传递凭据")
    void omitsBlankCredentials() {
        RedissonConfiguration.RedisConnectionProperties redis =
                new RedissonConfiguration.RedisConnectionProperties();
        redis.setHost("127.0.0.1");
        redis.setPort(6379);
        redis.setUsername(" ");
        redis.setPassword("");

        Config config = RedissonConfiguration.redissonConfig(redis,
                new RedissonConfiguration.RedissonProperties());
        SingleServerConfig server = config.useSingleServer();

        assertThat(config.getUsername()).isNull();
        assertThat(config.getPassword()).isNull();
        assertThat(server.getAddress()).isEqualTo("redis://127.0.0.1:6379");
    }
}
