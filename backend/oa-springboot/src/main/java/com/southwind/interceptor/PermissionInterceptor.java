package com.southwind.interceptor;

import com.southwind.annotation.RequirePermission;
import com.southwind.constant.SystemConstants;
import com.southwind.vo.ResultVO;
import com.southwind.common.UserContext;
import com.southwind.enums.RoleType;
import com.southwind.util.JwtTokenUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Arrays;

/**
 * 权限拦截器（增强版 - 支持JWT Token）
 * 
 * 工作流程：
 * 1. 从HTTP Header中获取Token
 * 2. 解析Token获取用户信息
 * 3. 设置用户上下文（ThreadLocal）
 * 4. 验证权限注解
 * 5. 请求结束后清理ThreadLocal
 */
@Component
public class PermissionInterceptor implements HandlerInterceptor {

    @Autowired
    private JwtTokenUtil jwtTokenUtil;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        // 如果不是方法级别的处理器，直接放行
        if (!(handler instanceof HandlerMethod)) {
            return true;
        }

        // ========== 第一步：从Header中获取Token并解析用户信息 ==========
        String token = extractToken(request);
        final UserContext.UserInfo[] userInfoHolder = new UserContext.UserInfo[1];
        
        if (token != null) {
            // 解析Token获取用户信息
            UserContext.UserInfo userInfo = jwtTokenUtil.parseToken(token);
            if (userInfo != null) {
                // 设置用户上下文
                UserContext.setCurrentUser(userInfo);
                userInfoHolder[0] = userInfo;
            }
        }

        // ========== 第二步：检查权限注解 ==========
        HandlerMethod handlerMethod = (HandlerMethod) handler;
        
        // 获取方法上的权限注解
        RequirePermission methodAnnotation = handlerMethod.getMethodAnnotation(RequirePermission.class);
        // 获取类上的权限注解
        RequirePermission classAnnotation = handlerMethod.getBeanType().getAnnotation(RequirePermission.class);
        
        // 如果方法和类都没有权限注解，直接放行
        if (methodAnnotation == null && classAnnotation == null) {
            return true;
        }
        
        // 优先使用方法上的注解
        RequirePermission annotation = methodAnnotation != null ? methodAnnotation : classAnnotation;
        
        // ========== 第三步：验证用户是否登录 ==========
        if (userInfoHolder[0] == null) {
            sendUnauthorizedResponse(response, SystemConstants.MSG_NOT_LOGIN);
            return false;
        }
        
        // ========== 第四步：验证用户角色 ==========
        RoleType[] allowedRoles = annotation.roles();
        boolean hasPermission = Arrays.stream(allowedRoles)
                .anyMatch(role -> role == userInfoHolder[0].getRole());
        
        if (!hasPermission) {
            sendUnauthorizedResponse(response, SystemConstants.MSG_NO_PERMISSION + "：" + annotation.description());
            return false;
        }
        
        return true;
    }

    /**
     * 从HTTP Header中提取Token
     * 格式：Authorization: Bearer <token>
     */
    private String extractToken(HttpServletRequest request) {
        String header = request.getHeader(SystemConstants.TOKEN_HEADER);
        if (header == null || header.isEmpty()) {
            return null;
        }
        
        if (header.startsWith(SystemConstants.TOKEN_PREFIX)) {
            return header.substring(SystemConstants.TOKEN_PREFIX.length());
        }
        
        return null;
    }

    /**
     * 发送未授权响应
     */
    private void sendUnauthorizedResponse(HttpServletResponse response, String message) throws Exception {
        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        response.setContentType("application/json;charset=UTF-8");

        ResultVO<Object> result = new ResultVO<>();
        result.setCode(-4);
        result.setMessage(message);

        ObjectMapper objectMapper = new ObjectMapper();
        response.getWriter().write(objectMapper.writeValueAsString(result));
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response,
                                Object handler, Exception ex) throws Exception {
        // 清除用户上下文，防止内存泄漏
        UserContext.clearCurrentUser();
    }
}