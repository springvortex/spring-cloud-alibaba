package com.zjc.consumer.controller;

import com.zjc.common.api.user.UserFeignApi;
import com.zjc.common.dto.UserDTO;
import com.zjc.common.web.ApiResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * {@link UserConsumerController} 单元测试。
 *
 * <p>验证 consumer 通过 Feign API 远程调用的委托逻辑，
 * Controller 不做业务处理，只负责转发。
 *
 * @author jiancai.zhong
 */
@DisplayName("用户消费者 Controller")
@ExtendWith(MockitoExtension.class)
class UserConsumerControllerTest {

    /**
     * 共享 Feign API 的 Mock 实例。
     */
    @Mock
    private UserFeignApi userFeignApi;

    @InjectMocks
    private UserConsumerController controller;

    /**
     * 验证 getUser 将请求转发给 Feign API 并返回结果。
     */
    @Test
    @DisplayName("getUser: 委托给 Feign API")
    void testGetUserDelegates() {
        UserDTO dto = new UserDTO();
        dto.setUserId(1L);
        dto.setUsername("zhangsan");
        when(userFeignApi.getUser(1L)).thenReturn(ApiResponse.success(dto));

        ApiResponse<UserDTO> resp = controller.getUser(1L);

        assertThat(resp.isSuccess()).isTrue();
        assertThat(resp.getData().getUsername()).isEqualTo("zhangsan");
        verify(userFeignApi).getUser(1L);
    }

    /**
     * 验证 list 将请求转发给 Feign API 并返回列表结果。
     */
    @Test
    @DisplayName("list: 委托给 Feign API")
    void testListDelegates() {
        UserDTO dto = new UserDTO();
        dto.setUserId(1L);
        when(userFeignApi.list()).thenReturn(ApiResponse.success(List.of(dto)));

        ApiResponse<List<UserDTO>> resp = controller.list();

        assertThat(resp.getData()).hasSize(1);
        verify(userFeignApi).list();
    }
}
