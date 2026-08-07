package com.zjc.provider;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

/**
 * Provider 服务启动类。
 *
 * <p>作为数据提供方，提供用户、商品、订单等核心业务的 CRUD 接口，
 * 注册到 Nacos 供 consumer 通过 Feign 或网关通过路由调用。
 * {@code @MapperScan} 指定 MyBatis Mapper 接口扫描路径。
 *
 * @author jiancai.zhong
 */
@SpringBootApplication
@EnableDiscoveryClient
@MapperScan("com.zjc.provider.mapper")
public class ProviderApplication {
    public static void main(String[] args) {
        SpringApplication.run(ProviderApplication.class, args);
    }
}
