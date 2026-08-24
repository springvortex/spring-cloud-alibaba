package com.zjc.common.lock;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@link DistributedLockProperties} 配置属性测试。
 *
 * @author jiancai.zhong
 */
@DisplayName("分布式锁配置属性")
class DistributedLockPropertiesTest {

    /**
     * 验证默认使用 Redis 和 30 秒 MySQL 租约。
     */
    @Test
    @DisplayName("默认使用 Redis 并保留 MySQL 租约配置")
    void testDefaultProperties() {
        DistributedLockProperties properties = new DistributedLockProperties();

        assertThat(properties.getProvider()).isEqualTo(DistributedLockProvider.REDIS);
        assertThat(properties.getMysql().getTableName()).isEqualTo("t_distributed_lock");
        assertThat(properties.getMysql().getLeaseTime()).isEqualTo(Duration.ofSeconds(30));
        assertThat(properties.getMysql().getRetryInterval()).isEqualTo(Duration.ofMillis(100));
    }
}
