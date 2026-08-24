package com.zjc.common.lock;

import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.EnumMap;
import java.util.Map;

/**
 * 分布式锁工厂自动装配。
 *
 * <p>按 classpath 和已有 Bean 装配可用实现，再通过 {@code zjc.distributed-lock.provider}
 * 选择默认实现。Redisson、JDBC 均为 optional 依赖，未使用的服务不会被迫引入。
 *
 * @author jiancai.zhong
 */
@AutoConfiguration
@AutoConfigureAfter(name = {
        "org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration",
        "org.springframework.boot.transaction.autoconfigure.TransactionAutoConfiguration"
})
@EnableConfigurationProperties(DistributedLockProperties.class)
public class DistributedLockAutoConfiguration {

    /**
     * Redisson 模板 Bean 名称。
     */
    private static final String REDIS_TEMPLATE_BEAN_NAME = "redissonDistributedLockTemplate";

    /**
     * MySQL 模板 Bean 名称。
     */
    private static final String MYSQL_TEMPLATE_BEAN_NAME = "mysqlDistributedLockTemplate";

    /**
     * ZooKeeper 占位模板 Bean 名称。
     */
    private static final String ZOOKEEPER_TEMPLATE_BEAN_NAME = "zookeeperDistributedLockTemplate";

    /**
     * 创建分布式锁工厂。
     *
     * <p>这里通过 Bean 名称和统一接口读取实现，避免公共自动配置直接引用 optional 依赖中的实现类。
     *
     * @param properties   分布式锁配置
     * @param beanFactory  当前 Bean 工厂
     * @return 分布式锁工厂
     */
    @Bean
    @ConditionalOnMissingBean(DistributedLockFactory.class)
    public DistributedLockFactory distributedLockFactory(
            DistributedLockProperties properties,
            ListableBeanFactory beanFactory) {
        Map<DistributedLockProvider, DistributedLockTemplate> templates =
                new EnumMap<>(DistributedLockProvider.class);
        registerTemplate(beanFactory, templates, DistributedLockProvider.REDIS, REDIS_TEMPLATE_BEAN_NAME);
        registerTemplate(beanFactory, templates, DistributedLockProvider.MYSQL, MYSQL_TEMPLATE_BEAN_NAME);
        registerTemplate(beanFactory, templates, DistributedLockProvider.ZOOKEEPER, ZOOKEEPER_TEMPLATE_BEAN_NAME);
        return new DistributedLockFactory(properties.getProvider(), templates);
    }

    /**
     * 注册当前上下文中已装配的锁实现。
     *
     * @param beanFactory  Bean 工厂
     * @param templates    实现收集容器
     * @param provider     实现类型
     * @param beanName     实现 Bean 名称
     */
    private static void registerTemplate(ListableBeanFactory beanFactory,
                                         Map<DistributedLockProvider, DistributedLockTemplate> templates,
                                         DistributedLockProvider provider,
                                         String beanName) {
        if (!beanFactory.containsBean(beanName)) {
            return;
        }
        templates.put(provider, beanFactory.getBean(beanName, DistributedLockTemplate.class));
    }

    /**
     * Redisson 实现装配。
     *
     * @author jiancai.zhong
     */
    @Configuration(proxyBeanMethods = false)
    @ConditionalOnClass(RedissonClient.class)
    @ConditionalOnBean(RedissonClient.class)
    static class RedisTemplateConfiguration {

        /**
         * 创建 Redisson 分布式锁模板。
         *
         * @param redissonClient Redisson 客户端
         * @return Redisson 锁模板
         */
        @Bean
        @ConditionalOnMissingBean(RedissonDistributedLockTemplate.class)
        public RedissonDistributedLockTemplate redissonDistributedLockTemplate(
                RedissonClient redissonClient) {
            return new RedissonDistributedLockTemplate(redissonClient);
        }
    }

    /**
     * MySQL 实现装配。
     *
     * @author jiancai.zhong
     */
    @Configuration(proxyBeanMethods = false)
    @ConditionalOnClass({JdbcTemplate.class, PlatformTransactionManager.class})
    @ConditionalOnBean({JdbcTemplate.class, PlatformTransactionManager.class})
    static class MysqlTemplateConfiguration {

        /**
         * 创建 MySQL 分布式锁模板。
         *
         * @param jdbcTemplate        JDBC 操作对象
         * @param transactionManager 事务管理器
         * @param properties         分布式锁配置
         * @return MySQL 锁模板
         */
        @Bean(destroyMethod = "close")
        @ConditionalOnMissingBean(MysqlDistributedLockTemplate.class)
        public MysqlDistributedLockTemplate mysqlDistributedLockTemplate(
                JdbcTemplate jdbcTemplate,
                PlatformTransactionManager transactionManager,
                DistributedLockProperties properties) {
            return new MysqlDistributedLockTemplate(
                    jdbcTemplate,
                    new TransactionTemplate(transactionManager),
                    properties.getMysql());
        }
    }

    /**
     * ZooKeeper 占位实现装配。
     *
     * @author jiancai.zhong
     */
    @Configuration(proxyBeanMethods = false)
    static class ZookeeperTemplateConfiguration {

        /**
         * 创建 ZooKeeper 占位锁模板。
         *
         * @return ZooKeeper 占位锁模板
         */
        @Bean
        @ConditionalOnMissingBean(ZookeeperDistributedLockTemplate.class)
        public ZookeeperDistributedLockTemplate zookeeperDistributedLockTemplate() {
            return new ZookeeperDistributedLockTemplate();
        }
    }
}
