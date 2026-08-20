package com.zjc.gateway.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@link SentinelGatewayConfiguration.ForwardedItemParser} unit tests.
 *
 * @author jiancai.zhong
 */
@DisplayName("网关 Sentinel 客户端 IP 解析")
class SentinelForwardedItemParserTest {

    @Test
    @DisplayName("可信代理转发时取 X-Forwarded-For 第一个 IP")
    void testTrustedProxyUsesForwardedFor() {
        SentinelGatewayConfiguration.ForwardedItemParser parser =
                new SentinelGatewayConfiguration.ForwardedItemParser(List.of("127.0.0.1"));
        MockServerWebExchange exchange = exchange("127.0.0.1", "10.1.2.3, 10.0.0.1");

        assertThat(parser.getRemoteAddress(exchange)).isEqualTo("10.1.2.3");
    }

    @Test
    @DisplayName("不可信直连客户端不能伪造 X-Forwarded-For")
    void testUntrustedDirectClientCannotSpoofForwardedFor() {
        SentinelGatewayConfiguration.ForwardedItemParser parser =
                new SentinelGatewayConfiguration.ForwardedItemParser(List.of("127.0.0.1"));
        MockServerWebExchange exchange = exchange("203.0.113.10", "10.1.2.3");

        assertThat(parser.getRemoteAddress(exchange)).isEqualTo("203.0.113.10");
    }

    private MockServerWebExchange exchange(String remoteAddress, String forwardedFor) {
        try {
            return MockServerWebExchange.from(MockServerHttpRequest.get("/api/v1/mail/send")
                    .remoteAddress(new InetSocketAddress(InetAddress.getByName(remoteAddress), 50000))
                    .header("X-Forwarded-For", forwardedFor)
                    .build());
        } catch (Exception exception) {
            throw new IllegalStateException(exception);
        }
    }
}
