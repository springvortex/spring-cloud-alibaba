package com.zjc.provider.controller;

import com.zjc.common.dto.UserDTO;
import com.zjc.common.web.ApiResponse;
import com.zjc.provider.entity.User;
import com.zjc.provider.service.UserService;
import jakarta.annotation.Resource;
import org.springframework.beans.BeanUtils;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

import java.util.List;

/**
 * 用户管理 REST 接口。
 *
 * <p>对外统一返回 {@link UserDTO}，{@link User} 实体不直接暴露，
 * 避免数据库结构（逻辑删除字段、更新时间等）泄露到接口契约中。
 * 所有方法返回值包装在 {@link ApiResponse} 里，保证响应结构一致。
 *
 * <p>CRUD 直接复用 {@link UserService}（继承 MyBatis-Plus 的 IService）
 * 自带的能力，无需在 service 层重复定义通用方法。
 *
 * @author jiancai.zhong
 */
@RestController
public class UserController {

    @Resource
    private UserService userService;

    /**
     * 根据ID查询单个用户。
     *
     * @param id 用户主键
     * @return 用户信息；记录不存在时 data 为 null
     */
    @GetMapping("/user/{id}")
    public ApiResponse<UserDTO> getUser(@PathVariable("id") Long id) {
        return ApiResponse.success(toDTO(userService.getById(id)));
    }

    /**
     * 查询全部有效用户。
     *
     * <p>逻辑删除的记录会被 MyBatis-Plus 自动过滤（依赖 logic-delete 配置），
     * 返回结果仅包含 is_deleted = 0 的用户。
     *
     * @return 用户列表，已按 DTO 过滤内部字段；无数据时返回空列表
     */
    @GetMapping("/user/list")
    public ApiResponse<List<UserDTO>> list() {
        List<UserDTO> list = userService.list().stream().map(this::toDTO).toList();
        return ApiResponse.success(list);
    }

    /**
     * 分页查询有效用户。
     *
     * <p>依赖分页拦截器自动改写 SQL，返回带总数与分页信息的 {@link Page}。
     * 逻辑删除记录同样会被自动过滤。
     *
     * @param current 当前页码，从 1 开始，默认 1
     * @param size    每页条数，默认 10
     * @return 分页结果，records 已转为 DTO
     */
    @GetMapping("/user/page")
    public ApiResponse<Page<UserDTO>> page(
            @RequestParam(value = "current", defaultValue = "1") long current,
            @RequestParam(value = "size", defaultValue = "10") long size) {
        Page<User> page = userService.page(new Page<>(current, size));
        Page<UserDTO> result = new Page<>(page.getCurrent(), page.getSize(), page.getTotal());
        result.setRecords(page.getRecords().stream().map(this::toDTO).toList());
        return ApiResponse.success(result);
    }

    /**
     * 新增用户。
     *
     * <p>DTO 转实体后入库，数据库自增主键会回填到实体，
     * 因此返回的 DTO 含生成后的 userId，调用方无需再查一次。
     *
     * @param dto 用户信息，无需传 userId、createTime 等系统生成字段
     * @return 含生成主键的完整用户信息
     */
    @PostMapping("/user")
    public ApiResponse<UserDTO> add(@RequestBody UserDTO dto) {
        User user = new User();
        BeanUtils.copyProperties(dto, user);
        userService.save(user);
        return ApiResponse.success(toDTO(user));
    }

    /**
     * 根据ID修改用户。
     *
     * <p>基于 MyBatis-Plus 的 updateById，按 userId 定位记录。
     * 默认只更新 DTO 中非 null 的字段，传 null 的字段保持原值不变。
     *
     * @param dto 待更新信息，必须携带 userId
     * @return 无业务数据
     */
    @PutMapping("/user")
    public ApiResponse<Void> update(@RequestBody UserDTO dto) {
        User user = new User();
        BeanUtils.copyProperties(dto, user);
        userService.updateById(user);
        return ApiResponse.success();
    }

    /**
     * 根据ID删除用户。
     *
     * <p>执行逻辑删除：将 is_deleted 置为 1，而非物理删除记录。
     *
     * @param id 用户主键
     * @return 无业务数据
     */
    @DeleteMapping("/user/{id}")
    public ApiResponse<Void> delete(@PathVariable("id") Long id) {
        userService.removeById(id);
        return ApiResponse.success();
    }

    /**
     * 实体转对外 DTO。
     *
     * <p>同名属性自动拷贝；isDeleted、updateTime 等内部字段因 DTO 不含，
     * 自然被忽略，从而实现内部字段不对外暴露。
     *
     * @param user 用户实体，允许为 null
     * @return 对外 DTO；入参为 null 时返回 null
     */
    private UserDTO toDTO(User user) {
        if (user == null) {
            return null;
        }
        UserDTO dto = new UserDTO();
        BeanUtils.copyProperties(user, dto);
        return dto;
    }
}