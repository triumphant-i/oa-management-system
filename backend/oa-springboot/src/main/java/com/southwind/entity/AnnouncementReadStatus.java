package com.southwind.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import java.io.Serializable;
import java.time.LocalDateTime;

@TableName("t_announcement_read_status")
public class AnnouncementReadStatus implements Serializable {

  private static final long serialVersionUID=1L;

  @TableId(value = "id", type = IdType.AUTO)
  private Integer id;

  @TableField("announcement_id")
  private Integer announcementId;

  @TableField("employee_id")
  private Integer employeeId;

  @TableField("is_read")
  private Integer isRead;

  @TableField("read_time")
  private LocalDateTime readTime;

  @TableField("create_time")
  private LocalDateTime createTime;

  // Getters
  public Integer getId() {
    return id;
  }

  public Integer getAnnouncementId() {
    return announcementId;
  }

  public Integer getEmployeeId() {
    return employeeId;
  }

  public Integer getIsRead() {
    return isRead;
  }

  public LocalDateTime getReadTime() {
    return readTime;
  }

  public LocalDateTime getCreateTime() {
    return createTime;
  }

  // Setters
  public void setId(Integer id) {
    this.id = id;
  }

  public void setAnnouncementId(Integer announcementId) {
    this.announcementId = announcementId;
  }

  public void setEmployeeId(Integer employeeId) {
    this.employeeId = employeeId;
  }

  public void setIsRead(Integer isRead) {
    this.isRead = isRead;
  }

  public void setReadTime(LocalDateTime readTime) {
    this.readTime = readTime;
  }

  public void setCreateTime(LocalDateTime createTime) {
    this.createTime = createTime;
  }
}
