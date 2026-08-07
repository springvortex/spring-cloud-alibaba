package com.zjc.consumer.controller;

import com.zjc.common.web.ApiResponse;
import com.zjc.consumer.service.FeignService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * {@link TessFeignController} 单元测试。
 *
 * <p>验证 Controller 正确委托给 Service 层。
 *
 * @author jiancai.zhong
 */
@DisplayName("Feign 测试 Controller")
@ExtendWith(MockitoExtension.class)
class TessFeignControllerTest {

    @Mock
    private FeignService feignService;

    @InjectMocks
    private TessFeignController controller;

    /**
     * 验证 getServerPort 将调用转发给 FeignService 并原样返回结果。
     */
    @Test
    @DisplayName("getServerPort: 委托给 FeignService")
    void testGetServerPortDelegates() {
        when(feignService.getServerPort()).thenReturn(ApiResponse.success("9001"));

        ApiResponse<String> resp = controller.getServerPort();

        assertThat(resp.isSuccess()).isTrue();
        assertThat(resp.getData()).isEqualTo("9001");
        verify(feignService).getServerPort();
    }
}
