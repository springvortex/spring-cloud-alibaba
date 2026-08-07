package com.zjc.gateway;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@link GatewayApplication} 基础测试。
 *
 * <p>网关模块无业务逻辑，验证启动类结构完整性。
 *
 * @author jiancai.zhong
 */
@DisplayName("网关启动类")
class GatewayApplicationTest {

    /**
     * 验证 main 方法存在且签名正确，确保启动入口未被误删。
     */
    @Test
    @DisplayName("main 方法存在")
    void testMainMethodExists() throws NoSuchMethodException {
        assertThat(GatewayApplication.class.getMethod("main", String[].class))
                .isNotNull();
    }
}
