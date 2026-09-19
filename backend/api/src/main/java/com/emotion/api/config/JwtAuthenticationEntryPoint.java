package com.emotion.api.config;

import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * JWT认证入口点实现类，用于处理未认证或认证失败的情况。
 * 当请求未经认证或认证失败时，该类会返回一个401 Unauthorized响应。
 */
@Component
public class JwtAuthenticationEntryPoint implements AuthenticationEntryPoint {

    /**
     * 当Spring Security的认证流程中出现异常，例如用户未提供认证信息或认证信息无效时，
     * 此方法会被调用。它负责向客户端发送一个HTTP响应，通常是一个错误响应。
     *
     * @param request      当前的HTTP请求对象
     * @param response     当前的HTTP响应对象
     * @param authException 认证过程中抛出的异常
     * @throws IOException      如果发送响应时发生I/O错误
     * @throws ServletException 如果在处理请求时发生Servlet错误
     */
    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
                         AuthenticationException authException) throws IOException, ServletException {
        // 设置HTTP响应状态码为401，表示未授权
        // 同时设置响应消息为"Unauthorized"，告知客户端请求未经过认证
        response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Unauthorized");
    }
}
