package com.zjc.consumer.service.impl;

import com.zjc.common.api.test.TestApi;
import com.zjc.common.web.ApiResponse;
import com.zjc.consumer.service.FeignService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

/**
 * {@link FeignService} 的实现类。
 *
 * <p>注入 common 模块共享的 {@link TestApi} Feign 代理，
 * 将远程调用委托给它，Controller 层只需面向 {@link FeignService} 接口编程。
 *
 * @author jiancai.zhong
 */
@Service
public class FeignServiceImpl implements FeignService {

    @Resource
    private TestApi testApi;

    /**
     * {@inheritDoc}
     *
     * <p>实际委托给 {@link TestApi#getServerPort()} 发起远程调用。
     */
    @Override
    public ApiResponse<String> getServerPort() {
        return testApi.getServerPort();
    }
}
