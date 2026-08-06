package com.zjc.provider.service.impl;

import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import com.zjc.provider.entity.OrderDetail;
import com.zjc.provider.mapper.OrderDetailMapper;
import com.zjc.provider.service.OrderDetailService;
import org.springframework.stereotype.Service;

/**
 * <p>
 * 订单明细表 服务实现类
 * </p>
 *
 * @author jiancai.zhong
 * @since 2026-08-06
 */
@Service
public class OrderDetailServiceImpl extends ServiceImpl<OrderDetailMapper, OrderDetail> implements OrderDetailService {

}
