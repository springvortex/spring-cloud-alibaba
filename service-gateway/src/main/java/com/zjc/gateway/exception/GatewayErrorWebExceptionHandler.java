package com.zjc.gateway.exception;

import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.springframework.boot.webflux.error.ErrorWebExceptionHandler;
import org.springframework.core.Ordered;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.ErrorResponse;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import tools.jackson.databind.ObjectMapper;

/**
 * 网关统一错误响应处理器。
 *
 * <p>Gateway 是 WebFlux 应用，不能复用 WebMVC 的 {@code @RestControllerAdvice}。
 * 这里接管 Boot 默认的 Whitelabel Error Page，将路由不存在、下游不可用等错误
 * 转换为与业务服务一致的 JSON 结构，方便前端统一解析。
 *
 * @author jiancai.zhong
 */
@Slf4j
@Component
@NullMarked
public class GatewayErrorWebExceptionHandler implements ErrorWebExceptionHandler, Ordered {

    /**
     * 高于 Boot 默认错误处理器的 -1，保证当前处理器优先生效。
     */
    private static final int ORDER = -2;

    private static final String CACHE_CONTROL_NO_STORE = "no-store";

    private final ObjectMapper objectMapper;

    public GatewayErrorWebExceptionHandler(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public Mono<Void> handle(ServerWebExchange exchange, Throwable ex) {
        if (exchange.getResponse().isCommitted()) {
            return Mono.error(ex);
        }

        HttpStatusCode status = resolveStatus(ex);
        int code = resolveBusinessCode(status);
        String message = resolveMessage(status);
        String path = exchange.getRequest().getPath().value();

        if (status.is5xxServerError()) {
            log.error("网关请求处理失败：{} {}", exchange.getRequest().getMethod(), path, ex);
        } else {
            log.warn("网关请求处理失败：{} {}，状态码：{}，原因：{}",
                    exchange.getRequest().getMethod(), path, status.value(), ex.getMessage());
        }

        exchange.getResponse().setStatusCode(status);
        exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);
        exchange.getResponse().getHeaders().setCacheControl(CACHE_CONTROL_NO_STORE);

        byte[] body = objectMapper.writeValueAsBytes(
                new GatewayErrorResponse(false, code, message, null, System.currentTimeMillis()));
        return exchange.getResponse()
                .writeWith(Mono.just(exchange.getResponse().bufferFactory().wrap(body)));
    }

    /**
     * 解析异常对应的 HTTP 状态码。
     *
     * @param ex WebFlux 处理链抛出的异常
     * @return HTTP 状态码
     */
    private HttpStatusCode resolveStatus(Throwable ex) {
        if (ex instanceof ErrorResponse errorResponse) {
            return errorResponse.getStatusCode();
        }
        return HttpStatusCode.valueOf(500);
    }

    /**
     * 将 HTTP 状态码映射为业务响应码，保持与 ApiResponseEnum 的码段规划一致。
     *
     * @param status HTTP 状态码
     * @return 业务响应码
     */
    private int resolveBusinessCode(HttpStatusCode status) {
        return switch (status.value()) {
            case 400 -> 101;
            case 401 -> 401;
            case 403 -> 403;
            case 404 -> 102;
            case 500 -> 500;
            case 503 -> 503;
            default -> -1;
        };
    }

    /**
     * 生成对外展示的稳定错误提示，不暴露内部异常细节。
     *
     * @param status HTTP 状态码
     * @return 错误提示文案
     */
    private String resolveMessage(HttpStatusCode status) {
        return switch (status.value()) {
            case 400 -> "请求体格式错误";
            case 401 -> "未认证";
            case 403 -> "无权限";
            case 404 -> "请求路径不存在，请检查接口地址或网关路由";
            case 500 -> "服务内部错误";
            case 503 -> "服务不可用，请稍后再试";
            default -> "操作失败";
        };
    }

    @Override
    public int getOrder() {
        return ORDER;
    }

    /**
     * 网关错误响应体，字段顺序与业务服务的 ApiResponse 保持一致。
     *
     * @param success   是否成功
     * @param code      业务响应码
     * @param message   提示信息
     * @param data      附加数据，当前统一为空
     * @param timestamp 响应生成时间戳
     */
    private record GatewayErrorResponse(boolean success,
                                        int code,
                                        String message,
                                        @Nullable Object data,
                                        long timestamp) {
    }
}
