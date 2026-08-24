package com.zjc.provider.service.impl;

import com.zjc.common.dto.GoodsDTO;
import com.zjc.provider.converter.GoodsConverter;
import com.zjc.provider.entity.Goods;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * {@link GoodsServiceImpl} 详情缓存入口测试。
 *
 * @author jiancai.zhong
 */
@DisplayName("商品详情缓存服务")
@ExtendWith(MockitoExtension.class)
class GoodsServiceImplTest {

    @Mock
    private GoodsConverter goodsConverter;

    @Spy
    @InjectMocks
    private GoodsServiceImpl goodsService;

    @Test
    @DisplayName("getGoods: 查询实体并转换为 DTO")
    void testGetGoodsConvertsEntity() {
        Goods goods = new Goods();
        goods.setGoodsId(1L);
        GoodsDTO dto = new GoodsDTO();
        dto.setGoodsId(1L);
        doReturn(goods).when(goodsService).getById(1L);
        when(goodsConverter.entityToDto(goods)).thenReturn(dto);

        assertThat(goodsService.getGoods(1L)).isSameAs(dto);
        verify(goodsService).getById(1L);
    }

    @Test
    @DisplayName("updateGoods / deleteGoods: 复用 MyBatis-Plus 写操作")
    void testWriteMethodsDelegateToMybatisPlus() {
        Goods goods = new Goods();
        goods.setGoodsId(1L);
        doReturn(true).when(goodsService).updateById(goods);
        doReturn(true).when(goodsService).removeById(1L);

        assertThat(goodsService.updateGoods(goods)).isTrue();
        assertThat(goodsService.deleteGoods(1L)).isTrue();
    }

    @Test
    @DisplayName("缓存配置: 详情缓存，写操作按 ID 驱逐")
    void testCacheAnnotations() throws NoSuchMethodException {
        Cacheable cacheable = GoodsServiceImpl.class
                .getMethod("getGoods", Long.class)
                .getAnnotation(Cacheable.class);
        CacheEvict update = GoodsServiceImpl.class
                .getMethod("updateGoods", Goods.class)
                .getAnnotation(CacheEvict.class);
        CacheEvict delete = GoodsServiceImpl.class
                .getMethod("deleteGoods", Long.class)
                .getAnnotation(CacheEvict.class);

        assertThat(cacheable).isNotNull();
        assertThat(cacheable.cacheNames()).containsExactly("provider:goods:id");
        assertThat(cacheable.key()).isEqualTo("#goodsId");
        assertThat(update.cacheNames()).containsExactly("provider:goods:id");
        assertThat(update.key()).isEqualTo("#goods.goodsId");
        assertThat(delete.cacheNames()).containsExactly("provider:goods:id");
        assertThat(delete.key()).isEqualTo("#goodsId");
    }
}
