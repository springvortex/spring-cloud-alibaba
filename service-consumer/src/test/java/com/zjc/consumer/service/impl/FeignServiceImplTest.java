package com.zjc.consumer.service.impl;

import com.zjc.common.api.test.TestApi;
import com.zjc.common.web.ApiResponse;
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
 * {@link FeignServiceImpl} 单元测试。
 *
 * <p>验证 Service 层正确委托给 common 模块的 {@link TestApi} Feign 代理。
 *
 * @author jiancai.zhong
 */
@DisplayName("Feign 服务实现")
@ExtendWith(MockitoExtension.class)
class FeignServiceImplTest {

    @Mock
    private TestApi testApi;

    @InjectMocks
    private FeignServiceImpl feignService;

    /**
     * 验证 getServerPort 将调用转发给 TestApi 并原样返回结果。
     */
    @Test
    @DisplayName("getServerPort: 委托给 TestApi 并返回结果")
    void testGetServerPortDelegatesToTestApi() {
        when(testApi.getServerPort()).thenReturn(ApiResponse.success("9001"));

        ApiResponse<String> resp = feignService.getServerPort();

        assertThat(resp.isSuccess()).isTrue();
        assertThat(resp.getData()).isEqualTo("9001");
        verify(testApi).getServerPort();
    }
}
