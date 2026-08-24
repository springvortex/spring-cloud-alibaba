package com.zjc.provider.config;

import lombok.Getter;
import lombok.Setter;
import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.redisson.config.SingleServerConfig;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

import java.time.Duration;

/**
 * Redisson 客户端装配。
 *
 * <p>连接信息复用 Spring Data Redis 配置，避免 dev/prod 维护两份 Redis 地址。
 * 客户端在第一条命令执行时才建立连接。
 *
 * @author jiancai.zhong
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnClass(RedissonClient.class)
@ConditionalOnProperty(prefix = "zjc.redisson", name = "enabled", havingValue = "true")
@EnableConfigurationProperties({RedissonConfiguration.RedisConnectionProperties.class,
        RedissonConfiguration.RedissonProperties.class})
public class RedissonConfiguration {

    /**
     * Redisson 客户端。
     *
     * @param redisProperties Spring Data Redis 连接配置
     * @param redissonProperties Redisson 扩展配置
     * @return Redisson 客户端
     */
    @Bean(destroyMethod = "shutdown")
    @ConditionalOnMissingBean(RedissonClient.class)
    public RedissonClient redissonClient(RedisConnectionProperties redisProperties,
                                         RedissonProperties redissonProperties) {
        return Redisson.create(redissonConfig(redisProperties, redissonProperties));
    }

    static Config redissonConfig(RedisConnectionProperties redisProperties,
                                 RedissonProperties redissonProperties) {
        Config config = new Config();
        config.setLazyInitialization(true);
        config.setLockWatchdogTimeout((int) redissonProperties.getLockWatchdogTimeout().toMillis());

        SingleServerConfig singleServer = config.useSingleServer();
        singleServer.setAddress("redis://" + redisProperties.getHost() + ":" + redisProperties.getPort())
                .setDatabase(redisProperties.getDatabase())
                .setConnectTimeout((int) redisProperties.getConnectTimeout().toMillis())
                .setTimeout((int) redisProperties.getTimeout().toMillis());

        if (StringUtils.hasText(redisProperties.getUsername())) {
            config.setUsername(redisProperties.getUsername());
        }
        if (StringUtils.hasText(redisProperties.getPassword())) {
            config.setPassword(redisProperties.getPassword());
        }
        return config;
    }

    /**
     * 只绑定业务所需的 Spring Data Redis 连接字段。
     *
     * @author jiancai.zhong
     */
    @Getter
    @Setter
    @ConfigurationProperties(prefix = "spring.data.redis")
    static class RedisConnectionProperties {

        private String host = "127.0.0.1";

        private int port = 6379;

        private String username;

        private String password;

        private Duration connectTimeout = Duration.ofSeconds(2);

        private Duration timeout = Duration.ofSeconds(2);

        private int database = 0;
    }

    /**
     * Redisson 扩展配置。
     *
     * @author jiancai.zhong
     */
    @Getter
    @Setter
    @ConfigurationProperties(prefix = "zjc.redisson")
    static class RedissonProperties {

        /**
         * 看门狗续期间隔基础值。使用不带 leaseTime 的 lock/tryLock 时自动续期。
         */
        private Duration lockWatchdogTimeout = Duration.ofSeconds(30);
    }
}
