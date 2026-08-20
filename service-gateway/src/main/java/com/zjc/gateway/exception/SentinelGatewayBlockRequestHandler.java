package com.zjc.gateway.exception;

import com.alibaba.csp.sentinel.adapter.gateway.sc.callback.BlockRequestHandler;
import org.jspecify.annotations.NullMarked;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.ServerResponse;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * Sentinel 限流/熔断拒绝响应。
 *
 * @author jiancai.zhong
 */
@NullMarked
@Component
public class SentinelGatewayBlockRequestHandler implements BlockRequestHandler {

    @Override
    public Mono<ServerResponse> handleRequest(ServerWebExchange exchange, Throwable ex) {
        return ServerResponse.status(HttpStatus.TOO_MANY_REQUESTS)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(new BlockedResponse(
                        false,
                        HttpStatus.TOO_MANY_REQUESTS.value(),
                        "请求过于频繁，请稍后再试",
                        null,
                        System.currentTimeMillis()));
    }

    private record BlockedResponse(boolean success,
                                   int code,
                                   String message,
                                   Object data,
                                   long timestamp) {
    }
}
