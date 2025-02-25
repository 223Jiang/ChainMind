package com.larkmt.cn.admin.config;


import com.larkmt.cn.admin.filter.JWTAuthenticationFilter;
import com.larkmt.cn.admin.filter.JWTAuthorizationFilter;
import com.larkmt.cn.admin.service.impl.UserDetailsServiceImpl;
import com.larkmt.core.util.Constants;
import org.springframework.context.annotation.Bean;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import javax.annotation.Resource;

/**
 * @Author: LarkMidTable
 * @Date: 2020/9/16 11:14
 * @Description:
 **/
@EnableWebSecurity
@EnableGlobalMethodSecurity(prePostEnabled = true)
public class SecurityConfig extends WebSecurityConfigurerAdapter {

    @Resource
    private UserDetailsService userDetailsService;

    @Bean
    UserDetailsService customUserService() { //注册UserDetailsService 的bean
        return new UserDetailsServiceImpl();
    }


    @Bean
    public BCryptPasswordEncoder bCryptPasswordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Override
    protected void configure(AuthenticationManagerBuilder auth) throws Exception {
        auth.userDetailsService(userDetailsService).passwordEncoder(bCryptPasswordEncoder());
    }

    /**
     * 配置HttpSecurity以定义请求的授权规则和安全设置
     * 此方法优先处理跨域请求配置和禁用CSRF防护，因为这些设置对整个应用的安全性至关重要
     * 接着，它定义了哪些请求路径是公开访问的，哪些需要身份验证，以及如何处理会话
     *
     * @param http HttpSecurity实例，用于配置Web安全
     * @throws Exception 配置过程中可能抛出的异常
     */
    @Override
    protected void configure(HttpSecurity http) throws Exception {
        // 配置跨域请求和禁用CSRF，适合于大多数现代的、无状态的API
        http.cors().and().csrf().disable()
                // 定义请求授权规则
                .authorizeRequests()
                // 允许静态资源和某些特定API路径无需身份验证即可访问
                .antMatchers("/static/**", "/index.html", "/favicon.ico", "/avatar.jpg").permitAll()
                .antMatchers("/larkmidtable/api/callback", "/larkmidtable/api/processCallback", "/larkmidtable/api/registry", "/larkmidtable/api/registryRemove").permitAll()
                // 允许Swagger文档和相关资源匿名访问，便于API文档的访问和测试
                .antMatchers("/doc.html", "/swagger-resources/**", "/webjars/**", "/*/api-docs").anonymous()
                // 任何其他请求都必须经过身份验证
                .anyRequest().authenticated()
                .and()
                // 添加自定义的JWT认证和授权过滤器，用于处理身份验证和授权
                .addFilter(new JWTAuthenticationFilter(authenticationManager()))
                .addFilter(new JWTAuthorizationFilter(authenticationManager()))
                // 配置会话管理策略，声明应用是无状态的，不会创建或使用会话
                .sessionManagement().sessionCreationPolicy(SessionCreationPolicy.STATELESS);
    }

    @Bean
    CorsConfigurationSource corsConfigurationSource() {
        final UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        CorsConfiguration config = new CorsConfiguration();
        config.addAllowedMethod(Constants.SPLIT_STAR);
        config.applyPermitDefaultValues();
        source.registerCorsConfiguration("/**", config);
        return source;
    }

}
