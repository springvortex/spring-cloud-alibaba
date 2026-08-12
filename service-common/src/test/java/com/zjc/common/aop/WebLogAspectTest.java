package com.zjc.common.aop;

import com.zjc.common.web.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.Signature;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * {@link WebLogAspect} 单元测试。
 *
 * <p>通过 Mock ProceedingJoinPoint 验证正常返回、异常传播和入参格式化逻辑。
 *
 * @author jiancai.zhong
 */
@DisplayName("Web 接口日志切面")
@ExtendWith(MockitoExtension.class)
class WebLogAspectTest {

    @Mock
    private ProceedingJoinPoint joinPoint;

    @InjectMocks
    private WebLogAspect aspect;

    /**
     * 模拟 HTTP 请求上下文。
     */
    @BeforeEach
    void setUpRequestContext() {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/test/123");
        ServletRequestAttributes attrs = new ServletRequestAttributes(request);
        RequestContextHolder.setRequestAttributes(attrs);
    }

    @AfterEach
    void clearRequestContext() {
        RequestContextHolder.resetRequestAttributes();
    }

    @Test
    @DisplayName("正常返回: 返回目标方法结果")
    void testLogAroundNormalReturn() throws Throwable {
        ApiResponse<String> expectedResult = ApiResponse.success("ok");
        Signature sig = mock(Signature.class);
        when(sig.getName()).thenReturn("testMethod");
        when(joinPoint.getTarget()).thenReturn(new Object());
        when(joinPoint.getSignature()).thenReturn(sig);
        when(joinPoint.getArgs()).thenReturn(new Object[]{"param1", 123});
        when(joinPoint.proceed()).thenReturn(expectedResult);

        Object result = aspect.logAround(joinPoint);

        assertThat(result).isSameAs(expectedResult);
    }

    @Test
    @DisplayName("异常传播: 目标方法抛出的异常原样传递")
    void testLogAroundExceptionPropagation() throws Throwable {
        RuntimeException expected = new RuntimeException("boom");
        Signature sig = mock(Signature.class);
        when(sig.getName()).thenReturn("errorMethod");
        when(joinPoint.getTarget()).thenReturn(new Object());
        when(joinPoint.getSignature()).thenReturn(sig);
        when(joinPoint.getArgs()).thenReturn(new Object[]{});
        when(joinPoint.proceed()).thenThrow(expected);

        assertThatThrownBy(() -> aspect.logAround(joinPoint))
                .isSameAs(expected);
    }

    @Test
    @DisplayName("空入参数组: 格式化为 []")
    void testLogAroundEmptyArgs() throws Throwable {
        Signature sig = mock(Signature.class);
        when(sig.getName()).thenReturn("noArgs");
        when(joinPoint.getTarget()).thenReturn(new Object());
        when(joinPoint.getSignature()).thenReturn(sig);
        when(joinPoint.getArgs()).thenReturn(new Object[]{});
        when(joinPoint.proceed()).thenReturn("result");

        Object result = aspect.logAround(joinPoint);

        assertThat(result).isEqualTo("result");
    }

    @Test
    @DisplayName("null 入参数组: 不抛异常")
    void testLogAroundNullArgs() throws Throwable {
        Signature sig = mock(Signature.class);
        when(sig.getName()).thenReturn("nullArgs");
        when(joinPoint.getTarget()).thenReturn(new Object());
        when(joinPoint.getSignature()).thenReturn(sig);
        when(joinPoint.getArgs()).thenReturn(null);
        when(joinPoint.proceed()).thenReturn(null);

        Object result = aspect.logAround(joinPoint);

        assertThat(result).isNull();
    }

    @Test
    @DisplayName("非序列化入参(HttpServletRequest): 仅记录类型名")
    void testLogAroundWithServletRequestArg() throws Throwable {
        Signature sig = mock(Signature.class);
        when(sig.getName()).thenReturn("withRequest");
        when(joinPoint.getTarget()).thenReturn(new Object());
        when(joinPoint.getSignature()).thenReturn(sig);

        HttpServletRequest servletRequest = new MockHttpServletRequest();
        when(joinPoint.getArgs()).thenReturn(new Object[]{servletRequest});
        when(joinPoint.proceed()).thenReturn("ok");

        Object result = aspect.logAround(joinPoint);

        assertThat(result).isEqualTo("ok");
    }
}
