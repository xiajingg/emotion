package com.emotion.api.config;

import cn.hutool.core.util.StrUtil;
import com.emotion.api.repository.po.WechatUser;
import com.emotion.api.service.WechatUserService;
import com.emotion.api.util.RedisUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.Order;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * JWT请求过滤器，继承自Spring Security的OncePerRequestFilter，确保每个请求只被过滤一次。
 * 该过滤器的主要职责是检查请求头中的JWT令牌，验证其有效性，并在验证成功后设置安全上下文。
 */
@Slf4j
@Order(1)
@Component
public class JwtRequestFilter extends OncePerRequestFilter {

    // 注入UserDetailsService实现，用于加载用户特定数据
    @Autowired
    private UserDetailsService jwtUserDetailsService;

    // 注入JwtTokenUtil实例，用于处理JWT令牌相关操作
    @Autowired
    private JwtTokenUtil jwtTokenUtil;
    @Autowired
    private WechatUserService wechatUserService;

    @Autowired
    private RedisUtil redisUtil;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        // 定义String数组，用于储存/user/api/v1/getOpenId
        String[] urls = new String[]{"/user/api/v1/getOpenId", "/user/api/v2/getOpenId", "/user/api/v1/test",
                "/favicon.ico","/files/view", "/user/getProcessId",
                "/pay/xiajing/onekey/prepay/notify", "/pay/xiajing/onekey/refund/notify"};
        // 获取请求路径
        String requestURI = request.getRequestURI();
        // 匹配requestURI是否存在于urls，如果存在则filterChain.doFilter(request, response);
        if (StrUtil.containsAny(requestURI, urls)) {
            filterChain.doFilter(request, response);
            return;
        }
        // 从请求头中获取Authorization字段，它应该包含JWT令牌
        String requestTokenHeader = request.getHeader("Authorization");
        log.info("requestTokenHeader: {}", requestTokenHeader);
        if (StrUtil.isBlank(requestTokenHeader)) {
            log.warn("请求缺少Authorization头");
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"code\":\"401\",\"message\":\"请先登录\"}");
            return;
        }

        // 提取Token（去掉"Bearer "前缀）
        String jwtToken = requestTokenHeader.substring(7);

        // 从Redis中根据Token获取openId
        String redisOpenId = (String) redisUtil.get("JWT_" + jwtToken);


        // 如果Redis中不存在对应的openId，说明Token无效或已过期
        if (redisOpenId == null) {
            log.warn("Token无效或已过期");
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"code\":\"401\",\"message\":\"Token无效或已过期\"}");
            return;
        }

        // 从Token中解析出openId（确保Token未被篡改）
        String openIdFromToken = jwtTokenUtil.getUsernameFromToken(jwtToken);
        // 从Token中解析出openId（可选步骤，确保Token未被篡改）
        if (!redisOpenId.equals(openIdFromToken)) {
            log.warn("Token中的用户信息不匹配");
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"code\":\"401\",\"message\":\"Token验证失败\"}");
            return;
        }

        // 从数据库中获取用户信息
        WechatUser user = wechatUserService.getUserByOpenId(redisOpenId); // 根据 openId 查询用户信息
        if (user == null) {
            log.warn("用户不存在: {}", redisOpenId);
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"code\":\"401\",\"message\":\"用户不存在\"}");
            return;
        }

        // 将用户信息设置到 Spring Security 上下文
        UsernamePasswordAuthenticationToken authenticationToken = new UsernamePasswordAuthenticationToken(
                user, null, user.getAuthorities());
        authenticationToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
        SecurityContextHolder.getContext().setAuthentication(authenticationToken);

        // 继续执行过滤器链
        filterChain.doFilter(request, response);
    }
    /**
     * 重写doFilterInternal方法，实现自定义的过滤逻辑。
     *
     * @param request      当前的HTTP请求对象
     * @param response     当前的HTTP响应对象
     * @param filterChain  过滤器链，用于继续后续的过滤操作
     * @throws ServletException 如果在处理请求时发生Servlet错误
     * @throws IOException      如果发送响应时发生I/O错误
     */
//    @Override
//    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
//            throws ServletException, IOException {
//        // 从请求头中获取Authorization字段，它应该包含JWT令牌
//        final String requestTokenHeader = request.getHeader("Authorization");
//
//        String username = null; // 用于存储从JWT令牌中解析出的用户名
//        String jwtToken = null; // 用于存储JWT令牌本身
//
//        // 检查请求头中的Authorization字段是否以"Bearer "开头
//        if (requestTokenHeader != null && requestTokenHeader.startsWith("Bearer ")) {
//            // 提取JWT令牌，即Bearer后面的部分
//            jwtToken = requestTokenHeader.substring(7);
//            try {
//                // 使用JwtTokenUtil从JWT令牌中解析用户名
//                username = jwtTokenUtil.getUsernameFromToken(jwtToken);
//            } catch (Exception e) {
//                // 记录日志：无法获取JWT令牌信息
//                logger.error("Unable to get JWT Token", e);
//            }
//        } else {
//            // 记录日志：JWT令牌不是以Bearer开头或者不存在
//            logger.warn("JWT Token does not begin with Bearer String");
//        }
//
//        // 如果成功解析出用户名，并且当前安全上下文中没有认证信息，则进行认证操作
//        if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
//            // 使用UserDetailsService加载用户信息
//            UserDetails userDetails = jwtUserDetailsService.loadUserByUsername(username);
//            // 验证JWT令牌是否有效，并且与加载的用户信息匹配
//            if (jwtTokenUtil.validateToken(jwtToken, userDetails.getUsername())) {
//                // 创建UsernamePasswordAuthenticationToken对象，表示认证信息
//                UsernamePasswordAuthenticationToken usernamePasswordAuthenticationToken =
//                        new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
//                // 设置认证信息的详情，包括IP地址和会话ID等
//                usernamePasswordAuthenticationToken
//                        .setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
//                // 将认证信息设置到安全上下文中，完成认证过程
//                SecurityContextHolder.getContext().setAuthentication(usernamePasswordAuthenticationToken);
//            }
//        }
//        // 继续过滤器链中的下一个过滤器，处理后续的请求  todo: 这个方法怎样继续实现
//        filterChain.doFilter(request, response);
//    }
}