package com.southwind.flowable.listener;

import com.southwind.entity.Approval;
import com.southwind.event.ApprovalEvent;
import com.southwind.service.ApprovalService;
import org.flowable.engine.ProcessEngine;
import org.flowable.engine.HistoryService;
import org.flowable.engine.history.HistoricProcessInstance;
import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.delegate.ExecutionListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

/**
 * 流程结束监听器
 * 当流程到达结束事件时，更新审批记录的状态
 * - 通过 EndEvent_Approved（或 ApprovedEndEvent）时，更新为"已通过"
 * - 通过 EndEvent_Rejected（或 RejectedEndEvent）时，更新为"已拒绝"
 */
@Component("approvalProcessEndListener")
public class ApprovalProcessEndListener implements ExecutionListener {

    private static final Logger logger = LoggerFactory.getLogger(ApprovalProcessEndListener.class);

    @Autowired
    private ApprovalService approvalService;

    @Autowired
    private ProcessEngine processEngine;
    
    @Autowired
    private ApplicationEventPublisher eventPublisher;

    @Override
    public void notify(DelegateExecution execution) {
        try {
            // 防御性检查：如果 Spring DI 未生效（如 BPMN 误用 flowable:class），提前报错而非 NPE
            if (approvalService == null) {
                logger.error("❌ ApprovalProcessEndListener: approvalService 未注入! " +
                        "请确认 BPMN 中使用 delegateExpression 而非 flowable:class");
                return;
            }

            String processInstanceId = execution.getProcessInstanceId();
            String currentActivityId = execution.getCurrentActivityId();
            
            logger.info("============ 流程结束事件触发 ============");
            logger.info("processInstanceId={}, endEventId={}", processInstanceId, currentActivityId);

            // 获取审批ID
            Object approvalIdObj = execution.getVariable("approvalId");
            if (approvalIdObj == null) {
                logger.error("❌ 审批ID不存在于流程变量中!");
                return;
            }
            
            Long approvalId = null;
            if (approvalIdObj instanceof Long) {
                approvalId = (Long) approvalIdObj;
            } else if (approvalIdObj instanceof Integer) {
                approvalId = ((Integer) approvalIdObj).longValue();
            } else if (approvalIdObj instanceof String) {
                approvalId = Long.parseLong((String) approvalIdObj);
            }
            
            if (approvalId == null) {
                logger.error("❌ 无法解析审批ID: {}", approvalIdObj);
                return;
            }

            // 根据结束事件类型判断审批结果
            // 支持的结束事件：
            // - EndEvent_Approved, ApprovedEndEvent → 已通过
            // - EndEvent_Rejected, RejectedEndEvent → 已拒绝
            boolean isApproved = "EndEvent_Approved".equals(currentActivityId) 
                    || "ApprovedEndEvent".equals(currentActivityId);
            
            logger.info("流程结束判定: approvalId={}, isApproved={}, eventId={}", 
                    approvalId, isApproved, currentActivityId);

            // 查询对应的审批记录
            Approval approval = approvalService.getById(approvalId.intValue());
            if (approval == null) {
                logger.warn("❌ 审批记录不存在: approvalId={}", approvalId);
                return;
            }

            logger.info("审批记录状态: approvalId={}, currentStatus={}", approvalId, approval.getStatus());

            // 只有在原状态为"待审批"时才更新
            if ("待审批".equals(approval.getStatus())) {
                Approval updateApproval = new Approval();
                updateApproval.setId(approval.getId());
                updateApproval.setStatus(isApproved ? "已通过" : "已拒绝");
                updateApproval.setUpdateTime(java.time.LocalDateTime.now());
                
                approvalService.updateById(updateApproval);
                
                logger.info("✓ 审批记录已更新: approvalId={}, newStatus={}", 
                        approvalId, updateApproval.getStatus());
                
                // ===== 关键修复：通知申请人最终结果 =====
                notifyApplicantFinalResult(approval, isApproved);
                
                logger.info("============ 流程结束事件处理完成 ============");
            } else {
                logger.warn("⚠ 审批记录状态已非待审批，无需更新: approvalId={}, status={}", 
                        approvalId, approval.getStatus());
            }

        } catch (Exception e) {
            logger.error("❌ 流程结束监听器执行异常: {}", e.getMessage(), e);
            // 监听器异常不应该中断流程
        }
    }
    
    /**
     * 通知申请人最终审批结果
     */
    private void notifyApplicantFinalResult(Approval approval, boolean isApproved) {
        try {
            logger.info("准备通知申请人最终结果: approvalId={}, applicantId={}, isApproved={}", 
                    approval.getId(), approval.getApplicantId(), isApproved);
            
            // 构造审批类型名称（用于消息文案）
            String approvalTypeName = getApprovalTypeName(approval.getApprovalType());
            
            // 确定事件类型和消息文案
            ApprovalEvent.EventType eventType = isApproved 
                    ? ApprovalEvent.EventType.APPROVED_FINAL 
                    : ApprovalEvent.EventType.REJECTED;
            
            String title = isApproved 
                    ? "审批已通过" 
                    : "审批已拒绝";
            
            String content = isApproved 
                    ? "您提交的【" + approval.getTitle() + "】" + approvalTypeName + "已审批通过。" 
                    : "您提交的【" + approval.getTitle() + "】" + approvalTypeName + "已被拒绝。";
            
            // 发布事件，让 ApprovalEventListener 消费并发送消息
            ApprovalEvent event = ApprovalEvent.builder()
                    .eventType(eventType)
                    .bizType(ApprovalEvent.BizType.APPROVAL)
                    .bizId(approval.getId())
                    .senderId(approval.getApproverId() != null ? approval.getApproverId() : 1)
                    .senderName(approval.getApproverName() != null ? approval.getApproverName() : "系统")
                    .receiverId(approval.getApplicantId())
                    .receiverName(approval.getApplicantName())
                    .title(title)
                    .content(content)
                    .jumpUrl("/approval/detail/" + approval.getId())
                    .isTodo(false) // 不是待办，只是通知
                    .build();
            
            eventPublisher.publishEvent(event);
            logger.info("✓ 已发布流程结束事件通知申请人: approvalId={}, eventType={}, receiver={}", 
                    approval.getId(), eventType, approval.getApplicantName());
            
        } catch (Exception e) {
            logger.error("❌ 通知申请人最终结果时出错: {}", e.getMessage(), e);
            // 通知异常不影响主流程
        }
    }
    
    /**
     * 获取审批类型的中文名称
     */
    private String getApprovalTypeName(String approvalType) {
        if (approvalType == null) return "申请";
        return switch (approvalType) {
            case "leave" -> "请假申请";
            case "business" -> "出差申请";
            case "overtime" -> "加班申请";
            case "reimburse" -> "报销申请";
            case "purchase" -> "采购申请";
            case "card" -> "补卡申请";
            default -> approvalType;
        };
    }
}