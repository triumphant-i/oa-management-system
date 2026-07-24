package com.southwind.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.time.LocalDateTime;

/**
 * 加班记录表
 * 独立于正常考勤表，同一天可以有多条加班记录
 */
@TableName("t_attendance_overtime")
public class AttendanceOvertime {

    /**
     * 主键ID
     */
    @TableId(type = IdType.AUTO)
    private Integer id;

    /**
     * 员工ID
     */
    @TableField("employee_id")
    private Integer employeeId;

    /**
     * 员工姓名（冗余字段）
     */
    @TableField("employee_name")
    private String employeeName;

    /**
     * 日期
     */
    @TableField("date")
    private String date;

    /**
     * 序号（同一天内的加班记录序号）
     */
    @TableField("sequence")
    private Integer sequence;

    /**
     * 加班开始时间
     */
    @TableField("start_time")
    private LocalDateTime startTime;

    /**
     * 加班结束时间
     */
    @TableField("end_time")
    private LocalDateTime endTime;

    /**
     * 加班类型（班前/班后/休息日加班）
     */
    @TableField("overtime_type")
    private String overtimeType;

    /**
     * 签到纬度
     */
    @TableField("check_in_latitude")
    private Double checkInLatitude;

    /**
     * 签到经度
     */
    @TableField("check_in_longitude")
    private Double checkInLongitude;

    /**
     * 签退纬度
     */
    @TableField("check_out_latitude")
    private Double checkOutLatitude;

    /**
     * 签退经度
     */
    @TableField("check_out_longitude")
    private Double checkOutLongitude;

    /**
     * 加班时长（小时）
     */
    @TableField("duration")
    private Double duration;

    /**
     * 审核状态（待审核/已通过/已拒绝）
     */
    @TableField("status")
    private String status;

    /**
     * 备注
     */
    @TableField("remark")
    private String remark;

    /**
     * 创建时间
     */
    @TableField("create_time")
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    @TableField("update_time")
    private LocalDateTime updateTime;

    // Getters
    public Integer getId() {
        return id;
    }

    public Integer getEmployeeId() {
        return employeeId;
    }

    public String getEmployeeName() {
        return employeeName;
    }

    public String getDate() {
        return date;
    }

    public Integer getSequence() {
        return sequence;
    }

    public LocalDateTime getStartTime() {
        return startTime;
    }

    public LocalDateTime getEndTime() {
        return endTime;
    }

    public String getOvertimeType() {
        return overtimeType;
    }

    public Double getCheckInLatitude() {
        return checkInLatitude;
    }

    public Double getCheckInLongitude() {
        return checkInLongitude;
    }

    public Double getCheckOutLatitude() {
        return checkOutLatitude;
    }

    public Double getCheckOutLongitude() {
        return checkOutLongitude;
    }

    public Double getDuration() {
        return duration;
    }

    public String getStatus() {
        return status;
    }

    public String getRemark() {
        return remark;
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

    public void setEmployeeId(Integer employeeId) {
        this.employeeId = employeeId;
    }

    public void setEmployeeName(String employeeName) {
        this.employeeName = employeeName;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public void setSequence(Integer sequence) {
        this.sequence = sequence;
    }

    public void setStartTime(LocalDateTime startTime) {
        this.startTime = startTime;
    }

    public void setEndTime(LocalDateTime endTime) {
        this.endTime = endTime;
    }

    public void setOvertimeType(String overtimeType) {
        this.overtimeType = overtimeType;
    }

    public void setCheckInLatitude(Double checkInLatitude) {
        this.checkInLatitude = checkInLatitude;
    }

    public void setCheckInLongitude(Double checkInLongitude) {
        this.checkInLongitude = checkInLongitude;
    }

    public void setCheckOutLatitude(Double checkOutLatitude) {
        this.checkOutLatitude = checkOutLatitude;
    }

    public void setCheckOutLongitude(Double checkOutLongitude) {
        this.checkOutLongitude = checkOutLongitude;
    }

    public void setDuration(Double duration) {
        this.duration = duration;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }

    public void setCreateTime(LocalDateTime createTime) {
        this.createTime = createTime;
    }

    public void setUpdateTime(LocalDateTime updateTime) {
        this.updateTime = updateTime;
    }
}
