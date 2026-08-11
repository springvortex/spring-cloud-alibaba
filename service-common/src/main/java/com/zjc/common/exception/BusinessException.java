package com.zjc.common.exception;

import com.zjc.common.constant.ErrorCode;
import lombok.Getter;

/**
 * 业务异常，供 Service / Controller 层抛出，由全局异常处理器统一拦截。
 *
 * <p>配合 {@link ErrorCode} 体系使用，错误码与提示信息由枚举统一管理：
 * <pre>{@code
 * throw new BusinessException(ApiResponseEnum.USER_NOT_FOUND);
 * throw new BusinessException(UserErrorCode.USER_DISABLED);
 * throw new BusinessException("自定义提示");
 * throw new BusinessException(10001, "用户不存在");
 * }</pre>
 *
 * @author jiancai.zhong
 */
@Getter
public class BusinessException extends RuntimeException {

    /**
     * 错误码
     */
    private final int code;

    /**
     * 使用 {@link ErrorCode} 枚举构造（推荐）。
     *
     * @param errorCode 错误码枚举
     */
    public BusinessException(ErrorCode errorCode) {
        super(errorCode.message());
        this.code = errorCode.code();
    }

    /**
     * 自定义提示信息，错误码默认 {@code -1}。
     *
     * @param message 错误提示
     */
    public BusinessException(String message) {
        super(message);
        this.code = -1;
    }

    /**
     * 自定义错误码 + 提示信息。
     *
     * @param code    错误码
     * @param message 错误提示
     */
    public BusinessException(int code, String message) {
        super(message);
        this.code = code;
    }

}
