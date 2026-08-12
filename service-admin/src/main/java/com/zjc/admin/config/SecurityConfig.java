package com.zjc.admin.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;

/**
 * SBA 面板安全配置。
 *
 * <p>所有页面（SBA UI、Swagger 等）都需要登录认证，仅 Actuator 端点免认证（SBA Server 需要采集自身监控数据）。
 * CSRF 全局禁用，因为 SBA 前端（Angular SPA）通过 REST 操作日志级别等，CSRF Token 会导致操作失败。
 *
 * <p>登录凭证通过环境变量注入，不写入配置文件：
 * <ul>
 *   <li>{@code SBA_USERNAME}：登录用户名（默认 admin）</li>
 *   <li>{@code SBA_PASSWORD}：登录密码（必填，不设则启动报错）</li>
 * </ul>
 *
 * @author jiancai.zhong
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Value("${spring.security.user.name:admin}")
    private String username;

    @Value("${spring.security.user.password}")
    private String password;

    /**
     * 安全过滤链配置。
     *
     * <p>规则：
     * <ul>
     *   <li>{@code /actuator/**}：免认证（SBA Server 自监控）</li>
     *   <li>其余所有路径：需要登录</li>
     * </ul>
     *
     * @param http HttpSecurity 构建器
     * @return 配置好的安全过滤链
     */
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) {
        http
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/actuator/**").permitAll()
                        .anyRequest().authenticated()
                )
                .formLogin(Customizer.withDefaults())
                .logout(logout -> logout
                        .logoutSuccessUrl("/login")
                        .permitAll()
                )
                .csrf(AbstractHttpConfigurer::disable)
                .rememberMe(remember -> remember.key("sba-remember-me"));
        return http.build();
    }

    /**
     * BCrypt 密码编码器。
     *
     * @return BCrypt 编码器实例
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * 内存用户详情服务，从环境变量读取凭证。
     *
     * @param encoder 密码编码器
     * @return 内存用户管理器
     */
    @Bean
    public UserDetailsService userDetailsService(PasswordEncoder encoder) {
        UserDetails user = User.builder()
                .username(username)
                .password(encoder.encode(password))
                .roles("ADMIN")
                .build();
        return new InMemoryUserDetailsManager(user);
    }
}
