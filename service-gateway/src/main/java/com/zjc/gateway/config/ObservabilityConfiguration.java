package com.zjc.gateway.config;

import jakarta.annotation.PostConstruct;
import org.springframework.context.annotation.Configuration;
import reactor.core.publisher.Hooks;

/**
 * 可观测性配置。
 *
 * <p>Gateway 的请求处理是 Reactor 异步链路。开启自动上下文传播后，
 * Micrometer Tracing 才能把 Reactor Context 中的 traceId/spanId
 * 恢复到回调线程的 MDC，供 Logback 输出。
 *
 * @author jiancai.zhong
 */
@Configuration(proxyBeanMethods = false)
public class ObservabilityConfiguration {

    @PostConstruct
    public void enableReactorContextPropagation() {
        Hooks.enableAutomaticContextPropagation();
    }
}
