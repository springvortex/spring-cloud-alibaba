package com.zjc.provider.controller;

import com.zjc.common.web.ApiResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@link TestController} 单元测试。
 *
 * <p>验证端口返回逻辑。{@code @Value} 字段通过 {@link ReflectionTestUtils}
 * 反射注入，不依赖 Spring 上下文。
 *
 * @author jiancai.zhong
 */
@DisplayName("连通性测试 Controller")
class TestControllerTest {

    private TestController testController;

    @BeforeEach
    void setUp() {
        testController = new TestController();
        // 模拟 @Value("${server.port}") 注入
        ReflectionTestUtils.setField(testController, "port", "9001");
    }

    /**
     * 验证 getServerPort 返回反射注入的端口号字符串。
     */
    @Test
    @DisplayName("getServerPort: 返回注入的端口号")
    void testGetServerPortReturnsPort() {
        ApiResponse<String> resp = testController.getServerPort();

        assertThat(resp.isSuccess()).isTrue();
        assertThat(resp.getData()).isEqualTo("9001");
    }
}
