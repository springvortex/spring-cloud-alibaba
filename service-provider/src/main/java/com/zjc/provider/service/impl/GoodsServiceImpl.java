package com.zjc.provider.service.impl;

import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import com.zjc.provider.entity.Goods;
import com.zjc.provider.mapper.GoodsMapper;
import com.zjc.provider.service.GoodsService;
import org.springframework.stereotype.Service;

/**
 * <p>
 * 商品表 服务实现类
 * </p>
 *
 * @author jiancai.zhong
 * @since 2026-08-06
 */
@Service
public class GoodsServiceImpl extends ServiceImpl<GoodsMapper, Goods> implements GoodsService {

}
