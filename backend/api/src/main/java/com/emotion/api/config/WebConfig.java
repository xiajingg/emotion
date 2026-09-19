package com.emotion.api.config;

import com.emotion.api.config.user.ValidUserArgumentResolver;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.List;

/**
 * Web配置类，用于自定义Spring MVC的配置。
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    /**
     * 添加自定义的参数解析器。
     *
     * @param resolvers 参数解析器列表，用于处理控制器方法的参数绑定。
     *                  通过向此列表中添加自定义的参数解析器，可以扩展Spring MVC的功能，
     *                  使其能够处理特定类型的参数。
     */
    @Override
    public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
        // 添加ValidUserArgumentResolver，用于处理验证用户的逻辑。
        resolvers.add(new ValidUserArgumentResolver());
    }
}

