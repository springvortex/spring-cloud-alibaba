package com.zjc.provider.config;

import com.alibaba.cloud.nacos.NacosConfigManager;
import com.alibaba.nacos.api.config.ConfigService;
import com.alibaba.nacos.api.config.listener.Listener;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

/**
 * Nacos 配置变更监听配置。
 *
 * <p>监听 dataId=激活环境（如 dev）、group=服务名的配置，
 * 当 Nacos 上的配置发生变更时触发回调，可用于邮件通知、日志记录、缓存刷新等。
 *
 * @author jiancai.zhong
 */
@Slf4j
@Configuration
public class NacosConfigListenerConfig {

    @Value("${spring.profiles.active}")
    private String activeProfile;

    @Value("${spring.application.name}")
    private String applicationName;

    /**
     * 注册 Nacos 配置变更监听器，dataId 和 group 与 application.yaml 中 config.import 保持一致。
     *
     * @param nacosConfigManager Nacos 配置管理器，由 Spring Cloud Alibaba 自动装配
     * @return ApplicationRunner，启动后异步注册监听
     */
    @Bean
    ApplicationRunner nacosConfigListenerRunner(NacosConfigManager nacosConfigManager) {
        return args -> {
            ConfigService configService = nacosConfigManager.getConfigService();
            configService.addListener(activeProfile, applicationName, new Listener() {
                @Override
                public Executor getExecutor() {
                    return Executors.newFixedThreadPool(4);
                }

                @Override
                public void receiveConfigInfo(String configInfo) {
                    log.info("监听到 Nacos 配置变更，dataId={}, group={}", activeProfile, applicationName);
                    log.info("变更内容：{}", configInfo);
                }
            });
            log.info("Nacos 配置监听器注册成功，dataId={}, group={}", activeProfile, applicationName);
        };
    }
}
