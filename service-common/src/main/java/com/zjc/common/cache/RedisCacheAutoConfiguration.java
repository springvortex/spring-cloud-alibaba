package com.zjc.common.cache;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.cache.autoconfigure.RedisCacheManagerBuilderCustomizer;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cache.annotation.CachingConfigurer;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.interceptor.CacheErrorHandler;
import org.springframework.cache.support.NullValue;
import org.springframework.context.annotation.Bean;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnection;
import org.springframework.data.redis.core.RedisOperations;
import org.springframework.data.redis.serializer.GenericJacksonJsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import tools.jackson.databind.jsontype.PolymorphicTypeValidator;
import tools.jackson.databind.jsontype.BasicPolymorphicTypeValidator;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Redis 缓存统一装配。
 *
 * <p>所有使用 Redis starter 的服务引入 common 后，自动获得一致的 JSON 序列化、key 前缀、
 * TTL 配置和缓存故障降级策略。未引入 Redis starter 的服务不会加载本配置。
 *
 * @author jiancai.zhong
 */
@AutoConfiguration
@EnableCaching
@EnableConfigurationProperties(RedisCacheProperties.class)
@ConditionalOnClass({RedisOperations.class, LettuceConnection.class})
@ConditionalOnProperty(prefix = "zjc.cache.redis", name = "enabled", havingValue = "true", matchIfMissing = true)
public class RedisCacheAutoConfiguration implements CachingConfigurer {

    private final ObjectProvider<CacheErrorHandler> cacheErrorHandlers;

    RedisCacheAutoConfiguration(ObjectProvider<CacheErrorHandler> cacheErrorHandlers) {
        this.cacheErrorHandlers = cacheErrorHandlers;
    }

    /**
     * Redis JSON 序列化器。
     *
     * <p>类型信息只允许来自 Spring Cache 的空值对象和项目内部 DTO，避免把 Redis 当作可信边界
     * 而引入任意类型反序列化风险。
     *
     * @return 通用 JSON 序列化器
     */
    @Bean
    @ConditionalOnMissingBean(GenericJacksonJsonRedisSerializer.class)
    public GenericJacksonJsonRedisSerializer redisJsonRedisSerializer() {
        PolymorphicTypeValidator typeValidator = BasicPolymorphicTypeValidator.builder()
                .allowIfSubType("com.zjc.")
                .allowIfSubType(NullValue.class)
                .build();

        return GenericJacksonJsonRedisSerializer.builder()
                .enableDefaultTyping(typeValidator)
                .enableSpringCacheNullValueSupport()
                .typePropertyName("@type")
                .build();
    }

    /**
     * Redis 缓存默认配置。
     *
     * @param properties 公共缓存配置
     * @param serializer JSON 序列化器
     * @return Redis 缓存默认配置
     */
    @Bean
    @ConditionalOnMissingBean(RedisCacheConfiguration.class)
    public RedisCacheConfiguration redisCacheConfiguration(RedisCacheProperties properties,
                                                           GenericJacksonJsonRedisSerializer serializer) {
        return baseConfiguration(properties, serializer);
    }

    /**
     * 按 cacheName 应用独立 TTL。
     *
     * @param properties 公共缓存配置
     * @param serializer JSON 序列化器
     * @return Redis CacheManager 扩展器
     */
    @Bean
    public RedisCacheManagerBuilderCustomizer redisCacheTtlCustomizer(RedisCacheProperties properties,
                                                                      GenericJacksonJsonRedisSerializer serializer) {
        return builder -> {
            Map<String, RedisCacheConfiguration> configurations = new LinkedHashMap<>();
            properties.getCacheTtls().forEach((cacheName, ttl) ->
                    configurations.put(cacheName, baseConfiguration(properties, serializer).entryTtl(ttl)));
            if (!configurations.isEmpty()) {
                builder.withInitialCacheConfigurations(configurations);
            }
        };
    }

    /**
     * 缓存故障降级处理器。
     *
     * @return CacheErrorHandler
     */
    @Bean
    @ConditionalOnMissingBean(CacheErrorHandler.class)
    public CacheErrorHandler resilientCacheErrorHandler() {
        return new ResilientCacheErrorHandler();
    }

    @Override
    public CacheErrorHandler errorHandler() {
        return cacheErrorHandlers.getIfAvailable(ResilientCacheErrorHandler::new);
    }

    private RedisCacheConfiguration baseConfiguration(RedisCacheProperties properties,
                                                     GenericJacksonJsonRedisSerializer serializer) {
        Duration ttl = properties.getDefaultTtl();
        return RedisCacheConfiguration.defaultCacheConfig()
                .computePrefixWith(cacheName -> properties.getKeyPrefix() + cacheName + ":")
                .entryTtl(ttl)
                .serializeKeysWith(RedisSerializationContext.SerializationPair
                        .fromSerializer(new StringRedisSerializer()))
                .serializeValuesWith(RedisSerializationContext.SerializationPair
                        .fromSerializer(serializer));
    }
}
