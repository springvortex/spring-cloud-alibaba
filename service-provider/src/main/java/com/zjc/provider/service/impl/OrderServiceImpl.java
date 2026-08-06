package com.zjc.provider.service.impl;

import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import com.zjc.provider.entity.Order;
import com.zjc.provider.mapper.OrderMapper;
import com.zjc.provider.service.OrderService;
import org.springframework.stereotype.Service;

/**
 * <p>
 * 订单主表 服务实现类
 * </p>
 *
 * @author jiancai.zhong
 * @since 2026-08-06
 */
@Service
public class OrderServiceImpl extends ServiceImpl<OrderMapper, Order> implements OrderService {

}
