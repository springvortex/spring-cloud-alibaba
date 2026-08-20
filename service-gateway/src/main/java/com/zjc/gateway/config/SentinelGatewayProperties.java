package com.zjc.gateway.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * 网关 Sentinel 限流与熔断规则配置。
 *
 * @author jiancai.zhong
 */
@Getter
@Setter
@ConfigurationProperties(prefix = "zjc.gateway.sentinel")
public class SentinelGatewayProperties {

    /**
     * 是否启用网关 Sentinel 流控。
     */
    private boolean enabled = true;

    /**
     * 允许解析 X-Forwarded-For 的直连代理地址。
     */
    private List<String> trustedProxies = List.of("127.0.0.1", "::1", "0:0:0:0:0:0:0:1");

    /**
     * 接口维度规则。同一个规则会同时生成全局 QPS 与单 IP QPS 限制。
     */
    private List<InterfaceRule> interfaces = new ArrayList<>();

    /**
     * 路由熔断默认阈值。
     */
    private CircuitRule circuit = new CircuitRule();

    /**
     * 校验配置，避免非法规则进入 Sentinel。
     */
    public void validate() {
        trustedProxies.stream()
                .filter(proxy -> proxy == null || proxy.isBlank())
                .findAny()
                .ifPresent(proxy -> {
                    throw new IllegalStateException("zjc.gateway.sentinel.trusted-proxies must not contain blank values");
                });
        Set<String> names = new HashSet<>();
        for (InterfaceRule rule : interfaces) {
            rule.validate();
            if (!names.add(rule.getName())) {
                throw new IllegalStateException("Sentinel interface rule name must be unique: " + rule.getName());
            }
            try {
                Pattern.compile(rule.getPattern());
            } catch (RuntimeException exception) {
                throw new IllegalStateException("Sentinel interface rule pattern is invalid: " + rule.getName(), exception);
            }
        }
        circuit.validate();
    }

    /**
     * 接口流控规则。
     */
    @Getter
    @Setter
    public static class InterfaceRule {

        /**
         * Sentinel API 分组名，需全局唯一。
         */
        private String name;

        /**
         * 接口路径正则，例如 {@code /api/[^/]+/provider/user/\d+}。
         */
        private String pattern;

        /**
         * 接口全局 QPS 上限。
         */
        private double totalQps = 100;

        /**
         * 单个客户端 IP 的 QPS 上限。
         */
        private double perIpQps = 20;

        /**
         * 统计窗口秒数。
         */
        private int intervalSec = 1;

        void validate() {
            if (name == null || name.isBlank()) {
                throw new IllegalStateException("Sentinel interface rule name is required");
            }
            if (pattern == null || pattern.isBlank()) {
                throw new IllegalStateException("Sentinel interface rule pattern is required: " + name);
            }
            if (totalQps <= 0 || perIpQps <= 0 || intervalSec <= 0) {
                throw new IllegalStateException("Sentinel interface rule thresholds must be positive: " + name);
            }
            if (perIpQps > totalQps) {
                throw new IllegalStateException("Sentinel per-IP QPS must not exceed total QPS: " + name);
            }
        }
    }

    /**
     * 路由熔断规则。
     */
    @Getter
    @Setter
    public static class CircuitRule {

        /**
         * 触发熔断的最小请求数。
         */
        private int minRequestAmount = 5;

        /**
         * 异常比例阈值，范围 0-1。
         */
        private double exceptionRatio = 0.5;

        /**
         * 慢请求 RT 阈值，单位毫秒。
         */
        private int slowRequestRtThresholdMs = 2000;

        /**
         * 慢请求比例阈值，范围 0-1。
         */
        private double slowRequestRatio = 0.8;

        /**
         * 统计窗口，单位毫秒。
         */
        private int statIntervalMs = 60_000;

        /**
         * 熔断恢复窗口，单位秒。
         */
        private int recoverySeconds = 10;

        void validate() {
            if (minRequestAmount <= 0 || slowRequestRtThresholdMs <= 0
                    || statIntervalMs <= 0 || recoverySeconds <= 0) {
                throw new IllegalStateException("Sentinel circuit thresholds must be positive");
            }
            if (exceptionRatio <= 0 || exceptionRatio > 1
                    || slowRequestRatio <= 0 || slowRequestRatio > 1) {
                throw new IllegalStateException("Sentinel circuit ratios must be in (0, 1]");
            }
        }
    }
}
