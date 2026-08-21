package com.zjc.gateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

/**
 * API 网关服务启动类。
 *
 * <p>基于 Spring Cloud Gateway，作为整个微服务集群的统一流量入口，
 * 负责请求路由转发、跨域处理、鉴权拦截等。
 * 通过 {@code @EnableDiscoveryClient} 从 Nacos 获取下游服务地址，
 * 路由规则由本地环境 Profile 维护，配置前缀为
 * {@code spring.cloud.gateway.server.webflux.routes}。
 *
 * @author jiancai.zhong
 */
@EnableDiscoveryClient
@SpringBootApplication
public class GatewayApplication {

    public static void main(String[] args) {
        SpringApplication.run(GatewayApplication.class, args);
    }
}
