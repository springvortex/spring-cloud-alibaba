package com.zjc.provider.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zjc.common.constant.ApiResponseEnum;
import com.zjc.common.dto.GoodsDTO;
import com.zjc.common.web.ApiResponse;
import com.zjc.provider.converter.GoodsConverter;
import com.zjc.provider.entity.Goods;
import com.zjc.provider.service.GoodsService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * {@link GoodsController} 单元测试。
 *
 * <p>验证商品 CRUD 逻辑和 Entity → DTO 转换，包含价格 BigDecimal 比对。
 *
 * @author jiancai.zhong
 */
@DisplayName("商品管理 Controller")
@ExtendWith(MockitoExtension.class)
class GoodsControllerTest {

    @Mock
    private GoodsService goodsService;

    @Mock
    private GoodsConverter goodsConverter;

    @InjectMocks
    private GoodsController goodsController;

    /**
     * 验证根据 ID 查询商品时返回正确的 DTO。
     */
    @Test
    @DisplayName("getGoods: 返回单个商品 DTO")
    void testGetGoodsReturnsDto() {
        Goods goods = new Goods();
        goods.setGoodsId(1L);
        goods.setGoodsName("iPhone");
        goods.setGoodsPrice(new BigDecimal("6999"));
        GoodsDTO dto = new GoodsDTO();
        dto.setGoodsId(1L);
        dto.setGoodsName("iPhone");
        dto.setGoodsPrice(new BigDecimal("6999"));
        when(goodsService.getById(1L)).thenReturn(goods);
        when(goodsConverter.entityToDto(goods)).thenReturn(dto);

        ApiResponse<GoodsDTO> resp = goodsController.getGoods(1L);

        assertThat(resp.isSuccess()).isTrue();
        assertThat(resp.getData().getGoodsName()).isEqualTo("iPhone");
        assertThat(resp.getData().getGoodsPrice()).isEqualByComparingTo("6999");
    }

    /**
     * 验证查询不存在的商品 ID 时，data 为 null。
     */
    @Test
    @DisplayName("getGoods: 商品不存在返回 null")
    void testGetGoodsNotFound() {
        when(goodsService.getById(999L)).thenReturn(null);
        when(goodsConverter.entityToDto(null)).thenReturn(null);

        ApiResponse<GoodsDTO> resp = goodsController.getGoods(999L);

        assertThat(resp.getData()).isNull();
    }

    /**
     * 验证列表查询返回的 DTO 列表内容正确。
     */
    @Test
    @DisplayName("list: 返回商品列表")
    void testListReturnsList() {
        Goods goods = new Goods();
        goods.setGoodsId(1L);
        goods.setGoodsName("iPhone");
        GoodsDTO dto = new GoodsDTO();
        dto.setGoodsId(1L);
        dto.setGoodsName("iPhone");
        when(goodsService.list()).thenReturn(List.of(goods));
        when(goodsConverter.entityListToDtoList(List.of(goods))).thenReturn(List.of(dto));

        ApiResponse<List<GoodsDTO>> resp = goodsController.list();

        assertThat(resp.getData()).hasSize(1);
        assertThat(resp.getData().get(0).getGoodsName()).isEqualTo("iPhone");
    }

    /**
     * 验证无数据时返回空列表。
     */
    @Test
    @DisplayName("list: 空列表")
    void testListEmpty() {
        when(goodsService.list()).thenReturn(Collections.emptyList());
        when(goodsConverter.entityListToDtoList(Collections.emptyList())).thenReturn(Collections.emptyList());

        ApiResponse<List<GoodsDTO>> resp = goodsController.list();

        assertThat(resp.getData()).isEmpty();
    }

