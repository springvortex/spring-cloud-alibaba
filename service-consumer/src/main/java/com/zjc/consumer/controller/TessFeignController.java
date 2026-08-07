package com.zjc.consumer.controller;

import com.zjc.common.web.ApiResponse;
import com.zjc.consumer.service.FeignService;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Feign 调用测试接口。
 *
 * <p>调用链路：浏览器 → 本 Controller → {@link FeignService}
 * → common 模块的 {@link com.zjc.common.api.test.TestApi}（Feign 代理）
 * → service-provider 的 {@code TestController}。
 *
 * <p>用于验证 consumer 能否通过 Feign 正常远程调用 provider，
 * 是微服务链路连通性最基础的冒烟测试入口。
 *
 * @author jiancai.zhong
 */
@RestController
public class TessFeignController {

    @Resource
    private FeignService feignService;

    /**
     * 远程获取 service-provider 的端口号。
     *
     * @return provider 实例端口，封装在统一响应体中
     */
    @GetMapping("/feign/port")
    public ApiResponse<String> getServerPort() {
        return feignService.getServerPort();
    }
}
