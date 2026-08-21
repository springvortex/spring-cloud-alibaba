package com.zjc.mail.config;

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
    @DisplayName("dev 使用共享测试 SMTP")
    void devUsesSharedTestSmtp() {
        Map<String, Object> dev = loadProfile("application-dev.yaml");

        assertThat(path(dev, "spring.mail.host")).isEqualTo("129.204.226.206");
        assertThat(path(dev, "spring.mail.port")).isEqualTo(1025);
        assertThat(path(dev, "spring.mail.properties.mail.smtp.auth")).isEqualTo(false);
        assertThat(path(dev, "spring.mail.properties.mail.smtp.ssl.enable")).isEqualTo(false);
    }

    @Test
    @DisplayName("prod 使用安全基线")
    void prodUsesSecureBaseline() {
        Map<String, Object> prod = loadProfile("application-prod.yaml");

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
