package com.zjc.common.lock;

import com.zjc.common.constant.ApiResponseEnum;
import com.zjc.common.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;

import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

/**
 * Redisson 分布式锁实现。
 *
 * <p>使用可重入锁，不显式传 {@code leaseTime}，由 Redisson 看门狗自动续期。
 *
 * @author jiancai.zhong
 */
@Slf4j
@RequiredArgsConstructor
public class RedissonDistributedLockTemplate implements DistributedLockTemplate {

    /**
     * 锁等待超时时返回给调用方的业务错误码。
     */
    private static final int LOCK_TIMEOUT_CODE = 503;

    /**
     * 默认锁等待超时提示。
     */
    private static final String DEFAULT_TIMEOUT_MESSAGE = "操作繁忙，请稍后再试";

    private final RedissonClient redissonClient;

    /**
     * 在分布式锁内执行业务逻辑。
     *
     * @param lockKey   锁 key
     * @param waitTime  获取锁最长等待时间
     * @param action    业务回调
     * @param <T>       业务返回值类型
     * @return 业务回调返回值
     */
    @Override
    public <T> T execute(String lockKey, Duration waitTime, Supplier<T> action) {
        return execute(lockKey, waitTime, DEFAULT_TIMEOUT_MESSAGE, action);
    }

    /**
     * 在分布式锁内执行业务逻辑，并自定义锁等待超时提示。
     *
     * @param lockKey        锁 key
     * @param waitTime       获取锁最长等待时间
     * @param timeoutMessage 锁等待超时提示
     * @param action         业务回调
     * @param <T>            业务返回值类型
     * @return 业务回调返回值
     */
    @Override
    public <T> T execute(String lockKey, Duration waitTime, String timeoutMessage, Supplier<T> action) {
        Objects.requireNonNull(lockKey, "lockKey must not be null");
        Objects.requireNonNull(waitTime, "waitTime must not be null");
        Objects.requireNonNull(timeoutMessage, "timeoutMessage must not be null");
        Objects.requireNonNull(action, "action must not be null");
        if (waitTime.isZero() || waitTime.isNegative()) {
            throw new IllegalArgumentException("waitTime must be positive");
        }

        RLock lock = redissonClient.getLock(lockKey);
        boolean locked = false;
        long lockStartTime = System.nanoTime();
        try {
            locked = lock.tryLock(waitTime.toMillis(), TimeUnit.MILLISECONDS);
            if (!locked) {
                log.warn("获取分布式锁超时：lockKey={}, waitTime={}", lockKey, waitTime);
                throw new BusinessException(LOCK_TIMEOUT_CODE, timeoutMessage);
            }
            log.debug("获取分布式锁成功：lockKey={}, waitMs={}", lockKey, elapsedMs(lockStartTime));
            return action.get();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("获取分布式锁被中断：lockKey={}", lockKey, e);
            throw new BusinessException(ApiResponseEnum.SERVICE_UNAVAILABLE);
        } finally {
            if (locked) {
                unlockQuietly(lock, lockKey);
            }
        }
    }

    /**
     * 释放当前线程持有的分布式锁，避免解锁异常掩盖业务结果。
     *
     * @param lock    Redisson 锁对象
     * @param lockKey 锁 key
     */
    private void unlockQuietly(RLock lock, String lockKey) {
        try {
            lock.unlock();
        } catch (IllegalMonitorStateException e) {
            log.warn("分布式锁在解锁前已丢失：lockKey={}", lockKey);
        } catch (RuntimeException e) {
            log.error("释放分布式锁失败：lockKey={}", lockKey, e);
        }
    }

    /**
     * 计算锁等待耗时。
     *
     * @param startTime 开始时间纳秒
     * @return 耗时毫秒
     */
    private static long elapsedMs(long startTime) {
        return Math.round((System.nanoTime() - startTime) / 1_000_000.0);
    }
}
