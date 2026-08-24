package com.zjc.common.api.goods;

import com.zjc.common.api.goods.factory.GoodsFeignFallbackFactory;
import com.zjc.common.dto.GoodsPurchaseRequestDTO;
import com.zjc.common.dto.GoodsPurchaseResponseDTO;
import com.zjc.common.web.ApiResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * 商品服务共享 Feign 客户端，远程调用 service-provider 的商品购买接口。
 *
 * <p>Feign 请求拦截器会根据目标服务名自动追加 {@code /api/v1/provider} 前缀；
 * LoadBalancer 会从 Nacos 的 service-provider 实例列表中选择一个节点。
 *
 * @author jiancai.zhong
 */
@FeignClient(
        name = "service-provider",
        contextId = "goodsFeignApi",
        fallbackFactory = GoodsFeignFallbackFactory.class
)
public interface GoodsFeignApi {

    /**
     * 购买指定商品。
     *
     * @param goodsId 商品主键
     * @param request 购买请求
     * @return 订单与剩余库存信息；远程调用失败时返回业务繁忙失败响应
     */
    @PostMapping("/goods/{id}/purchase")
    ApiResponse<GoodsPurchaseResponseDTO> purchase(
            @PathVariable("id") Long goodsId,
            @RequestBody GoodsPurchaseRequestDTO request);
}
