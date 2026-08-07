package com.zjc.consumer.service;

import com.zjc.common.web.ApiResponse;

public interface FeignService {

    ApiResponse<String> getServerPort();
}
