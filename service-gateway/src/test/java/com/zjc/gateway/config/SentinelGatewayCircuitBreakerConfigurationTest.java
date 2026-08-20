package com.zjc.gateway.config;

import com.alibaba.cloud.circuitbreaker.sentinel.ReactiveSentinelCircuitBreakerFactory;
import com.alibaba.csp.sentinel.slots.block.degrade.DegradeRule;
import com.alibaba.csp.sentinel.slots.block.degrade.DegradeRuleManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.cloud.client.circuitbreaker.Customizer;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@link SentinelGatewayConfiguration} circuit breaker unit tests.
 *
 * @author jiancai.zhong
 */
@DisplayName("网关 Sentinel 路由熔断规则")
class SentinelGatewayCircuitBreakerConfigurationTest {

    @Test
    @DisplayName("每个路由熔断资源同时配置异常比例和慢请求比例")
    void testCircuitBreakerResourceHasExceptionAndSlowRequestRules() {
        ReactiveSentinelCircuitBreakerFactory factory = new ReactiveSentinelCircuitBreakerFactory();
        Customizer<ReactiveSentinelCircuitBreakerFactory> customizer =
                new SentinelGatewayConfiguration().sentinelCircuitBreakerCustomizer(new SentinelGatewayProperties());

        try {
            customizer.customize(factory);
            factory.create("provider-route-circuit");

            Set<DegradeRule> rules = DegradeRuleManager.getRulesOfResource("provider-route-circuit");
            assertThat(rules).hasSize(2);
            assertThat(rules).extracting(DegradeRule::getGrade)
                    .containsExactlyInAnyOrder(1, 0);
            assertThat(rules).allSatisfy(rule -> {
                assertThat(rule.getMinRequestAmount()).isEqualTo(5);
                assertThat(rule.getTimeWindow()).isEqualTo(10);
            });
        } finally {
            DegradeRuleManager.loadRules(List.of());
        }
    }
}
