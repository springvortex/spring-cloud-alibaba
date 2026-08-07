package com.zjc.consumer.controller;

import com.zjc.common.web.ApiResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@link TestConfigController} 单元测试。
 *
 * <p>验证 Nacos 配置拼接逻辑。{@code @Value} 字段通过反射注入，
 * 不依赖 Spring 上下文和 Nacos 服务。
 *
 * @author jiancai.zhong
 */
@DisplayName("配置刷新测试 Controller")
class TestConfigControllerTest {

    private TestConfigController controller;

    @BeforeEach
    void setUp() {
        controller = new TestConfigController();
        ReflectionTestUtils.setField(controller, "msg", "hello");
        ReflectionTestUtils.setField(controller, "pub", "world");
    }

    /**
     * 验证 getMsg 将两个配置项以冒号拼接返回。
     */
    @Test
    @DisplayName("getMsg: 拼接 msg 和 pub")
    void testGetMsgConcatenates() {
        ApiResponse<String> resp = controller.getMsg();

        assertThat(resp.isSuccess()).isTrue();
        assertThat(resp.getData()).isEqualTo("hello:world");
    }
}
