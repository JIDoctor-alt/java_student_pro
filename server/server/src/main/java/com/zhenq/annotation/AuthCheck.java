package com.zhenq.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 权限校验注解
 * <p>
 * 标注在 Controller 方法上，配合 {@code AuthInterceptor} 切面进行登录态与角色校验。
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface AuthCheck {

    /**
     * 必须具有的角色（为空则仅校验登录）
     */
    String mustRole() default "";
}
