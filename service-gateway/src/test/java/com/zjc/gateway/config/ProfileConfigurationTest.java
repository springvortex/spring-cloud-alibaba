package com.zjc.gateway.config;

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
    @DisplayName("环境路由完整且互不覆盖")
    void environmentRoutesAreComplete() {
        Map<String, Object> dev = loadProfile("application-dev.yaml");
        Map<String, Object> prod = loadProfile("application-prod.yaml");

        assertThat(routeIds(dev)).containsExactly(
                "provider-openapi-route",
                "consumer-openapi-route",
                "mail-openapi-route",
                "provider-route",
                "consumer-route",
                "mail-route"
        );
        assertThat(routeIds(prod)).containsExactly(
                "provider-route",
                "consumer-route",
                "mail-route"
        );
    }

    @Test
    @DisplayName("Sentinel 只限制网关入口接口")
    void sentinelOnlyLimitsGatewayEntryInterfaces() {
        Map<String, Object> sentinel = loadResource("config/application-sentinel.yaml");
        List<?> interfaces = (List<?>) path(sentinel, "zjc.gateway.sentinel.interfaces");

        assertThat(interfaces).hasSize(2);
        Map<?, ?> defaultRule = (Map<?, ?>) interfaces.get(0);
        assertThat(defaultRule.get("name")).isEqualTo("non-mail-interfaces");
        String defaultPattern = String.valueOf(defaultRule.get("pattern"));
        assertThat(defaultPattern).isEqualTo("/api/(?![^/]+/mail/send$).*");
        assertThat(defaultRule.get("total-qps")).isEqualTo(100);
        assertThat(defaultRule.get("per-ip-qps")).isEqualTo(10);
        assertThat(java.util.regex.Pattern.matches(defaultPattern, "/api/v1/consumer/user/1")).isTrue();
        assertThat(java.util.regex.Pattern.matches(defaultPattern, "/api/v1/provider/user/1")).isTrue();
        assertThat(java.util.regex.Pattern.matches(defaultPattern, "/api/v1/mail/send")).isFalse();

        Map<?, ?> mailRule = (Map<?, ?>) interfaces.get(1);
        assertThat(mailRule.get("name")).isEqualTo("mail-send");
        assertThat(mailRule.get("pattern")).isEqualTo("/api/[^/]+/mail/send");
        assertThat(mailRule.get("total-qps")).isEqualTo(20);
        assertThat(mailRule.get("per-ip-qps")).isEqualTo(2);
    }

    private List<String> routeIds(Map<String, Object> profile) {
        List<?> routes = (List<?>) path(profile,
                "spring.cloud.gateway.server.webflux.routes");
        return routes.stream()
                .map(route -> (Map<?, ?>) route)
                .map(route -> String.valueOf(route.get("id")))
                .toList();
    }

    @Test
    @DisplayName("prod 使用安全基线")
    void prodUsesSecureBaseline() {
        Map<String, Object> application = loadResource("application.yaml");
        Map<String, Object> dev = loadProfile("application-dev.yaml");
        Map<String, Object> prod = loadProfile("application-prod.yaml");

        assertThat(((List<?>) path(application, "spring.profiles.include")).stream())
                .anyMatch("cors"::equals);
        assertThat(path(dev, "spring.cloud.gateway.server.webflux.globalcors")).isNull();
        assertThat(path(prod, "spring.cloud.gateway.server.webflux.globalcors")).isNull();
        Map<String, Object> cors = loadResource("config/application-cors.yaml");
        assertThat(path(cors,
                "spring.cloud.gateway.server.webflux.globalcors.cors-configurations.[/**].allowed-origin-patterns"))
                .isEqualTo(List.of("*"));
        assertThat(path(cors,
                "spring.cloud.gateway.server.webflux.globalcors.cors-configurations.[/**].allowed-headers"))
                .isEqualTo(List.of("*"));
        assertThat(path(cors,
                "spring.cloud.gateway.server.webflux.globalcors.cors-configurations.[/**].allowed-methods"))
                .isEqualTo(List.of("*"));
        assertThat(path(cors,
                "spring.cloud.gateway.server.webflux.globalcors.cors-configurations.[/**].allow-credentials"))
                .isEqualTo(false);
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
    @DisplayName("服务启用优雅停机")
    void serviceEnablesGracefulShutdown() {
        Map<String, Object> application = loadResource("application.yaml");
        Map<String, Object> shutdown = loadResource("config/application-shutdown.yaml");

        assertThat(path(application, "spring.profiles.include")).asList().contains("shutdown");
        assertThat(path(shutdown, "server.shutdown")).isEqualTo("graceful");
        assertThat(path(shutdown, "spring.lifecycle.timeout-per-shutdown-phase"))
                .isEqualTo("30s");
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