    /**
     * 验证分页查询的 total 和 records 数量都正确。
     */
    @Test
    @DisplayName("page: 分页查询")
    void testPageReturnsPage() {
        Page<Goods> page = new Page<>(1, 5, 2);
        Goods g1 = new Goods();
        g1.setGoodsId(1L);
        g1.setGoodsName("iPhone");
        Goods g2 = new Goods();
        g2.setGoodsId(2L);
        g2.setGoodsName("iPad");
        page.setRecords(List.of(g1, g2));
        GoodsDTO d1 = new GoodsDTO();
        d1.setGoodsName("iPhone");
        GoodsDTO d2 = new GoodsDTO();
        d2.setGoodsName("iPad");
        when(goodsService.page(any(Page.class))).thenReturn(page);
        when(goodsConverter.entityListToDtoList(List.of(g1, g2))).thenReturn(List.of(d1, d2));

        ApiResponse<Page<GoodsDTO>> resp = goodsController.page(1, 5);

        assertThat(resp.getData().getTotal()).isEqualTo(2);
        assertThat(resp.getData().getRecords()).hasSize(2);
    }

    /**
     * 验证新增商品后返回回填的 DTO。
     */
    @Test
    @DisplayName("add: 新增商品")
    void testAddReturnsDto() {
        GoodsDTO dto = new GoodsDTO();
        dto.setGoodsName("new product");
        dto.setGoodsPrice(new BigDecimal("100"));
        Goods entity = new Goods();
        entity.setGoodsName("new product");
        GoodsDTO resultDto = new GoodsDTO();
        resultDto.setGoodsName("new product");
        when(goodsConverter.dtoToEntity(dto)).thenReturn(entity);
        when(goodsConverter.entityToDto(entity)).thenReturn(resultDto);
        when(goodsService.save(any(Goods.class))).thenReturn(true);

        ApiResponse<GoodsDTO> resp = goodsController.add(dto);

        assertThat(resp.isSuccess()).isTrue();
        assertThat(resp.getData().getGoodsName()).isEqualTo("new product");
        verify(goodsService).save(any(Goods.class));
    }

    /**
     * 验证修改商品时调用 updateById。
     */
    @Test
    @DisplayName("update: 修改商品")
    void testUpdateSuccess() {
        GoodsDTO dto = new GoodsDTO();
        dto.setGoodsId(1L);
        dto.setGoodsName("updated");
        Goods entity = new Goods();
        entity.setGoodsId(1L);
        when(goodsConverter.dtoToEntity(dto)).thenReturn(entity);
        when(goodsService.updateById(any(Goods.class))).thenReturn(true);

        ApiResponse<Void> resp = goodsController.update(dto);

        assertThat(resp.isSuccess()).isTrue();
        verify(goodsService).updateById(any(Goods.class));
    }

    @Test
    @DisplayName("update: 商品不存在返回资源不存在")
    void testUpdateNotFound() {
        GoodsDTO dto = new GoodsDTO();
        dto.setGoodsId(999L);
        Goods entity = new Goods();
        entity.setGoodsId(999L);
        when(goodsConverter.dtoToEntity(dto)).thenReturn(entity);
        when(goodsService.updateById(entity)).thenReturn(false);

        ApiResponse<Void> resp = goodsController.update(dto);

        assertThat(resp.isSuccess()).isFalse();
        assertThat(resp.getCode()).isEqualTo(ApiResponseEnum.NOT_FOUND.code());
        assertThat(resp.getMessage()).isEqualTo(ApiResponseEnum.NOT_FOUND.message());
    }

    /**
     * 验证删除商品时调用 removeById。
     */
    @Test
    @DisplayName("delete: 逻辑删除")
    void testDeleteSuccess() {
        when(goodsService.removeById(1L)).thenReturn(true);

        ApiResponse<Void> resp = goodsController.delete(1L);

        assertThat(resp.isSuccess()).isTrue();
        verify(goodsService).removeById(1L);
    }

    @Test
    @DisplayName("delete: 商品不存在返回资源不存在")
    void testDeleteNotFound() {
        when(goodsService.removeById(999L)).thenReturn(false);

        ApiResponse<Void> resp = goodsController.delete(999L);

        assertThat(resp.isSuccess()).isFalse();
        assertThat(resp.getCode()).isEqualTo(ApiResponseEnum.NOT_FOUND.code());
    }
}
