package com.zjc.common.constant;

/**
 * 接口响应码与提示信息标准枚举，供统一响应封装 {@code ApiResponse} 与
 * {@code BusinessException} 使用。
 *
 * <p>实现了 {@link ErrorCode}，允许直接传入 {@code BusinessException} 构造。
 *
 * <p><b>码段规划：</b>
 * <ul>
 *   <li>{@code 0} — 成功</li>
 *   <li>{@code -1} — 通用失败（未分类）</li>
 *   <li>{@code 1xx} — 参数校验类</li>
 *   <li>{@code 4xx} — 认证授权类（与 HTTP 语义对齐）</li>
 *   <li>{@code 5xx} — 服务端异常类（与 HTTP 语义对齐）</li>
 * </ul>
 *
 * @author jiancai.zhong
 */
public enum ApiResponseEnum implements ErrorCode {

    /**
     * 请求成功。
     */
    SUCCESS(0, "操作成功"),

    /**
     * 通用失败（未分类的错误）。
     */
    FAILURE(-1, "操作失败"),

    /**
     * 参数非法（校验不通过）。
     */
    PARAM_INVALID(100, "参数非法"),

    /**
     * 请求体缺失或无法解析。
     */
    BAD_REQUEST(101, "请求体格式错误"),

    /**
     * 资源不存在。
     */
    NOT_FOUND(102, "资源不存在"),

    /**
     * 数据冲突（如唯一约束冲突）。
     */
    CONFLICT(103, "数据冲突"),

    /**
     * 未认证（未登录或 token 无效）。
     */
    UNAUTHORIZED(401, "未认证"),

    /**
     * 无权限访问。
     */
    FORBIDDEN(403, "无权限"),

    /**
     * 服务内部错误。
     */
    INTERNAL_ERROR(500, "服务内部错误"),

    /**
     * 服务不可用（降级或维护中）。
     */
    SERVICE_UNAVAILABLE(503, "服务不可用");

    private final int code;
    private final String message;

    ApiResponseEnum(int code, String message) {
        this.code = code;
        this.message = message;
    }

    @Override
    public int code() {
        return code;
    }

    @Override
    public String message() {
        return message;
    }
}
