package com.zjc.mail;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

/**
 * 邮件服务启动类。
 *
 * <p>提供统一邮件收发能力，其他微服务通过 Feign（{@code MailFeignApi}）调用。
 * 发件人、SMTP 配置由本模块 Nacos 配置统一管理，调用方无需关心。
 *
 * @author jiancai.zhong
 */
@SpringBootApplication
@EnableDiscoveryClient
@MapperScan("com.zjc.mail.mapper")
public class MailApplication {

    public static void main(String[] args) {
        SpringApplication.run(MailApplication.class, args);
    }
}
