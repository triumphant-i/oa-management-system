package com.southwind.constant;

/**
 * 系统常量类
 * 统一管理硬编码字符串、表字段名、状态值等
 */
public class SystemConstants {

    // ========== 员工状态常量 ==========
    public static final String EMPLOYEE_STATUS_ACTIVE = "在职";
    public static final String EMPLOYEE_STATUS_INACTIVE = "离职";
    
    // ========== 表字段名常量 ==========
    public static final String COLUMN_DEPARTMENT_ID = "department_id";
    public static final String COLUMN_STATUS = "status";
    public static final String COLUMN_CREATE_TIME = "create_time";
    public static final String COLUMN_USERNAME = "username";
    public static final String COLUMN_ROLE = "role";
    
    // ========== 搜索字段白名单 ==========
    // 允许前端搜索的字段名（防止SQL注入）
    public static final String[] ALLOWED_SEARCH_FIELDS = {
        "name",        // 姓名
        "username",    // 用户名
        "phone",       // 电话
        "email",       // 邮箱
        "status"       // 状态
    };
    
    // ========== 分页常量 ==========
    public static final Integer MAX_PAGE_SIZE = 100;    // 最大分页大小
    public static final Integer DEFAULT_PAGE_SIZE = 10; // 默认分页大小
    
    // ========== 错误提示常量 ==========
    public static final String MSG_NOT_LOGIN = "未登录或登录已过期";
    public static final String MSG_NO_PERMISSION = "权限不足";
    public static final String MSG_EMPLOYEE_NOT_FOUND = "员工不存在";
    public static final String MSG_USERNAME_EXISTS = "用户名已存在";
    public static final String MSG_PASSWORD_ERROR = "密码错误";
    public static final String MSG_OPERATION_FAILED = "操作失败";
    public static final String MSG_OPERATION_SUCCESS = "操作成功";
    
    // ========== Token常量 ==========
    public static final String TOKEN_HEADER = "Authorization";
    public static final String TOKEN_PREFIX = "Bearer ";
    
    // ========== 角色常量 ==========
    public static final String ROLE_SYSTEM_ADMIN = "SYSTEM_ADMIN";
    public static final String ROLE_DEPARTMENT_MANAGER = "DEPARTMENT_MANAGER";
    public static final String ROLE_PROCESS_ADMIN = "PROCESS_ADMIN";
    public static final String ROLE_EMPLOYEE = "EMPLOYEE";
    
    /**
     * 验证搜索字段是否合法（防止SQL注入）
     * @param field 字段名
     * @return 是否合法
     */
    public static boolean isValidSearchField(String field) {
        if (field == null || field.isEmpty()) {
            return false;
        }
        
        for (String allowed : ALLOWED_SEARCH_FIELDS) {
            if (allowed.equals(field)) {
                return true;
            }
        }
        
        return false;
    }
    
    /**
     * 验证分页大小是否合法
     * @param size 分页大小
     * @return 合法的大小
     */
    public static Integer validatePageSize(Integer size) {
        if (size == null || size <= 0) {
            return DEFAULT_PAGE_SIZE;
        }
        if (size > MAX_PAGE_SIZE) {
            return MAX_PAGE_SIZE;
        }
        return size;
    }
    
    /**
     * 验证页码是否合法
     * @param page 页码
     * @return 合法的页码
     */
    public static Integer validatePageNum(Integer page) {
        if (page == null || page <= 0) {
            return 1;
        }
        return page;
    }
}