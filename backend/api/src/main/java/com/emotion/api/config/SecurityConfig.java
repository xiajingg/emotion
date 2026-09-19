package com.emotion.api.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.NoOpPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

// 使用@Configuration注解，表明这是一个配置类，用于定义Spring Security的配置
// 使用@EnableWebSecurity注解，启用Spring Security的web安全功能
@Order(1)
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    // 注入自定义的用户服务，用于加载用户特定数据和执行用户认证逻辑
    @Autowired
    private UserDetailsService userDetailsService;

    // 注入JWT认证失败处理器，当用户认证失败时，会调用该处理器来处理认证失败的逻辑
    @Autowired
    private JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;

    // 注入JWT请求过滤器，该过滤器会在每个请求之前执行，用于验证JWT的有效性
    @Autowired
    private JwtRequestFilter jwtRequestFilter;

    // 定义一个Bean，用于创建密码编码器实例，这里使用的是NoOp密码编码器
    @Bean
    public PasswordEncoder passwordEncoder() {
        return NoOpPasswordEncoder.getInstance();
    }

    // 配置认证提供者
    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder());
        return provider;
    }

    // 配置HttpSecurity，定义安全策略，如哪些URL需要保护，如何保护等
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http.csrf(csrf -> csrf.disable()) // 关闭CSRF保护
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/user/login",
                                "/user/register",
                                "/api/files/upload",
                                "/api/files/get-url",
                                "/actuator/*",
                                "/user/api/getOpenId",
                                "/user/api/v2/getOpenId",
                                "/user/api/v1/test",
                                "/user/api/v1/getOpenId",
                                "/files/view",
                                "/user/getProcessId",
                                "/pay/xiajing/onekey/prepay/notify",
                                "/pay/xiajing/onekey/refund/notify").permitAll() // 指定不需要认证的路径
                        .anyRequest().authenticated() // 其他所有请求都需要认证
                )
                .exceptionHandling(exception -> exception
                        .authenticationEntryPoint(jwtAuthenticationEntryPoint) // 设置认证失败处理器
                )
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS) // 设置会话管理策略为无状态
                )
                // 配置异步支持，允许SSE长连接
                .requestCache(cache -> cache.disable()); // 禁用请求缓存，避免SSE连接问题

        // 在UsernamePasswordAuthenticationFilter之前添加JWT请求过滤器
        http.addFilterBefore(jwtRequestFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    // 暴露AuthenticationManager为一个Bean，以便在其他地方可以注入使用
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }
}
