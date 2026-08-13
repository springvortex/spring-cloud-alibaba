package com.zjc.consumer.controller;

import com.zjc.common.api.user.UserFeignApi;
import com.zjc.common.dto.UserDTO;
import com.zjc.common.web.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Consumer 侧用户接口。
 *
 * <p>展示 consumer 如何通过 common 模块共享的 Feign API {@link UserFeignApi} 调用 provider，
 * 调用链路为：浏览器 → 本 Controller → {@link UserFeignApi}（Feign 代理）
 * → service-provider 的 {@code UserController}。
 *
 * <p>provider 不可用时由 {@link com.zjc.common.api.user.factory.UserFeignFallbackFactory}
 * 自动降级，上层无需 try-catch。
 *
 * @author jiancai.zhong
 */
@Tag(name = "用户消费者", description = "consumer 通过 Feign 调用 provider 的用户接口")
@RestController
public class UserConsumerController {

    /**
     * 用户服务共享 Feign 客户端，由 common 模块提供。
     */
    @Resource
    private UserFeignApi userFeignApi;

    /**
     * 远程查询单个用户，底层通过 Feign 代理调用 service-provider。
     *
     * @param id 用户主键
     * @return 用户信息，降级时 data 为 null
     */
    @Operation(summary = "远程查询用户（Feign + 降级演示）")
    @GetMapping("/consumer/user/{id}")
    public ApiResponse<UserDTO> getUser(
            @Parameter(description = "用户主键") @PathVariable("id") Long id) {
        return userFeignApi.getUser(id);
    }

    /**
     * 远程查询用户列表，底层通过 Feign 代理调用 service-provider。
     *
     * @return 用户列表，降级时返回空列表
     */
    @Operation(summary = "远程查询用户列表")
    @GetMapping("/consumer/user/list")
    public ApiResponse<List<UserDTO>> list() {
        return userFeignApi.list();
    }
}
