package com.zjc.consumer.feign;

import com.zjc.common.dto.UserDTO;
import com.zjc.common.web.ApiResponse;
import com.zjc.consumer.feign.factory.UserFeignFallbackFactory;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.List;

/**
 * Consumer 专属的 Feign 客户端，远程调用 service-provider 的用户接口。
 *
 * <p>和 common 模块里 {@code TestApi} 的区别在于：这个接口只有 consumer 自己用，
 * 不需要被其他服务共享，所以放在 consumer 内部包 {@code com.zjc.consumer.feign} 下，
 * 而不是塞进 common。这正是 {@code @EnableFeignClients} 保留扫描
 * {@code com.zjc.consumer} 包的意义——让本地定义的 Feign 客户端也能被注册。
 *
 * <p><b>属性说明：</b>
 * <ul>
 *   <li>{@code contextId = "userFeignClient"}：Bean 的唯一标识。
 *       因为 common 里 {@code TestApi} 也指向 {@code service-provider}，
 *       不加 contextId 会导致两个客户端的子配置冲突（超时、拦截器等以 contextId 隔离）。</li>
 *   <li>{@code fallbackFactory}：降级工厂。provider 不可用 / 超时 / 抛异常时自动走兜底逻辑，
 *       还能拿到失败原因（异常对象），比 {@code fallback} 更适合生产排查。</li>
 * </ul>
 *
 * @author jiancai.zhong
 */
@FeignClient(
        name = "service-provider",
        contextId = "userFeignClient",
        fallbackFactory = UserFeignFallbackFactory.class
)
public interface UserFeignClient {

    /**
     * 根据用户ID查询用户信息。
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
