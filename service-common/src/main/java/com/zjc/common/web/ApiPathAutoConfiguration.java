package com.zjc.common.web;

import feign.RequestInterceptor;
import feign.Target;
import org.jspecify.annotations.NonNull;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigurationPackages;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.Environment;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.config.annotation.PathMatchConfigurer;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.List;

/**
 * REST API 路径和 OpenAPI 分组规范的自动配置。
 *
 * @author jiancai.zhong
 */
@AutoConfiguration
@EnableConfigurationProperties(ApiPathProperties.class)
public class ApiPathAutoConfiguration {

    /**
     * 为每个已配置的 API 版本追加一个全局 MVC 路径前缀。
     *
     * @param properties  API 路径配置
     * @param environment 应用环境
     * @param beanFactory Bean 工厂，用于发现应用包
     * @return Spring MVC 配置器
     */
    @Bean
    public WebMvcConfigurer apiPathWebMvcConfigurer(ApiPathProperties properties,
                                                    Environment environment,
                                                    BeanFactory beanFactory) {
        ApiPathResolver resolver = resolver(properties, environment, beanFactory);
        return new WebMvcConfigurer() {
            @Override
            public void configurePathMatch(@NonNull PathMatchConfigurer configurer) {
                for (String version : resolver.getVersions()) {
                    String pathPrefix = resolver.prefix(version);
                    configurer.addPathPrefix(pathPrefix, controllerType ->
                            resolver.appliesTo(controllerType) && version.equals(resolver.version(controllerType)));
                }
            }
        };
    }

    /**
     * 为每个已配置的 API 版本创建一个 Springdoc 分组。
     *
     * @param properties  API 路径配置
     * @param environment 应用环境
     * @param beanFactory Bean 工厂，用于发现应用包
     * @return 按版本生成的 OpenAPI 分组列表
     */
    @Bean
    public List<GroupedOpenApi> apiVersionGroupedOpenApis(ApiPathProperties properties,
                                                          Environment environment,
                                                          BeanFactory beanFactory) {
        ApiPathResolver resolver = resolver(properties, environment, beanFactory);
        return resolver.getVersions().stream()
                .map(version -> GroupedOpenApi.builder()
                        .group(version + ApiPathConstants.NAME_SEPARATOR + resolver.getModuleName())
                        .pathsToMatch(resolver.prefix(version) + ApiPathConstants.PATH_WILDCARD)
                        .build())
                .toList();
    }

    /**
     * 为目标为内部 {@code service-*} 服务的 Feign 调用追加标准前缀。
     *
     * @param properties API 路径配置
     * @return Feign 请求拦截器
     */
    @Bean
    public RequestInterceptor standardApiPathFeignInterceptor(ApiPathProperties properties) {
        return request -> {
            Target<?> target = request.feignTarget();
            if (target == null || !StringUtils.hasText(target.name())
                    || !target.name().contains(ApiPathConstants.NAME_SEPARATOR)) {
                return;
            }
            String resourcePath = request.path();
            if (!StringUtils.hasText(resourcePath)
                    || !resourcePath.startsWith(ApiPathConstants.PATH_SEPARATOR)) {
                return;
            }
            ApiPathResolver targetResolver = ApiPathResolver.of(target.name(), properties);
            String version = targetResolver.version(request.methodMetadata().method());
            String apiPrefix = targetResolver.prefix(version);
            if (!resourcePath.startsWith(apiPrefix + ApiPathConstants.PATH_SEPARATOR)
                    && !resourcePath.equals(apiPrefix)) {
                request.uri(apiPrefix + resourcePath);
            }
        };
    }

    private ApiPathResolver resolver(ApiPathProperties properties, Environment environment, BeanFactory beanFactory) {
        if (properties.getBasePackages().isEmpty() && AutoConfigurationPackages.has(beanFactory)) {
            properties.setBasePackages(AutoConfigurationPackages.get(beanFactory));
        }
        return ApiPathResolver.of(environment.getProperty("spring.application.name"), properties);
    }
}
