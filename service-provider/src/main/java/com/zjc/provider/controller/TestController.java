package com.zjc.provider.controller;

import com.zjc.common.web.ApiResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class TestController {

    @Value("${server.port}")
    private String port;

    @GetMapping("/port")
    public ApiResponse<String> getServerPort() {
        return ApiResponse.success(port);
    }
}
