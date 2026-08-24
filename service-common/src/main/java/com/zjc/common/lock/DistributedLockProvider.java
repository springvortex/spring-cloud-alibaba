package com.zjc.common.lock;

/**
 * 分布式锁实现类型。
 *
 * @author jiancai.zhong
 */
public enum DistributedLockProvider {

    /**
     * Redisson 实现，支持可重入锁和看门狗自动续期。
     */
    REDIS,

    /**
     * MySQL 租约表实现，支持可重入锁和后台续期。
     */
    MYSQL,

    /**
     * ZooKeeper 实现，当前仅保留工厂占位。
     */
    ZOOKEEPER
}
