package com.zjc.common.web;

import com.zjc.common.constant.ApiResponseEnum;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@link ApiResponse} 统一响应封装的单元测试。
 *
 * <p>覆盖所有静态工厂方法、Builder 链式构建、isSuccess 判断、timestamp 自动生成，
 * 确保成功/失败两条主链路的每个分支都被验证。
 *
 * @author jiancai.zhong
 */
@DisplayName("ApiResponse 统一响应封装")
class ApiResponseTest {

    @Nested
    @DisplayName("成功响应")
    class SuccessTests {

        /**
         * 验证无参 success() 返回标准成功响应，data 为 null。
         */
        @Test
        @DisplayName("success(): 无数据成功响应")
        void testSuccessNoData() {
            ApiResponse<Void> resp = ApiResponse.success();
            assertThat(resp.isSuccess()).isTrue();
            assertThat(resp.getCode()).isEqualTo(ApiResponseEnum.SUCCESS.code());
            assertThat(resp.getMessage()).isEqualTo(ApiResponseEnum.SUCCESS.message());
            assertThat(resp.getData()).isNull();
            assertThat(resp.getTimestamp()).isNotNull();
        }

        /**
         * 验证 success(data) 能正确携带业务数据。
         */
        @Test
        @DisplayName("success(data): 携带数据成功响应")
        void testSuccessWithData() {
            ApiResponse<String> resp = ApiResponse.success("hello");
            assertThat(resp.isSuccess()).isTrue();
            assertThat(resp.getData()).isEqualTo("hello");
        }

        /**
         * 验证 successMessage(message) 只设置提示文案，不携带数据。
         */
        @Test
        @DisplayName("successMessage(message): 自定义提示，无数据")
        void testSuccessMessage() {
            ApiResponse<Void> resp = ApiResponse.successMessage("done");
            assertThat(resp.isSuccess()).isTrue();
            assertThat(resp.getMessage()).isEqualTo("done");
            assertThat(resp.getData()).isNull();
        }

        /**
         * 验证 success(message, data) 同时设置提示信息和业务数据。
         */
        @Test
        @DisplayName("success(message, data): 自定义提示 + 数据")
        void testSuccessMessageAndData() {
            ApiResponse<Integer> resp = ApiResponse.success("ok", 42);
            assertThat(resp.isSuccess()).isTrue();
            assertThat(resp.getMessage()).isEqualTo("ok");
            assertThat(resp.getData()).isEqualTo(42);
        }
    }

    @Nested
    @DisplayName("失败响应")
    class FailureTests {

        /**
         * 验证无参 failure() 返回默认失败响应（code=-1）。
         */
        @Test
        @DisplayName("failure(): 默认失败")
        void testFailureDefault() {
            ApiResponse<Void> resp = ApiResponse.failure();
            assertThat(resp.isSuccess()).isFalse();
            assertThat(resp.getCode()).isEqualTo(ApiResponseEnum.FAILURE.code());
            assertThat(resp.getMessage()).isEqualTo(ApiResponseEnum.FAILURE.message());
        }

        /**
         * 验证 failure(data) 能在失败响应中携带附属数据。
         */
        @Test
        @DisplayName("failure(data): 携带数据的失败")
        void testFailureWithData() {
            ApiResponse<String> resp = ApiResponse.failure("error detail");
            assertThat(resp.isSuccess()).isFalse();
            assertThat(resp.getData()).isEqualTo("error detail");
        }

        /**
         * 验证 failureMessage(message) 自定义错误提示信息。
         */
        @Test
        @DisplayName("failureMessage(message): 自定义错误信息")
        void testFailureMessage() {
            ApiResponse<Void> resp = ApiResponse.failureMessage("param invalid");
            assertThat(resp.isSuccess()).isFalse();
            assertThat(resp.getMessage()).isEqualTo("param invalid");
        }

        /**
         * 验证 failure(code, message) 同时自定义错误码和提示信息。
         */
        @Test
        @DisplayName("failure(code, message): 自定义错误码 + 信息")
        void testFailureCodeAndMessage() {
            ApiResponse<Void> resp = ApiResponse.failure(404, "not found");
            assertThat(resp.isSuccess()).isFalse();
            assertThat(resp.getCode()).isEqualTo(404);
            assertThat(resp.getMessage()).isEqualTo("not found");
        }

