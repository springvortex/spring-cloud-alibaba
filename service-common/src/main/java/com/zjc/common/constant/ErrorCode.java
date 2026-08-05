package com.zjc.common.constant;

/**
 * 错误码契约接口。
 *
 * <p>业务方可让自定义枚举实现本接口，从而与 {@code ApiResponseEnum} 统一接入
 * {@code BusinessException} 与全局异常处理体系。只需保证 {@code code} 在全局范围内唯一即可。
 *
 * <pre>{@code
 * public enum UserErrorCode implements ErrorCode {
 *     USER_NOT_FOUND(10001, "用户不存在"),
 *     USER_DISABLED(10002, "用户已禁用");
 *
 *     @Override
 *     public int code() { return code; }
 *     @Override
 *     public String message() { return message; }
 * }
 * }</pre>
 *
 * @author jiancai.zhong
 */
public interface ErrorCode {

    /**
     * @return 错误码，全局唯一
     */
    int code();

    /**
     * @return 错误提示信息
     */
    String message();
}
