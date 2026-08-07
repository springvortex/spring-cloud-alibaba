package com.zjc.common.api.test;


import com.zjc.common.web.ApiResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;

@FeignClient(value = "service-provider",contextId = "testFeignApi")
public interface TestApi {

    @GetMapping("/port")
    ApiResponse<String> getServerPort();
}
