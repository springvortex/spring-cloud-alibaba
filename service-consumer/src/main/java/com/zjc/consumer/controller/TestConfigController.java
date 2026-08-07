package com.zjc.consumer.controller;

import com.zjc.common.web.ApiResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Nacos 配置中心动态刷新测试接口。
 *
 * <p>{@code @RefreshScope} 使本 Bean 的属性在 Nacos 配置变更后自动重新注入，
 * 无需重启服务即可生效。{@code @Value} 注入的值来自 Nacos 下发的配置，
 * 冒号后为本地兜底默认值（Nacos 中不存在该 key 时使用）。
 *
 * <p>用于验证 Nacos 配置中心的读取与热更新能力。
 *
 * @author jiancai.zhong
 */
@RestController
@RefreshScope
public class TestConfigController {

    /**
     * 从 Nacos 读取 {@code demo.msg}，缺省值为 {@code default}
     */
    @Value("${demo.msg:default}")
    private String msg;

    /**
     * 从 Nacos 读取 {@code pub.name}，缺省值为 {@code zhangSan}
     */
    @Value("${pub.name:zhangSan}")
    private String pub;

    /**
     * 返回两个配置项的拼接结果，用于在浏览器直观验证配置是否生效与刷新。
     *
     * @return {@code msg:pub} 拼接字符串
     */
    @GetMapping("/config")
    public ApiResponse<String> getMsg() {
        return ApiResponse.success(msg + ":" + pub);
    }
}
