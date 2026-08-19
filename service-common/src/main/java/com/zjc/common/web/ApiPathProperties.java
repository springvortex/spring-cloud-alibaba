package com.zjc.common.web;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;

/**
 * 统一 REST API 路径规范的配置项。
 *
 * @author jiancai.zhong
 */
@Setter
@Getter
@ConfigurationProperties(prefix = "zjc.api")
public class ApiPathProperties {

    /**
     * 全局 API 根路径，通常为 {@code /api}。
     */
    private String prefix = "/api";

    /**
     * 当前服务暴露的 API 版本列表。未配置 {@link #defaultVersion} 时，
     * 第一个版本作为默认版本。
     */
    private List<String> versions = new ArrayList<>();

    /**
     * 未标注 {@code @ApiVersion} 的 Controller 使用的默认版本。
     */
    private String defaultVersion;

    /**
     * 可选的应用包范围，用于替代 Spring Boot 自动推断的配置包。
     */
    private List<String> basePackages = new ArrayList<>();

    /**
     * 返回按配置顺序排列的版本列表，并过滤空值、去除重复项。
     *
     * @return 归一化后的版本列表
     */
    public List<String> normalizedVersions() {
        LinkedHashSet<String> result = new LinkedHashSet<>();
        for (String version : versions) {
            if (StringUtils.hasText(version)) {
                result.add(version.trim());
            }
        }
        return List.copyOf(result);
    }

    /**
     * 返回实际生效的默认版本。
     *
     * @return 显式配置的默认版本；未配置时返回第一个版本
     */
    public String effectiveDefaultVersion() {
        List<String> normalized = normalizedVersions();
        if (!StringUtils.hasText(defaultVersion)) {
            return normalized.isEmpty() ? null : normalized.getFirst();
        }
        return defaultVersion.trim();
    }

    /**
     * 校验至少配置一个版本，且默认版本必须在版本列表中。
     */
    public void validate() {
        List<String> normalized = normalizedVersions();
        if (normalized.isEmpty()) {
            throw new IllegalStateException("zjc.api.versions must contain at least one version");
        }
        String effectiveDefault = effectiveDefaultVersion();
        if (!normalized.contains(effectiveDefault)) {
            throw new IllegalStateException("zjc.api.default-version must be one of zjc.api.versions");
        }
    }
}
