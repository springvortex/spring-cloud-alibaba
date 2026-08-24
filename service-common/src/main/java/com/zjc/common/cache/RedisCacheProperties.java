package com.zjc.common.cache;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Redis 缓存公共配置。
 *
 * @author jiancai.zhong
 */
@Setter
@Getter
@ConfigurationProperties(prefix = "zjc.cache.redis")
public class RedisCacheProperties {

    /**
     * 是否启用公共 Redis 缓存装配。
     */
    private boolean enabled = true;

    /**
     * 全局 key 前缀，最终格式为：{@code prefix + cacheName + ":" + key}。
     */
    private String keyPrefix = "zjc:";

    /**
     * 默认缓存 TTL。
     */
    private Duration defaultTtl = Duration.ofMinutes(30);

    /**
     * 按 cacheName 覆盖 TTL。
     */
    private Map<String, Duration> cacheTtls = new LinkedHashMap<>();

}
