package com.zjc.gateway.config;

import com.alibaba.cloud.circuitbreaker.sentinel.ReactiveSentinelCircuitBreakerFactory;
import com.alibaba.cloud.circuitbreaker.sentinel.SentinelConfigBuilder;
import com.alibaba.csp.sentinel.EntryType;
import com.alibaba.csp.sentinel.adapter.gateway.common.SentinelGatewayConstants;
import com.alibaba.csp.sentinel.adapter.gateway.common.api.ApiDefinition;
import com.alibaba.csp.sentinel.adapter.gateway.common.api.ApiPathPredicateItem;
import com.alibaba.csp.sentinel.adapter.gateway.common.api.GatewayApiDefinitionManager;
import com.alibaba.csp.sentinel.adapter.gateway.common.rule.GatewayFlowRule;
import com.alibaba.csp.sentinel.adapter.gateway.common.rule.GatewayParamFlowItem;
import com.alibaba.csp.sentinel.adapter.gateway.common.rule.GatewayRuleManager;
import com.alibaba.csp.sentinel.adapter.gateway.sc.SentinelGatewayFilter;
import com.alibaba.csp.sentinel.adapter.gateway.sc.ServerWebExchangeItemParser;
import com.alibaba.csp.sentinel.adapter.gateway.sc.callback.GatewayCallbackManager;
import com.alibaba.csp.sentinel.slots.block.RuleConstant;
import com.alibaba.csp.sentinel.slots.block.degrade.DegradeRule;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.client.circuitbreaker.Customizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.web.server.ServerWebExchange;

import java.net.InetSocketAddress;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Sentinel Gateway 装配与本地规则基线。
 *
 * <p>Sentinel 官方 SCG Filter 按“路由 + API 分组”建立资源；接口规则使用 API 分组，
 * IP 维度通过 {@code CLIENT_IP} 参数流控实现。路由熔断由 Sentinel Reactive CircuitBreaker
 * 独立建立资源，两条链路互不混淆。
 *
 * @author jiancai.zhong
 */
@Slf4j
@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty(prefix = "zjc.gateway.sentinel", name = "enabled", havingValue = "true", matchIfMissing = true)
@EnableConfigurationProperties(SentinelGatewayProperties.class)
public class SentinelGatewayConfiguration {

    /**
     * 注册支持可信代理头解析的 SCG Filter。
     *
     * @param properties Sentinel 配置
     * @return Sentinel Gateway 全局过滤器
     */
    @Bean
    public SentinelGatewayFilter sentinelGatewayFilter(SentinelGatewayProperties properties) {
        properties.validate();
        ForwardedItemParser itemParser = new ForwardedItemParser(properties.getTrustedProxies());
        GatewayCallbackManager.setRequestOriginParser(itemParser::getRemoteAddress);
        return new SentinelGatewayFilter(Ordered.HIGHEST_PRECEDENCE, itemParser);
    }

