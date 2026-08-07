package com.zjc.provider.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zjc.common.dto.UserDTO;
import com.zjc.common.web.ApiResponse;
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
import static org.mockito.Mockito.*;

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

    @InjectMocks
    private UserController userController;

    /**
     * 验证根据 ID 查询用户时，Service 返回的数据能正确转换为 DTO。
     */
    @Test
    @DisplayName("getUser: 返回单个用户 DTO")
    void testGetUserReturnsDto() {
        User user = new User();
        user.setUserId(1L);
        user.setUsername("zhangsan");
        user.setNickName("张三");
        when(userService.getById(1L)).thenReturn(user);

        ApiResponse<UserDTO> resp = userController.getUser(1L);

        assertThat(resp.isSuccess()).isTrue();
        assertThat(resp.getData().getUserId()).isEqualTo(1L);
        assertThat(resp.getData().getUsername()).isEqualTo("zhangsan");
        verify(userService).getById(1L);
    }

    /**
     * 验证查询不存在的用户 ID 时，返回成功响应但 data 为 null。
     */
    @Test
    @DisplayName("getUser: 用户不存在时返回 null data")
    void testGetUserNotFound() {
        when(userService.getById(999L)).thenReturn(null);

        ApiResponse<UserDTO> resp = userController.getUser(999L);

        assertThat(resp.isSuccess()).isTrue();
        assertThat(resp.getData()).isNull();
    }

    /**
     * 验证列表查询返回的每个 Entity 都被正确转换为 DTO。
     */
    @Test
    @DisplayName("list: 返回用户 DTO 列表")
    void testListReturnsDtoList() {
        User user = new User();
        user.setUserId(1L);
        user.setUsername("zhangsan");
        when(userService.list()).thenReturn(List.of(user));

        ApiResponse<List<UserDTO>> resp = userController.list();

        assertThat(resp.isSuccess()).isTrue();
        assertThat(resp.getData()).hasSize(1);
        assertThat(resp.getData().get(0).getUsername()).isEqualTo("zhangsan");
    }

    /**
     * 验证数据库无数据时返回空列表而非 null。
     */
    @Test
    @DisplayName("list: 无数据时返回空列表")
    void testListEmpty() {
        when(userService.list()).thenReturn(Collections.emptyList());

        ApiResponse<List<UserDTO>> resp = userController.list();

        assertThat(resp.getData()).isEmpty();
    }

    /**
     * 验证分页查询时 total 和 records 都正确映射到 DTO 页。
     */
    @Test
    @DisplayName("page: 分页查询返回 DTO 页")
    void testPageReturnsDtoPage() {
        Page<User> page = new Page<>(1, 10, 1);
        User user = new User();
        user.setUserId(1L);
        user.setUsername("zhangsan");
        page.setRecords(List.of(user));
        when(userService.page(any(Page.class))).thenReturn(page);

        ApiResponse<Page<UserDTO>> resp = userController.page(1, 10);

        assertThat(resp.isSuccess()).isTrue();
        assertThat(resp.getData().getTotal()).isEqualTo(1);
        assertThat(resp.getData().getRecords()).hasSize(1);
        assertThat(resp.getData().getRecords().get(0).getUserId()).isEqualTo(1L);
    }

    /**
     * 验证新增用户时 DTO 正确复制到 Entity 并调用 save，返回回填后的 DTO。
     */
    @Test
    @DisplayName("add: 新增用户后返回 DTO")
    void testAddReturnsDto() {
        UserDTO dto = new UserDTO();
        dto.setUsername("lisi");
        dto.setNickName("李四");
        when(userService.save(any(User.class))).thenReturn(true);

        ApiResponse<UserDTO> resp = userController.add(dto);

        assertThat(resp.isSuccess()).isTrue();
        assertThat(resp.getData().getUsername()).isEqualTo("lisi");
        verify(userService).save(any(User.class));
    }

    /**
     * 验证修改用户时 DTO 正确复制到 Entity 并调用 updateById。
     */
    @Test
    @DisplayName("update: 修改用户")
    void testUpdateSuccess() {
        UserDTO dto = new UserDTO();
        dto.setUserId(1L);
        dto.setUsername("updated");
        when(userService.updateById(any(User.class))).thenReturn(true);

        ApiResponse<Void> resp = userController.update(dto);

        assertThat(resp.isSuccess()).isTrue();
        verify(userService).updateById(any(User.class));
    }

    /**
     * 验证删除用户时正确调用 removeById 实现逻辑删除。
     */
    @Test
    @DisplayName("delete: 逻辑删除用户")
    void testDeleteSuccess() {
        when(userService.removeById(1L)).thenReturn(true);

        ApiResponse<Void> resp = userController.delete(1L);

        assertThat(resp.isSuccess()).isTrue();
        verify(userService).removeById(1L);
    }
}
