package com.zjc.consumer.controller;

import com.zjc.common.web.ApiResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RefreshScope
public class TestConfigController {

    @Value("${demo.msg:default}")
    private String msg;

    @Value("${pub.name:zhangSan}")
    private String pub;

    @GetMapping("/config")
    public ApiResponse<String> getMsg() {
        return ApiResponse.success(msg + ":" + pub);
    }
}