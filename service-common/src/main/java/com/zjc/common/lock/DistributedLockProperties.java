package com.zjc.common.lock;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/**
 * 分布式锁公共配置。
 *
 * @author jiancai.zhong
 */
@Getter
@Setter
@ConfigurationProperties(prefix = "zjc.distributed-lock")
public class DistributedLockProperties {

    /**
     * 当前使用的锁实现。
     */
    private DistributedLockProvider provider = DistributedLockProvider.REDIS;

    /**
     * MySQL 实现的专属配置。
     */
    private final Mysql mysql = new Mysql();

    /**
     * MySQL 分布式锁配置。
     *
     * @author jiancai.zhong
 */
    @Getter
    @Setter
    public static class Mysql {

        /**
         * 锁租约表名，只允许字母、数字、下划线和点号。
         */
        private String tableName = "t_distributed_lock";

        /**
         * 锁租约时长，续期周期为其三分之一。
         */
        private Duration leaseTime = Duration.ofSeconds(30);

        /**
         * 未获取锁时的重试间隔。
         */
        private Duration retryInterval = Duration.ofMillis(100);
    }
}
