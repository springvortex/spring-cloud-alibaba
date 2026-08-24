package com.zjc.common.lock;

import java.time.Duration;
import java.util.function.Supplier;

/**
 * ZooKeeper 分布式锁占位实现。
 *
 * <p>当前只保留工厂接入能力，尚未引入 Curator 或实现 ZNode 临时顺序节点锁。
 *
 * @author jiancai.zhong
 */
public class ZookeeperDistributedLockTemplate implements DistributedLockTemplate {

    /**
     * 在分布式锁内执行业务逻辑。
     *
     * @param lockKey  锁 key
     * @param waitTime 获取锁最长等待时间
     * @param action   业务回调
     * @param <T>      业务返回值类型
     * @return 业务回调返回值
     */
    @Override
    public <T> T execute(String lockKey, Duration waitTime, Supplier<T> action) {
        return execute(lockKey, waitTime, "操作繁忙，请稍后再试", action);
    }

    /**
     * 当前 Provider 尚未实现，调用时显式失败。
     *
     * @param lockKey        锁 key
     * @param waitTime       获取锁最长等待时间
     * @param timeoutMessage 锁等待超时提示
     * @param action         业务回调
     * @param <T>            业务返回值类型
     * @return 不会正常返回
     */
    @Override
    public <T> T execute(String lockKey, Duration waitTime, String timeoutMessage, Supplier<T> action) {
        throw new UnsupportedOperationException("ZooKeeper distributed lock is not implemented yet");
    }
}
