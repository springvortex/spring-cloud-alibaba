package com.zjc.provider.service;

import com.baomidou.mybatisplus.spring.service.IService;
import com.zjc.common.dto.GoodsDTO;
import com.zjc.provider.entity.Goods;

/**
 * 商品表服务接口。
 *
 * <p>继承 MyBatis-Plus 的 {@link IService}，开箱即用单表 CRUD、
 * 批量操作、查询构造器等通用能力，无需重复定义。
 *
 * @author jiancai.zhong
 * @since 2026-08-06
 */
public interface GoodsService extends IService<Goods> {

    /**
     * 查询商品详情并使用 Redis 缓存。
     *
     * @param goodsId 商品 ID
     * @return 商品 DTO
     */
    GoodsDTO getGoods(Long goodsId);

    /**
     * 更新商品并清理详情缓存。
     *
     * @param goods 商品实体
     * @return 是否更新成功
     */
    boolean updateGoods(Goods goods);

    /**
     * 删除商品并清理详情缓存。
     *
     * @param goodsId 商品 ID
     * @return 是否删除成功
     */
    boolean deleteGoods(Long goodsId);
}
