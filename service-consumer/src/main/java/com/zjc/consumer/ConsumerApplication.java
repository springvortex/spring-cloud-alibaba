package com.zjc.consumer;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;

/**
 * Consumer 服务启动类。
 *
 * <p>通过 {@code @EnableFeignClients} 显式指定扫描包：
 * <ul>
 *   <li>{@code com.zjc.consumer} — 扫描 consumer 内部自定义的 Feign 客户端
 *       （如 {@code UserFeignClient}），让非默认包下的本地客户端也能被注册</li>
 *   <li>{@code com.zjc.common.api} — 扫描 common 模块里共享的 Feign API 契约
 *       （如 {@code TestApi}），让 consumer 直接复用而无需重复声明</li>
 * </ul>
 *
 * @author jiancai.zhong
 */
@SpringBootApplication
@EnableDiscoveryClient
@EnableFeignClients(basePackages = {"com.zjc.consumer", "com.zjc.common.api"})
public class ConsumerApplication {
    public static void main(String[] args) {
        SpringApplication.run(ConsumerApplication.class, args);
    }
}
