package com.zjc.gateway.exception;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.http.codec.HttpMessageWriter;
import org.springframework.http.codec.ServerCodecConfigurer;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.reactive.function.server.ServerResponse;
import org.springframework.web.reactive.result.view.ViewResolver;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@link SentinelGatewayBlockRequestHandler} unit tests.
 *
 * @author jiancai.zhong
 */
@DisplayName("网关 Sentinel 限流响应")
class SentinelGatewayBlockRequestHandlerTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    @DisplayName("限流时返回 429 和统一 JSON")
    void testBlockedRequestReturnsUnifiedResponse() {
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/v1/provider/user/1").build());
        ServerResponse response = new SentinelGatewayBlockRequestHandler()
                .handleRequest(exchange, new IllegalStateException("flow limit"))
                .block();

        assertThat(response).isNotNull();
        assertThat(response.statusCode().value()).isEqualTo(429);

        response.writeTo(exchange, new TestContext()).block();

        assertThat(exchange.getResponse().getStatusCode().value()).isEqualTo(429);
        assertThat(exchange.getResponse().getHeaders().getContentType())
                .isEqualTo(MediaType.APPLICATION_JSON);
        JsonNode body = objectMapper.readTree(exchange.getResponse().getBodyAsString().block());
        assertThat(body.get("success").asBoolean()).isFalse();
        assertThat(body.get("code").asInt()).isEqualTo(429);
        assertThat(body.get("message").asString()).isEqualTo("请求过于频繁，请稍后再试");
        assertThat(body.get("data").isNull()).isTrue();
        assertThat(body.get("timestamp").isNumber()).isTrue();
    }

    private static final class TestContext implements ServerResponse.Context {

        @Override
        public List<HttpMessageWriter<?>> messageWriters() {
            return ServerCodecConfigurer.create().getWriters();
        }

        @Override
        public List<ViewResolver> viewResolvers() {
            return List.of();
        }
    }
}
