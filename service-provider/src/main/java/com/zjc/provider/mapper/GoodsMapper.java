package com.zjc.provider.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zjc.provider.entity.Goods;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

/**
 * 商品表 Mapper 接口。
 *
 * <p>继承 MyBatis-Plus 的 {@link BaseMapper}，已内置单表 CRUD 方法，
 * 一般无需在此声明额外方法。
 *
 * @author jiancai.zhong
 * @since 2026-08-06
 */
public interface GoodsMapper extends BaseMapper<Goods> {

    /**
     * 在数据库层原子扣减库存，作为分布式锁之外的一致性兜底。
     *
     * @param goodsId 商品 ID
     * @param quantity 购买数量
     * @return 影响行数；0 表示商品不可购买或库存不足
     */
    @Update("""
            UPDATE t_goods
               SET stock = stock - #{quantity}
             WHERE goods_id = #{goodsId}
               AND status = 1
               AND is_deleted = 0
               AND stock >= #{quantity}
            """)
    int decreaseStock(@Param("goodsId") Long goodsId, @Param("quantity") int quantity);
}
