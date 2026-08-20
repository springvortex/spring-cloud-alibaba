package com.zjc.provider.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zjc.common.constant.ApiResponseEnum;
import com.zjc.common.dto.UserDTO;
import com.zjc.common.web.ApiResponse;
import com.zjc.provider.converter.UserConverter;
import com.zjc.provider.entity.User;
import com.zjc.provider.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.validation.annotation.Validated;
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
 * Entity <-> DTO 转换使用 {@link UserConverter}（MapStruct 编译期生成，零反射）。
 *
 * @author jiancai.zhong
 */
@Tag(name = "用户管理", description = "用户的增删改查")
@RestController
@Validated
public class UserController {

    @Resource
    private UserService userService;

    @Resource
    private UserConverter userConverter;

    @Operation(summary = "根据ID查询单个用户")
    @GetMapping("/user/{id}")
    public ApiResponse<UserDTO> getUser(
            @Parameter(description = "用户主键") @PathVariable("id") Long id) {
        return ApiResponse.success(userConverter.entityToDto(userService.getById(id)));
    }

    @Operation(summary = "查询全部有效用户")
    @GetMapping("/user/list")
    public ApiResponse<List<UserDTO>> list() {
        return ApiResponse.success(userConverter.entityListToDtoList(userService.list()));
    }

    @Operation(summary = "分页查询有效用户")
    @GetMapping("/user/page")
    public ApiResponse<Page<UserDTO>> page(
            @Parameter(description = "当前页码，从1开始")
            @Min(value = 1, message = "当前页码必须从1开始")
            @RequestParam(value = "current", defaultValue = "1") long current,
            @Parameter(description = "每页条数，范围1-100")
            @Min(value = 1, message = "每页条数不能小于1")
            @Max(value = 100, message = "每页条数不能超过100")
            @RequestParam(value = "size", defaultValue = "10") long size) {
        Page<User> page = userService.page(new Page<>(current, size));
        Page<UserDTO> result = new Page<>(page.getCurrent(), page.getSize(), page.getTotal());
        result.setRecords(userConverter.entityListToDtoList(page.getRecords()));
        return ApiResponse.success(result);
    }

    @Operation(summary = "新增用户")
    @PostMapping("/user")
    public ApiResponse<UserDTO> add(@Valid @RequestBody UserDTO dto) {
        User user = userConverter.dtoToEntity(dto);
        userService.save(user);
        return ApiResponse.success(userConverter.entityToDto(user));
    }

    @Operation(summary = "根据ID修改用户")
    @PutMapping("/user")
    public ApiResponse<Void> update(@Valid @RequestBody UserDTO dto) {
        boolean updated = userService.updateById(userConverter.dtoToEntity(dto));
        return updated ? ApiResponse.success() : ApiResponse.failure(ApiResponseEnum.NOT_FOUND);
    }

    @Operation(summary = "根据ID删除用户（逻辑删除）")
    @DeleteMapping("/user/{id}")
    public ApiResponse<Void> delete(
            @Parameter(description = "用户主键") @PathVariable("id") Long id) {
        boolean removed = userService.removeById(id);
        return removed ? ApiResponse.success() : ApiResponse.failure(ApiResponseEnum.NOT_FOUND);
    }
}
