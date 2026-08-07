package com.zjc.provider.config;

import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@link MybatisPlusConfig} 单元测试。
 *
 * <p>验证分页拦截器被正确注册到 MybatisPlusInterceptor 中。
 *
 * @author jiancai.zhong
 */
@DisplayName("MyBatis-Plus 配置")
class MybatisPlusConfigTest {

    private final MybatisPlusConfig config = new MybatisPlusConfig();

    /**
     * 验证拦截器链中包含 PaginationInnerInterceptor 实例，
     * 确保分页查询功能可用。
     */
    @Test
    @DisplayName("mybatisPlusInterceptor: 包含分页拦截器")
    void testMybatisPlusInterceptorContainsPagination() {
        MybatisPlusInterceptor interceptor = config.mybatisPlusInterceptor();

        assertThat(interceptor).isNotNull();
        assertThat(interceptor.getInterceptors())
                .anyMatch(inner -> inner instanceof PaginationInnerInterceptor);
    }
}
