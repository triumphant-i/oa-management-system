package com.southwind.annotation;

import com.southwind.enums.RoleType;

import java.lang.annotation.*;

/**
 * 权限注解
 * 用于标记Controller方法需要特定的权限才能访问
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RequirePermission {
    /**
     * 允许访问的角色列表
     */
    RoleType[] roles() default {};
    
    /**
     * 权限描述
     */
    String description() default "";
}