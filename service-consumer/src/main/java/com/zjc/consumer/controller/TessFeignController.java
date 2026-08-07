package com.zjc.consumer.controller;

import com.zjc.common.web.ApiResponse;
import com.zjc.consumer.service.FeignService;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class TessFeignController {

    @Resource
    private FeignService feignService;

    @GetMapping("/feign/port")
    public ApiResponse<String> getServerPort() {
        return feignService.getServerPort();
    }
}
