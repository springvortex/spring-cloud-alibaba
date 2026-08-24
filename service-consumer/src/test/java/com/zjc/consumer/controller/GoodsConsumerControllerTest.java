package com.zjc.consumer.controller;

import com.zjc.common.api.goods.GoodsFeignApi;
import com.zjc.common.dto.GoodsPurchaseRequestDTO;
import com.zjc.common.dto.GoodsPurchaseResponseDTO;
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
 * {@link GoodsConsumerController} 单元测试。
 *
 * @author jiancai.zhong
 */
@DisplayName("商品消费者 Controller")
@ExtendWith(MockitoExtension.class)
class GoodsConsumerControllerTest {

    /**
     * 共享 Feign API 的 Mock 实例。
     */
    @Mock
    private GoodsFeignApi goodsFeignApi;

    @InjectMocks
    private GoodsConsumerController controller;

    /**
     * 验证购买请求委托给 Feign API，由 LoadBalancer 选择 provider 实例。
     */
    @Test
    @DisplayName("purchase: 委托给 Feign API")
    void testPurchaseDelegates() {
        GoodsPurchaseRequestDTO request = new GoodsPurchaseRequestDTO();
        request.setUserId(1L);
        request.setQuantity(2);
        GoodsPurchaseResponseDTO result = new GoodsPurchaseResponseDTO();
        result.setOrderId(100L);
        when(goodsFeignApi.purchase(1L, request)).thenReturn(ApiResponse.success(result));

        ApiResponse<GoodsPurchaseResponseDTO> response = controller.purchase(1L, request);

        assertThat(response.isSuccess()).isTrue();
        assertThat(response.getData()).isSameAs(result);
        verify(goodsFeignApi).purchase(1L, request);
    }
}
