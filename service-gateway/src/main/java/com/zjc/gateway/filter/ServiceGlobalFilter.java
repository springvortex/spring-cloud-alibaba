package com.zjc.gateway.filter;

import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NullMarked;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * 网关全局请求日志过滤器。
 *
 * <p>记录每个经过网关的请求开始、结束时间和耗时。使用 {@code doFinally}
 * 保证请求正常完成、异常终止或被取消时都会输出结束日志。
 *
 * @author jiancai.zhong
 */
@Slf4j
@Component
@NullMarked
public class ServiceGlobalFilter implements GlobalFilter, Ordered {

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String method = request.getMethod().name();
        String uri = request.getURI().toString();
        long startTime = System.currentTimeMillis();

        log.info("请求【{} {}】开始：时间：{}", method, uri, startTime);

        return chain.filter(exchange)
                .doFinally(signalType -> {
                    long endTime = System.currentTimeMillis();
                    log.info("请求【{} {}】结束：时间：{}，耗时：{}ms，信号：{}",
                            method, uri, endTime, endTime - startTime, signalType);
                });
    }

    @Override
    public int getOrder() {
        return 0;
    }
}
