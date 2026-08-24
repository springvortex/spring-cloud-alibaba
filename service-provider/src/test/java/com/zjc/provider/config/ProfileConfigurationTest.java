package com.zjc.provider.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.yaml.snakeyaml.Yaml;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Environment profile structure tests.
 */
@DisplayName("Environment Profile 配置")
class ProfileConfigurationTest {

    @Test
    @DisplayName("所有 YAML Profile 均可解析")
    void allYamlProfilesAreParseable() throws IOException {
        try (Stream<Path> paths = Files.walk(Path.of("src/main/resources"))) {
            List<Path> yamlFiles = paths
                    .filter(path -> path.toString().endsWith(".yaml"))
                    .toList();

            assertThat(yamlFiles).isNotEmpty();
            for (Path path : yamlFiles) {
                try (InputStream input = Files.newInputStream(path)) {
                    Object loaded = new Yaml().load(input);
                    assertThat(loaded).as(path.toString()).isNotNull();
                }
            }
        }
    }

    @Test
    @DisplayName("prod 使用安全基线")
    void prodUsesSecureBaseline() {
        Map<String, Object> prod = loadProfile("application-prod.yaml");

        assertThat(path(prod, "spring.datasource.url").toString()).contains("sslMode=REQUIRED");
        assertThat(path(prod, "mybatis-plus.configuration.log-impl")).isNull();
        assertThat(path(prod, "management.tracing.sampling.probability")).isEqualTo(0.1D);
        assertThat(path(prod, "spring.cloud.nacos.discovery.username")).isNull();
        assertThat(path(prod, "spring.cloud.nacos.discovery.password")).isNull();
        Map<String, Object> nacos = loadResource("config/application-nacos.yaml");
        assertThat(path(nacos, "spring.cloud.nacos.discovery.username")).isEqualTo("nacos");
        assertThat(path(nacos, "spring.cloud.nacos.discovery.password")).isEqualTo("nacos");
        assertThat(path(prod, "springdoc.api-docs.enabled")).isEqualTo(false);
        assertThat(path(prod, "springdoc.swagger-ui.enabled")).isEqualTo(false);
    }

    @Test
    @DisplayName("Redis 公共配置与环境地址符合约定")
    void redisConfigurationFollowsEnvironmentConvention() {
        Map<String, Object> common = loadResource("config/application-redis.yaml");
        Map<String, Object> dev = loadProfile("application-dev.yaml");
        Map<String, Object> prod = loadProfile("application-prod.yaml");
        Map<String, Object> application = loadResource("application.yaml");

        assertThat(path(application, "spring.profiles.include")).asList().contains("redis");
        assertThat(path(common, "spring.cache.type")).isEqualTo("redis");
        assertThat(path(common, "zjc.cache.redis.enabled")).isEqualTo(true);
        assertThat(path(common, "zjc.cache.redis.key-prefix")).isEqualTo("zjc:");
        assertThat(path(common, "zjc.cache.redis.default-ttl")).isEqualTo("30m");
        assertThat(path(common, "zjc.redisson.enabled")).isEqualTo(true);
        assertThat(path(common, "zjc.redisson.lock-watchdog-timeout")).isEqualTo("30s");

        @SuppressWarnings("unchecked")
        Map<String, Object> cacheTtls = (Map<String, Object>) path(common, "zjc.cache.redis.cache-ttls");
        assertThat(cacheTtls)
                .containsEntry("provider:user:id", "30m")
                .containsEntry("provider:goods:id", "30m");

        assertThat(path(dev, "spring.data.redis.host")).isEqualTo("129.204.226.206");
        assertThat(path(dev, "spring.data.redis.port")).isEqualTo(6379);
        assertThat(path(dev, "spring.data.redis.password").toString())
                .startsWith("ENC(")
                .endsWith(")");
        assertThat(path(prod, "spring.data.redis.host")).isEqualTo("127.0.0.1");
        assertThat(path(prod, "spring.data.redis.port")).isEqualTo(6379);
        assertThat(path(prod, "spring.data.redis.password").toString())
                .startsWith("ENC(")
                .endsWith(")");
    }

    /**
     * Verify that distributed lock switching is isolated from Redis cache configuration.
     */
    @Test
    @DisplayName("分布式锁工厂配置符合约定")
    void distributedLockConfigurationFollowsConvention() {
        Map<String, Object> common = loadResource("config/application-lock.yaml");
        Map<String, Object> application = loadResource("application.yaml");

        assertThat(path(application, "spring.profiles.include")).asList().contains("lock");
        assertThat(path(common, "zjc.distributed-lock.provider")).isEqualTo("redis");
        assertThat(path(common, "zjc.distributed-lock.mysql.table-name"))
                .isEqualTo("t_distributed_lock");
        assertThat(path(common, "zjc.distributed-lock.mysql.lease-time")).isEqualTo("30s");
        assertThat(path(common, "zjc.distributed-lock.mysql.retry-interval")).isEqualTo("100ms");
    }

    private Map<String, Object> loadProfile(String name) {
        return loadResource(name);
    }

    private Map<String, Object> loadResource(String name) {
        try (InputStream input = Files.newInputStream(Path.of("src/main/resources", name))) {
            return new Yaml().load(input);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    private Object path(Map<String, Object> source, String property) {
        Object value = source;
        for (String key : property.split("\\.")) {
            if (!(value instanceof Map<?, ?> map)) {
                return null;
            }
            value = map.get(key);
        }
        return value;
    }
}
