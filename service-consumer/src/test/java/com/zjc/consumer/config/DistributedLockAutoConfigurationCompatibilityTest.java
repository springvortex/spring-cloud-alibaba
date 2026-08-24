package com.zjc.consumer.config;

import com.zjc.common.lock.DistributedLockAutoConfiguration;
import com.zjc.common.lock.DistributedLockFactory;
import com.zjc.common.lock.DistributedLockProvider;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 分布式锁自动装配在无 Redis/JDBC 模块中的兼容性测试。
 *
 * <p>consumer 没有引入 Redisson、JDBC 和 Transaction 运行时，自动装配不能因为 optional 实现类缺失而启动失败。
 *
 * @author jiancai.zhong
 */
@DisplayName("无锁基础设施模块的分布式锁装配")
class DistributedLockAutoConfigurationCompatibilityTest {

    /**
     * Spring 上下文测试器。
     */
    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(DistributedLockAutoConfiguration.class));

    /**
     * 验证未引入 Redis/JDBC 基础设施时，只保留 ZooKeeper 占位实现且上下文可启动。
     */
    @Test
    @DisplayName("缺少 Redis 与 JDBC 依赖时上下文正常启动")
    void startsWithoutOptionalLockInfrastructure() {
        contextRunner.run(context -> {
            assertThat(context).hasNotFailed();
            assertThat(context).hasSingleBean(DistributedLockFactory.class);
            assertThat(context.getBean(DistributedLockFactory.class)
                    .findTemplate(DistributedLockProvider.REDIS)).isEmpty();
            assertThat(context.getBean(DistributedLockFactory.class)
                    .findTemplate(DistributedLockProvider.MYSQL)).isEmpty();
            assertThat(context.getBean(DistributedLockFactory.class)
                    .findTemplate(DistributedLockProvider.ZOOKEEPER)).isPresent();
        });
    }
}
