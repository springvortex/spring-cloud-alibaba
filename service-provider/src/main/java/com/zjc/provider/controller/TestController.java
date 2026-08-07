package com.zjc.provider.controller;

import com.zjc.common.web.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 服务连通性测试接口。
 *
 * <p>返回当前实例的端口号，供 consumer 通过 Feign 远程调用验证链路是否通畅。
 * 对应 common 模块 {@link com.zjc.common.api.test.TestApi#getServerPort()} 的服务端实现。
 *
 * @author jiancai.zhong
 */
@Slf4j
@RestController
public class TestController {

    @Value("${server.port}")
    private String port;

    /**
     * 返回当前实例的监听端口。
     *
     * @return 端口号字符串，封装在统一响应体中
     */
    @GetMapping("/port")
    public ApiResponse<String> getServerPort() {
        log.info("port: {}", port);
        return ApiResponse.success(port);
    }
}
