package com.qw.desensitize.config;

import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.OptimisticLockerInnerInterceptor;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * mybatis plus 配置
 *
 * @author alvin
 */
@Configuration
@MapperScan("com.qw.desensitize.mapper")
public class MybatisPlusConfig {


    @Bean
    public MybatisPlusInterceptor paginationInterceptor() {
        /*
         * mybatis plus 分页插件拦截器
         * 攻击 SQL 阻断解析器
         */
        MybatisPlusInterceptor mybatisPlusInterceptor = new MybatisPlusInterceptor();
        mybatisPlusInterceptor.addInnerInterceptor(new OptimisticLockerInnerInterceptor());

        return mybatisPlusInterceptor;
    }
}