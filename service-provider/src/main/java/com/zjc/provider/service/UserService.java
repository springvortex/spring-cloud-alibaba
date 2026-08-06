package com.zjc.provider.service;

import com.baomidou.mybatisplus.spring.service.IService;
import com.zjc.provider.entity.User;

/**
 * <p>
 * 用户表 服务类
 * </p>
 *
 * @author jiancai.zhong
 * @since 2026-08-06
 */
public interface UserService extends IService<User> {

    User selectUserById(Long id);

}
