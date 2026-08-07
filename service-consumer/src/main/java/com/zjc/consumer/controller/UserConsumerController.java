package com.zjc.consumer.controller;

import com.zjc.common.dto.UserDTO;
import com.zjc.common.web.ApiResponse;
import com.zjc.consumer.feign.UserFeignClient;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Consumer 侧用户接口演示。
 *
 * <p>展示 consumer 如何通过本地 Feign 客户端 {@link UserFeignClient} 调用 provider，
 * 调用链路为：浏览器 → 本 Controller → {@link UserFeignClient}（Feign 代理）
 * → service-provider 的 {@code UserController}。
 *
 * <p>provider 不可用时由 {@link com.zjc.consumer.feign.factory.UserFeignFallbackFactory}
 * 自动降级，上层无需 try-catch。
 *
 * @author jiancai.zhong
 */
@Tag(name = "用户消费者", description = "consumer 通过 Feign 调用 provider 的用户接口")
@RestController
public class UserConsumerController {

    @Resource
    private UserFeignClient userFeignClient;

    @Operation(summary = "远程查询用户（Feign + 降级演示）")
    @GetMapping("/consumer/user/{id}")
    public ApiResponse<UserDTO> getUser(
            @Parameter(description = "用户主键") @PathVariable("id") Long id) {
        // 无需 try-catch：失败时 fallback 自动返回兜底数据
        return userFeignClient.getUser(id);
    }

    @Operation(summary = "远程查询用户列表")
    @GetMapping("/consumer/user/list")
    public ApiResponse<List<UserDTO>> list() {
        return userFeignClient.list();
    }
}
