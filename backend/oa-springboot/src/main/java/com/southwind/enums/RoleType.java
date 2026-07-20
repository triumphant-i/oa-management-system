package com.southwind.enums;

/**
 * 用户角色枚举
 * 根据权限矩阵定义的角色类型
 */
public enum RoleType {
    /**
     * 系统管理员
     * 拥有最高权限，可以管理所有模块
     */
    SYSTEM_ADMIN("系统管理员", "SYSTEM_ADMIN"),
    
    /**
     * 部门主管
     * 可以管理本部门的员工和部门信息
     */
    DEPARTMENT_MANAGER("部门主管", "DEPARTMENT_MANAGER"),
    
    /**
     * 流程管理员
     * 负责工作流引擎的设计和配置
     */
    PROCESS_ADMIN("流程管理员", "PROCESS_ADMIN"),
    
    /**
     * 普通员工
     * 只能查看和管理自己的信息
     */
    EMPLOYEE("普通员工", "EMPLOYEE");
    
    private String name;
    private String code;
    
    RoleType(String name, String code) {
        this.name = name;
        this.code = code;
    }
    
    public String getName() {
        return name;
    }
    
    public String getCode() {
        return code;
    }
    
    /**
     * 根据代码获取角色类型
     */
    public static RoleType getByCode(String code) {
        for (RoleType roleType : RoleType.values()) {
            if (roleType.getCode().equals(code)) {
                return roleType;
            }
        }
        return EMPLOYEE; // 默认返回普通员工
    }
}