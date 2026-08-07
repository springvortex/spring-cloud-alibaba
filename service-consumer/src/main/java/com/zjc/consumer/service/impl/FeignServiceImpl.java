package com.zjc.consumer.service.impl;

import com.zjc.common.api.test.TestApi;
import com.zjc.common.web.ApiResponse;
import com.zjc.consumer.service.FeignService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

@Service
public class FeignServiceImpl implements FeignService {

    @Resource
    private TestApi testApi;

    @Override
    public ApiResponse<String> getServerPort() {
        return testApi.getServerPort();
    }
}
