package com.zjc.provider.service;

import com.baomidou.mybatisplus.spring.service.IService;
import com.zjc.common.dto.UserDTO;
import com.zjc.provider.entity.User;

/**
 * 用户表服务接口。
 *
 * <p>继承 MyBatis-Plus 的 {@link IService}，开箱即用单表 CRUD、
 * 批量操作、查询构造器等通用能力，无需重复定义。
 * 如有复杂业务逻辑，在此接口声明对应方法。
 *
 * @author jiancai.zhong
 * @since 2026-08-06
 */
public interface UserService extends IService<User> {

    /**
     * 查询用户详情并使用 Redis 缓存。
     *
     * @param userId 用户 ID
     * @return 用户 DTO
     */
    UserDTO getUser(Long userId);

    /**
     * 更新用户并清理详情缓存。
     *
     * @param user 用户实体
     * @return 是否更新成功
     */
    boolean updateUser(User user);

    /**
     * 删除用户并清理详情缓存。
     *
     * @param userId 用户 ID
     * @return 是否删除成功
     */
    boolean deleteUser(Long userId);
}
