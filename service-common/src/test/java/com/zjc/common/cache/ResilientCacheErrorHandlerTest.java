package com.zjc.common.cache;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.cache.Cache;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * 缓存故障降级处理器测试。
 *
 * @author jiancai.zhong
 */
@DisplayName("缓存故障降级处理器")
class ResilientCacheErrorHandlerTest {

    @Test
    @DisplayName("Redis 读写异常不会传递给业务调用方")
    void testReadAndWriteErrorsDoNotBreakBusinessCall() {
        ResilientCacheErrorHandler handler = new ResilientCacheErrorHandler();
        Cache cache = mock(Cache.class);
        when(cache.getName()).thenReturn("provider:user:id");
        RuntimeException exception = new IllegalStateException("redis unavailable");

        assertThatCode(() -> handler.handleCacheGetError(exception, cache, "1"))
                .doesNotThrowAnyException();
        assertThatCode(() -> handler.handleCachePutError(exception, cache, "1", "value"))
                .doesNotThrowAnyException();
        assertThatCode(() -> handler.handleCacheEvictError(exception, cache, "1"))
                .doesNotThrowAnyException();
        assertThatCode(() -> handler.handleCacheClearError(exception, cache))
                .doesNotThrowAnyException();
    }
}
