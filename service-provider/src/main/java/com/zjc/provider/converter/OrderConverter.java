package com.zjc.provider.converter;

import com.zjc.common.dto.OrderDTO;
import com.zjc.common.dto.OrderDetailDTO;
import com.zjc.provider.entity.Order;
import com.zjc.provider.entity.OrderDetail;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

/**
 * 订单 Entity <-> DTO 转换器（含订单明细）。
 *
 * <p>MapStruct 在编译期生成实现类，零反射。
 * orderDetails 在 Controller 中手动聚合，此处忽略。
 *
 * @author jiancai.zhong
 */
@Mapper(componentModel = "spring")
public interface OrderConverter {

    /**
     * 订单 Entity 转 DTO，忽略 orderDetails（在 Controller 中手动聚合）。
     *
     * @param order 订单实体，为 {@code null} 时返回 {@code null}
     * @return 订单 DTO
     */
    @Mapping(target = "orderDetails", ignore = true)
    OrderDTO entityToDto(Order order);

    /**
     * 订单明细 Entity 转 DTO。
     *
     * @param detail 订单明细实体，为 {@code null} 时返回 {@code null}
     * @return 订单明细 DTO
     */
    OrderDetailDTO entityToDto(OrderDetail detail);

    /**
     * 订单 DTO 转 Entity，忽略 isDeleted、createTime、updateTime、payTime（由业务逻辑管理）。
     *
     * @param dto 订单 DTO
     * @return 订单实体
     */
    @Mapping(target = "isDeleted", ignore = true)
    @Mapping(target = "updateTime", ignore = true)
    @Mapping(target = "createTime", ignore = true)
    @Mapping(target = "payTime", ignore = true)
    Order dtoToEntity(OrderDTO dto);

    /**
     * 订单 Entity 列表批量转 DTO 列表。
     *
     * @param orders 订单实体列表，为 {@code null} 时返回 {@code null}
     * @return 订单 DTO 列表
     */
    List<OrderDTO> entityListToDtoList(List<Order> orders);
}
