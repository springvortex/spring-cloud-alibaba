package com.zjc.common.constant;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@link ApiResponseEnum} 单元测试。
 *
 * <p>验证枚举值正确实现了 {@link ErrorCode} 接口，code 和 message 取值符合预期。
 *
 * @author jiancai.zhong
 */
@DisplayName("ApiResponseEnum 响应码枚举")
class ApiResponseEnumTest {

    @Test
    @DisplayName("所有枚举值均实现 ErrorCode 接口")
    void testAllImplementErrorCode() {
        for (ApiResponseEnum value : ApiResponseEnum.values()) {
            assertThat(value).isInstanceOf(ErrorCode.class);
        }
    }

    @Test
    @DisplayName("SUCCESS: code=0, message=操作成功")
    void testSuccess() {
        assertThat(ApiResponseEnum.SUCCESS.code()).isZero();
        assertThat(ApiResponseEnum.SUCCESS.message()).isEqualTo("操作成功");
    }

    @Test
    @DisplayName("FAILURE: code=-1")
    void testFailure() {
        assertThat(ApiResponseEnum.FAILURE.code()).isEqualTo(-1);
    }

    @Test
    @DisplayName("PARAM_INVALID: code=100")
    void testParamInvalid() {
        assertThat(ApiResponseEnum.PARAM_INVALID.code()).isEqualTo(100);
    }

    @Test
    @DisplayName("NOT_FOUND: code=102")
    void testNotFound() {
        assertThat(ApiResponseEnum.NOT_FOUND.code()).isEqualTo(102);
    }

    @Test
    @DisplayName("INTERNAL_ERROR: code=500")
    void testInternalError() {
        assertThat(ApiResponseEnum.INTERNAL_ERROR.code()).isEqualTo(500);
    }

    @Test
    @DisplayName("UNAUTHORIZED: code=401")
    void testUnauthorized() {
        assertThat(ApiResponseEnum.UNAUTHORIZED.code()).isEqualTo(401);
    }

    @Test
    @DisplayName("SERVICE_UNAVAILABLE: code=503")
    void testServiceUnavailable() {
        assertThat(ApiResponseEnum.SERVICE_UNAVAILABLE.code()).isEqualTo(503);
    }
}
