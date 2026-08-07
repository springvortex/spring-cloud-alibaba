package com.zjc.consumer.feign.factory;

import com.zjc.common.web.ApiResponse;
import com.zjc.consumer.feign.UserFeignClient;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@link UserFeignFallbackFactory} 单元测试。
 *
 * <p>验证远程调用失败时降级逻辑返回正确的兜底数据：
 * 单个查询返回 null data，列表查询返回空列表。
 *
 * @author jiancai.zhong
 */
@DisplayName("用户 Feign 降级工厂")
class UserFeignFallbackFactoryTest {

    private final UserFeignFallbackFactory factory = new UserFeignFallbackFactory();

    /**
     * 验证 getUser 降级时返回成功响应，但 data 为 null。
     */
    @Test
    @DisplayName("getUser 降级: 返回成功响应但 data 为 null")
    void testFallbackGetUserReturnsNullData() {
        UserFeignClient fallback = factory.create(new RuntimeException("timeout"));

        ApiResponse<com.zjc.common.dto.UserDTO> resp = fallback.getUser(1L);

        assertThat(resp.isSuccess()).isTrue();
        assertThat(resp.getData()).isNull();
    }

    /**
     * 验证 list 降级时返回成功响应，data 为空列表（不返回 null 避免 NPE）。
     */
    @Test
    @DisplayName("list 降级: 返回成功响应但 data 为空列表")
    void testFallbackListReturnsEmptyList() {
        UserFeignClient fallback = factory.create(new RuntimeException("provider down"));

        ApiResponse<List<com.zjc.common.dto.UserDTO>> resp = fallback.list();

        assertThat(resp.isSuccess()).isTrue();
        assertThat(resp.getData()).isEmpty();
    }

    /**
     * 验证不同类型的异常（RuntimeException、Error）都能正常触发降级，不会抛出。
     */
    @Test
    @DisplayName("不同异常原因均能正常降级")
    void testFallbackVariousExceptions() {
        UserFeignClient fallback1 = factory.create(new IllegalStateException("conn refused"));
        UserFeignClient fallback2 = factory.create(new Error("OOM"));

        assertThat(fallback1.getUser(1L).isSuccess()).isTrue();
        assertThat(fallback2.list().isSuccess()).isTrue();
    }
}
