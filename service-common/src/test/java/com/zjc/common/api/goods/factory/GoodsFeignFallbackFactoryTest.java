package com.zjc.common.api.goods.factory;

import com.zjc.common.api.goods.GoodsFeignApi;
import com.zjc.common.constant.ApiResponseEnum;
import com.zjc.common.dto.GoodsPurchaseRequestDTO;
import com.zjc.common.dto.GoodsPurchaseResponseDTO;
import com.zjc.common.web.ApiResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@link GoodsFeignFallbackFactory} 单元测试。
 *
 * @author jiancai.zhong
 */
@DisplayName("商品 Feign 降级工厂")
class GoodsFeignFallbackFactoryTest {

    /**
     * 被测的降级工厂实例。
     */
    private final GoodsFeignFallbackFactory factory = new GoodsFeignFallbackFactory();

    /**
     * 验证购买接口降级时返回业务繁忙失败响应，且不生成本地兜底订单。
     */
    @Test
    @DisplayName("purchase 降级: 返回业务繁忙失败响应")
    void testFallbackPurchaseReturnsServiceUnavailable() {
        GoodsPurchaseRequestDTO request = new GoodsPurchaseRequestDTO();
        request.setUserId(1L);
        request.setQuantity(1);

        ApiResponse<GoodsPurchaseResponseDTO> response =
                factory.create(new RuntimeException("provider down")).purchase(1L, request);

        assertThat(response.isSuccess()).isFalse();
        assertThat(response.getCode()).isEqualTo(ApiResponseEnum.SERVICE_UNAVAILABLE.code());
        assertThat(response.getMessage()).isEqualTo("业务繁忙，请稍后再试");
        assertThat(response.getData()).isNull();
    }
}
