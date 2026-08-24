package com.zjc.common.lock;

import java.util.EnumMap;
import java.util.Map;
import java.util.Optional;

/**
 * 分布式锁工厂。
 *
 * <p>按配置的 Provider 返回对应模板实现，也支持业务在特殊场景显式选择其他已装配实现。
 *
 * @author jiancai.zhong
 */
public class DistributedLockFactory {

    private final DistributedLockProvider provider;

    private final Map<DistributedLockProvider, DistributedLockTemplate> templates;

    /**
     * 创建分布式锁工厂。
     *
     * @param provider  默认锁实现类型
     * @param templates 当前应用可用的锁实现映射
     */
    public DistributedLockFactory(DistributedLockProvider provider,
                                  Map<DistributedLockProvider, DistributedLockTemplate> templates) {
        this.provider = provider;
        this.templates = new EnumMap<>(DistributedLockProvider.class);
        this.templates.putAll(templates);
    }

    /**
     * 获取配置指定的默认锁实现。
     *
     * @return 分布式锁模板
     */
    public DistributedLockTemplate getTemplate() {
        return getTemplate(provider);
    }

    /**
     * 获取指定类型的锁实现。
     *
     * @param lockProvider 锁实现类型
     * @return 分布式锁模板
     * @throws IllegalStateException 指定实现未装配或未启用
     */
    public DistributedLockTemplate getTemplate(DistributedLockProvider lockProvider) {
        DistributedLockTemplate template = templates.get(lockProvider);
        if (template == null) {
            throw new IllegalStateException("Distributed lock provider is unavailable: " + lockProvider);
        }
        return template;
    }

    /**
     * 查询指定类型的锁实现是否可用。
     *
     * @param lockProvider 锁实现类型
     * @return 实现存在时返回模板
     */
    public Optional<DistributedLockTemplate> findTemplate(DistributedLockProvider lockProvider) {
        return Optional.ofNullable(templates.get(lockProvider));
    }
}
