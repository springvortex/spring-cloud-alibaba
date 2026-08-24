package com.zjc.common.lock;

import java.time.Duration;
import java.util.function.Supplier;

/**
 * 分布式锁统一模板。
 *
 * <p>业务代码只依赖本接口，由工厂根据配置选择 Redis、MySQL 或其他存储实现。
 *
 * @author jiancai.zhong
 */
public interface DistributedLockTemplate {

    /**
     * 在分布式锁内执行业务逻辑。
     *
     * @param lockKey  锁 key，建议使用业务域前缀，例如 {@code zjc:goods:lock:1}
     * @param waitTime 获取锁最长等待时间，必须大于 0
     * @param action   业务回调
     * @param <T>      业务返回值类型
     * @return 业务回调返回值
     */
    <T> T execute(String lockKey, Duration waitTime, Supplier<T> action);

    /**
     * 在分布式锁内执行业务逻辑，并自定义锁等待超时提示。
     *
     * @param lockKey        锁 key，建议使用业务域前缀，例如 {@code zjc:goods:lock:1}
     * @param waitTime       获取锁最长等待时间，必须大于 0
     * @param timeoutMessage 锁等待超时提示
     * @param action         业务回调
     * @param <T>            业务返回值类型
     * @return 业务回调返回值
     */
    <T> T execute(String lockKey, Duration waitTime, String timeoutMessage, Supplier<T> action);
}
