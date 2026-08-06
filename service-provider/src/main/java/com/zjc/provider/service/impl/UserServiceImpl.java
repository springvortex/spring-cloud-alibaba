package com.zjc.provider.service.impl;

import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import com.zjc.provider.entity.User;
import com.zjc.provider.mapper.UserMapper;
import com.zjc.provider.service.UserService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

/**
 * <p>
 * 用户表 服务实现类
 * </p>
 *
 * @author jiancai.zhong
 * @since 2026-08-06
 */
@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {

    @Resource
    private UserMapper userMapper;

    @Override
    public User selectUserById(Long id) {
        return userMapper.selectById(id);
    }
}
