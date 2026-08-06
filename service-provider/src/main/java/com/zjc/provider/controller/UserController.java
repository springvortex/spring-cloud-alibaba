package com.zjc.provider.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zjc.common.dto.UserDTO;
import com.zjc.common.web.ApiResponse;
import com.zjc.provider.entity.User;
import com.zjc.provider.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "用户管理", description = "用户的增删改查")
@RestController
public class UserController {

    @Resource
    private UserService userService;

    @Operation(summary = "根据ID查询单个用户")
    @GetMapping("/user/{id}")
    public ApiResponse<UserDTO> getUser(
            @Parameter(description = "用户主键") @PathVariable("id") Long id) {
        return ApiResponse.success(toDTO(userService.getById(id)));
    }

    @Operation(summary = "查询全部有效用户")
    @GetMapping("/user/list")
    public ApiResponse<List<UserDTO>> list() {
        List<UserDTO> list = userService.list().stream().map(this::toDTO).toList();
        return ApiResponse.success(list);
    }

    @Operation(summary = "分页查询有效用户")
    @GetMapping("/user/page")
    public ApiResponse<Page<UserDTO>> page(
            @Parameter(description = "当前页码，从1开始") @RequestParam(value = "current", defaultValue = "1") long current,
            @Parameter(description = "每页条数") @RequestParam(value = "size", defaultValue = "10") long size) {
        Page<User> page = userService.page(new Page<>(current, size));
        Page<UserDTO> result = new Page<>(page.getCurrent(), page.getSize(), page.getTotal());
        result.setRecords(page.getRecords().stream().map(this::toDTO).toList());
        return ApiResponse.success(result);
    }

    @Operation(summary = "新增用户")
    @PostMapping("/user")
    public ApiResponse<UserDTO> add(@Valid @RequestBody UserDTO dto) {
        User user = new User();
        BeanUtils.copyProperties(dto, user);
        userService.save(user);
        return ApiResponse.success(toDTO(user));
    }

    @Operation(summary = "根据ID修改用户")
    @PutMapping("/user")
    public ApiResponse<Void> update(@Valid @RequestBody UserDTO dto) {
        User user = new User();
        BeanUtils.copyProperties(dto, user);
        userService.updateById(user);
        return ApiResponse.success();
    }

    @Operation(summary = "根据ID删除用户（逻辑删除）")
    @DeleteMapping("/user/{id}")
    public ApiResponse<Void> delete(
            @Parameter(description = "用户主键") @PathVariable("id") Long id) {
        userService.removeById(id);
        return ApiResponse.success();
    }

    /**
     * Entity 转 DTO，过滤内部字段
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