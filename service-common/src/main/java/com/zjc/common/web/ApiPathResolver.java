package com.zjc.common.web;

import com.zjc.common.web.annotation.ApiVersion;
import lombok.Getter;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Locale;

/**
 * 解析服务和 Controller 使用的标准化 API 前缀。
 *
 * @author jiancai.zhong
 */
public final class ApiPathResolver {

    @Getter
    private final String moduleName;

    private final String prefix;

    @Getter
    private final List<String> versions;

    private final String defaultVersion;

    private final List<String> basePackages;

    private ApiPathResolver(String moduleName, String prefix, List<String> versions,
                            String defaultVersion, List<String> basePackages) {
        this.moduleName = moduleName;
        this.prefix = prefix;
        this.versions = versions;
        this.defaultVersion = defaultVersion;
        this.basePackages = basePackages;
    }

    /**
     * 根据服务名和路径配置创建解析器。
     *
     * @param serviceName Spring 应用名，例如 {@code service-provider}
     * @param properties  API 路径配置
     * @return 路径解析器实例
     */
    public static ApiPathResolver of(String serviceName, ApiPathProperties properties) {
        if (!StringUtils.hasText(serviceName)) {
            throw new IllegalStateException("spring.application.name is required by the API path convention");
        }
        properties.validate();
        return new ApiPathResolver(
                moduleName(serviceName),
                normalizePath(properties.getPrefix()),
                properties.normalizedVersions(),
                properties.effectiveDefaultVersion(),
                properties.getBasePackages()
        );
    }

    /**
     * 截取服务名最后一个短横线之后的模块名。
     *
     * @param serviceName 服务名
     * @return 模块名
     */
    public static String moduleName(String serviceName) {
        int separator = serviceName.lastIndexOf('-');
        return separator < 0 ? serviceName : serviceName.substring(separator + 1);
    }

    /**
     * 构建指定版本的标准前缀。
     *
     * @param version 已配置的版本号
     * @return 前缀，例如 {@code /api/v1/provider}
     */
    public String prefix(String version) {
        if (!versions.contains(version)) {
            throw new IllegalArgumentException("Unknown API version: " + version);
        }
        return prefix + "/" + version + "/" + moduleName;
    }

    /**
     * 返回 Controller 生效的 API 版本。
     *
     * @param controllerType Controller 类型
     * @return 已配置的版本号
     */
    public String version(Class<?> controllerType) {
        ApiVersion annotation = AnnotatedElementUtils.findMergedAnnotation(controllerType, ApiVersion.class);
        String version = annotation == null ? defaultVersion : annotation.value();
        return requireConfigured(version, controllerType.getName());
    }

    /**
     * 返回 Feign API 方法使用的目标版本。
     *
     * @param method Feign 接口方法
     * @return 已配置的版本号
     */
    public String version(java.lang.reflect.Method method) {
        ApiVersion annotation = AnnotatedElementUtils.findMergedAnnotation(method, ApiVersion.class);
        String version = annotation == null ? defaultVersion : annotation.value();
        return requireConfigured(version, method.toString());
    }

    private String requireConfigured(String version, String source) {
        if (!versions.contains(version)) {
            throw new IllegalStateException(
                    "%s uses unconfigured API version %s".formatted(source, version));
        }
        return version;
    }

    /**
     * 判断全局前缀是否应用于指定处理器类型。
     *
     * @param controllerType 处理器类型
     * @return Controller 位于已配置应用包内时返回 {@code true}
     */
    public boolean appliesTo(Class<?> controllerType) {
        if (basePackages == null || basePackages.isEmpty()) {
            return true;
        }
        String packageName = controllerType.getPackageName();
        return basePackages.stream().anyMatch(packageName::startsWith);
    }

    private static String normalizePath(String value) {
        String normalized = StringUtils.hasText(value) ? value.trim() : "/api";
        if (!normalized.startsWith("/")) {
            normalized = "/" + normalized;
        }
        while (normalized.length() > 1 && normalized.endsWith("/")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        return normalized.toLowerCase(Locale.ROOT);
    }
}
