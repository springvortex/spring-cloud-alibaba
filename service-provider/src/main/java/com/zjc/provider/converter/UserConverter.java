package com.zjc.provider.converter;

import com.zjc.common.dto.UserDTO;
import com.zjc.provider.entity.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

/**
 * 用户 Entity <-> DTO 转换器。
 *
 * <p>MapStruct 在编译期生成实现类，零反射。
 *
 * @author jiancai.zhong
 */
@Mapper(componentModel = "spring")
public interface UserConverter {

    /**
     * 用户 Entity 转 DTO。
     *
     * @param user 用户实体，为 {@code null} 时返回 {@code null}
     * @return 用户 DTO
     */
    UserDTO entityToDto(User user);

    /**
     * 用户 DTO 转 Entity，忽略 isDeleted、createTime、updateTime（由数据库管理）。
     *
     * @param dto 用户 DTO
     * @return 用户实体
     */
    @Mapping(target = "isDeleted", ignore = true)
    @Mapping(target = "updateTime", ignore = true)
    @Mapping(target = "createTime", ignore = true)
    User dtoToEntity(UserDTO dto);

    /**
     * 用户 Entity 列表批量转 DTO 列表。
     *
     * @param users 用户实体列表，为 {@code null} 时返回 {@code null}
     * @return 用户 DTO 列表
     */
    List<UserDTO> entityListToDtoList(List<User> users);
}
