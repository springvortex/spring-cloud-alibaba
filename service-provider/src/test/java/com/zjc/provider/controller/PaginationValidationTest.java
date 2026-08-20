package com.zjc.provider.controller;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.validation.annotation.Validated;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 分页参数约束测试。
 */
@DisplayName("分页参数校验")
class PaginationValidationTest {

    @Test
    @DisplayName("page: current 和 size 只能取受限范围")
    void testPaginationParameterConstraints() throws NoSuchMethodException {
        assertPaginationParameters(UserController.class);
        assertPaginationParameters(GoodsController.class);
        assertPaginationParameters(OrderController.class);
    }

    private void assertPaginationParameters(Class<?> controllerClass) throws NoSuchMethodException {
        assertThat(controllerClass.getAnnotation(Validated.class)).isNotNull();

        Method method = controllerClass.getMethod("page", long.class, long.class);
        Parameter current = method.getParameters()[0];
        Parameter size = method.getParameters()[1];

        assertThat(current.getAnnotation(Min.class))
                .isNotNull()
                .extracting(Min::value)
                .isEqualTo(1L);

        assertThat(size.getAnnotation(Min.class))
                .isNotNull()
                .extracting(Min::value)
                .isEqualTo(1L);
        assertThat(size.getAnnotation(Max.class))
                .isNotNull()
                .extracting(Max::value)
                .isEqualTo(100L);
    }
}
