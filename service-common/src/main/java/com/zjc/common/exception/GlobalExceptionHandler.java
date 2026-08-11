package com.zjc.common.exception;

import com.zjc.common.constant.ApiResponseEnum;
import com.zjc.common.web.ApiResponse;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.stream.Collectors;

/**
 * 全局异常处理器，统一拦截各层抛出的异常并用 {@link ApiResponse} 包装返回。
 *
 * <p>异常处理优先级：业务异常 > 参数校验异常 > 请求异常 > 兜底异常。
 * 所有异常均会记录日志，便于排查。
 *
 * @author jiancai.zhong
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 业务异常：透传异常自身的错误码与提示信息。
     *
     * @param e 业务异常
     * @return 失败响应，携带异常自身的错误码与提示信息
     */
    @ExceptionHandler(BusinessException.class)
    public ApiResponse<Void> handleBusinessException(BusinessException e) {
        log.warn("业务异常: code={}, message={}", e.getCode(), e.getMessage());
        return ApiResponse.failure(e.getCode(), e.getMessage());
    }

    /**
     * {@code @RequestBody} 校验失败（{@code @Valid} + {@code @RequestBody}）。
     *
     * @param e 参数校验异常
     * @return 失败响应，提示具体校验不通过的字段
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ApiResponse<Void> handleMethodArgumentNotValid(MethodArgumentNotValidException e) {
        String message = e.getBindingResult().getFieldErrors().stream()
                .map(this::formatFieldError)
                .collect(Collectors.joining("; "));
        log.warn("参数校验失败: {}", message);
        return ApiResponse.failure(ApiResponseEnum.PARAM_INVALID.code(), message);
    }

    /**
     * 表单参数校验失败（{@code @Validated} + 对象绑定）。
     *
     * @param e 参数绑定异常
     * @return 失败响应，提示具体校验不通过的字段
     */
    @ExceptionHandler(BindException.class)
    public ApiResponse<Void> handleBindException(BindException e) {
        String message = e.getBindingResult().getFieldErrors().stream()
                .map(this::formatFieldError)
                .collect(Collectors.joining("; "));
        log.warn("参数绑定失败: {}", message);
        return ApiResponse.failure(ApiResponseEnum.PARAM_INVALID.code(), message);
    }

    /**
     * {@code @RequestParam} / {@code @PathVariable} 校验失败（{@code @Validated} + 约束注解）。
     *
     * @param e 约束校验异常
     * @return 失败响应，提示具体校验不通过的约束信息
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public ApiResponse<Void> handleConstraintViolation(ConstraintViolationException e) {
        String message = e.getConstraintViolations().stream()
                .map(ConstraintViolation::getMessage)
                .collect(Collectors.joining("; "));
        log.warn("约束校验失败: {}", message);
        return ApiResponse.failure(ApiResponseEnum.PARAM_INVALID.code(), message);
    }

    /**
     * 缺少必填的 {@code @RequestParam}。
     *
     * @param e 缺少参数异常
     * @return 失败响应，提示缺少的参数名
     */
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ApiResponse<Void> handleMissingParam(MissingServletRequestParameterException e) {
        String message = "缺少必填参数: " + e.getParameterName();
        log.warn(message);
        return ApiResponse.failure(ApiResponseEnum.PARAM_INVALID.code(), message);
    }

    /**
     * 请求体无法解析（JSON 格式错误或缺失）。
     *
     * <p>异常消息非空时透传 {@code e.getMessage()}（如 JSON 解析失败的具体位置），
     * 消息为空时回退到默认提示。
     *
     * @param e 消息解析异常
     * @return 失败响应，提示请求体格式错误
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ApiResponse<Void> handleNotReadable(HttpMessageNotReadableException e) {
        log.warn("请求体解析失败: {}", e.getMessage());
        String message = e.getMessage() != null ? e.getMessage() : ApiResponseEnum.BAD_REQUEST.message();
        return ApiResponse.failure(ApiResponseEnum.BAD_REQUEST.code(), message);
    }

    /**
     * 请求路径不存在。
     *
     * @param e 资源未找到异常
     * @return 失败响应，提示资源不存在
     */
    @ExceptionHandler(NoResourceFoundException.class)
    public ApiResponse<Void> handleNoResourceFound(NoResourceFoundException e) {
        // 浏览器自动请求 favicon.ico，属于正常行为，不记录日志
        if ("favicon.ico".equals(e.getResourcePath())) {
            return ApiResponse.failure(ApiResponseEnum.NOT_FOUND);
        }
        log.warn("请求路径不存在: {}", e.getResourcePath());
        return ApiResponse.failure(ApiResponseEnum.NOT_FOUND);
    }

    /**
     * 请求方法不支持（如 POST 访问了 GET 接口）。
     *
     * @param e 方法不支持异常
     * @return 失败响应，提示不支持的请求方法
     */
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ApiResponse<Void> handleMethodNotSupported(HttpRequestMethodNotSupportedException e) {
        log.warn("不支持的请求方法: {}", e.getMethod());
        return ApiResponse.failureMessage("不支持的请求方法: " + e.getMethod());
    }

    /**
     * 兜底：未预期的异常，防止堆栈泄露给前端。
     *
     * <p>异常消息非空时透传 {@code e.getMessage()}（如 {@code / by zero}），
     * 消息为空时（如裸 {@code NullPointerException}）回退到默认提示，
     * 避免前端拿到一个空洞的 {@code null}。完整堆栈仍会记录到日志中。
     *
     * @param e 未预期异常
     * @return 失败响应，携带异常原因或默认提示
     */
    @ExceptionHandler(Exception.class)
    public ApiResponse<Void> handleException(Exception e) {
        log.error("未预期异常", e);
        String message = e.getMessage() != null ? e.getMessage() : ApiResponseEnum.INTERNAL_ERROR.message();
        return ApiResponse.failure(ApiResponseEnum.INTERNAL_ERROR.code(), message);
    }

    /**
     * 格式化字段校验错误为 "字段名: 错误信息"。
     *
     * @param fieldError 字段校验错误
     * @return 格式化后的错误描述
     */
    private String formatFieldError(FieldError fieldError) {
        return fieldError.getField() + ": " + fieldError.getDefaultMessage();
    }
}