        /**
         * 验证通过 {@link ApiResponseEnum} 枚举构建失败响应，code/message 取自枚举。
         */
        @Test
        @DisplayName("failure(ApiResponseEnum): 枚举构建失败响应")
        void testFailureEnum() {
            ApiResponse<Void> resp = ApiResponse.failure(ApiResponseEnum.UNAUTHORIZED);
            assertThat(resp.isSuccess()).isFalse();
            assertThat(resp.getCode()).isEqualTo(ApiResponseEnum.UNAUTHORIZED.code());
            assertThat(resp.getMessage()).isEqualTo(ApiResponseEnum.UNAUTHORIZED.message());
        }
    }

    @Nested
    @DisplayName("Builder 链式构建")
    class BuilderTests {

        /**
         * 验证 ok() 快捷方法能初始化为标准成功配置，再设置 data。
         */
        @Test
        @DisplayName("ok() + data(): 构建成功响应")
        void testBuilderOk() {
            ApiResponse<String> resp = ApiResponse.<String>builder()
                    .ok()
                    .data("built")
                    .build();
            assertThat(resp.isSuccess()).isTrue();
            assertThat(resp.getCode()).isEqualTo(ApiResponseEnum.SUCCESS.code());
            assertThat(resp.getData()).isEqualTo("built");
        }

        /**
         * 验证 fail() 快捷方法能初始化为标准失败配置。
         */
        @Test
        @DisplayName("fail(): 构建失败响应")
        void testBuilderFail() {
            ApiResponse<Void> resp = ApiResponse.<Void>builder()
                    .fail()
                    .build();
            assertThat(resp.isSuccess()).isFalse();
            assertThat(resp.getCode()).isEqualTo(ApiResponseEnum.FAILURE.code());
        }

        /**
         * 验证手动设置全部字段时 Builder 能正确传递。
         */
        @Test
        @DisplayName("手动设置全部字段")
        void testBuilderCustom() {
            ApiResponse<String> resp = ApiResponse.<String>builder()
                    .success(false)
                    .code(500)
                    .message("server error")
                    .data("ctx")
                    .build();
            assertThat(resp.isSuccess()).isFalse();
            assertThat(resp.getCode()).isEqualTo(500);
            assertThat(resp.getMessage()).isEqualTo("server error");
            assertThat(resp.getData()).isEqualTo("ctx");
        }

        /**
         * 验证仅设置 data 时，code/message 保持字段默认值（SUCCESS 枚举）。
         */
        @Test
        @DisplayName("仅设置 data，code/message 回退到默认值")
        void testBuilderPartial() {
            ApiResponse<String> resp = ApiResponse.<String>builder()
                    .data("only data")
                    .build();
            // 未调 success() 前默认 success=null → isSuccess() 返回 false
            assertThat(resp.isSuccess()).isFalse();
            // code/message 未设置时保持字段默认值（SUCCESS 枚举）
            assertThat(resp.getCode()).isEqualTo(ApiResponseEnum.SUCCESS.code());
            assertThat(resp.getData()).isEqualTo("only data");
        }
    }

    /**
     * 验证 success=null 时 isSuccess() 返回 false，不会因拆箱抛 NPE。
     */
    @Test
    @DisplayName("isSuccess(): success=null 时返回 false")
    void testIsSuccessNullSafety() {
        ApiResponse<Void> resp = new ApiResponse<>();
        resp.setSuccess(null);
        assertThat(resp.isSuccess()).isFalse();
    }

    /**
     * 验证 timestamp 在创建时自动生成，且取值在合理的时间区间内。
     */
    @Test
    @DisplayName("timestamp: 创建时自动生成")
    void testTimestampAutoGenerated() {
        long before = System.currentTimeMillis();
        ApiResponse<Void> resp = ApiResponse.success();
        long after = System.currentTimeMillis();
        assertThat(resp.getTimestamp()).isBetween(before, after);
    }
}
