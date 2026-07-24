package com.southwind.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import java.io.Serializable;
import java.time.LocalDateTime;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 部门实体类
 * 改造自Building类
 */
@Data
@EqualsAndHashCode(callSuper = false)
@TableName("t_department")
public class Department implements Serializable {

  private static final long serialVersionUID=1L;

  /**
   * 部门ID
   */
  @TableId(value = "id", type = IdType.AUTO)
  private Integer id;

  /**
   * 部门名称
   */
  private String name;

  /**
   * 部门负责人姓名
   */
  @TableField("manager_name")
  private String managerName;

  /**
   * 部门负责人ID（关联员工表）
   */
  @TableField("manager_id")
  private Integer managerId;

  /**
   * 负责人电话
   */
  @TableField("manager_phone")
  private String managerPhone;

  /**
   * 上级部门ID（0表示顶级部门）
   */
  @TableField("parent_id")
  private Integer parentId;

  /**
   * 部门层级（1表示一级，2表示二级，以此类推）
   */
  private Integer level;

  /**
   * 部门路径（用逗号分隔的部门ID路径，如：1,2,3）
   */
  private String path;

  /**
   * 部门描述
   */
  private String description;

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

  public String getManagerName() {
    return managerName;
  }

  public Integer getManagerId() {
    return managerId;
  }

  public String getManagerPhone() {
    return managerPhone;
  }

  public Integer getParentId() {
    return parentId;
  }

  public Integer getLevel() {
    return level;
  }

  public String getPath() {
    return path;
  }

  public String getDescription() {
    return description;
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

  public void setManagerName(String managerName) {
    this.managerName = managerName;
  }

  public void setManagerId(Integer managerId) {
    this.managerId = managerId;
  }

  public void setManagerPhone(String managerPhone) {
    this.managerPhone = managerPhone;
  }

  public void setParentId(Integer parentId) {
    this.parentId = parentId;
  }

  public void setLevel(Integer level) {
    this.level = level;
  }

  public void setPath(String path) {
    this.path = path;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public void setVersion(Integer version) {
    this.version = version;
  }

}