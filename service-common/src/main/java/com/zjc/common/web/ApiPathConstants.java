package com.zjc.common.web;

/**
 * 统一 API 路径规则中复用的固定符号。
 *
 * @author jiancai.zhong
 */
final class ApiPathConstants {

    /**
     * URL 路径分隔符。
     */
    static final String PATH_SEPARATOR = "/";

    /**
     * 复合命名（服务名、OpenAPI 分组名）的分隔符。
     */
    static final String NAME_SEPARATOR = "-";

    /**
     * Ant 风格的全路径通配符。
     */
    static final String PATH_WILDCARD = "/**";

    /**
     * 全局 API 根路径的默认值。
     */
    static final String DEFAULT_PREFIX = "/api";

    private ApiPathConstants() {
    }
}
