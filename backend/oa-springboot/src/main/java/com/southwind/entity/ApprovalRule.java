package com.southwind.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("t_approval_rule")
public class ApprovalRule {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String approvalType;
    private String conditionField;
    private BigDecimal minValue;
    private BigDecimal maxValue;
    private String requiredRoles;
    private String signType;
    private Boolean notifySuperiorOnReject;
    private Boolean notifyAdminOnReject;
    private Integer priority;
    private Integer status;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;

    // 手动添加 getter 方法
    public Long getId() {
        return id;
    }

    public String getApprovalType() {
        return approvalType;
    }

    public String getConditionField() {
        return conditionField;
    }

    public BigDecimal getMinValue() {
        return minValue;
    }

    public BigDecimal getMaxValue() {
        return maxValue;
    }

    public String getRequiredRoles() {
        return requiredRoles;
    }

    public String getSignType() {
        return signType;
    }

    public Boolean getNotifySuperiorOnReject() {
        return notifySuperiorOnReject;
    }

    public Boolean getNotifyAdminOnReject() {
        return notifyAdminOnReject;
    }

    public Integer getPriority() {
        return priority;
    }

    public Integer getStatus() {
        return status;
    }

    public LocalDateTime getCreateTime() {
        return createTime;
    }

    public LocalDateTime getUpdateTime() {
        return updateTime;
    }

    // 手动添加 setter 方法
    public void setId(Long id) {
        this.id = id;
    }

    public void setApprovalType(String approvalType) {
        this.approvalType = approvalType;
    }

    public void setConditionField(String conditionField) {
        this.conditionField = conditionField;
    }

    public void setMinValue(BigDecimal minValue) {
        this.minValue = minValue;
    }

    public void setMaxValue(BigDecimal maxValue) {
        this.maxValue = maxValue;
    }

    public void setRequiredRoles(String requiredRoles) {
        this.requiredRoles = requiredRoles;
    }

    public void setSignType(String signType) {
        this.signType = signType;
    }

    public void setNotifySuperiorOnReject(Boolean notifySuperiorOnReject) {
        this.notifySuperiorOnReject = notifySuperiorOnReject;
    }

    public void setNotifyAdminOnReject(Boolean notifyAdminOnReject) {
        this.notifyAdminOnReject = notifyAdminOnReject;
    }

    public void setPriority(Integer priority) {
        this.priority = priority;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    public void setCreateTime(LocalDateTime createTime) {
        this.createTime = createTime;
    }

    public void setUpdateTime(LocalDateTime updateTime) {
        this.updateTime = updateTime;
    }
}