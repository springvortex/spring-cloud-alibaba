package com.zjc.common.api.user;

import com.zjc.common.api.user.factory.UserFeignFallbackFactory;
import com.zjc.common.dto.UserDTO;
import com.zjc.common.web.ApiResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.List;

/**
 * 用户服务共享 Feign 客户端，远程调用 service-provider 的用户接口。
 *
 * <p>放在 common 模块的 {@code api.user} 包下，所有依赖 common 的服务
 * 都能直接注入并复用这个 Feign 声明，无需各自重复定义。
 *
 * <p>{@code contextId = "userFeignApi"} 用于在 OpenFeign 配置中隔离本客户端的
 * 超时、拦截器等子配置，避免与指向同一服务的
 * 其他客户端（如 {@code testFeignApi}）产生配置冲突。
 *
 * <p>{@code fallbackFactory} 提供降级能力：provider 不可用 / 超时 / 抛异常时
 * 自动走兜底逻辑，还能拿到失败原因（异常对象），
 * 比 {@code fallback} 更适合生产排查。
 *
 * @author jiancai.zhong
 */
@FeignClient(
        name = "service-provider",
        contextId = "userFeignApi",
        fallbackFactory = UserFeignFallbackFactory.class
)
public interface UserFeignApi {

    /**
     * 根据用户 ID 查询用户信息。
     *
     * @param userId 用户主键
     * @return 用户信息（降级时返回 null 包装在成功响应里）
     */
    @GetMapping("/user/{id}")
    ApiResponse<UserDTO> getUser(@PathVariable("id") Long userId);

    /**
     * 查询全部有效用户列表。
     *
     * @return 用户列表（降级时返回空列表）
     */
    @GetMapping("/user/list")
    ApiResponse<List<UserDTO>> list();
}
