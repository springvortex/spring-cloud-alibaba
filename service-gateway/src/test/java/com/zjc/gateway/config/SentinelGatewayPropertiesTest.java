package com.zjc.gateway.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

/**
 * {@link SentinelGatewayProperties} unit tests.
 *
 * @author jiancai.zhong
 */
@DisplayName("网关 Sentinel 配置校验")
class SentinelGatewayPropertiesTest {

    @Test
    @DisplayName("接口单 IP 阈值不能超过全局阈值")
    void testPerIpQpsCannotExceedTotalQps() {
        SentinelGatewayProperties properties = new SentinelGatewayProperties();
        properties.setInterfaces(List.of(newRule(100, 101)));

        assertThatIllegalStateException()
                .isThrownBy(properties::validate)
                .withMessageContaining("provider-user-detail");
    }

    @Test
    @DisplayName("接口名称、路径和阈值必须有效")
    void testInterfaceRuleMustBeValid() {
        SentinelGatewayProperties.InterfaceRule rule = new SentinelGatewayProperties.InterfaceRule();
        SentinelGatewayProperties properties = new SentinelGatewayProperties();
        properties.setInterfaces(List.of(rule));

        assertThatIllegalStateException()
                .isThrownBy(properties::validate)
                .withMessageContaining("name is required");

        rule.setName("provider-user-detail");
        assertThatIllegalStateException()
                .isThrownBy(properties::validate)
                .withMessageContaining("pattern is required");

        rule.setPattern("/api/[^/]+/provider/user/\\d+");
        rule.setTotalQps(0);
        assertThatIllegalStateException()
                .isThrownBy(properties::validate)
                .withMessageContaining("thresholds must be positive");
    }

    @Test
    @DisplayName("熔断阈值必须有效")
    void testCircuitRuleMustBeValid() {
        SentinelGatewayProperties properties = new SentinelGatewayProperties();
        properties.getCircuit().setExceptionRatio(1.1);

        assertThatIllegalStateException()
                .isThrownBy(properties::validate)
                .withMessageContaining("ratios must be in (0, 1]");
    }

    @Test
    @DisplayName("可信代理不能包含空值")
    void testTrustedProxyMustNotBeBlank() {
        SentinelGatewayProperties properties = new SentinelGatewayProperties();
        properties.setTrustedProxies(List.of("127.0.0.1", " "));

        assertThatIllegalStateException()
                .isThrownBy(properties::validate)
                .withMessageContaining("trusted-proxies");
    }

    @Test
    @DisplayName("接口名称必须唯一且正则必须可编译")
    void testInterfaceNameMustBeUniqueAndPatternMustCompile() {
        SentinelGatewayProperties.InterfaceRule validRule = newRule(100, 10);
        SentinelGatewayProperties.InterfaceRule duplicatedRule = newRule(100, 10);
        SentinelGatewayProperties duplicatedProperties = new SentinelGatewayProperties();
        duplicatedProperties.setInterfaces(List.of(validRule, duplicatedRule));

        assertThatIllegalStateException()
                .isThrownBy(duplicatedProperties::validate)
                .withMessageContaining("name must be unique");

        SentinelGatewayProperties.InterfaceRule invalidPatternRule = newRule(100, 10);
        invalidPatternRule.setPattern("/api/[^/+/provider/user/\\d+");
        SentinelGatewayProperties invalidPatternProperties = new SentinelGatewayProperties();
        invalidPatternProperties.setInterfaces(List.of(invalidPatternRule));

        assertThatIllegalStateException()
                .isThrownBy(invalidPatternProperties::validate)
                .withMessageContaining("pattern is invalid");
    }

    @Test
    @DisplayName("合法配置通过校验")
    void testValidProperties() {
        SentinelGatewayProperties properties = new SentinelGatewayProperties();
        properties.setInterfaces(List.of(newRule(100, 10)));

        properties.validate();

        assertThat(properties.getCircuit().getRecoverySeconds()).isPositive();
    }

    private SentinelGatewayProperties.InterfaceRule newRule(double totalQps, double perIpQps) {
        SentinelGatewayProperties.InterfaceRule rule = new SentinelGatewayProperties.InterfaceRule();
        rule.setName("provider-user-detail");
        rule.setPattern("/api/[^/]+/provider/user/\\d+");
        rule.setTotalQps(totalQps);
        rule.setPerIpQps(perIpQps);
        rule.setIntervalSec(1);
        return rule;
    }
}
