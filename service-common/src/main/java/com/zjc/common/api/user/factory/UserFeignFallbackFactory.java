package com.zjc.common.api.user.factory;

import com.zjc.common.api.user.UserFeignApi;
import com.zjc.common.dto.UserDTO;
import com.zjc.common.web.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

/**
 * {@link UserFeignApi} 的降级工厂。
 *
 * <p>当远程调用失败时（provider 宕机、网络超时、返回异常等），
 * Feign 会自动创建本工厂的实例并注入 {@link Throwable} 失败原因，
 * 由 {@link #create(Throwable)} 返回的兜底代理对象接管后续调用。
 *
 * <p>相比 {@code @FeignClient(fallback = ...)} 直接指定降级类，
 * {@code FallbackFactory} 的优势是能拿到异常对象，方便记录失败原因做监控告警。
 *
 * <p><b>降级策略：</b>
 * <ul>
 *   <li>单个查询：返回空数据的成功响应，上层用 {@code data == null} 判断是否命中降级</li>
 *   <li>列表查询：返回空列表，保证上层 forEach / 分页不会 NPE</li>
 * </ul>
 * 实际项目里应根据业务取舍：是返回兜底数据（可用性优先）还是直接抛错（一致性优先）。
 *
 * @author jiancai.zhong
 */
@Slf4j
@Component
public class UserFeignFallbackFactory implements FallbackFactory<UserFeignApi> {

    /**
     * 创建降级代理对象，远程调用失败时由 Feign 自动回调。
     *
     * @param cause 远程调用失败原因（超时、连接拒绝、服务不可用等）
     * @return 降级代理，返回兜底数据而非抛异常
     */
    @Override
    public UserFeignApi create(Throwable cause) {
        log.error("调用 service-provider 用户接口失败，触发降级", cause);

        return new UserFeignApi() {

            @Override
            public ApiResponse<UserDTO> getUser(Long userId) {
                log.warn("getUser 降级，userId={}", userId);
                return ApiResponse.success();
            }

            @Override
            public ApiResponse<List<UserDTO>> list() {
                log.warn("list 降级，返回空列表");
                return ApiResponse.success(Collections.emptyList());
            }
        };
    }
}
