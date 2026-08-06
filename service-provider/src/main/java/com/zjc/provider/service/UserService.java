package com.zjc.provider.service;

import com.baomidou.mybatisplus.spring.service.IService;
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

}