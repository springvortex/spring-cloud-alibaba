package com.zjc.provider.config;

import com.alibaba.cloud.nacos.NacosConfigManager;
import com.alibaba.nacos.api.config.ConfigService;
import com.alibaba.nacos.api.config.listener.Listener;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

/**
 * Nacos 配置变更监听配置。
 *
 * <p>监听 dataId=激活环境（如 dev）、group=服务名的配置，
 * 当 Nacos 上的配置发生变更时输出变更后的完整内容，
 * 可用于邮件通知、日志记录、缓存刷新等。
 * local 配置来源会关闭 Nacos Config，本配置也会随之跳过装配。
 *
 * @author jiancai.zhong
 */
@Slf4j
@Configuration
@ConditionalOnProperty(name = "spring.cloud.nacos.config.enabled", havingValue = "true", matchIfMissing = true)
public class NacosConfigListenerConfig {

    @Value("${zjc.config.env}")
    private String configEnv;

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
            log.info("Nacos 配置监听器注册成功，dataId={}, group={}", configEnv, applicationName);

            configService.addListener(configEnv, applicationName, new Listener() {
                @Override
                public Executor getExecutor() {
                    return Executors.newFixedThreadPool(4);
                }

                @Override
                public void receiveConfigInfo(String configInfo) {
                    log.info("Nacos 配置变更通知 ==> dataId={}, group={}", configEnv, applicationName);
                    // todo: 如果启用了邮件通知则发送邮件通知，否则不打印日志，不做其他的处理
                }
            });
        };
    }
}
