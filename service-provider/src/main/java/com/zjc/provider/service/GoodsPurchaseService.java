package com.zjc.provider.service;

import com.zjc.common.dto.GoodsPurchaseRequestDTO;
import com.zjc.common.dto.GoodsPurchaseResponseDTO;

/**
 * 商品购买服务。
 *
 * @author jiancai.zhong
 */
public interface GoodsPurchaseService {

    /**
     * 按商品维度加分布式锁并完成购买。
     *
     * @param goodsId 商品 ID
     * @param request 购买请求
     * @return 购买结果
     */
    GoodsPurchaseResponseDTO purchase(Long goodsId, GoodsPurchaseRequestDTO request);
}
