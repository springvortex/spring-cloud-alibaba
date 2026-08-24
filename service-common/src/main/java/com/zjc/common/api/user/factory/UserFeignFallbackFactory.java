package com.zjc.common.api.user.factory;

import com.zjc.common.api.user.UserFeignApi;
import com.zjc.common.constant.ApiResponseEnum;
import com.zjc.common.dto.UserDTO;
import com.zjc.common.web.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

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
 *   <li>统一返回失败响应，错误码为 503，提示「业务繁忙，请稍后再试」</li>
 *   <li>失败原因只写入日志，不透出给调用方</li>
 * </ul>
 * 这样可以区分「查询成功但数据不存在」和「下游服务不可用」。
 *
 * @author jiancai.zhong
 */
@Slf4j
@Component
public class UserFeignFallbackFactory implements FallbackFactory<UserFeignApi> {

    /**
     * 降级时的对外提示，不暴露连接超时、下游地址等基础设施细节。
     */
    private static final String FALLBACK_MESSAGE = "业务繁忙，请稍后再试";

    /**
     * 创建降级代理对象，远程调用失败时由 Feign 自动回调。
     *
     * @param cause 远程调用失败原因（超时、连接拒绝、服务不可用等）
     * @return 降级代理，返回服务不可用的失败响应而非抛异常
     */
    @Override
    public UserFeignApi create(Throwable cause) {
        log.error("调用 service-provider 用户接口失败，触发降级", cause);

        return new UserFeignApi() {

            @Override
            public ApiResponse<UserDTO> getUser(Long userId) {
                log.warn("getUser 降级，userId={}", userId);
                return serviceUnavailable();
            }

            @Override
            public ApiResponse<List<UserDTO>> list() {
                log.warn("list 降级");
                return serviceUnavailable();
            }
        };
    }

    private static <T> ApiResponse<T> serviceUnavailable() {
        return ApiResponse.failure(ApiResponseEnum.SERVICE_UNAVAILABLE.code(), FALLBACK_MESSAGE);
    }
}
