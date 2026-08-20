package com.zjc.gateway.config;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.cloud.client.circuitbreaker.ReactiveCircuitBreakerFactory;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.SpringCloudCircuitBreakerFilterFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.DispatcherHandler;
import reactor.core.publisher.Mono;

/**
 * Sentinel 版 Gateway CircuitBreaker 过滤器。
 *
 * @author jiancai.zhong
 */
@Component
public class SentinelGatewayCircuitBreakerFilterFactory extends SpringCloudCircuitBreakerFilterFactory {

    public SentinelGatewayCircuitBreakerFilterFactory(
            ReactiveCircuitBreakerFactory circuitBreakerFactory,
            ObjectProvider<DispatcherHandler> dispatcherHandlerProvider) {
        super(circuitBreakerFactory, dispatcherHandlerProvider);
    }

    @Override
    public GatewayFilter apply(Config config) {
        String routeId = config.getRouteId();
        if (routeId != null && !routeId.isBlank()) {
            if (config.getName() == null || config.getName().isBlank()) {
                config.setName(routeId + "-circuit");
            }
            if (config.getFallbackUri() == null) {
                config.setFallbackUri("forward:/gateway/fallback/" + routeId);
            }
        }
        return super.apply(config);
    }

    @Override
    protected Mono<Void> handleErrorWithoutFallback(Throwable throwable, boolean resumeWithoutError) {
        if (resumeWithoutError) {
            return Mono.empty();
        }
        return Mono.error(throwable);
    }
}
