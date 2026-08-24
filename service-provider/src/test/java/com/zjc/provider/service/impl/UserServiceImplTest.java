package com.zjc.provider.service.impl;

import com.zjc.common.dto.UserDTO;
import com.zjc.provider.converter.UserConverter;
import com.zjc.provider.entity.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * {@link UserServiceImpl} 详情缓存入口测试。
 *
 * @author jiancai.zhong
 */
@DisplayName("用户详情缓存服务")
@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

    @Mock
    private UserConverter userConverter;

    @Spy
    @InjectMocks
    private UserServiceImpl userService;

    @Test
    @DisplayName("getUser: 查询实体并转换为 DTO")
    void testGetUserConvertsEntity() {
        User user = new User();
        user.setUserId(1L);
        UserDTO dto = new UserDTO();
        dto.setUserId(1L);
        doReturn(user).when(userService).getById(1L);
        when(userConverter.entityToDto(user)).thenReturn(dto);

        assertThat(userService.getUser(1L)).isSameAs(dto);
        verify(userService).getById(1L);
    }

    @Test
    @DisplayName("updateUser / deleteUser: 复用 MyBatis-Plus 写操作")
    void testWriteMethodsDelegateToMybatisPlus() {
        User user = new User();
        user.setUserId(1L);
        doReturn(true).when(userService).updateById(user);
        doReturn(true).when(userService).removeById(1L);

        assertThat(userService.updateUser(user)).isTrue();
        assertThat(userService.deleteUser(1L)).isTrue();
    }

    @Test
    @DisplayName("缓存配置: 详情缓存，写操作按 ID 驱逐")
    void testCacheAnnotations() throws NoSuchMethodException {
        Cacheable cacheable = UserServiceImpl.class
                .getMethod("getUser", Long.class)
                .getAnnotation(Cacheable.class);
        CacheEvict update = UserServiceImpl.class
                .getMethod("updateUser", User.class)
                .getAnnotation(CacheEvict.class);
        CacheEvict delete = UserServiceImpl.class
                .getMethod("deleteUser", Long.class)
                .getAnnotation(CacheEvict.class);

        assertThat(cacheable).isNotNull();
        assertThat(cacheable.cacheNames()).containsExactly("provider:user:id");
        assertThat(cacheable.key()).isEqualTo("#userId");
        assertThat(update.cacheNames()).containsExactly("provider:user:id");
        assertThat(update.key()).isEqualTo("#user.userId");
        assertThat(delete.cacheNames()).containsExactly("provider:user:id");
        assertThat(delete.key()).isEqualTo("#userId");
    }
}
