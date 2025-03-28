package com.tencent.supersonic.config;

import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author JiangWeiWei
 */
@Configuration
public class MyBatisPlusConfig {

    /**
     * 分页插件配置
     *
     * @return
     */
    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor(){

        // 初始化 MybatisPlusInterceptor 核心插件
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
        // 添加自动分页插件 PaginationInnerInterceptor
        interceptor.addInnerInterceptor(new PaginationInnerInterceptor());
        // 返回
        return interceptor;
    }
}
