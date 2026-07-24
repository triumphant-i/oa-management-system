package com.southwind.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import java.io.Serializable;
import java.time.LocalDateTime;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 员工实体类
 */
@Data
@EqualsAndHashCode(callSuper = false)
@TableName("t_employee")
public class Employee implements Serializable {

  private static final long serialVersionUID=1L;

  /**
   * 员工ID
   */
  @TableId(type = IdType.AUTO)
  private Integer id;

  /**
   * 用户名
   */
  private String username;

  /**
   * 密码
   */
  private String password;

  /**
   * 姓名
   */
  private String name;

  /**
   * 性别
   */
  private String gender;

  /**
   * 联系电话
   */
  private String phone;

  /**
   * 电子邮箱
   */
  private String email;

  /**
   * 部门ID
   */
  @TableField("department_id")
  private Integer departmentId;

  /**
   * 班次ID
   */
  @TableField("shift_id")
  private Integer shiftId;

  /**
   * 职位
   */
  private String position;

  /**
   * 状态(在职/离职)
   */
  private String status;

  /**
   * 用户角色(SYSTEM_ADMIN/DEPARTMENT_MANAGER/PROCESS_ADMIN/EMPLOYEE)
   */
  private String role;

  /**
   * 头像URL
   */
  private String avatar;

  /**
   * 入职日期
   */
  private String joinDate;

  /**
   * 版本号（乐观锁）
   */
  @Version
  @TableField("version")
  private Integer version;

  /**
   * 创建时间
   */
  private LocalDateTime createTime;

  /**
   * 更新时间
   */
  private LocalDateTime updateTime;

  // 手动添加 getter 方法（解决 Lombok 编译问题）
  public Integer getId() {
    return id;
  }

  public String getName() {
    return name;
  }

  public String getUsername() {
    return username;
  }

  public String getPassword() {
    return password;
  }

  public String getGender() {
    return gender;
  }

  public String getPhone() {
    return phone;
  }

  public String getEmail() {
    return email;
  }

  public Integer getDepartmentId() {
    return departmentId;
  }

  public Integer getShiftId() {
    return shiftId;
  }

  public String getPosition() {
    return position;
  }

  public String getStatus() {
    return status;
  }

  public String getRole() {
    return role;
  }

  public String getAvatar() {
    return avatar;
  }

  public String getJoinDate() {
    return joinDate;
  }

  public Integer getVersion() {
    return version;
  }

  // 手动添加 setter 方法
  public void setId(Integer id) {
    this.id = id;
  }

  public void setName(String name) {
    this.name = name;
  }

  public void setUsername(String username) {
    this.username = username;
  }

  public void setPassword(String password) {
    this.password = password;
  }

  public void setGender(String gender) {
    this.gender = gender;
  }

  public void setPhone(String phone) {
    this.phone = phone;
  }

  public void setEmail(String email) {
    this.email = email;
  }

  public void setDepartmentId(Integer departmentId) {
    this.departmentId = departmentId;
  }

  public void setShiftId(Integer shiftId) {
    this.shiftId = shiftId;
  }

  public void setPosition(String position) {
    this.position = position;
  }

  public void setStatus(String status) {
    this.status = status;
  }

  public void setRole(String role) {
    this.role = role;
  }

  public void setAvatar(String avatar) {
    this.avatar = avatar;
  }

  public void setJoinDate(String joinDate) {
    this.joinDate = joinDate;
  }

  public void setVersion(Integer version) {
    this.version = version;
  }

}