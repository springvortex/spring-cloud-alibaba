package com.zjc.common.web;

import com.zjc.common.web.annotation.ApiVersion;
import feign.MethodMetadata;
import feign.RequestInterceptor;
import feign.RequestTemplate;
import feign.Target;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.cloud.openfeign.support.SpringMvcContract;
import org.springframework.web.bind.annotation.GetMapping;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Feign standardized path interceptor tests.
 *
 * @author jiancai.zhong
 */
@DisplayName("Feign 标准 API 路径")
class ApiPathFeignInterceptorTest {

    private final ApiPathAutoConfiguration configuration = new ApiPathAutoConfiguration();

    @Test
    @DisplayName("默认调用使用目标服务的 v1 标准前缀")
    void testDefaultVersionUsesTargetPrefix() {
        RequestTemplate template = requestTemplate("one");

        interceptor().apply(template);

        assertThat(template.path()).isEqualTo("/api/v1/provider/resource");
    }

    @Test
    @DisplayName("Feign 方法版本覆盖默认版本")
    void testMethodVersionOverridesDefault() {
        RequestTemplate template = requestTemplate("two");

        interceptor().apply(template);

        assertThat(template.path()).isEqualTo("/api/v2/provider/resource");
    }

    @Test
    @DisplayName("已经携带标准前缀的路径不会重复追加")
    void testExistingPrefixIsNotDuplicated() {
        RequestTemplate template = requestTemplate("one");
        template.uri("/api/v1/provider/resource");

        interceptor().apply(template);

        assertThat(template.path()).isEqualTo("/api/v1/provider/resource");
    }

    @Test
    @DisplayName("追加前缀时保留查询参数")
    void testQueryParametersArePreserved() {
        RequestTemplate template = requestTemplate("one");
        template.query("type", "all");

        interceptor().apply(template);

        assertThat(template.path()).isEqualTo("/api/v1/provider/resource");
        assertThat(template.queryLine()).isEqualTo("?type=all");
    }

    private RequestInterceptor interceptor() {
        ApiPathProperties properties = new ApiPathProperties();
        properties.setVersions(List.of("v1", "v2"));
        properties.setDefaultVersion("v1");
        return configuration.standardApiPathFeignInterceptor(properties);
    }

    private RequestTemplate requestTemplate(String methodName) {
        Method method = Arrays.stream(TestApi.class.getMethods())
                .filter(candidate -> candidate.getName().equals(methodName))
                .findFirst()
                .orElseThrow();
        MethodMetadata metadata = new SpringMvcContract().parseAndValidateMetadata(TestApi.class, method);
        RequestTemplate template = new RequestTemplate().uri("/resource");
        template.methodMetadata(metadata);
        template.feignTarget(new Target.HardCodedTarget<>((Class<?>) TestApi.class, "service-provider"));
        return template;
    }

    /**
     * 用于解析 Feign 方法路径和版本信息的测试接口。
     *
     * @author jiancai.zhong
     */
    interface TestApi {

        @GetMapping("/resource")
        String one();

        @GetMapping("/resource")
        @ApiVersion("v2")
        String two();
    }
}
