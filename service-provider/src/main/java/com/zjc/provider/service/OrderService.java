package com.zjc.provider.service;

import com.baomidou.mybatisplus.spring.service.IService;
import com.zjc.provider.entity.Order;
import com.zjc.provider.entity.OrderDetail;

import java.util.List;

/**
 * 订单主表服务接口。
 *
 * <p>继承 MyBatis-Plus 的 {@link IService}，开箱即用单表 CRUD、
 * 批量操作、查询构造器等通用能力，无需重复定义。
 *
 * @author jiancai.zhong
 * @since 2026-08-06
 */
public interface OrderService extends IService<Order> {

    /**
     * 保存订单主表与明细。
     *
     * @param order   订单主表实体，保存后会回填主键
     * @param details 订单明细实体列表，可为空
     */
    void saveWithDetails(Order order, List<OrderDetail> details);

    /**
     * 逻辑删除订单主表与明细。
     *
     * @param orderId 订单主键
     * @return 订单不存在时返回 {@code false}
     */
    boolean removeWithDetails(Long orderId);
}
