package com.zjc.common.cache;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
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

    /**
     * 允许携带多态类型信息的 JDK 类型；项目内部 {@code com.zjc.*} 类型默认始终允许。
     *
     * <p>配置值支持完整类名，例如 {@code java.math.BigDecimal}。不要添加
     * {@code java.lang.Object} 或宽泛的 JDK 包前缀，避免扩大 Redis 反序列化攻击面。
     */
    private List<String> allowedSubTypes = new ArrayList<>(List.of(BigDecimal.class.getName()));

}
