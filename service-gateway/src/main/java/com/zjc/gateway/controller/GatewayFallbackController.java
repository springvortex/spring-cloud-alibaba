package com.zjc.gateway.controller;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * 路由熔断后的内部兜底接口。
 *
 * @author jiancai.zhong
 */
@NullMarked
@RestController
public class GatewayFallbackController {

    @RequestMapping("/gateway/fallback/{route}")
    @ResponseStatus(HttpStatus.SERVICE_UNAVAILABLE)
    public GatewayFallbackResponse fallback(@PathVariable("route") String route) {
        return new GatewayFallbackResponse(
                false,
                HttpStatus.SERVICE_UNAVAILABLE.value(),
                "下游服务暂不可用，请稍后再试",
                null,
                System.currentTimeMillis());
    }

    /**
     * 与业务服务的统一响应结构保持一致。
     */
    private record GatewayFallbackResponse(boolean success,
                                           int code,
                                           String message,
                                           @Nullable Object data,
                                           long timestamp) {
    }
}
