package com.southwind.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.southwind.entity.Approval;
import com.southwind.event.ApprovalEvent;
import com.southwind.mapper.ApprovalMapper;
import com.southwind.service.ApprovalService;
import com.southwind.service.DepartmentService;
import com.southwind.service.EmployeeService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.flowable.task.api.Task;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.southwind.service.flowable.ApprovalFlowableService;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

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
    private ApprovalFlowableService approvalFlowableService;

    @Autowired
    private EmployeeService employeeService;

    @Autowired
    private DepartmentService departmentService;

    @Autowired
    private org.flowable.engine.RuntimeService flowableRuntimeService;

    @Autowired
    private org.flowable.engine.HistoryService flowableHistoryService;

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
        // 保存审批记录
        boolean saveResult = this.save(approval);
        if (!saveResult || approval.getId() == null) {
            logger.error("Failed to save approval: {}", approval);
            return null;
        }

        logger.info("Approval submitted: id={}, applicantId={}", approval.getId(), approval.getApplicantId());

        // 启动 Flowable 流程
        String processInstanceId = null;
        try {
            processInstanceId = approvalFlowableService.startApprovalProcess(approval);
            approval.setProcessInstanceId(processInstanceId);
            
            // 更新 approval 记录，保存流程实例 ID
            UpdateWrapper<Approval> updateWrapper = new UpdateWrapper<>();
            updateWrapper.eq("id", approval.getId());
            this.update(approval, updateWrapper);
            
            logger.info("Flowable process started: processInstanceId={}, approvalId={}", processInstanceId, approval.getId());
        } catch (Exception e) {
            logger.error("Error starting Flowable process: {}", e.getMessage(), e);
            // 流程启动失败不影响主流程，回退到原有逻辑
        }

        // 发布 SUBMITTED 事件，通知第一级审批人（从 Flowable 查询真实分配人）
        try {
            // ===== 关键改进：从 Flowable 查询第一级任务的真实分配人 =====
            Integer firstApproverId = null;
            String firstApproverName = null;
            
            if (processInstanceId != null && !processInstanceId.isEmpty()) {
                try {
                    // 查询该流程实例的所有待办任务
                    List<org.flowable.task.api.Task> tasks = approvalFlowableService.getProcessTasks(processInstanceId);
                    
                    if (!tasks.isEmpty()) {
                        // 取第一个任务的分配人
                        org.flowable.task.api.Task firstTask = tasks.get(0);
                        String assigneeStr = firstTask.getAssignee();
                        
                        if (assigneeStr != null && !assigneeStr.isEmpty()) {
                            try {
                                firstApproverId = Integer.parseInt(assigneeStr);
                                logger.info("✓ 从 Flowable 查询到第一级审批人: taskId={}, assignee={}", 
                                    firstTask.getId(), firstApproverId);
                            } catch (NumberFormatException e) {
                                logger.warn("⚠ Flowable 任务分配人格式异常: assignee={}, 使用兜底逻辑", assigneeStr);
                            }
                        } else {
                            logger.warn("⚠ Flowable 任务分配人为空: taskId={}, 这表示审批规则解析可能有问题", 
                                firstTask.getId());
                        }
                    } else {
                        logger.warn("⚠ 流程实例未找到待办任务: processInstanceId={}", processInstanceId);
                    }
                } catch (Exception e) {
                    logger.warn("⚠ 从 Flowable 查询第一级审批人时出错: {}, 使用兜底逻辑", e.getMessage());
                }
            }
            
            // 兜底逻辑：如果从 Flowable 找不到分配人，使用系统管理员
            if (firstApproverId == null) {
                logger.warn("❌ 未能确定第一级审批人，使用兜底逻辑（系统管理员）");
                firstApproverId = 1;
                firstApproverName = "系统管理员";
            } else {
                // 从数据库查询审批人的名称
                firstApproverName = getEmployeeName(firstApproverId);
                if (firstApproverName == null) {
                    firstApproverName = "审批人ID:" + firstApproverId;
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
            logger.info("✓ SUBMITTED event published: bizId={}, receiver={}, receiverName={}", 
                approval.getId(), firstApproverId, firstApproverName);
        } catch (Exception e) {
            logger.error("❌ Error publishing SUBMITTED event: {}", e.getMessage(), e);
            // 事件发布异常不影响主流程
        }

        return approval;
    }

    @Override
    @Transactional
    public int approveApproval(Approval approval, boolean approved) {
        // 获取原审批记录
        Approval existing = this.getById(approval.getId());
        if (existing == null) {
            logger.warn("Approval not found: id={}", approval.getId());
            return 0;
        }

        // ============ 关键改进：不直接更新 t_approval 的 status ============
        // 原流程是直接更新为"已通过"或"已拒绝"，导致多级审批时单个审批就变成已通过
        // 新流程：只完成 Flowable 任务，由流程结束监听器负责更新 t_approval 的最终状态
        
        logger.info("============ 开始处理审批任务 ============");
        logger.info("approvalId={}, approverId={}, approved={}, processInstanceId={}", 
                approval.getId(), approval.getApproverId(), approved, existing.getProcessInstanceId());

        int taskCompletedCount = 0;

        // 处理 Flowable 流程任务
        if (existing.getProcessInstanceId() != null && !existing.getProcessInstanceId().isEmpty()) {
            try {
                // 查询当前用户的待办任务
                List<org.flowable.task.api.Task> tasks = approvalFlowableService.getPendingTasks(approval.getApproverId());
                
                logger.info("Found {} pending tasks for userId={}", tasks.size(), approval.getApproverId());
                
                org.flowable.task.api.Task matchedTask = null;
                
                // 首先查找分配给当前用户的任务
                for (org.flowable.task.api.Task task : tasks) {
                    if (task.getProcessInstanceId().equals(existing.getProcessInstanceId())) {
                        matchedTask = task;
                        break;
                    }
                }
                
                // 如果没有找到匹配的任务，尝试查找该流程实例中任何待办任务（管理员可以处理任何任务）
                if (matchedTask == null) {
                    List<org.flowable.task.api.Task> allProcessTasks = approvalFlowableService.getProcessTasks(existing.getProcessInstanceId());
                    logger.info("Found {} active tasks in process: {}", allProcessTasks.size(), existing.getProcessInstanceId());
                    
                    // 查找第一个待办任务
                    for (org.flowable.task.api.Task task : allProcessTasks) {
                        if (task.getAssignee() != null) {
                            matchedTask = task;
                            logger.info("Found pending task for process: taskId={}, taskName={}, assignee={}", 
                                    task.getId(), task.getName(), task.getAssignee());
                            break;
                        }
                    }
                }
                
                if (matchedTask == null) {
                    logger.warn("❌ 未找到对应的 Flowable 任务!");
                    logger.warn("   processInstanceId={}, approverId={}", 
                            existing.getProcessInstanceId(), approval.getApproverId());
                    logger.warn("   这可能意味着：");
                    logger.warn("   1. 用户未被分配该任务");
                    logger.warn("   2. 该任务已被其他用户处理");
                    logger.warn("   3. 流程实例不存在");
                    return 0;
                }
                
                logger.info("Found matching Flowable task: taskId={}, taskName={}, processInstanceId={}", 
                        matchedTask.getId(), matchedTask.getName(), matchedTask.getProcessInstanceId());
                
                // 如果任务不是分配给当前用户，重新分配（管理员权限）
                if (!String.valueOf(approval.getApproverId()).equals(matchedTask.getAssignee())) {
                    logger.info("任务原分配人不是当前用户，重新分配: taskId={}, oldAssignee={}, newAssignee={}", 
                            matchedTask.getId(), matchedTask.getAssignee(), approval.getApproverId());
                    approvalFlowableService.getTaskService().setAssignee(matchedTask.getId(), String.valueOf(approval.getApproverId()));
                }
                
                // 完成任务（注意：此处不修改 t_approval 的状态）
                if (approved) {
                    approvalFlowableService.approveTask(matchedTask.getId(), approval.getApproverId(), approval.getApproveReason());
                    logger.info("✓ 任务已通过: taskId={}, approvalId={}, approverId={}", 
                            matchedTask.getId(), approval.getId(), approval.getApproverId());
                } else {
                    approvalFlowableService.rejectTask(matchedTask.getId(), approval.getApproverId(), approval.getApproveReason());
                    logger.info("✗ 任务已拒绝: taskId={}, approvalId={}, approverId={}", 
                            matchedTask.getId(), approval.getId(), approval.getApproverId());
                }
                taskCompletedCount++;
                
            } catch (Exception e) {
                logger.error("❌ 完成 Flowable 任务时出错: {}", e.getMessage(), e);
                return 0;
            }
        } else {
            logger.warn("❌ 审批记录没有关联的流程实例: approvalId={}", approval.getId());
            logger.warn("   无法启动Flowable工作流，可能是流程启动失败");
            return 0;
        }

        // ============ 发布事件通知 ============
        // 注意：此时 t_approval 的状态仍为"待审批"，直到流程全部完成
        // 这个事件用于通知当前审批人的操作，以及如果流程继续，通知下一级审批人
        try {
            org.flowable.engine.history.HistoricProcessInstance processHistory = 
                    approvalFlowableService.getProcessInstanceHistory(existing.getProcessInstanceId());
            
            boolean isProcessEnded = processHistory != null && processHistory.getEndTime() != null;
            logger.info("流程状态检查: processInstanceId={}, isEnded={}", 
                    existing.getProcessInstanceId(), isProcessEnded);
            
            // ===== 关键修复：如果流程已结束，直接更新 t_approval 的状态 =====
            if (isProcessEnded) {
                logger.info("流程已结束，更新审批记录状态: approvalId={}, approved={}", 
                        approval.getId(), approved);
                
                Approval updateApproval = new Approval();
                updateApproval.setId(approval.getId());
                updateApproval.setStatus(approved ? "已通过" : "已拒绝");
                updateApproval.setApproverId(approval.getApproverId());
                updateApproval.setApproverName(approval.getApproverName());
                updateApproval.setApproveReason(approval.getApproveReason());
                updateApproval.setApproveTime(java.time.LocalDateTime.now());
                updateApproval.setUpdateTime(java.time.LocalDateTime.now());
                
                this.updateById(updateApproval);
                
                logger.info("✓ 审批记录状态已更新: approvalId={}, newStatus={}", 
                        approval.getId(), updateApproval.getStatus());
            }
            
            if (approved) {
                // 处理审批通过的情况
                handleApprovalPassed(approval, existing, isProcessEnded);
            } else {
                // 处理拒绝的情况
                handleApprovalRejected(approval, existing);
            }
            
        } catch (Exception e) {
            logger.error("发布事件时出错: {}", e.getMessage(), e);
        }

        logger.info("============ 审批任务处理完成 ============");
        return taskCompletedCount;
    }

    /**
     * 处理审批通过的通知逻辑
     */
    private void handleApprovalPassed(Approval approval, Approval existing, boolean isProcessEnded) {
        String approvalTypeName = getApprovalTypeName(existing.getApprovalType());

        if (isProcessEnded) {
            // ===== 流程最终结束：通知申请人最终结果 =====
            String content = "您提交的【" + existing.getTitle() + "】" + approvalTypeName + "已通过所有审批。";
            logger.info("流程已全部通过，通知申请人: approvalId={}, applicantId={}", approval.getId(), existing.getApplicantId());

            ApprovalEvent event = ApprovalEvent.builder()
                    .eventType(ApprovalEvent.EventType.APPROVED_FINAL)
                    .bizType(ApprovalEvent.BizType.APPROVAL)
                    .bizId(approval.getId())
                    .senderId(approval.getApproverId())
                    .senderName(approval.getApproverName())
                    .receiverId(existing.getApplicantId())
                    .receiverName(existing.getApplicantName())
                    .title("您的申请已全部通过")
                    .content(content)
                    .jumpUrl("/approval/detail/" + approval.getId())
                    .isTodo(false)
                    .build();

            eventPublisher.publishEvent(event);
            logger.info("✓ 最终审批通过事件已发布: approvalId={}", approval.getId());
        } else {
            // ===== 中间节点通过：通知下一级审批人（待办），通知申请人（知会） =====
            logger.info("审批节点通过，将通知下一级审批人: approvalId={}, approverId={}",
                    approval.getId(), approval.getApproverId());

            // 通知下一级审批人
            if (existing.getProcessInstanceId() != null) {
                notifyNextApprovers(approval, existing);
            }
        }
    }

    /**
     * 处理拒绝的通知逻辑
     * 通知策略：
     * 1. 申请人（实时，含拒绝理由）—— 必须
     * 2. 已审批的前序人员（实时，含拒绝理由）—— 必须
     * 3. 申请人上级（根据规则配置）—— 可选
     * 4. 流程管理员（根据规则配置）—— 可选
     */
    private void handleApprovalRejected(Approval approval, Approval existing) {
        String rejectReason = approval.getApproveReason();
        if (rejectReason == null || rejectReason.trim().isEmpty()) {
            logger.warn("拒绝理由为空，但在Controller层已校验，此处仅记录警告");
            rejectReason = "无理由";
        }

        // 1. 收集需要通知的人员
        java.util.List<Integer> receiverIds = new java.util.ArrayList<>();
        java.util.List<String> receiverNames = new java.util.ArrayList<>();

        // 申请人 —— 必须通知
        receiverIds.add(existing.getApplicantId());
        receiverNames.add(existing.getApplicantName());

        // 找出历史上已同意的前序审批人 —— 必须通知（让他们知道流程已被后续节点拒绝）
        java.util.Set<Integer> previousApproverIds = findPreviousApprovedApprovers(existing.getProcessInstanceId());
        for (Integer prevApproverId : previousApproverIds) {
            if (!prevApproverId.equals(approval.getApproverId()) && !prevApproverId.equals(existing.getApplicantId())) {
                receiverIds.add(prevApproverId);
                String name = getEmployeeName(prevApproverId);
                receiverNames.add(name != null ? name : "审批人ID:" + prevApproverId);
            }
        }

        // 获取审批规则，判断是否需要通知上级和管理员
        boolean notifySuperior = false;
        boolean notifyAdmin = false;
        try {
            Map<String, Object> vars = flowableRuntimeService.getVariables(existing.getProcessInstanceId());
            if (vars != null) {
                notifySuperior = Boolean.TRUE.equals(vars.get("notifySuperiorOnReject"));
                notifyAdmin = Boolean.TRUE.equals(vars.get("notifyAdminOnReject"));
            }
        } catch (Exception e) {
            logger.warn("获取流程变量失败，使用默认值: {}", e.getMessage());
        }

        // 通知申请人的上级（部门负责人）—— 可选
        if (notifySuperior) {
            Integer superiorId = findApplicantSuperior(existing);
            if (superiorId != null && !receiverIds.contains(superiorId)) {
                receiverIds.add(superiorId);
                String name = getEmployeeName(superiorId);
                receiverNames.add(name != null ? name : "上级ID:" + superiorId);
            }
        }

        // 通知系统管理员（ID为1的用户）—— 可选
        if (notifyAdmin) {
            Integer adminId = 1;
            if (!receiverIds.contains(adminId)) {
                receiverIds.add(adminId);
                receiverNames.add("系统管理员");
            }
        }

        // 2. 发送通知
        String title = "审批已被拒绝";
        String content = approval.getApproverName() + "已拒绝" + getApprovalTypeName(existing.getApprovalType()) +
                        "【" + existing.getTitle() + "】，理由：" + rejectReason +
                        "。请点击查看详情。";

        ApprovalEvent event = ApprovalEvent.builder()
                .eventType(ApprovalEvent.EventType.REJECTED)
                .bizType(ApprovalEvent.BizType.APPROVAL)
                .bizId(approval.getId())
                .senderId(approval.getApproverId())
                .senderName(approval.getApproverName())
                .receiverIds(receiverIds)
                .receiverNames(receiverNames)
                .title(title)
                .content(content)
                .jumpUrl("/approval/detail/" + approval.getId())
                .isTodo(false)
                .build();

        eventPublisher.publishEvent(event);
        logger.info("✓ 拒绝通知已发布: approvalId={}, receiverCount={}, receivers={}",
                approval.getId(), receiverIds.size(), receiverIds);

        // 3. 更新 t_approval 状态为"已拒绝"（因为流程已经终结，由监听器触发但可能延迟）
        // 这里直接显式更新，确保申请人看到的状态是实时的
        try {
            Approval updateApproval = new Approval();
            updateApproval.setId(existing.getId());
            updateApproval.setStatus("已拒绝");
            updateApproval.setApproverId(approval.getApproverId());
            updateApproval.setApproverName(approval.getApproverName());
            updateApproval.setApproveReason(rejectReason);
            updateApproval.setApproveTime(java.time.LocalDateTime.now());
            updateApproval.setUpdateTime(java.time.LocalDateTime.now());
            this.updateById(updateApproval);
            logger.info("✓ 审批记录已更新为已拒绝: approvalId={}", existing.getId());
        } catch (Exception e) {
            logger.warn("更新审批记录状态失败（流程结束监听器会兜底）: {}", e.getMessage());
        }
    }

    /**
     * 找出历史上已同意的前序审批人
     */
    private java.util.Set<Integer> findPreviousApprovedApprovers(String processInstanceId) {
        java.util.Set<Integer> approverIds = new java.util.LinkedHashSet<>();
        try {
            List<org.flowable.task.api.history.HistoricTaskInstance> historicTasks = 
                approvalFlowableService.getHistoricTasks(processInstanceId);
            
            for (org.flowable.task.api.history.HistoricTaskInstance task : historicTasks) {
                if (task.getEndTime() != null && task.getAssignee() != null) {
                    try {
                        // 检查该任务的审批结果是否为同意
                        List<org.flowable.variable.api.history.HistoricVariableInstance> vars = 
                            flowableHistoryService.createHistoricVariableInstanceQuery()
                                .taskId(task.getId())
                                .variableName("outcome")
                                .list();
                        
                        boolean approved = false;
                        for (org.flowable.variable.api.history.HistoricVariableInstance var : vars) {
                            if ("APPROVE".equals(var.getValue())) {
                                approved = true;
                                break;
                            }
                        }
                        
                        if (approved) {
                            Integer approverId = Integer.parseInt(task.getAssignee());
                            approverIds.add(approverId);
                        }
                    } catch (NumberFormatException e) {
                        // 忽略无效的assignee
                    }
                }
            }
        } catch (Exception e) {
            logger.warn("查询历史审批人失败: {}", e.getMessage());
        }
        return approverIds;
    }

    /**
     * 查找申请人的上级（部门负责人）
     */
    private Integer findApplicantSuperior(Approval approval) {
        try {
            if (approval.getApplicantDepartmentId() != null) {
                com.southwind.entity.Department dept = departmentService.getById(approval.getApplicantDepartmentId());
                if (dept != null && dept.getManagerId() != null) {
                    return dept.getManagerId();
                }
            }
        } catch (Exception e) {
            logger.warn("查找申请人上级失败: {}", e.getMessage());
        }
        return null;
    }

    /**
     * 通知下一级审批人
     * 从 Flowable 查询当前所有待办任务，拿到真实分配人后发送待办通知 + 实时消息
     */
    private void notifyNextApprovers(Approval approval, Approval existing) {
        try {
            List<org.flowable.task.api.Task> nextTasks =
                approvalFlowableService.getProcessTasks(existing.getProcessInstanceId());

            if (!nextTasks.isEmpty()) {
                logger.info("通知下一级审批人: approvalId={}, taskCount={}",
                    approval.getId(), nextTasks.size());

                java.util.Set<Integer> notified = new java.util.HashSet<>();

                for (org.flowable.task.api.Task nextTask : nextTasks) {
                    String nextAssigneeStr = nextTask.getAssignee();

                    if (nextAssigneeStr != null && !nextAssigneeStr.isEmpty()) {
                        try {
                            Integer nextApproverId = Integer.parseInt(nextAssigneeStr);

                            // 避免重复通知同一人
                            if (notified.contains(nextApproverId)) {
                                logger.info("跳过重复通知: approverId={}", nextApproverId);
                                continue;
                            }
                            notified.add(nextApproverId);

                            String content = existing.getApplicantName() + "提交的" +
                                getApprovalTypeName(existing.getApprovalType()) +
                                "【" + existing.getTitle() + "】" +
                                "已通过前一级审核，需要您继续审批。";

                            ApprovalEvent nextEvent = ApprovalEvent.builder()
                                    .eventType(ApprovalEvent.EventType.APPROVED_NODE)
                                    .bizType(ApprovalEvent.BizType.APPROVAL)
                                    .bizId(approval.getId())
                                    .senderId(approval.getApproverId())
                                    .senderName(approval.getApproverName())
                                    .receiverId(nextApproverId)
                                    .receiverName(getEmployeeName(nextApproverId) != null ?
                                        getEmployeeName(nextApproverId) : "审批人ID:" + nextApproverId)
                                    .title("新的待审批申请")
                                    .content(content)
                                    .jumpUrl("/approval/detail/" + approval.getId())
                                    .isTodo(true)
                                    .build();

                            eventPublisher.publishEvent(nextEvent);
                            logger.info("✓ 已通知下一级审批人: approvalId={}, nextApproverId={}, taskId={}, taskName={}",
                                approval.getId(), nextApproverId, nextTask.getId(), nextTask.getName());
                        } catch (NumberFormatException e) {
                            logger.warn("⚠ 下一级任务分配人格式异常: assignee={}, taskId={}",
                                nextAssigneeStr, nextTask.getId());
                        }
                    } else {
                        logger.warn("⚠ 下一级任务分配人为空: taskId={}, taskName={}",
                            nextTask.getId(), nextTask.getName());
                    }
                }

                if (notified.isEmpty()) {
                    logger.warn("⚠ 未找到有效的下一级审批人（可能为空分配），不影响流程");
                }
            } else {
                logger.info("✓ 流程中没有更多待办任务，流程将在所有审批完成后自动结束");
            }
        } catch (Exception e) {
            logger.warn("⚠ 查询下一级审批人时出错: {}, 但不影响当前流程继续", e.getMessage());
        }
    }

    @Override
    @Transactional
    public int withdrawApproval(Integer id, Integer applicantId, LocalDateTime updateTime, Integer approverId, String approverName) {
        // 获取原审批记录
        Approval existing = this.getById(id);
        if (existing == null) {
            logger.warn("Approval not found for withdrawal: id={}", id);
            return 0;
        }

        // 撤回审批
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

        // 终止 Flowable 流程
        if (existing.getProcessInstanceId() != null && !existing.getProcessInstanceId().isEmpty()) {
            try {
                approvalFlowableService.withdrawProcess(existing.getProcessInstanceId());
                logger.info("Flowable process withdrawn: processInstanceId={}", existing.getProcessInstanceId());
            } catch (Exception e) {
                logger.error("Error withdrawing Flowable process: {}", e.getMessage(), e);
                // 流程操作失败不影响主流程
            }
        }

        // 发布 WITHDRAWN 事件，通知当前审批人（如果有的话）
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

    /**
     * 根据员工 ID 获取员工名称
     */
    private String getEmployeeName(Integer employeeId) {
        if (employeeId == null) {
            return null;
        }
        try {
            com.southwind.entity.Employee employee = employeeService.getById(employeeId);
            if (employee != null && employee.getName() != null) {
                return employee.getName();
            }
        } catch (Exception e) {
            logger.warn("Failed to get employee name for id {}: {}", employeeId, e.getMessage());
        }
        return null;
    }

    @Override
    public com.southwind.vo.PageVO listMyApprovals(Integer applicantId, String status, int page, int size) {
        logger.info("查询我的审批列表: applicantId={}, status={}, page={}, size={}", 
            applicantId, status, page, size);
        
        // 构建查询条件
        QueryWrapper<Approval> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("applicant_id", applicantId);
        
        // 可选的状态过滤
        if (status != null && !status.isEmpty()) {
            queryWrapper.eq("status", status);
        }
        
        // 按创建时间倒序排列
        queryWrapper.orderByDesc("create_time");
        
        // 分页查询
        com.baomidou.mybatisplus.extension.plugins.pagination.Page<Approval> mpPage = 
            new com.baomidou.mybatisplus.extension.plugins.pagination.Page<>(page, size);
        
        // 执行查询
        com.baomidou.mybatisplus.extension.plugins.pagination.Page<Approval> resultPage = 
            this.page(mpPage, queryWrapper);
        
        // 构建返回结果
        com.southwind.vo.PageVO pageVO = new com.southwind.vo.PageVO();
        pageVO.setRecords(resultPage.getRecords());
        pageVO.setTotal(resultPage.getTotal());
        pageVO.setCurrent(resultPage.getCurrent());
        pageVO.setSize(resultPage.getSize());
        pageVO.setData(resultPage.getRecords());
        
        logger.info("查询我的审批列表完成: 共{}条记录", resultPage.getTotal());
        return pageVO;
    }
}