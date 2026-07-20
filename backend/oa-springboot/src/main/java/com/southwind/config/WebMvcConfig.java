package com.southwind.config;

import com.southwind.interceptor.PermissionInterceptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Spring MVC配置类
 * 注册拦截器
 */
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    @Autowired
    private PermissionInterceptor permissionInterceptor;

    /**
     * 添加拦截器
     */
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(permissionInterceptor)
                .addPathPatterns("/**")  // 拦截所有路径
                .excludePathPatterns(    // 排除不需要拦截的路径
                        "/employee/login",      // 登录接口
                        "/error",               // 错误页面
                        "/swagger-resources/**", // Swagger资源
                        "/webjars/**",          // WebJars
                        "/v2/api-docs",         // API文档
                        "/csrf"                 // CSRF
                );
    }
}