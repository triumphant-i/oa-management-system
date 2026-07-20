package com.southwind.common;

import com.southwind.entity.Employee;
import com.southwind.enums.RoleType;

/**
 * 用户上下文
 * 使用ThreadLocal存储当前登录用户信息
 */
public class UserContext {
    
    private static final ThreadLocal<UserInfo> userThreadLocal = new ThreadLocal<>();
    
    /**
     * 用户信息封装类
     */
    public static class UserInfo {
        private Integer userId;
        private String username;
        private String name;
        private RoleType role;
        private Integer departmentId;
        
        public UserInfo(Integer userId, String username, String name, RoleType role, Integer departmentId) {
            this.userId = userId;
            this.username = username;
            this.name = name;
            this.role = role;
            this.departmentId = departmentId;
        }
        
        // Getter方法
        public Integer getUserId() {
            return userId;
        }
        
        public String getUsername() {
            return username;
        }
        
        public String getName() {
            return name;
        }
        
        public RoleType getRole() {
            return role;
        }
        
        public Integer getDepartmentId() {
            return departmentId;
        }
    }
    
    /**
     * 设置当前用户信息
     */
    public static void setCurrentUser(UserInfo userInfo) {
        userThreadLocal.set(userInfo);
    }
    
    /**
     * 获取当前用户信息
     */
    public static UserInfo getCurrentUser() {
        return userThreadLocal.get();
    }
    
    /**
     * 清除当前用户信息
     */
    public static void clearCurrentUser() {
        userThreadLocal.remove();
    }
    
    /**
     * 判断当前用户是否是系统管理员
     */
    public static boolean isSystemAdmin() {
        UserInfo user = getCurrentUser();
        return user != null && user.getRole() == RoleType.SYSTEM_ADMIN;
    }
    
    /**
     * 判断当前用户是否是部门主管
     */
    public static boolean isDepartmentManager() {
        UserInfo user = getCurrentUser();
        return user != null && user.getRole() == RoleType.DEPARTMENT_MANAGER;
    }
    
    /**
     * 获取当前用户ID
     */
    public static Integer getCurrentUserId() {
        UserInfo user = getCurrentUser();
        return user != null ? user.getUserId() : null;
    }
    
    /**
     * 获取当前用户部门ID
     */
    public static Integer getCurrentUserDepartmentId() {
        UserInfo user = getCurrentUser();
        return user != null ? user.getDepartmentId() : null;
    }
    
    /**
     * 判断当前用户是否有权限访问指定部门的数据
     */
    public static boolean canAccessDepartment(Integer targetDepartmentId) {
        UserInfo user = getCurrentUser();
        if (user == null) {
            return false;
        }
        
        // 系统管理员可以访问所有部门
        if (user.getRole() == RoleType.SYSTEM_ADMIN) {
            return true;
        }
        
        // 部门主管只能访问自己管理的部门
        if (user.getRole() == RoleType.DEPARTMENT_MANAGER) {
            return user.getDepartmentId() != null && 
                   user.getDepartmentId().equals(targetDepartmentId);
        }
        
        return false;
    }
}