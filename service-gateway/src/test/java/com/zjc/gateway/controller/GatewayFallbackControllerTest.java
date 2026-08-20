package com.zjc.gateway.controller;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.reactive.server.WebTestClient;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@link GatewayFallbackController} unit tests.
 *
 * @author jiancai.zhong
 */
@DisplayName("网关路由兜底响应")
class GatewayFallbackControllerTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    @DisplayName("下游故障时返回 503 和统一 JSON")
    void testFallbackReturnsUnifiedResponse() {
        String body = WebTestClient.bindToController(new GatewayFallbackController())
                .configureClient()
                .baseUrl("http://localhost")
                .build()
                .get()
                .uri("/gateway/fallback/provider-route")
                .exchange()
                .expectStatus()
                .isEqualTo(503)
                .expectBody(String.class)
                .returnResult()
                .getResponseBody();

        JsonNode json = objectMapper.readTree(body);
        assertThat(json.get("success").asBoolean()).isFalse();
        assertThat(json.get("code").asInt()).isEqualTo(503);
        assertThat(json.get("message").asString()).isEqualTo("下游服务暂不可用，请稍后再试");
        assertThat(json.get("data").isNull()).isTrue();
        assertThat(json.get("timestamp").isNumber()).isTrue();
    }
}
