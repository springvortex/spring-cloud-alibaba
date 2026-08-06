package com.zjc.provider.service.impl;

import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import com.zjc.provider.entity.User;
import com.zjc.provider.mapper.UserMapper;
import com.zjc.provider.service.UserService;
import org.springframework.stereotype.Service;

/**
 * 用户表服务实现。
 *
 * <p>继承 {@link ServiceImpl}，泛型指定 Mapper 与实体，
 * 由 MyBatis-Plus 自动装配单表 CRUD 实现，无需手写通用方法。
 *
 * @author jiancai.zhong
 * @since 2026-08-06
 */
@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {

}