package com.zjc.common.lock;

import com.zjc.common.exception.BusinessException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * {@link RedissonDistributedLockTemplate} 分布式锁模板测试.
 *
 * @author jiancai.zhong
 */
@DisplayName("Redisson 分布式锁模板")
@ExtendWith(MockitoExtension.class)
class DistributedLockTemplateTest {

    /**
     * 测试锁 key。
     */
    private static final String LOCK_KEY = "zjc:test:lock:1";

    /**
     * 测试锁等待时间。
     */
    private static final Duration WAIT_TIME = Duration.ofMillis(500);

    @Mock
    private RedissonClient redissonClient;

    @Mock
    private RLock lock;

    @InjectMocks
    private RedissonDistributedLockTemplate lockTemplate;

    /**
     * 验证获取锁后执行业务回调，并释放当前线程持有的锁。
     */
    @Test
    @DisplayName("execute: 获取锁后执行业务并释放锁")
    void executesActionAndUnlocks() throws InterruptedException {
        when(redissonClient.getLock(LOCK_KEY)).thenReturn(lock);
        when(lock.tryLock(WAIT_TIME.toMillis(), TimeUnit.MILLISECONDS)).thenReturn(true);

        String result = lockTemplate.execute(LOCK_KEY, WAIT_TIME, () -> "ok");

        assertThat(result).isEqualTo("ok");
        verify(lock).unlock();
    }

    /**
     * 验证锁等待超时时快速失败，不执行业务，也不尝试解锁。
     */
    @Test
    @DisplayName("execute: 锁等待超时返回自定义业务提示")
    void throwsBusinessExceptionWhenLockTimeout() throws InterruptedException {
        when(redissonClient.getLock(LOCK_KEY)).thenReturn(lock);
        when(lock.tryLock(WAIT_TIME.toMillis(), TimeUnit.MILLISECONDS)).thenReturn(false);

        assertThatThrownBy(() -> lockTemplate.execute(
                LOCK_KEY, WAIT_TIME, "当前人数过多", () -> "ok"))
                .isInstanceOf(BusinessException.class)
                .hasMessage("当前人数过多");

        verify(lock, never()).unlock();
    }

    /**
     * 验证业务回调抛出异常时原样传递，并在 finally 中释放锁。
     */
    @Test
    @DisplayName("execute: 业务异常原样传递且仍释放锁")
    void unlocksWhenActionThrows() throws InterruptedException {
        when(redissonClient.getLock(LOCK_KEY)).thenReturn(lock);
        when(lock.tryLock(WAIT_TIME.toMillis(), TimeUnit.MILLISECONDS)).thenReturn(true);
        IllegalStateException expected = new IllegalStateException("business failed");

        assertThatThrownBy(() -> lockTemplate.execute(LOCK_KEY, WAIT_TIME, () -> {
            throw expected;
        })).isSameAs(expected);

        verify(lock).unlock();
    }

    /**
     * 验证解锁前锁已丢失时不掩盖业务结果。
     */
    @Test
    @DisplayName("execute: 锁丢失时不掩盖业务结果")
    void ignoresLostLockWhenUnlocking() throws InterruptedException {
        when(redissonClient.getLock(LOCK_KEY)).thenReturn(lock);
        when(lock.tryLock(WAIT_TIME.toMillis(), TimeUnit.MILLISECONDS)).thenReturn(true);
        doThrow(new IllegalMonitorStateException("lock lost")).when(lock).unlock();

        String result = lockTemplate.execute(LOCK_KEY, WAIT_TIME, () -> "ok");

        assertThat(result).isEqualTo("ok");
    }

    /**
     * 验证 Redis 解锁异常不掩盖已经完成的业务结果。
     */
    @Test
    @DisplayName("execute: 解锁失败不掩盖业务结果")
    void ignoresUnlockFailure() throws InterruptedException {
        when(redissonClient.getLock(LOCK_KEY)).thenReturn(lock);
        when(lock.tryLock(WAIT_TIME.toMillis(), TimeUnit.MILLISECONDS)).thenReturn(true);
        doThrow(new IllegalStateException("redis unavailable")).when(lock).unlock();

        String result = lockTemplate.execute(LOCK_KEY, WAIT_TIME, () -> "ok");

        assertThat(result).isEqualTo("ok");
    }

    /**
     * 验证获取锁被中断时恢复线程中断标记并返回服务不可用。
     */
    @Test
    @DisplayName("execute: 获取锁被中断时恢复中断标记")
    void restoresInterruptFlagAndThrows() throws InterruptedException {
        when(redissonClient.getLock(LOCK_KEY)).thenReturn(lock);
        when(lock.tryLock(WAIT_TIME.toMillis(), TimeUnit.MILLISECONDS))
                .thenThrow(new InterruptedException());

        assertThatThrownBy(() -> lockTemplate.execute(LOCK_KEY, WAIT_TIME, () -> "ok"))
                .isInstanceOf(BusinessException.class);
        assertThat(Thread.interrupted()).isTrue();
    }
}
