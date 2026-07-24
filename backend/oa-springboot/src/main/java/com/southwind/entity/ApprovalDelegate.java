package com.southwind.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 审批代理（请假委托）实体类
 * 用于记录审批人的临时委托（如请假期间的代理）
 */
@TableName("t_approval_delegate")
public class ApprovalDelegate implements Serializable {

  private static final long serialVersionUID = 1L;

  /**
   * 代理ID
   */
  @TableId(value = "id", type = IdType.AUTO)
  private Integer id;

  /**
   * 委托人ID（原审批人）
   */
  @TableField("delegator_id")
  private Integer delegatorId;

  /**
   * 代理人ID
   */
  @TableField("delegate_id")
  private Integer delegateId;

  /**
   * 限定审批类型（NULL表示所有类型都代理）
   */
  @TableField("approval_type")
  private String approvalType;

  /**
   * 代理生效时间
   */
  @TableField("start_time")
  private LocalDateTime startTime;

  /**
   * 代理失效时间
   */
  @TableField("end_time")
  private LocalDateTime endTime;

  /**
   * 状态（active / expired / revoked）
   */
  @TableField("status")
  private String status;

  /**
   * 创建时间
   */
  @TableField("create_time")
  private LocalDateTime createTime;

  // Getters
  public Integer getId() {
    return id;
  }

  public Integer getDelegatorId() {
    return delegatorId;
  }

  public Integer getDelegateId() {
    return delegateId;
  }

  public String getApprovalType() {
    return approvalType;
  }

  public LocalDateTime getStartTime() {
    return startTime;
  }

  public LocalDateTime getEndTime() {
    return endTime;
  }

  public String getStatus() {
    return status;
  }

  public LocalDateTime getCreateTime() {
    return createTime;
  }

  // Setters
  public void setId(Integer id) {
    this.id = id;
  }

  public void setDelegatorId(Integer delegatorId) {
    this.delegatorId = delegatorId;
  }

  public void setDelegateId(Integer delegateId) {
    this.delegateId = delegateId;
  }

  public void setApprovalType(String approvalType) {
    this.approvalType = approvalType;
  }

  public void setStartTime(LocalDateTime startTime) {
    this.startTime = startTime;
  }

  public void setEndTime(LocalDateTime endTime) {
    this.endTime = endTime;
  }

  public void setStatus(String status) {
    this.status = status;
  }

  public void setCreateTime(LocalDateTime createTime) {
    this.createTime = createTime;
  }
}
