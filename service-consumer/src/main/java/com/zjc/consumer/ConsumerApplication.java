package com.zjc.consumer;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;

/**
 * Consumer 服务启动类。
 *
 * <p>通过 {@code @EnableFeignClients} 指定扫描 {@code com.zjc.common.api} 包，
 * 直接复用 common 模块中共享的 Feign API 契约，
 * 包括 {@code TestApi}、{@code MailFeignApi}、{@code UserFeignApi}，
 * consumer 无需在本地重复声明。
 *
 * @author jiancai.zhong
 */
@SpringBootApplication
@EnableDiscoveryClient
@EnableFeignClients(basePackages = {"com.zjc.common.api"})
public class ConsumerApplication {
    /**
     * 启动 Consumer 服务，注册到 Nacos 并加入微服务集群。
     *
     * @param args 启动参数，可传入 {@code --server.port} 等 Spring Boot 标准参数
     */
    public static void main(String[] args) {
        SpringApplication.run(ConsumerApplication.class, args);
    }
}
