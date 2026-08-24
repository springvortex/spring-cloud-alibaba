package com.zjc.common.api.goods;

import com.zjc.common.dto.GoodsPurchaseRequestDTO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;

import java.lang.reflect.Method;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@link GoodsFeignApi} 接口契约测试。
 *
 * @author jiancai.zhong
 */
@DisplayName("商品 Feign 契约")
class GoodsFeignApiTest {

    /**
     * 验证购买接口显式声明 JSON 请求类型，避免并发首次调用时编码歧义。
     *
     * @throws NoSuchMethodException 测试方法不存在时失败
     */
    @Test
    @DisplayName("purchase: 显式声明 JSON 请求类型")
    void purchaseShouldDeclareJsonContentType() throws NoSuchMethodException {
        Method purchase = GoodsFeignApi.class.getDeclaredMethod(
                "purchase", Long.class, GoodsPurchaseRequestDTO.class);
        PostMapping mapping = purchase.getAnnotation(PostMapping.class);

        assertThat(mapping).isNotNull();
        assertThat(mapping.value()).containsExactly("/goods/{id}/purchase");
        assertThat(mapping.consumes()).containsExactly(MediaType.APPLICATION_JSON_VALUE);
    }
}
