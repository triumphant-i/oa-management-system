package com.southwind.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.time.LocalTime;

@TableName("t_shift")
public class Shift implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;

    private String name;

    @TableField("work_start")
    private LocalTime workStart;

    @TableField("work_end")
    private LocalTime workEnd;

    @TableField("lunch_start")
    private LocalTime lunchStart;

    @TableField("lunch_end")
    private LocalTime lunchEnd;

    @TableField("lunch_deduct_minutes")
    private Integer lunchDeductMinutes;

    @TableField("late_grace_minutes")
    private Integer lateGraceMinutes;

    @TableField("late_threshold_minutes")
    private Integer lateThresholdMinutes;

    @TableField("overtime_threshold_minutes")
    private Integer overtimeThresholdMinutes;

    @TableField("is_default")
    private Integer isDefault;

    @TableField("is_custom")
    private Integer isCustom;

    @TableField("employee_id")
    private Integer employeeId;

    private String description;

    @TableField("create_time")
    private LocalDateTime createTime;

    @TableField("update_time")
    private LocalDateTime updateTime;

    // Getters
    public Integer getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public LocalTime getWorkStart() {
        return workStart;
    }

    public LocalTime getWorkEnd() {
        return workEnd;
    }

    public LocalTime getLunchStart() {
        return lunchStart;
    }

    public LocalTime getLunchEnd() {
        return lunchEnd;
    }

    public Integer getLunchDeductMinutes() {
        return lunchDeductMinutes;
    }

    public Integer getLateGraceMinutes() {
        return lateGraceMinutes;
    }

    public Integer getLateThresholdMinutes() {
        return lateThresholdMinutes;
    }

    public Integer getOvertimeThresholdMinutes() {
        return overtimeThresholdMinutes;
    }

    public Integer getIsDefault() {
        return isDefault;
    }

    public Integer getIsCustom() {
        return isCustom;
    }

    public Integer getEmployeeId() {
        return employeeId;
    }

    public String getDescription() {
        return description;
    }

    public LocalDateTime getCreateTime() {
        return createTime;
    }

    public LocalDateTime getUpdateTime() {
        return updateTime;
    }

    // Setters
    public void setId(Integer id) {
        this.id = id;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setWorkStart(LocalTime workStart) {
        this.workStart = workStart;
    }

    public void setWorkEnd(LocalTime workEnd) {
        this.workEnd = workEnd;
    }

    public void setLunchStart(LocalTime lunchStart) {
        this.lunchStart = lunchStart;
    }

    public void setLunchEnd(LocalTime lunchEnd) {
        this.lunchEnd = lunchEnd;
    }

    public void setLunchDeductMinutes(Integer lunchDeductMinutes) {
        this.lunchDeductMinutes = lunchDeductMinutes;
    }

    public void setLateGraceMinutes(Integer lateGraceMinutes) {
        this.lateGraceMinutes = lateGraceMinutes;
    }

    public void setLateThresholdMinutes(Integer lateThresholdMinutes) {
        this.lateThresholdMinutes = lateThresholdMinutes;
    }

    public void setOvertimeThresholdMinutes(Integer overtimeThresholdMinutes) {
        this.overtimeThresholdMinutes = overtimeThresholdMinutes;
    }

    public void setIsDefault(Integer isDefault) {
        this.isDefault = isDefault;
    }

    public void setIsCustom(Integer isCustom) {
        this.isCustom = isCustom;
    }

    public void setEmployeeId(Integer employeeId) {
        this.employeeId = employeeId;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setCreateTime(LocalDateTime createTime) {
        this.createTime = createTime;
    }

    public void setUpdateTime(LocalDateTime updateTime) {
        this.updateTime = updateTime;
    }
}
