package com.zjc.common.web.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 声明 Controller 提供的 API 版本。
 *
 * <p>未标注该注解的 Controller 会使用配置的默认版本；标注非默认版本的
 * Controller 保持资源路径不变，改用对应版本的全局前缀。
 *
 * @author jiancai.zhong
 */
@Documented
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface ApiVersion {

    /**
     * 已配置的版本号，例如 {@code v2}。标注在 Controller 上时选择该
     * Controller 的版本；标注在 Feign 方法上时选择目标版本，并覆盖默认版本。
     */
    String value();
}
