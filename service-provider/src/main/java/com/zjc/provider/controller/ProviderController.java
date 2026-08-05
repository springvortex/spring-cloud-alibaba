package com.zjc.provider.controller;

import com.zjc.common.web.ApiResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ProviderController {
    @GetMapping(value = "/provide/version")
    public ApiResponse<String> info() {
        return ApiResponse.success("1.0.0");
    }
}