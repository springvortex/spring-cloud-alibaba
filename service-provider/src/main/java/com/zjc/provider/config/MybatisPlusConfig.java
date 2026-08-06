package com.zjc.provider.config;

import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * MyBatis-Plus 配置。
 *
 * <p>注册分页等内置拦截器。分页查询依赖 {@link PaginationInnerInterceptor}，
 * 它需要 JSqlParser 来改写 SQL（追加 limit），所以 pom 里需引入
 * mybatis-plus-jsqlparser 依赖（3.5.9 起从 mybatis-plus 拆分）。
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