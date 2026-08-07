package com.zjc.provider.controller;

import com.zjc.common.web.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
public class TestController {

    @Value("${server.port}")
    private String port;

    @GetMapping("/port")
    public ApiResponse<String> getServerPort() {
        log.info("port: {}", port);
        return ApiResponse.success(port);
    }
}
