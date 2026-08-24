package com.zjc.common.lock;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * {@link ZookeeperDistributedLockTemplate} 占位实现测试。
 *
 * @author jiancai.zhong
 */
@DisplayName("ZooKeeper 分布式锁占位实现")
class ZookeeperDistributedLockTemplateTest {

    /**
     * 验证占位实现被调用时显式失败。
     */
    @Test
    @DisplayName("execute: 显式提示尚未实现")
    void throwsNotImplemented() {
        ZookeeperDistributedLockTemplate template = new ZookeeperDistributedLockTemplate();

        assertThatThrownBy(() -> template.execute("zjc:test:lock:1", Duration.ofSeconds(1), () -> "ok"))
                .isInstanceOf(UnsupportedOperationException.class)
                .hasMessageContaining("not implemented");
    }
}
