package com.zjc.common.cache;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.Cache;
import org.springframework.cache.interceptor.CacheErrorHandler;

/**
 * 缓存故障时的业务降级策略。
 *
 * <p>读和写入失败不影响业务请求；清理失败可能导致旧数据继续存在，必须记录较高级别日志。
 *
 * @author jiancai.zhong
 */
@Slf4j
public class ResilientCacheErrorHandler implements CacheErrorHandler {

    @Override
    public void handleCacheGetError(RuntimeException exception, Cache cache, Object key) {
        log.warn("读取缓存失败，降级执行业务：cache={}, key={}", cache.getName(), key, exception);
    }

    @Override
    public void handleCachePutError(RuntimeException exception, Cache cache, Object key, Object value) {
        log.warn("写入缓存失败，业务结果保持不变：cache={}, key={}", cache.getName(), key, exception);
    }

    @Override
    public void handleCacheEvictError(RuntimeException exception, Cache cache, Object key) {
        log.error("清理缓存失败，旧数据可能保留至 TTL 到期：cache={}, key={}", cache.getName(), key, exception);
    }

    @Override
    public void handleCacheClearError(RuntimeException exception, Cache cache) {
        log.error("清空缓存失败，旧数据可能保留至 TTL 到期：cache={}", cache.getName(), exception);
    }
}
