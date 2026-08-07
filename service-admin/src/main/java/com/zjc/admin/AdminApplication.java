package com.zjc.admin;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

/**
 * Admin 服务启动类。
 *
 * <p>后台管理模块入口，通过 {@code @EnableDiscoveryClient} 注册到 Nacos，
 * 供网关或其他微服务通过服务发现进行调用。
 *
 * @author jiancai.zhong
 */
@SpringBootApplication
@EnableDiscoveryClient
public class AdminApplication {
    public static void main(String[] args) {
        SpringApplication.run(AdminApplication.class, args);
    }
}
