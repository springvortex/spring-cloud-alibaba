package com.zjc.consumer.service;

import com.zjc.common.web.ApiResponse;

/**
 * Feign 远程调用服务接口。
 *
 * <p>对 Controller 层屏蔽具体的 Feign 客户端细节，
 * 由 {@link com.zjc.consumer.service.impl.FeignServiceImpl} 负责实现，
 * 内部委托 common 模块的 {@link com.zjc.common.api.test.TestApi} 发起远程调用。
 *
 * @author jiancai.zhong
 */
public interface FeignService {

    /**
     * 远程获取 service-provider 的服务端口。
     *
     * @return provider 实例端口号，封装在统一响应体中
     */
    ApiResponse<String> getServerPort();
}
