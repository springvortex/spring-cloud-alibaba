package com.zjc.admin.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.lang.reflect.Field;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@link SecurityConfig} 单元测试。
 *
 * <p>验证密码编码器和内存用户详情服务的配置正确性。
 *
 * @author jiancai.zhong
 */
@DisplayName("SBA 安全配置")
class SecurityConfigTest {

    private SecurityConfig createConfig(String username, String password) throws Exception {
        SecurityConfig config = new SecurityConfig();
        Field nameField = SecurityConfig.class.getDeclaredField("username");
        nameField.setAccessible(true);
        nameField.set(config, username);
        Field pwdField = SecurityConfig.class.getDeclaredField("password");
        pwdField.setAccessible(true);
        pwdField.set(config, password);
        return config;
    }

    @Test
    @DisplayName("passwordEncoder: 返回 BCryptPasswordEncoder")
    void testPasswordEncoder() {
        SecurityConfig config = new SecurityConfig();
        PasswordEncoder encoder = config.passwordEncoder();

        assertThat(encoder).isInstanceOf(BCryptPasswordEncoder.class);
        assertThat(encoder.matches("test123", encoder.encode("test123"))).isTrue();
    }

    @Test
    @DisplayName("userDetailsService: 正确加载用户名和角色")
    void testUserDetailsService() throws Exception {
        SecurityConfig config = createConfig("admin", "secret123");
        PasswordEncoder encoder = config.passwordEncoder();
        UserDetailsService uds = config.userDetailsService(encoder);

        UserDetails user = uds.loadUserByUsername("admin");

        assertThat(user.getUsername()).isEqualTo("admin");
        assertThat(user.getAuthorities()).anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
    }

    @Test
    @DisplayName("userDetailsService: 密码经过 BCrypt 加密")
    void testPasswordIsEncoded() throws Exception {
        SecurityConfig config = createConfig("admin", "mypassword");
        PasswordEncoder encoder = config.passwordEncoder();
        UserDetailsService uds = config.userDetailsService(encoder);

        UserDetails user = uds.loadUserByUsername("admin");

        assertThat(encoder.matches("mypassword", user.getPassword())).isTrue();
        assertThat(user.getPassword()).isNotEqualTo("mypassword");
    }

    @Test
    @DisplayName("userDetailsService: 自定义用户名")
    void testCustomUsername() throws Exception {
        SecurityConfig config = createConfig("operator", "pwd");
        PasswordEncoder encoder = config.passwordEncoder();
        UserDetailsService uds = config.userDetailsService(encoder);

        UserDetails user = uds.loadUserByUsername("operator");

        assertThat(user.getUsername()).isEqualTo("operator");
    }
}
