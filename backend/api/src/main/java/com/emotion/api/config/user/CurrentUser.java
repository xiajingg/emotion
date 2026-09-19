package com.emotion.api.config.user;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 注解@CurrentUser用于标记方法参数，以表示该参数代表当前操作的用户。
 * 通过这个注解，可以在运行时通过反射获取到这个参数，从而获取当前操作的用户信息。
 * 主要应用于权限检查、日志记录等场景，以便动态获取到执行操作的用户信息。
 */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
public @interface CurrentUser {
}

