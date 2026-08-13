package com.zjc.provider.converter;

import com.zjc.common.dto.GoodsDTO;
import com.zjc.provider.entity.Goods;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

/**
 * 商品 Entity <-> DTO 转换器。
 *
 * <p>MapStruct 在编译期生成实现类，零反射。
 * Entity 中的 isDeleted、updateTime 等内部字段因 DTO 中不存在，自动忽略。
 *
 * @author jiancai.zhong
 */
@Mapper(componentModel = "spring")
public interface GoodsConverter {

    /**
     * 商品 Entity 转 DTO。
     *
     * @param goods 商品实体，为 {@code null} 时返回 {@code null}
     * @return 商品 DTO
     */
    GoodsDTO entityToDto(Goods goods);

    /**
     * 商品 DTO 转 Entity，忽略 isDeleted、createTime、updateTime（由数据库管理）。
     *
     * @param dto 商品 DTO
     * @return 商品实体
     */
    @Mapping(target = "isDeleted", ignore = true)
    @Mapping(target = "updateTime", ignore = true)
    @Mapping(target = "createTime", ignore = true)
    Goods dtoToEntity(GoodsDTO dto);

    /**
     * 商品 Entity 列表批量转 DTO 列表。
     *
     * @param goodsList 商品实体列表，为 {@code null} 时返回 {@code null}
     * @return 商品 DTO 列表
     */
    List<GoodsDTO> entityListToDtoList(List<Goods> goodsList);
}
