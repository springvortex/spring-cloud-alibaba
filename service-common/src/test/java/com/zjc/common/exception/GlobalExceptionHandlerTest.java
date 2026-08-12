package com.zjc.common.exception;

import com.zjc.common.constant.ApiResponseEnum;
import com.zjc.common.web.ApiResponse;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.MethodParameter;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * {@link GlobalExceptionHandler} 单元测试。
 *
 * <p>覆盖所有异常处理器分支，验证错误码、提示信息和日志行为。
 *
 * @author jiancai.zhong
 */
@DisplayName("全局异常处理器")
@ExtendWith(MockitoExtension.class)
class GlobalExceptionHandlerTest {

    @InjectMocks
    private GlobalExceptionHandler handler;

    // ==================== BusinessException ====================

    @Nested
    @DisplayName("业务异常")
    class BusinessExceptionTests {

        @Test
        @DisplayName("透传异常自身的错误码与提示信息")
        void testHandleBusinessException() {
            BusinessException e = new BusinessException(10001, "用户不存在");

            ApiResponse<Void> resp = handler.handleBusinessException(e);

            assertThat(resp.isSuccess()).isFalse();
            assertThat(resp.getCode()).isEqualTo(10001);
            assertThat(resp.getMessage()).isEqualTo("用户不存在");
        }

        @Test
        @DisplayName("默认错误码 -1 的业务异常")
        void testHandleBusinessExceptionDefaultCode() {
            BusinessException e = new BusinessException("自定义提示");

            ApiResponse<Void> resp = handler.handleBusinessException(e);

            assertThat(resp.getCode()).isEqualTo(-1);
            assertThat(resp.getMessage()).isEqualTo("自定义提示");
        }
    }

    // ==================== MethodArgumentNotValidException ====================

    @Nested
    @DisplayName("@RequestBody 校验失败")
    class MethodArgumentNotValidTests {

        @Test
        @DisplayName("拼接多个字段校验错误")
        void testHandleMethodArgumentNotValidMultipleErrors() {
            BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(new Object(), "dto");
            bindingResult.addError(new FieldError("dto", "username", "不能为空"));
            bindingResult.addError(new FieldError("dto", "email", "格式不正确"));
            MethodArgumentNotValidException e = new MethodArgumentNotValidException(
                    mock(MethodParameter.class), bindingResult);

            ApiResponse<Void> resp = handler.handleMethodArgumentNotValid(e);

            assertThat(resp.isSuccess()).isFalse();
            assertThat(resp.getCode()).isEqualTo(ApiResponseEnum.PARAM_INVALID.code());
            assertThat(resp.getMessage()).contains("username: 不能为空");
            assertThat(resp.getMessage()).contains("email: 格式不正确");
        }
    }

    // ==================== BindException ====================

    @Nested
    @DisplayName("表单参数校验失败")
    class BindExceptionTests {

        @Test
        @DisplayName("拼接字段校验错误")
        void testHandleBindException() {
            BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(new Object(), "form");
            bindingResult.addError(new FieldError("form", "name", "长度需在3-20个字符之间"));
            BindException e = new BindException(bindingResult);

            ApiResponse<Void> resp = handler.handleBindException(e);

            assertThat(resp.getCode()).isEqualTo(ApiResponseEnum.PARAM_INVALID.code());
            assertThat(resp.getMessage()).contains("name: 长度需在3-20个字符之间");
        }
    }

    // ==================== ConstraintViolationException ====================

    @Nested
    @DisplayName("约束校验失败")
    class ConstraintViolationTests {

        @Test
        @DisplayName("拼接约束校验消息")
        void testHandleConstraintViolation() {
            @SuppressWarnings("unchecked")
            ConstraintViolation<String> violation = mock(ConstraintViolation.class);
            when(violation.getMessage()).thenReturn("不能为空");
            ConstraintViolationException e = new ConstraintViolationException(Set.of(violation));

            ApiResponse<Void> resp = handler.handleConstraintViolation(e);

            assertThat(resp.getCode()).isEqualTo(ApiResponseEnum.PARAM_INVALID.code());
            assertThat(resp.getMessage()).contains("不能为空");
        }
    }

