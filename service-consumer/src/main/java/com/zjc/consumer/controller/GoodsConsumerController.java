package com.zjc.consumer.controller;

import com.zjc.common.api.goods.GoodsFeignApi;
import com.zjc.common.dto.GoodsPurchaseRequestDTO;
import com.zjc.common.dto.GoodsPurchaseResponseDTO;
import com.zjc.common.web.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * Consumer 侧商品购买入口。
 *
 * <p>调用链路为：客户端 → consumer → GoodsFeignApi → LoadBalancer
 * → service-provider 集群中的任意实例。provider 多实例部署时，
 * 分布式锁在 Redis 层全局生效，保证同一商品扣库存互斥。
 *
 * @author jiancai.zhong
 */
@Tag(name = "商品消费者", description = "consumer 通过 Feign 调用 provider 的商品购买接口")
@RestController
public class GoodsConsumerController {

    /**
     * 商品服务共享 Feign 客户端，由 common 模块提供。
     */
    @Resource
    private GoodsFeignApi goodsFeignApi;

    /**
     * 购买指定商品，底层通过 Feign 和 LoadBalancer 调用 provider 集群。
     *
     * @param id      商品主键
     * @param request 购买请求
     * @return 订单与剩余库存信息，远程调用失败时返回业务繁忙失败响应
     */
    @Operation(summary = "远程购买商品（Feign + LoadBalancer 演示）")
    @PostMapping("/goods/{id}/purchase")
    public ApiResponse<GoodsPurchaseResponseDTO> purchase(
            @Parameter(description = "商品主键") @PathVariable("id") Long id,
            @Valid @RequestBody GoodsPurchaseRequestDTO request) {
        return goodsFeignApi.purchase(id, request);
    }
}
