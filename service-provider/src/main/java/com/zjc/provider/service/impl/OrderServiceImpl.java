package com.zjc.provider.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import com.zjc.common.constant.ApiResponseEnum;
import com.zjc.common.exception.BusinessException;
import com.zjc.provider.entity.Order;
import com.zjc.provider.entity.OrderDetail;
import com.zjc.provider.mapper.OrderMapper;
import com.zjc.provider.service.OrderDetailService;
import com.zjc.provider.service.OrderService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 订单主表服务实现。
 *
 * <p>继承 {@link ServiceImpl}，泛型指定 Mapper 与实体，
 * 由 MyBatis-Plus 自动装配单表 CRUD 实现，无需手写通用方法。
 *
 * @author jiancai.zhong
 * @since 2026-08-06
 */
@Service
public class OrderServiceImpl extends ServiceImpl<OrderMapper, Order> implements OrderService {

    @Resource
    private OrderDetailService orderDetailService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveWithDetails(Order order, List<OrderDetail> details) {
        if (!save(order)) {
            throw new BusinessException(ApiResponseEnum.INTERNAL_ERROR);
        }

        details.forEach(detail -> {
            detail.setId(null);
            detail.setOrderId(order.getOrderId());
            detail.setOrderNo(order.getOrderNo());
        });

        if (!details.isEmpty() && !orderDetailService.saveBatch(details)) {
            throw new BusinessException(ApiResponseEnum.INTERNAL_ERROR);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean removeWithDetails(Long orderId) {
        if (!removeById(orderId)) {
            return false;
        }

        orderDetailService.remove(new LambdaQueryWrapper<OrderDetail>()
                .eq(OrderDetail::getOrderId, orderId));
        return true;
    }
}
