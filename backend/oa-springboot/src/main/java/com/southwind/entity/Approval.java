package com.southwind.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 审批实体类
 * 改造自Application类
 */
@Data
@EqualsAndHashCode(callSuper = false)
@TableName("t_approval")
public class Approval implements Serializable {

  private static final long serialVersionUID=1L;

  /**
   * 审批ID
   */
  @TableId(value = "id", type = IdType.AUTO)
  private Integer id;

  /**
   * 申请人ID
   */
  @TableField("applicant_id")
  private Integer applicantId;

  /**
   * 申请人姓名
   */
  @TableField("applicant_name")
  private String applicantName;

  /**
   * 申请人部门ID
   */
  @TableField("applicant_department_id")
  private Integer applicantDepartmentId;

  /**
   * 审批类型(请假/出差/加班/报销/采购)
   */
  @TableField("approval_type")
  private String approvalType;

  /**
   * 申请标题
   */
  private String title;

  /**
   * 申请内容
   */
  private String content;

  /**
   * 开始时间
   */
  @TableField("start_time")
  private LocalDateTime startTime;

  /**
   * 结束时间
   */
  @TableField("end_time")
  private LocalDateTime endTime;

  /**
   * 金额(报销/采购)
   */
  private BigDecimal amount;

  /**
   * 请假类型(事假/病假/年假等)
   */
  @TableField("leave_type")
  private String leaveType;

  /**
   * 出差城市
   */
  @TableField("dest_city")
  private String destCity;

  /**
   * 加班日期
   */
  @TableField("work_date")
  private String workDate;

  /**
   * 开始时间(仅时间，用于加班/补卡)
   */
  @TableField("start_time_only")
  private String startTimeOnly;

  /**
   * 结束时间(仅时间，用于加班)
   */
  @TableField("end_time_only")
  private String endTimeOnly;

  /**
   * 报销类型
   */
  @TableField("expense_type")
  private String expenseType;

  /**
   * 费用日期(报销)
   */
  @TableField("expense_date")
  private String expenseDate;

  /**
   * 物品名称(采购)
   */
  @TableField("goods_name")
  private String goodsName;

  /**
   * 采购数量
   */
  private Integer quantity;

  /**
   * 单价(采购)
   */
  @TableField("unit_price")
  private BigDecimal unitPrice;

  /**
   * 补卡日期
   */
  @TableField("card_date")
  private String cardDate;

  /**
   * 补卡时间
   */
  @TableField("card_time")
  private String cardTime;

  /**
   * 补卡类型(迟到补卡/早退补卡/漏签补卡)
   */
  @TableField("card_type")
  private String cardType;

  /**
   * 状态(待审批/已通过/已拒绝/已撤回)
   */
  private String status;

  /**
   * 审批人ID
   */
  @TableField("approver_id")
  private Integer approverId;

  /**
   * 审批人姓名
   */
  @TableField("approver_name")
  private String approverName;

  /**
   * 审批时间
   */
  @TableField("approve_time")
  private LocalDateTime approveTime;

  /**
   * 审批意见
   */
  @TableField("approve_reason")
  private String approveReason;

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

  public Integer getApplicantId() {
    return applicantId;
  }

  public String getApplicantName() {
    return applicantName;
  }

  public Integer getApplicantDepartmentId() {
    return applicantDepartmentId;
  }

  public String getApprovalType() {
    return approvalType;
  }

  public String getTitle() {
    return title;
  }

  public String getContent() {
    return content;
  }

  public LocalDateTime getStartTime() {
    return startTime;
  }

  public LocalDateTime getEndTime() {
    return endTime;
  }

  public BigDecimal getAmount() {
    return amount;
  }

  public String getLeaveType() {
    return leaveType;
  }

  public String getDestCity() {
    return destCity;
  }

  public String getWorkDate() {
    return workDate;
  }

  public String getStartTimeOnly() {
    return startTimeOnly;
  }

  public String getEndTimeOnly() {
    return endTimeOnly;
  }

  public String getExpenseType() {
    return expenseType;
  }

  public String getExpenseDate() {
    return expenseDate;
  }

  public String getGoodsName() {
    return goodsName;
  }

  public Integer getQuantity() {
    return quantity;
  }

  public BigDecimal getUnitPrice() {
    return unitPrice;
  }

  public String getCardDate() {
    return cardDate;
  }

  public String getCardTime() {
    return cardTime;
  }

  public String getCardType() {
    return cardType;
  }

  public String getStatus() {
    return status;
  }

  public Integer getApproverId() {
    return approverId;
  }

  public String getApproverName() {
    return approverName;
  }

  public LocalDateTime getApproveTime() {
    return approveTime;
  }

  public String getApproveReason() {
    return approveReason;
  }

  // 手动添加 setter 方法
  public void setId(Integer id) {
    this.id = id;
  }

  public void setApplicantId(Integer applicantId) {
    this.applicantId = applicantId;
  }

  public void setApplicantName(String applicantName) {
    this.applicantName = applicantName;
  }

  public void setApplicantDepartmentId(Integer applicantDepartmentId) {
    this.applicantDepartmentId = applicantDepartmentId;
  }

  public void setApprovalType(String approvalType) {
    this.approvalType = approvalType;
  }

  public void setTitle(String title) {
    this.title = title;
  }

  public void setContent(String content) {
    this.content = content;
  }

  public void setStartTime(LocalDateTime startTime) {
    this.startTime = startTime;
  }

  public void setEndTime(LocalDateTime endTime) {
    this.endTime = endTime;
  }

  public void setAmount(BigDecimal amount) {
    this.amount = amount;
  }

  public void setLeaveType(String leaveType) {
    this.leaveType = leaveType;
  }

  public void setDestCity(String destCity) {
    this.destCity = destCity;
  }

  public void setWorkDate(String workDate) {
    this.workDate = workDate;
  }

  public void setStartTimeOnly(String startTimeOnly) {
    this.startTimeOnly = startTimeOnly;
  }

  public void setEndTimeOnly(String endTimeOnly) {
    this.endTimeOnly = endTimeOnly;
  }

  public void setExpenseType(String expenseType) {
    this.expenseType = expenseType;
  }

  public void setExpenseDate(String expenseDate) {
    this.expenseDate = expenseDate;
  }

  public void setGoodsName(String goodsName) {
    this.goodsName = goodsName;
  }

  public void setQuantity(Integer quantity) {
    this.quantity = quantity;
  }

  public void setUnitPrice(BigDecimal unitPrice) {
    this.unitPrice = unitPrice;
  }

  public void setCardDate(String cardDate) {
    this.cardDate = cardDate;
  }

  public void setCardTime(String cardTime) {
    this.cardTime = cardTime;
  }

  public void setCardType(String cardType) {
    this.cardType = cardType;
  }

  public void setStatus(String status) {
    this.status = status;
  }

  public void setApproverId(Integer approverId) {
    this.approverId = approverId;
  }

  public void setApproverName(String approverName) {
    this.approverName = approverName;
  }

  public void setApproveTime(LocalDateTime approveTime) {
    this.approveTime = approveTime;
  }

  public void setApproveReason(String approveReason) {
    this.approveReason = approveReason;
  }

  public void setUpdateTime(LocalDateTime updateTime) {
    this.updateTime = updateTime;
  }

  public void setCreateTime(LocalDateTime createTime) {
    this.createTime = createTime;
  }

}