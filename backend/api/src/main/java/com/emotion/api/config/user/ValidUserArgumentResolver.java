package com.emotion.api.config.user;

import com.emotion.api.repository.po.WechatUser;
import org.springframework.core.MethodParameter;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

/**
 * 此类实现HandlerMethodArgumentResolver接口，用于验证和解析方法参数是否为有效用户。
 * 它的目的是在处理Web请求时，确保传入的方法参数符合特定的验证规则，
 * 例如，确保参数代表的用户是已知的或参数是合法的用户令牌。
 */
public class ValidUserArgumentResolver implements HandlerMethodArgumentResolver {

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return parameter.hasParameterAnnotation(CurrentUser.class);
    }

    @Override
    public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer,
                                  NativeWebRequest webRequest, WebDataBinderFactory binderFactory) {
        // 获取当前认证的用户信息
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication != null && authentication.getPrincipal() instanceof UserDetails) {
            WechatUser userDetails = (WechatUser) authentication.getPrincipal();
            // 根据用户名从数据库加载用户信息
            UserPrincipal userPrincipal = new UserPrincipal();
            userPrincipal.setUserOpenId(userDetails.getOpenId());
            userPrincipal.setUserId(userDetails.getId());
            return userPrincipal;
        }

        return null;
    }
}
