package com.zjc.provider.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zjc.common.dto.UserDTO;
import com.zjc.common.web.ApiResponse;
import com.zjc.provider.converter.UserConverter;
import com.zjc.provider.entity.User;
import com.zjc.provider.service.UserService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * {@link UserController} 单元测试。
 *
 * <p>使用纯 Mockito mock 掉 Service 层，验证 Controller 的 CRUD 逻辑
 * 以及 Entity → DTO 转换是否正确，不依赖 Spring 上下文和数据库。
 *
 * @author jiancai.zhong
 */
@DisplayName("用户管理 Controller")
@ExtendWith(MockitoExtension.class)
class UserControllerTest {

    @Mock
    private UserService userService;

    @Mock
    private UserConverter userConverter;

    @InjectMocks
    private UserController userController;

    @Test
    @DisplayName("getUser: 返回单个用户 DTO")
    void testGetUserReturnsDto() {
        User user = new User();
        user.setUserId(1L);
        user.setUsername("zhangsan");
        UserDTO dto = new UserDTO();
        dto.setUserId(1L);
        dto.setUsername("zhangsan");
        when(userService.getById(1L)).thenReturn(user);
        when(userConverter.entityToDto(user)).thenReturn(dto);

        ApiResponse<UserDTO> resp = userController.getUser(1L);

        assertThat(resp.isSuccess()).isTrue();
        assertThat(resp.getData().getUserId()).isEqualTo(1L);
        assertThat(resp.getData().getUsername()).isEqualTo("zhangsan");
        verify(userService).getById(1L);
    }

    @Test
    @DisplayName("getUser: 用户不存在时返回 null data")
    void testGetUserNotFound() {
        when(userService.getById(999L)).thenReturn(null);
        when(userConverter.entityToDto(null)).thenReturn(null);

        ApiResponse<UserDTO> resp = userController.getUser(999L);

        assertThat(resp.isSuccess()).isTrue();
        assertThat(resp.getData()).isNull();
    }

    @Test
    @DisplayName("list: 返回用户 DTO 列表")
    void testListReturnsDtoList() {
        User user = new User();
        user.setUserId(1L);
        user.setUsername("zhangsan");
        UserDTO dto = new UserDTO();
        dto.setUserId(1L);
        dto.setUsername("zhangsan");
        when(userService.list()).thenReturn(List.of(user));
        when(userConverter.entityListToDtoList(List.of(user))).thenReturn(List.of(dto));

        ApiResponse<List<UserDTO>> resp = userController.list();

        assertThat(resp.isSuccess()).isTrue();
        assertThat(resp.getData()).hasSize(1);
        assertThat(resp.getData().get(0).getUsername()).isEqualTo("zhangsan");
    }

    @Test
    @DisplayName("list: 无数据时返回空列表")
    void testListEmpty() {
        when(userService.list()).thenReturn(Collections.emptyList());
        when(userConverter.entityListToDtoList(Collections.emptyList())).thenReturn(Collections.emptyList());

        ApiResponse<List<UserDTO>> resp = userController.list();

        assertThat(resp.getData()).isEmpty();
    }

    @Test
    @DisplayName("page: 分页查询返回 DTO 页")
    void testPageReturnsDtoPage() {
        Page<User> page = new Page<>(1, 10, 1);
        User user = new User();
        user.setUserId(1L);
        user.setUsername("zhangsan");
        page.setRecords(List.of(user));
        UserDTO dto = new UserDTO();
        dto.setUserId(1L);
        when(userService.page(any(Page.class))).thenReturn(page);
        when(userConverter.entityListToDtoList(List.of(user))).thenReturn(List.of(dto));

        ApiResponse<Page<UserDTO>> resp = userController.page(1, 10);

        assertThat(resp.isSuccess()).isTrue();
        assertThat(resp.getData().getTotal()).isEqualTo(1);
        assertThat(resp.getData().getRecords()).hasSize(1);
        assertThat(resp.getData().getRecords().get(0).getUserId()).isEqualTo(1L);
    }

    @Test
    @DisplayName("add: 新增用户后返回 DTO")
    void testAddReturnsDto() {
        UserDTO dto = new UserDTO();
        dto.setUsername("lisi");
        User entity = new User();
        entity.setUsername("lisi");
        UserDTO resultDto = new UserDTO();
        resultDto.setUsername("lisi");
        when(userConverter.dtoToEntity(dto)).thenReturn(entity);
        when(userConverter.entityToDto(entity)).thenReturn(resultDto);
        when(userService.save(any(User.class))).thenReturn(true);

        ApiResponse<UserDTO> resp = userController.add(dto);

        assertThat(resp.isSuccess()).isTrue();
        assertThat(resp.getData().getUsername()).isEqualTo("lisi");
        verify(userService).save(any(User.class));
    }

    @Test
    @DisplayName("update: 修改用户")
    void testUpdateSuccess() {
        UserDTO dto = new UserDTO();
        dto.setUserId(1L);
        dto.setUsername("updated");
        User entity = new User();
        entity.setUserId(1L);
        when(userConverter.dtoToEntity(dto)).thenReturn(entity);
        when(userService.updateById(any(User.class))).thenReturn(true);

        ApiResponse<Void> resp = userController.update(dto);

        assertThat(resp.isSuccess()).isTrue();
        verify(userService).updateById(any(User.class));
    }

    @Test
    @DisplayName("delete: 逻辑删除用户")
    void testDeleteSuccess() {
        when(userService.removeById(1L)).thenReturn(true);

        ApiResponse<Void> resp = userController.delete(1L);

        assertThat(resp.isSuccess()).isTrue();
        verify(userService).removeById(1L);
    }
}