    /**
     * 启动时加载本地接口/API 分组规则基线。
     *
     * @param properties Sentinel 配置
     * @return 初始化任务
     */
    @Bean
    public ApplicationRunner sentinelGatewayRuleInitializer(SentinelGatewayProperties properties) {
        return args -> {
            List<SentinelGatewayProperties.InterfaceRule> rules = properties.getInterfaces();
            Set<ApiDefinition> apiDefinitions = new HashSet<>();
            Set<GatewayFlowRule> flowRules = new HashSet<>();

            for (SentinelGatewayProperties.InterfaceRule rule : rules) {
                apiDefinitions.add(new ApiDefinition(rule.getName())
                        .setPredicateItems(Set.of(new ApiPathPredicateItem()
                                .setPattern(rule.getPattern())
                                .setMatchStrategy(SentinelGatewayConstants.URL_MATCH_STRATEGY_REGEX))));

                flowRules.add(new GatewayFlowRule(rule.getName())
                        .setResourceMode(SentinelGatewayConstants.RESOURCE_MODE_CUSTOM_API_NAME)
                        .setGrade(RuleConstant.FLOW_GRADE_QPS)
                        .setCount(rule.getTotalQps())
                        .setIntervalSec(rule.getIntervalSec())
                        .setControlBehavior(RuleConstant.CONTROL_BEHAVIOR_DEFAULT));

                flowRules.add(new GatewayFlowRule(rule.getName())
                        .setResourceMode(SentinelGatewayConstants.RESOURCE_MODE_CUSTOM_API_NAME)
                        .setGrade(RuleConstant.FLOW_GRADE_QPS)
                        .setCount(rule.getPerIpQps())
                        .setIntervalSec(rule.getIntervalSec())
                        .setControlBehavior(RuleConstant.CONTROL_BEHAVIOR_DEFAULT)
                        .setParamItem(new GatewayParamFlowItem()
                                .setParseStrategy(SentinelGatewayConstants.PARAM_PARSE_STRATEGY_CLIENT_IP)));
            }

            GatewayApiDefinitionManager.loadApiDefinitions(apiDefinitions);
            GatewayRuleManager.loadRules(flowRules);
            log.info("Sentinel Gateway 规则已加载：API 分组 {} 个，流控规则 {} 条", apiDefinitions.size(), flowRules.size());
        };
    }

    /**
     * 为每条路由熔断器配置异常比例与慢请求比例规则。
     *
     * @param properties Sentinel 配置
     * @return Reactive Sentinel CircuitBreaker 定制器
     */
    @Bean
    public Customizer<ReactiveSentinelCircuitBreakerFactory> sentinelCircuitBreakerCustomizer(
            SentinelGatewayProperties properties) {
        return factory -> factory.configureDefault(id -> {
            SentinelGatewayProperties.CircuitRule rule = properties.getCircuit();
            List<DegradeRule> degradeRules = List.of(
                    new DegradeRule(id)
                            .setGrade(RuleConstant.DEGRADE_GRADE_EXCEPTION_RATIO)
                            .setCount(rule.getExceptionRatio())
                            .setMinRequestAmount(rule.getMinRequestAmount())
                            .setStatIntervalMs(rule.getStatIntervalMs())
                            .setTimeWindow(rule.getRecoverySeconds()),
                    new DegradeRule(id)
                            .setGrade(RuleConstant.DEGRADE_GRADE_RT)
                            .setCount(rule.getSlowRequestRtThresholdMs())
                            .setSlowRatioThreshold(rule.getSlowRequestRatio())
                            .setMinRequestAmount(rule.getMinRequestAmount())
                            .setStatIntervalMs(rule.getStatIntervalMs())
                            .setTimeWindow(rule.getRecoverySeconds()));
            return new SentinelConfigBuilder()
                    .resourceName(id)
                    .entryType(EntryType.OUT)
                    .rules(degradeRules)
                    .build();
        });
    }

    /**
     * 只有可信代理转发时才采信 X-Forwarded-For，避免直连客户端伪造来源 IP。
     */
    static final class ForwardedItemParser extends ServerWebExchangeItemParser {

        private static final String FORWARDED_FOR = "X-Forwarded-For";

        private final Set<String> trustedProxies;

        ForwardedItemParser(List<String> trustedProxies) {
            this.trustedProxies = Set.copyOf(trustedProxies);
        }

        @Override
        public String getRemoteAddress(ServerWebExchange exchange) {
            String directAddress = Optional.ofNullable(exchange.getRequest().getRemoteAddress())
                    .map(InetSocketAddress::getAddress)
                    .map(address -> address.getHostAddress())
                    .orElse("");

            String forwardedFor = exchange.getRequest().getHeaders().getFirst(FORWARDED_FOR);
            if (trustedProxies.contains(directAddress) && forwardedFor != null && !forwardedFor.isBlank()) {
                String candidate = forwardedFor.split(",", 2)[0].trim();
                if (!candidate.isBlank() && !"unknown".equalsIgnoreCase(candidate)) {
                    return candidate;
                }
            }
            return directAddress;
        }
    }
}
