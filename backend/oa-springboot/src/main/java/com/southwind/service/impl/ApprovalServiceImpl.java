package com.southwind.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.southwind.entity.Approval;
import com.southwind.entity.Department;
import com.southwind.event.ApprovalEvent;
import com.southwind.mapper.ApprovalMapper;
import com.southwind.service.ApprovalService;
import com.southwind.service.DepartmentService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class ApprovalServiceImpl extends ServiceImpl<ApprovalMapper, Approval> implements ApprovalService {

    private static final Logger logger = LoggerFactory.getLogger(ApprovalServiceImpl.class);
    
    private static final String STATUS_PENDING = "待审批";
    private static final String STATUS_WITHDRAWN = "已撤回";
    private static final String STATUS_APPROVED = "已通过";
    private static final String STATUS_REJECTED = "已拒绝";

    @Autowired
    private ApplicationEventPublisher eventPublisher;

    @Autowired
    private DepartmentService departmentService;

    @Override
    public int updateApprovalIfPending(Approval approval) {
        UpdateWrapper<Approval> updateWrapper = new UpdateWrapper<>();
        updateWrapper.eq("id", approval.getId()).eq("status", STATUS_PENDING);
        return this.baseMapper.update(approval, updateWrapper);
    }

    @Override
    public int withdrawIfApplicantPending(Integer id, Integer applicantId, LocalDateTime updateTime) {
        Approval updateApproval = new Approval();
        updateApproval.setStatus(STATUS_WITHDRAWN);
        updateApproval.setUpdateTime(updateTime);

        UpdateWrapper<Approval> updateWrapper = new UpdateWrapper<>();
        updateWrapper.eq("id", id)
                .eq("applicant_id", applicantId)
                .eq("status", STATUS_PENDING);
        return this.baseMapper.update(updateApproval, updateWrapper);
    }

    @Override
    @Transactional
    public Approval submitApproval(Approval approval) {
        boolean saveResult = this.save(approval);
        if (!saveResult || approval.getId() == null) {
            logger.error("Failed to save approval: {}", approval);
            return null;
        }

        logger.info("Approval submitted: id={}, applicantId={}, departmentId={}", 
            approval.getId(), approval.getApplicantId(), approval.getApplicantDepartmentId());

        try {
            Integer firstApproverId = 1;
            String firstApproverName = "系统管理员";

            if (approval.getApplicantDepartmentId() != null) {
                Department department = departmentService.getById(approval.getApplicantDepartmentId());
                if (department != null && department.getManagerId() != null) {
                    firstApproverId = department.getManagerId();
                    firstApproverName = department.getManagerName();
                    logger.info("Found department manager: id={}, name={}", firstApproverId, firstApproverName);
                }
            }

            ApprovalEvent event = ApprovalEvent.builder()
                    .eventType(ApprovalEvent.EventType.SUBMITTED)
                    .bizType(ApprovalEvent.BizType.APPROVAL)
                    .bizId(approval.getId())
                    .senderId(approval.getApplicantId())
                    .senderName(approval.getApplicantName())
                    .receiverId(firstApproverId)
                    .receiverName(firstApproverName)
                    .title("新的待审批单据")
                    .content("您有一条新的" + getApprovalTypeName(approval.getApprovalType()) + "待审批。标题：" + approval.getTitle())
                    .jumpUrl("/approval/detail/" + approval.getId())
                    .build();

            eventPublisher.publishEvent(event);
            logger.info("SUBMITTED event published: bizId={}, receiver={}", approval.getId(), firstApproverId);
        } catch (Exception e) {
            logger.error("Error publishing SUBMITTED event: {}", e.getMessage(), e);
        }

        return approval;
    }

    @Override
    @Transactional
    public int approveApproval(Approval approval, boolean approved) {
        Approval existing = this.getById(approval.getId());
        if (existing == null) {
            logger.warn("Approval not found: id={}", approval.getId());
            return 0;
        }

        approval.setStatus(approved ? STATUS_APPROVED : STATUS_REJECTED);
        approval.setApproveTime(LocalDateTime.now());
        approval.setUpdateTime(LocalDateTime.now());

        UpdateWrapper<Approval> updateWrapper = new UpdateWrapper<>();
        updateWrapper.eq("id", approval.getId()).eq("status", STATUS_PENDING);
        int affected = this.baseMapper.update(approval, updateWrapper);

        if (affected == 0) {
            logger.warn("Failed to update approval status (already changed): id={}", approval.getId());
            return 0;
        }

        logger.info("Approval processed: id={}, approved={}", approval.getId(), approved);

        try {
            ApprovalEvent.EventType eventType = approved 
                ? ApprovalEvent.EventType.APPROVED_FINAL 
                : ApprovalEvent.EventType.REJECTED;

            String title = approved ? "申请已通过" : "申请已拒绝";
            String reason = approval.getApproveReason() != null && !approval.getApproveReason().isEmpty()
                ? "，理由：" + approval.getApproveReason()
                : "";
            String content = approved
                ? "您的" + getApprovalTypeName(existing.getApprovalType()) + "已由" + approval.getApproverName() + "审批通过。"
                : "您的" + getApprovalTypeName(existing.getApprovalType()) + "已由" + approval.getApproverName() + "拒绝" + reason + "。";

            ApprovalEvent event = ApprovalEvent.builder()
                    .eventType(eventType)
                    .bizType(ApprovalEvent.BizType.APPROVAL)
                    .bizId(approval.getId())
                    .senderId(approval.getApproverId())
                    .senderName(approval.getApproverName())
                    .receiverId(existing.getApplicantId())
                    .receiverName(existing.getApplicantName())
                    .title(title)
                    .content(content)
                    .jumpUrl("/approval/detail/" + approval.getId())
                    .isTodo(false)
                    .build();

            eventPublisher.publishEvent(event);
            logger.info("{} event published: bizId={}, receiver={}", eventType, approval.getId(), existing.getApplicantId());
        } catch (Exception e) {
            logger.error("Error publishing approval result event: {}", e.getMessage(), e);
        }

        return affected;
    }

    @Override
    @Transactional
    public int withdrawApproval(Integer id, Integer applicantId, LocalDateTime updateTime, Integer approverId, String approverName) {
        Approval existing = this.getById(id);
        if (existing == null) {
            logger.warn("Approval not found for withdrawal: id={}", id);
            return 0;
        }

        Approval updateApproval = new Approval();
        updateApproval.setStatus(STATUS_WITHDRAWN);
        updateApproval.setUpdateTime(updateTime);

        UpdateWrapper<Approval> updateWrapper = new UpdateWrapper<>();
        updateWrapper.eq("id", id)
                .eq("applicant_id", applicantId)
                .eq("status", STATUS_PENDING);
        int affected = this.baseMapper.update(updateApproval, updateWrapper);

        if (affected == 0) {
            logger.warn("Failed to withdraw approval (status not pending): id={}", id);
            return 0;
        }

        logger.info("Approval withdrawn: id={}, applicantId={}", id, applicantId);

        try {
            ApprovalEvent event = ApprovalEvent.builder()
                    .eventType(ApprovalEvent.EventType.WITHDRAWN)
                    .bizType(ApprovalEvent.BizType.APPROVAL)
                    .bizId(id)
                    .senderId(applicantId)
                    .senderName(existing.getApplicantName())
                    .receiverId(approverId)
                    .receiverName(approverName)
                    .title("待审批单据已被撤回")
                    .content("申请人已撤回了" + getApprovalTypeName(existing.getApprovalType()) + "。")
                    .jumpUrl("/approval/detail/" + id)
                    .isTodo(false)
                    .build();

            eventPublisher.publishEvent(event);
            logger.info("WITHDRAWN event published: bizId={}, receiver={}", id, approverId);
        } catch (Exception e) {
            logger.error("Error publishing WITHDRAWN event: {}", e.getMessage(), e);
        }

        return affected;
    }

    private String getApprovalTypeName(String type) {
        if (type == null) return "申请";
        return switch (type) {
            case "leave" -> "请假申请";
            case "business" -> "出差申请";
            case "overtime" -> "加班申请";
            case "reimburse" -> "报销申请";
            case "purchase" -> "采购申请";
            case "card" -> "补卡申请";
            default -> type;
        };
    }
}