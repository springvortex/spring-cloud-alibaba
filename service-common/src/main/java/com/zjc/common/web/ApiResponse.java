package com.zjc.common.web;

import com.zjc.common.constant.ApiResponseEnum;
import com.zjc.common.constant.ErrorCode;
import lombok.Getter;
import lombok.Setter;

import java.io.Serial;
import java.io.Serializable;

/**
 * 全局统一接口响应封装，REST Controller 默认返回实体。
 *
 * <p>统一响应结构为 {@code success + code + message + data + timestamp}，
 * 前端只需判断 {@code success} 即可分支处理，{@code code} 供细粒度错误区分。
 * 默认使用 {@link ApiResponseEnum} 的标准编码，也支持自定义 {@link ErrorCode}。
 *
 * <p><b>使用示例：</b>
 * <pre>{@code
 * // 成功
 * ApiResponse<User> ok = ApiResponse.success(user);
 * ApiResponse<Void> ok2 = ApiResponse.successMessage("操作成功");
 *
 * // 失败
 * ApiResponse<Void> fail = ApiResponse.failureMessage("参数非法");
 *
 * // 链式构建
 * ApiResponse<User> resp = ApiResponse.<User>builder()
 *         .ok()
 *         .data(user)
 *         .build();
 * }</pre>
 *
 * @param <T> 响应数据泛型
 * @author jiancai.zhong
 */
public class ApiResponse<T> implements Serializable {

    @Serial
    private static final long serialVersionUID = 1492116327070318294L;

    /**
     * 是否请求成功
     *
     * <p>只生成 setter，读取统一走 {@link #isSuccess()}，避免 Boolean 为空时拆箱异常。
     */
    @Setter
    protected Boolean success;

    /**
     * 响应码
     */
    @Getter
    @Setter
    private Integer code = ApiResponseEnum.SUCCESS.code();

    /**
     * 响应提示信息
     */
    @Getter
    @Setter
    private String message = ApiResponseEnum.SUCCESS.message();

    /**
     * 业务返回数据
     */
    @Getter
    @Setter
    private T data;

    /**
     * 响应生成时间戳（毫秒），实例创建固定，不可外部修改
     */
    @Getter
    private final Long timestamp = System.currentTimeMillis();

    /**
     * 判断当前响应是否为成功状态。
     *
     * @return {@code success} 字段为 {@code true} 时返回 {@code true}
     */
    public boolean isSuccess() {
        return Boolean.TRUE.equals(success);
    }

    /**
     * 成功响应，无数据
     */
    public static <T> ApiResponse<T> success() {
        ApiResponse<T> response = new ApiResponse<>();
        response.setSuccess(true);
        return response;
    }

    /**
     * 成功响应，携带返回数据
     *
     * @param data 返回数据
     */
    public static <T> ApiResponse<T> success(T data) {
        ApiResponse<T> response = new ApiResponse<>();
        response.setSuccess(true);
        response.setData(data);
        return response;
    }

    /**
     * 成功响应，自定义提示信息，无数据
     *
     * <p>方法名显式带 {@code Message}，避免与 {@link #success(Object)} 在 {@code T=String} 时
     * 产生重载歧义：若命名为 {@code success(String)}，调用 {@code success("x")} 会被绑定到
     * 本方法，字符串被当作提示信息而非业务数据。
     *
     * @param message 提示文案
     */
    public static <T> ApiResponse<T> successMessage(String message) {
        ApiResponse<T> response = new ApiResponse<>();
        response.setSuccess(true);
        response.setMessage(message);
        return response;
    }

    /**
     * 成功响应，自定义提示 + 返回数据
     *
     * @param message 提示文案
     * @param data    返回数据
     */
    public static <T> ApiResponse<T> success(String message, T data) {
        ApiResponse<T> response = new ApiResponse<>();
        response.setSuccess(true);
        response.setMessage(message);
        response.setData(data);
        return response;
    }

    /**
     * 默认失败响应
     */
    public static <T> ApiResponse<T> failure() {
        return failure(ApiResponseEnum.FAILURE);
    }

    /**
     * 默认失败响应，附带自定义数据
     *
     * @param data 错误附属数据
     */
    public static <T> ApiResponse<T> failure(T data) {
        ApiResponse<T> response = baseFailure();
        response.setData(data);
        return response;
    }

    /**
     * 默认失败响应，自定义错误信息
     *
     * <p>方法名显式带 {@code Message}，避免与 {@link #failure(Object)} 在 {@code T=String} 时
     * 产生重载歧义。
     *
     * @param message 错误提示
     */
    public static <T> ApiResponse<T> failureMessage(String message) {
        ApiResponse<T> response = baseFailure();
        response.setMessage(message);
        return response;
    }

    /**
     * 自定义错误码 + 错误信息
     *
     * @param code    响应码
     * @param message 错误信息
     */
    public static <T> ApiResponse<T> failure(Integer code, String message) {
        ApiResponse<T> response = baseFailure();
        response.setCode(code);
        response.setMessage(message);
        return response;
    }

    /**
     * 使用枚举构建失败响应（推荐业务异常使用）
     *
     * @param responseEnum 响应枚举
     */
    public static <T> ApiResponse<T> failure(ApiResponseEnum responseEnum) {
        ApiResponse<T> response = baseFailure();
        response.setCode(responseEnum.code());
        response.setMessage(responseEnum.message());
        return response;
    }

    private static <T> ApiResponse<T> baseFailure() {
        ApiResponse<T> apiResponse = new ApiResponse<>();
        apiResponse.setSuccess(false);
        apiResponse.setCode(ApiResponseEnum.FAILURE.code());
        apiResponse.setMessage(ApiResponseEnum.FAILURE.message());
        return apiResponse;
    }

    /**
     * 获取构建器入口
     */
    public static <T> Builder<T> builder() {
        return new Builder<>();
    }

    public static final class Builder<T> {
        private boolean success;
        private Integer code;
        private String message;
        private T data;

        /**
         * 快速初始化【成功】标准配置
         * success=true，code=SUCCESS枚举编码
         */
        public Builder<T> ok() {
            this.success = true;
            this.code = ApiResponseEnum.SUCCESS.code();
            this.message = ApiResponseEnum.SUCCESS.message();
            return this;
        }

        /**
         * 快速初始化【失败】标准配置
         * success=false，code=FAILURE枚举编码
         */
        public Builder<T> fail() {
            this.success = false;
            this.code = ApiResponseEnum.FAILURE.code();
            this.message = ApiResponseEnum.FAILURE.message();
            return this;
        }

        /**
         * 设置成功标志。
         *
         * @param success 是否成功
         * @return 当前构建器
         */
        public Builder<T> success(boolean success) {
            this.success = success;
            return this;
        }

        /**
         * 设置响应码。
         *
         * @param code 响应码
         * @return 当前构建器
         */
        public Builder<T> code(Integer code) {
            this.code = code;
            return this;
        }

        /**
         * 设置提示信息。
         *
         * @param message 提示信息
         * @return 当前构建器
         */
        public Builder<T> message(String message) {
            this.message = message;
            return this;
        }

        /**
         * 设置业务数据。
         *
         * @param data 业务数据
         * @return 当前构建器
         */
        public Builder<T> data(T data) {
            this.data = data;
            return this;
        }

        /**
         * 构建最终响应对象
         */
        public ApiResponse<T> build() {
            ApiResponse<T> response = new ApiResponse<>();
            response.setSuccess(success);
            if (code != null) {
                response.setCode(code);
            }
            if (message != null) {
                response.setMessage(message);
            }
            response.setData(data);
            return response;
        }
    }
}
