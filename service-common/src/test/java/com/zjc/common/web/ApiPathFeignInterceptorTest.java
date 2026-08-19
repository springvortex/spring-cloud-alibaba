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
    void testDefaultVersionUsesTargetPrefix() throws NoSuchMethodException {
        RequestTemplate template = requestTemplate(TestApi.class, "one", "service-provider");

        interceptor().apply(template);

        assertThat(template.path()).isEqualTo("/api/v1/provider/resource");
    }

    @Test
    @DisplayName("Feign 方法版本覆盖默认版本")
    void testMethodVersionOverridesDefault() throws NoSuchMethodException {
        RequestTemplate template = requestTemplate(TestApi.class, "two", "service-provider");

        interceptor().apply(template);

        assertThat(template.path()).isEqualTo("/api/v2/provider/resource");
    }

    @Test
    @DisplayName("已经携带标准前缀的路径不会重复追加")
    void testExistingPrefixIsNotDuplicated() throws NoSuchMethodException {
        RequestTemplate template = requestTemplate(TestApi.class, "one", "service-provider");
        template.uri("/api/v1/provider/resource");

        interceptor().apply(template);

        assertThat(template.path()).isEqualTo("/api/v1/provider/resource");
    }

    @Test
    @DisplayName("追加前缀时保留查询参数")
    void testQueryParametersArePreserved() throws NoSuchMethodException {
        RequestTemplate template = requestTemplate(TestApi.class, "one", "service-provider");
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

    private RequestTemplate requestTemplate(Class<?> apiType, String methodName,
                                            String serviceName) throws NoSuchMethodException {
        Method method = Arrays.stream(apiType.getMethods())
                .filter(candidate -> candidate.getName().equals(methodName))
                .findFirst()
                .orElseThrow();
        MethodMetadata metadata = new SpringMvcContract().parseAndValidateMetadata(apiType, method);
        RequestTemplate template = new RequestTemplate().uri("/resource");
        template.methodMetadata(metadata);
        template.feignTarget(new Target.HardCodedTarget<>(apiType, serviceName));
        return template;
    }

    interface TestApi {

        @GetMapping("/resource")
        String one();

        @GetMapping("/resource")
        @ApiVersion("v2")
        String two();
    }
}
