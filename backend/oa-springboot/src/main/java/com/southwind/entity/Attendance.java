package com.southwind.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = false)
@TableName("t_attendance")
public class Attendance implements Serializable {

  private static final long serialVersionUID=1L;

  @TableId(value = "id", type = IdType.AUTO)
  private Integer id;

  @TableField("employee_id")
  private Integer employeeId;

  @TableField("employee_name")
  private String employeeName;

  private LocalDate date;

  @TableField("check_in_time")
  private LocalDateTime checkInTime;

  @TableField("check_out_time")
  private LocalDateTime checkOutTime;

  private String status;

  private String remark;

  private LocalDateTime createTime;

  private LocalDateTime updateTime;

  @TableField("check_in_latitude")
  private Double checkInLatitude;

  @TableField("check_in_longitude")
  private Double checkInLongitude;

  @TableField("check_out_latitude")
  private Double checkOutLatitude;

  @TableField("check_out_longitude")
  private Double checkOutLongitude;

  public Integer getId() {
    return id;
  }

  public void setId(Integer id) {
    this.id = id;
  }

  public Integer getEmployeeId() {
    return employeeId;
  }

  public void setEmployeeId(Integer employeeId) {
    this.employeeId = employeeId;
  }

  public String getEmployeeName() {
    return employeeName;
  }

  public void setEmployeeName(String employeeName) {
    this.employeeName = employeeName;
  }

  public LocalDate getDate() {
    return date;
  }

  public void setDate(LocalDate date) {
    this.date = date;
  }

  public LocalDateTime getCheckInTime() {
    return checkInTime;
  }

  public void setCheckInTime(LocalDateTime checkInTime) {
    this.checkInTime = checkInTime;
  }

  public LocalDateTime getCheckOutTime() {
    return checkOutTime;
  }

  public void setCheckOutTime(LocalDateTime checkOutTime) {
    this.checkOutTime = checkOutTime;
  }

  public String getStatus() {
    return status;
  }

  public void setStatus(String status) {
    this.status = status;
  }

  public String getRemark() {
    return remark;
  }

  public void setRemark(String remark) {
    this.remark = remark;
  }

  public LocalDateTime getCreateTime() {
    return createTime;
  }

  public void setCreateTime(LocalDateTime createTime) {
    this.createTime = createTime;
  }

  public LocalDateTime getUpdateTime() {
    return updateTime;
  }

  public void setUpdateTime(LocalDateTime updateTime) {
    this.updateTime = updateTime;
  }

  public Double getCheckInLatitude() {
    return checkInLatitude;
  }

  public void setCheckInLatitude(Double checkInLatitude) {
    this.checkInLatitude = checkInLatitude;
  }

  public Double getCheckInLongitude() {
    return checkInLongitude;
  }

  public void setCheckInLongitude(Double checkInLongitude) {
    this.checkInLongitude = checkInLongitude;
  }

  public Double getCheckOutLatitude() {
    return checkOutLatitude;
  }

  public void setCheckOutLatitude(Double checkOutLatitude) {
    this.checkOutLatitude = checkOutLatitude;
  }

  public Double getCheckOutLongitude() {
    return checkOutLongitude;
  }

  public void setCheckOutLongitude(Double checkOutLongitude) {
    this.checkOutLongitude = checkOutLongitude;
  }

}