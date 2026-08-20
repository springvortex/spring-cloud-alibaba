package com.zjc.gateway.config;

import com.alibaba.cloud.circuitbreaker.sentinel.ReactiveSentinelCircuitBreakerFactory;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.web.reactive.DispatcherHandler;

import java.net.URI;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@link SentinelGatewayCircuitBreakerFilterFactory} unit tests.
 *
 * @author jiancai.zhong
 */
@DisplayName("网关 Sentinel 默认熔断过滤器")
class SentinelGatewayCircuitBreakerFilterFactoryTest {

    @Test
    @DisplayName("默认过滤器按路由生成熔断资源名和兜底地址")
    void testDefaultFilterUsesRouteId() {
        SentinelGatewayCircuitBreakerFilterFactory factory = newFactory();
        SentinelGatewayCircuitBreakerFilterFactory.Config config =
                new SentinelGatewayCircuitBreakerFilterFactory.Config();
        config.setRouteId("provider-route");

        GatewayFilter gatewayFilter = factory.apply(config);

        assertThat(gatewayFilter).isNotNull();
        assertThat(config.getName()).isEqualTo("provider-route-circuit");
        assertThat(config.getFallbackUri())
                .isEqualTo(URI.create("forward:/gateway/fallback/provider-route"));
    }

    @Test
    @DisplayName("显式配置不会被路由 ID 覆盖")
    void testExplicitConfigurationIsPreserved() {
        SentinelGatewayCircuitBreakerFilterFactory factory = newFactory();
        SentinelGatewayCircuitBreakerFilterFactory.Config config =
                new SentinelGatewayCircuitBreakerFilterFactory.Config();
        config.setRouteId("provider-route");
        config.setName("custom-circuit");
        config.setFallbackUri("forward:/custom-fallback");

        factory.apply(config);

        assertThat(config.getName()).isEqualTo("custom-circuit");
        assertThat(config.getFallbackUri()).isEqualTo(URI.create("forward:/custom-fallback"));
    }

    private SentinelGatewayCircuitBreakerFilterFactory newFactory() {
        return new SentinelGatewayCircuitBreakerFilterFactory(
                new ReactiveSentinelCircuitBreakerFactory(),
                Mockito.mock(ObjectProvider.class));
    }
}
