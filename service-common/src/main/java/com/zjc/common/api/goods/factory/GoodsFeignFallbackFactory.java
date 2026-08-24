package com.zjc.common.api.goods.factory;

import com.zjc.common.api.goods.GoodsFeignApi;
import com.zjc.common.constant.ApiResponseEnum;
import com.zjc.common.dto.GoodsPurchaseRequestDTO;
import com.zjc.common.dto.GoodsPurchaseResponseDTO;
import com.zjc.common.web.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

/**
 * {@link GoodsFeignApi} 的降级工厂。
 *
 * <p>购买属于写操作，远程服务不可用时不做本地兜底下单，只返回业务繁忙失败响应，
 * 避免制造半成功订单或丢失购买请求状态。
 *
 * @author jiancai.zhong
 */
@Slf4j
@Component
public class GoodsFeignFallbackFactory implements FallbackFactory<GoodsFeignApi> {

    /**
     * 降级时的对外提示，不暴露连接超时、下游地址等基础设施细节。
     */
    private static final String FALLBACK_MESSAGE = "业务繁忙，请稍后再试";

    /**
     * 创建降级代理对象，远程调用失败时由 Feign 自动回调。
     *
     * @param cause 远程调用失败原因
     * @return 降级代理，返回服务不可用的失败响应
     */
    @Override
    public GoodsFeignApi create(Throwable cause) {
        log.error("调用 service-provider 商品购买接口失败，触发降级", cause);

        return (goodsId, request) -> {
            log.warn("purchase 降级，goodsId={}, userId={}",
                    goodsId, request == null ? null : request.getUserId());
            return ApiResponse.failure(ApiResponseEnum.SERVICE_UNAVAILABLE.code(), FALLBACK_MESSAGE);
        };
    }
}
