package com.southwind.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 审批角色定义实体类
 * 用于配置各种审批角色及其解析策略
 */
@TableName("t_approval_role")
public class ApprovalRole implements Serializable {

  private static final long serialVersionUID = 1L;

  /**
   * 角色ID
   */
  @TableId(value = "id", type = IdType.AUTO)
  private Integer id;

  /**
   * 角色编码（DEPT_MANAGER / FINANCE_STAFF / FINANCE_MANAGER / GM）
   */
  @TableField("role_code")
  private String roleCode;

  /**
   * 角色名称
   */
  @TableField("role_name")
  private String roleName;

  /**
   * 解析策略
   * OWN_DEPT_MANAGER: 发起人所在部门负责人
   * FIXED_DEPT_MANAGER: 固定部门负责人（运行时动态查询）
   * FIXED_EMPLOYEE: 固定员工
   */
  @TableField("resolve_strategy")
  private String resolveStrategy;

  /**
   * 固定部门ID（当 resolve_strategy = FIXED_DEPT_MANAGER 时使用）
   */
  @TableField("fixed_department_id")
  private Integer fixedDepartmentId;

  /**
   * 固定员工ID（当 resolve_strategy = FIXED_EMPLOYEE 时使用）
   */
  @TableField("fixed_employee_id")
  private Integer fixedEmployeeId;

  /**
   * 创建时间
   */
  @TableField("create_time")
  private LocalDateTime createTime;

  // Getters
  public Integer getId() {
    return id;
  }

  public String getRoleCode() {
    return roleCode;
  }

  public String getRoleName() {
    return roleName;
  }

  public String getResolveStrategy() {
    return resolveStrategy;
  }

  public Integer getFixedDepartmentId() {
    return fixedDepartmentId;
  }

  public Integer getFixedEmployeeId() {
    return fixedEmployeeId;
  }

  public LocalDateTime getCreateTime() {
    return createTime;
  }

  // Setters
  public void setId(Integer id) {
    this.id = id;
  }

  public void setRoleCode(String roleCode) {
    this.roleCode = roleCode;
  }

  public void setRoleName(String roleName) {
    this.roleName = roleName;
  }

  public void setResolveStrategy(String resolveStrategy) {
    this.resolveStrategy = resolveStrategy;
  }

  public void setFixedDepartmentId(Integer fixedDepartmentId) {
    this.fixedDepartmentId = fixedDepartmentId;
  }

  public void setFixedEmployeeId(Integer fixedEmployeeId) {
    this.fixedEmployeeId = fixedEmployeeId;
  }

  public void setCreateTime(LocalDateTime createTime) {
    this.createTime = createTime;
  }
}
