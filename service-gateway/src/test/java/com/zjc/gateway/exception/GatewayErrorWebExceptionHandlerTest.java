package com.zjc.gateway.exception;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.server.ResponseStatusException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@link GatewayErrorWebExceptionHandler} unit tests.
 *
 * @author jiancai.zhong
 */
@DisplayName("网关统一错误响应")
class GatewayErrorWebExceptionHandlerTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    @DisplayName("路由不存在时返回统一 JSON")
    void testRouteNotFoundReturnsJson() {
        GatewayErrorWebExceptionHandler handler = newHandler();
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/not-exists").build());

        handler.handle(exchange, new ResponseStatusException(HttpStatus.NOT_FOUND, "Route not found"))
                .block();

        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(exchange.getResponse().getHeaders().getContentType())
                .isEqualTo(MediaType.APPLICATION_JSON);
        JsonNode body = objectMapper.readTree(exchange.getResponse().getBodyAsString().block());
        assertThat(body.get("success").asBoolean()).isFalse();
        assertThat(body.get("code").asInt()).isEqualTo(102);
        assertThat(body.get("message").asString()).isEqualTo("请求路径不存在，请检查接口地址或网关路由");
        assertThat(body.get("data").isNull()).isTrue();
        assertThat(body.get("timestamp").isNumber()).isTrue();
    }

    @Test
    @DisplayName("下游不可用时返回 503 和友好提示")
    void testServiceUnavailableReturnsFriendlyResponse() {
        GatewayErrorWebExceptionHandler handler = newHandler();
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/v1/provider/user/1").build());

        handler.handle(exchange, new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Unable to find instance"))
                .block();

        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
        JsonNode body = objectMapper.readTree(exchange.getResponse().getBodyAsString().block());
        assertThat(body.get("code").asInt()).isEqualTo(503);
        assertThat(body.get("message").asString()).isEqualTo("服务不可用，请稍后再试");
    }

    private GatewayErrorWebExceptionHandler newHandler() {
        return new GatewayErrorWebExceptionHandler(objectMapper);
    }
}
