package com.zjc.mail.config;

import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * MyBatis-Plus 配置。
 *
 * <p>注册分页等内置拦截器。分页查询依赖 {@link PaginationInnerInterceptor}。
 *
 * @author jiancai.zhong
 */
@Configuration
public class MybatisPlusConfig {

    /**
     * 分页拦截器，指定数据库类型为 MySQL。
     *
     * @return 配置好分页内部拦截器的 MybatisPlusInterceptor
     */
    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor() {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
        interceptor.addInnerInterceptor(new PaginationInnerInterceptor(DbType.MYSQL));
        return interceptor;
    }
}
