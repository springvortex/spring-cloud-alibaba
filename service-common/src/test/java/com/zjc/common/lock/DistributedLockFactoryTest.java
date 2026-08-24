package com.zjc.common.lock;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * {@link DistributedLockFactory} 单元测试。
 *
 * @author jiancai.zhong
 */
@DisplayName("分布式锁工厂")
class DistributedLockFactoryTest {

    /**
     * 验证工厂按默认配置返回实现。
     */
    @Test
    @DisplayName("getTemplate: 返回默认实现")
    void returnsDefaultTemplate() {
        ZookeeperDistributedLockTemplate template = new ZookeeperDistributedLockTemplate();
        DistributedLockFactory factory = new DistributedLockFactory(
                DistributedLockProvider.ZOOKEEPER,
                Map.of(DistributedLockProvider.ZOOKEEPER, template));

        assertThat(factory.getTemplate()).isSameAs(template);
    }

    /**
     * 验证指定未装配实现时给出明确错误。
     */
    @Test
    @DisplayName("getTemplate: 指定实现不可用时失败")
    void throwsWhenProviderUnavailable() {
        DistributedLockFactory factory = new DistributedLockFactory(
                DistributedLockProvider.REDIS, Map.of());

        assertThatThrownBy(() -> factory.getTemplate())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("REDIS");
    }
}
