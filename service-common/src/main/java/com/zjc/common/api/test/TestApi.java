package com.zjc.common.api.test;


import com.zjc.common.web.ApiResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * 测试用共享 Feign 客户端，远程调用 service-provider 的 {@code /port} 接口。
 *
 * <p>放在 common 模块的 {@code api} 包下，目的是让所有依赖 common 的服务
 * 都能直接注入并复用这个 Feign 声明，无需各自重复定义。
 * 这也是 common 模块承载"跨服务共享 API 契约"这一职责的体现。
 *
 * <p>{@code contextId = "testFeignApi"} 用于在 OpenFeign 配置中隔离本客户端的
 * 超时、拦截器等子配置，避免与指向同一服务的其他客户端（如 consumer 里的
 * {@code userFeignClient}）产生配置冲突。
 *
 * @author jiancai.zhong
 */
@FeignClient(value = "service-provider", contextId = "testFeignApi")
public interface TestApi {

    /**
     * 获取 service-provider 的服务端口，用于验证 Feign 调用链路是否通畅。
     *
     * @return provider 实例的端口号字符串
     */
    @GetMapping("/port")
    ApiResponse<String> getServerPort();
}