    // ==================== MissingServletRequestParameterException ====================

    @Test
    @DisplayName("缺少必填参数: 返回参数名")
    void testHandleMissingParam() {
        MissingServletRequestParameterException e = new MissingServletRequestParameterException(
                "userId", "Long");

        ApiResponse<Void> resp = handler.handleMissingParam(e);

        assertThat(resp.getCode()).isEqualTo(ApiResponseEnum.PARAM_INVALID.code());
        assertThat(resp.getMessage()).contains("userId");
    }

    // ==================== HttpMessageNotReadableException ====================

    @Nested
    @DisplayName("请求体解析失败")
    class HttpMessageNotReadableTests {

        @Test
        @DisplayName("getMessage 非空时透传原因")
        void testHandleNotReadableWithMessage() {
            HttpMessageNotReadableException e = new HttpMessageNotReadableException(
                    "JSON parse error at line 1", (org.springframework.http.HttpInputMessage) null);

            ApiResponse<Void> resp = handler.handleNotReadable(e);

            assertThat(resp.getCode()).isEqualTo(ApiResponseEnum.BAD_REQUEST.code());
            assertThat(resp.getMessage()).contains("JSON parse error");
        }
    }

    // ==================== NoResourceFoundException ====================

    @Nested
    @DisplayName("资源不存在")
    class NoResourceFoundTests {

        @Test
        @DisplayName("favicon.ico 不记日志，返回 NOT_FOUND")
        void testHandleFavicon() {
            NoResourceFoundException e = mock(NoResourceFoundException.class);
            when(e.getResourcePath()).thenReturn("favicon.ico");

            ApiResponse<Void> resp = handler.handleNoResourceFound(e);

            assertThat(resp.getCode()).isEqualTo(ApiResponseEnum.NOT_FOUND.code());
        }

        @Test
        @DisplayName("其他路径返回 NOT_FOUND")
        void testHandleOtherPath() {
            NoResourceFoundException e = mock(NoResourceFoundException.class);
            when(e.getResourcePath()).thenReturn("/api/unknown");

            ApiResponse<Void> resp = handler.handleNoResourceFound(e);

            assertThat(resp.getCode()).isEqualTo(ApiResponseEnum.NOT_FOUND.code());
        }
    }

    // ==================== HttpRequestMethodNotSupportedException ====================

    @Test
    @DisplayName("不支持的请求方法")
    void testHandleMethodNotSupported() {
        HttpRequestMethodNotSupportedException e = new HttpRequestMethodNotSupportedException("DELETE");

        ApiResponse<Void> resp = handler.handleMethodNotSupported(e);

        assertThat(resp.isSuccess()).isFalse();
        assertThat(resp.getMessage()).contains("DELETE");
    }

    // ==================== 兜底 Exception ====================

    @Nested
    @DisplayName("兜底异常")
    class GenericExceptionTests {

        @Test
        @DisplayName("getMessage 非空时透传异常原因")
        void testHandleExceptionWithMessage() {
            Exception e = new ArithmeticException("/ by zero");

            ApiResponse<Void> resp = handler.handleException(e);

            assertThat(resp.getCode()).isEqualTo(ApiResponseEnum.INTERNAL_ERROR.code());
            assertThat(resp.getMessage()).isEqualTo("/ by zero");
        }

        @Test
        @DisplayName("getMessage 为空时回退默认提示")
        void testHandleExceptionNullMessage() {
            Exception e = new NullPointerException();

            ApiResponse<Void> resp = handler.handleException(e);

            assertThat(resp.getCode()).isEqualTo(ApiResponseEnum.INTERNAL_ERROR.code());
            assertThat(resp.getMessage()).isEqualTo(ApiResponseEnum.INTERNAL_ERROR.message());
        }
    }
}
