package com.zjc.common.exception;

import com.zjc.common.constant.ApiResponseEnum;
import com.zjc.common.constant.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@link BusinessException} 单元测试。
 *
 * <p>覆盖三种构造方式及错误码/消息透传行为。
 *
 * @author jiancai.zhong
 */
@DisplayName("业务异常")
class BusinessExceptionTest {

    /**
     * 基于 {@link com.zjc.common.constant.ErrorCode} 构造异常的测试。
     *
     * @author jiancai.zhong
     */
    @Nested
    @DisplayName("ErrorCode 枚举构造")
    class ErrorCodeConstructorTests {

        @Test
        @DisplayName("透传 ApiResponseEnum 的 code 和 message")
        void testFromApiResponseEnum() {
            BusinessException e = new BusinessException(ApiResponseEnum.NOT_FOUND);

            assertThat(e.getCode()).isEqualTo(ApiResponseEnum.NOT_FOUND.code());
            assertThat(e.getMessage()).isEqualTo(ApiResponseEnum.NOT_FOUND.message());
        }

        @Test
        @DisplayName("透传自定义 ErrorCode 枚举的 code 和 message")
        void testFromCustomErrorCode() {
            ErrorCode custom = new ErrorCode() {
                @Override
                public int code() {
                    return 20001;
                }

                @Override
                public String message() {
                    return "自定义错误";
                }
            };
            BusinessException e = new BusinessException(custom);

            assertThat(e.getCode()).isEqualTo(20001);
            assertThat(e.getMessage()).isEqualTo("自定义错误");
        }
    }

    @Test
    @DisplayName("自定义提示信息，错误码默认 -1")
    void testMessageOnly() {
        BusinessException e = new BusinessException("操作不允许");

        assertThat(e.getCode()).isEqualTo(-1);
        assertThat(e.getMessage()).isEqualTo("操作不允许");
    }

    @Test
    @DisplayName("自定义错误码 + 提示信息")
    void testCodeAndMessage() {
        BusinessException e = new BusinessException(50001, "库存不足");

        assertThat(e.getCode()).isEqualTo(50001);
        assertThat(e.getMessage()).isEqualTo("库存不足");
    }

    @Test
    @DisplayName("继承 RuntimeException，可被 throw/catch(RuntimeException)")
    void testIsRuntimeException() {
        BusinessException e = new BusinessException("test");

        assertThat(e).isInstanceOf(RuntimeException.class);
    }
}
