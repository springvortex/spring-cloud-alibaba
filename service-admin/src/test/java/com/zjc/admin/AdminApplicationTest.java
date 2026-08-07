package com.zjc.admin;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@link AdminApplication} 基础测试。
 *
 * <p>Admin 模块无业务逻辑，验证启动类结构完整性。
 *
 * @author jiancai.zhong
 */
@DisplayName("Admin 启动类")
class AdminApplicationTest {

    /**
     * 验证 main 方法存在且签名正确，确保启动入口未被误删。
     */
    @Test
    @DisplayName("main 方法存在")
    void testMainMethodExists() throws NoSuchMethodException {
        assertThat(AdminApplication.class.getMethod("main", String[].class))
                .isNotNull();
    }
}
