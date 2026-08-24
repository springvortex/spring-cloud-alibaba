package com.zjc.common.lock;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.redisson.api.RedissonClient;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * {@link DistributedLockAutoConfiguration} 自动装配测试。
 *
 * @author jiancai.zhong
 */
@DisplayName("分布式锁工厂自动装配")
class DistributedLockAutoConfigurationTest {

    /**
     * Spring 上下文测试器。
     */
    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(DistributedLockAutoConfiguration.class));

    /**
     * 验证注册 RedissonClient 后 Redis 和 ZooKeeper 占位实现可用，工厂默认选择 Redis。
     */
    @Test
    @DisplayName("注册 RedissonClient 后工厂选择 Redis 实现")
    void createsRedisTemplateWhenRedissonClientExists() {
        contextRunner
                .withUserConfiguration(RedissonClientConfiguration.class)
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context).hasSingleBean(RedissonDistributedLockTemplate.class);
                    assertThat(context).hasSingleBean(DistributedLockFactory.class);
                    assertThat(context).hasSingleBean(ZookeeperDistributedLockTemplate.class);
                    assertThat(context.getBean(DistributedLockFactory.class).getTemplate())
                            .isInstanceOf(RedissonDistributedLockTemplate.class);
                });
    }

    /**
     * 验证注册 JDBC 和事务管理器后 MySQL 实现可用。
     */
    @Test
    @DisplayName("注册 JDBC 与事务管理器后工厂选择 MySQL 实现")
    void createsMysqlTemplateWhenJdbcInfrastructureExists() {
        contextRunner
                .withUserConfiguration(MysqlInfrastructureConfiguration.class)
                .withPropertyValues("zjc.distributed-lock.provider=mysql")
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context).hasSingleBean(MysqlDistributedLockTemplate.class);
                    assertThat(context).hasSingleBean(DistributedLockFactory.class);
                    assertThat(context.getBean(DistributedLockFactory.class).getTemplate())
                            .isInstanceOf(MysqlDistributedLockTemplate.class);
                });
    }

    /**
     * 验证 ZooKeeper 当前是可选择的占位实现。
     */
    @Test
    @DisplayName("ZooKeeper 实现可被选中但调用时未实现")
    void selectsZookeeperPlaceholder() {
        contextRunner
                .withPropertyValues("zjc.distributed-lock.provider=zookeeper")
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context).hasSingleBean(ZookeeperDistributedLockTemplate.class);
                    assertThat(context.getBean(DistributedLockFactory.class).getTemplate())
                            .isInstanceOf(ZookeeperDistributedLockTemplate.class);
                });
    }

    /**
     * 验证未注册对应基础设施时，工厂不会提供该实现。
     */
    @Test
    @DisplayName("未注册 RedissonClient 时 Redis 实现不可用")
    void doesNotCreateRedisTemplateWithoutRedissonClient() {
        contextRunner.run(context -> {
            assertThat(context).hasNotFailed();
            assertThat(context).doesNotHaveBean(RedissonDistributedLockTemplate.class);
            assertThat(context).getBean(DistributedLockFactory.class)
                    .satisfies(factory -> assertThat(factory.findTemplate(DistributedLockProvider.REDIS))
                            .isEmpty());
        });
    }

    /**
     * 提供 Redisson 客户端的测试配置。
     */
    @Configuration
    static class RedissonClientConfiguration {

        /**
         * Mock Redisson 客户端。
         *
         * @return Redisson 客户端
         */
        @Bean
        RedissonClient redissonClient() {
            return mock(RedissonClient.class);
        }
    }

    /**
     * 提供 MySQL 基础设施的测试配置。
     */
    @Configuration
    static class MysqlInfrastructureConfiguration {

        /**
         * Mock JDBC 操作对象。
         *
         * @return JDBC 操作对象
         */
        @Bean
        JdbcTemplate jdbcTemplate() {
            return mock(JdbcTemplate.class);
        }

        /**
         * Mock 事务管理器。
         *
         * @return 事务管理器
         */
        @Bean
        PlatformTransactionManager transactionManager() {
            return mock(PlatformTransactionManager.class);
        }
    }
}
