package com.southwind.service.flowable;

import com.southwind.entity.Approval;
import com.southwind.entity.ApprovalRule;
import com.southwind.service.ApprovalRoleResolver;
import com.southwind.service.flowable.ApprovalRuleEvaluator;
import org.flowable.engine.HistoryService;
import org.flowable.engine.ProcessEngine;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.TaskService;
import org.flowable.engine.history.HistoricProcessInstance;
import org.flowable.task.api.history.HistoricTaskInstance;
import org.flowable.task.api.Task;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * Flowable工作流引擎服务
 * 封装对 Flowable RuntimeService/TaskService/HistoryService 的调用
 */
@Service
public class ApprovalFlowableService {

    private static final Logger logger = LoggerFactory.getLogger(ApprovalFlowableService.class);

    @Autowired
    private ProcessEngine processEngine;

    @Autowired
    private RuntimeService runtimeService;

    @Autowired
    private TaskService taskService;

    @Autowired
    private HistoryService historyService;

    @Autowired
    private ApprovalRoleResolver approvalRoleResolver;

    @Autowired
    private ApprovalRuleEvaluator approvalRuleEvaluator;

    /**
     * 启动审批流程
     * 只设置原始参数，规则评估由 BPMN 中的 RuleEvaluationTask 完成（Flowable 驱动）
     *
     * @param approval 审批单信息
     * @return 流程实例ID
     */
    public String startApprovalProcess(Approval approval) {
        try {
            Map<String, Object> variables = new HashMap<>();
            long startTime = System.currentTimeMillis();

            // 传入流程变量：基本信息
            variables.put("approvalType", approval.getApprovalType());
            variables.put("applicantId", approval.getApplicantId());
            variables.put("applicantName", approval.getApplicantName());
            variables.put("approvalId", approval.getId());

            // 传入流程变量：判断字段（根据单据类型）
            if (approval.getAmount() != null) {
                variables.put("amount", approval.getAmount().doubleValue());
            }

            // 计算请假天数
            if ("leave".equals(approval.getApprovalType()) &&
                approval.getStartTime() != null &&
                approval.getEndTime() != null) {
                long daysBetween = java.time.temporal.ChronoUnit.DAYS.between(
                    approval.getStartTime(),
                    approval.getEndTime()
                );
                variables.put("leaveDays", (int) daysBetween + 1);
            }

            // 计算出差天数
            if ("business".equals(approval.getApprovalType()) &&
                approval.getStartTime() != null &&
                approval.getEndTime() != null) {
                long daysBetween = java.time.temporal.ChronoUnit.DAYS.between(
                    approval.getStartTime(),
                    approval.getEndTime()
                );
                variables.put("businessDays", (int) daysBetween + 1);
            }

            variables.put("nrOfRejectedInstances", 0);
            variables.put("isAllApproved", null);

            // 启动流程实例 — BPMN 中的 RuleEvaluationTask 和 RoleResolverTask
            // 会依次执行：评估规则 → 解析审批人 → 分流到单签/并行会签/会签后串签
            String processDefinitionKey = "approvalProcess";
            String businessKey = "approval_" + approval.getId();

            org.flowable.engine.runtime.ProcessInstance processInstance =
                runtimeService.startProcessInstanceByKey(processDefinitionKey, businessKey, variables);

            long elapsed = System.currentTimeMillis() - startTime;
            logger.info("Process started: processInstanceId={}, approvalId={}, approvalType={}, elapsed={}ms",
                processInstance.getId(), approval.getId(), approval.getApprovalType(), elapsed);

            return processInstance.getId();
        } catch (Exception e) {
            logger.error("Error starting approval process for approvalId: {}", approval.getId(), e);
            throw new RuntimeException("启动审批流程失败: " + e.getMessage(), e);
        }
    }

    /**
     * 获取待审批任务列表（当前登录用户的任务）
     *
     * @param userId 用户ID
     * @return 待审批任务列表
     */
    public List<Task> getPendingTasks(Integer userId) {
        try {
            String assignee = String.valueOf(userId);
            List<Task> tasks = taskService.createTaskQuery()
                    .taskAssignee(assignee)
                    .active()
                    .list();

            logger.info("Found {} pending tasks for userId: {}", tasks.size(), userId);
            return tasks;
        } catch (Exception e) {
            logger.error("Error getting pending tasks for userId: {}", userId, e);
            return new ArrayList<>();
        }
    }

    /**
     * 完成审批任务（同意）
     *
     * @param taskId        任务ID
     * @param userId        审批人ID
     * @param approvalComment 审批意见
     * @throws Exception 任务不存在或审批失败时抛出异常
     */
    public void approveTask(String taskId, Integer userId, String approvalComment) throws Exception {
        try {
            Task task = taskService.createTaskQuery().taskId(taskId).singleResult();
            if (task == null) {
                throw new RuntimeException("任务不存在或已被处理: " + taskId);
            }

            Map<String, Object> variables = new HashMap<>();
            variables.put("outcome", "APPROVE");
            variables.put("approvalComment", approvalComment);
            variables.put("approverId", userId);
            variables.put("isAllApproved", true);

            // 【新增】任务局部变量：专门用于历史记录追溯"这一个具体的会签任务"自己的决定，
            // 不会像流程级 outcome 那样被同一节点的下一个并行任务覆盖。
            // 只影响历史查询展示，不影响 completionCondition / 网关条件表达式（它们读的是流程级变量，行为不变）。
            taskService.setVariableLocal(taskId, "outcome", "APPROVE");
            taskService.setVariableLocal(taskId, "approvalComment", approvalComment);

            taskService.complete(taskId, variables);
            logger.info("Task approved: taskId={}, userId={}", taskId, userId);
        } catch (Exception e) {
            logger.error("Error approving task: {}", taskId, e);
            throw e;
        }
    }

    /**
     * 完成审批任务（驳回）
     *
     * @param taskId        任务ID
     * @param userId        审批人ID
     * @param rejectReason  驳回原因
     * @throws Exception 任务不存在或审批失败时抛出异常
     */
    public void rejectTask(String taskId, Integer userId, String rejectReason) throws Exception {
        try {
            Task task = taskService.createTaskQuery().taskId(taskId).singleResult();
            if (task == null) {
                throw new RuntimeException("任务不存在或已被处理: " + taskId);
            }

            Map<String, Object> variables = new HashMap<>();
            variables.put("outcome", "REJECT");
            variables.put("rejectReason", rejectReason);
            variables.put("approverId", userId);
            variables.put("isAllApproved", false);

            // 【新增】任务局部变量，理由同上
            taskService.setVariableLocal(taskId, "outcome", "REJECT");
            taskService.setVariableLocal(taskId, "rejectReason", rejectReason);

            taskService.complete(taskId, variables);
            logger.info("Task rejected: taskId={}, userId={}", taskId, userId);
        } catch (Exception e) {
            logger.error("Error rejecting task: {}", taskId, e);
            throw e;
        }
    }

    /**
     * 撤回流程实例
     *
     * @param processInstanceId 流程实例ID
     * @throws Exception 流程实例不存在或撤回失败时抛出异常
     */
    public void withdrawProcess(String processInstanceId) throws Exception {
        try {
            org.flowable.engine.runtime.ProcessInstance processInstance = 
                runtimeService.createProcessInstanceQuery()
                        .processInstanceId(processInstanceId)
                        .singleResult();

            if (processInstance == null) {
                throw new RuntimeException("流程实例不存在或已结束: " + processInstanceId);
            }

            runtimeService.deleteProcessInstance(processInstanceId, "申请人撤回");
            logger.info("Process withdrawn: processInstanceId={}", processInstanceId);
        } catch (Exception e) {
            logger.error("Error withdrawing process: {}", processInstanceId, e);
            throw e;
        }
    }

    /**
     * 根据审批角色和申请人信息解析审批人，并设置到流程变量中
     *
     * @param executionId   执行实例ID
     * @param requiredRoles 需要的审批角色列表
     * @param applicantId   申请人ID
     * @param approvalType  审批类型
     */
    public void resolveAndSetApprovers(String executionId, String requiredRoles, 
                                       Integer applicantId, String approvalType) {
        try {
            // 解析审批人
            List<Integer> approverIds = approvalRoleResolver.resolveApprovers(
                requiredRoles, applicantId, approvalType
            );

            if (approverIds.isEmpty()) {
                logger.warn("No approvers resolved for: requiredRoles={}, applicantId={}", 
                    requiredRoles, applicantId);
            }

            // 设置到流程变量供多实例任务使用
            List<String> approverList = new ArrayList<>();
            for (Integer approverId : approverIds) {
                approverList.add(String.valueOf(approverId));
            }

            runtimeService.setVariable(executionId, "approverList", approverList);
            logger.info("Set approverList for execution: executionId={}, approverCount={}", 
                executionId, approverList.size());
        } catch (Exception e) {
            logger.error("Error resolving approvers: {}", e.getMessage(), e);
        }
    }

    /**
     * 查询流程实例中的所有待办任务
     *
     * @param processInstanceId 流程实例ID
     * @return 任务列表
     */
    public List<Task> getProcessTasks(String processInstanceId) {
        try {
            List<Task> tasks = taskService.createTaskQuery()
                    .processInstanceId(processInstanceId)
                    .active()
                    .orderByTaskCreateTime()
                    .asc()
                    .list();

            logger.info("Found {} active tasks for process: {}", tasks.size(), processInstanceId);
            return tasks;
        } catch (Exception e) {
            logger.error("Error getting process tasks for processInstanceId: {}", processInstanceId, e);
            return new ArrayList<>();
        }
    }

    /**
     * 查询流程实例历史任务实例（用于显示审批进度）
     *
     * @param processInstanceId 流程实例ID
     * @return 历史任务实例列表（按创建时间排序）
     */
    public List<HistoricTaskInstance> getHistoricTasks(String processInstanceId) {
        try {
            List<HistoricTaskInstance> tasks = 
                historyService.createHistoricTaskInstanceQuery()
                    .processInstanceId(processInstanceId)
                    .orderByHistoricTaskInstanceStartTime()
                    .asc()
                    .list();

            logger.info("Found {} historic tasks for process: {}", tasks.size(), processInstanceId);
            return tasks;
        } catch (Exception e) {
            logger.error("Error getting historic tasks for process: {}", processInstanceId, e);
            return new ArrayList<>();
        }
    }

    /**
     * 查询流程实例状态
     *
     * @param processInstanceId 流程实例ID
     * @return 流程实例信息
     */
    public HistoricProcessInstance getProcessInstanceHistory(String processInstanceId) {
        try {
            HistoricProcessInstance processInstance = 
                historyService.createHistoricProcessInstanceQuery()
                    .processInstanceId(processInstanceId)
                    .singleResult();

            if (processInstance != null) {
                logger.info("Process history: processInstanceId={}, endTime={}, deleteReason={}", 
                    processInstanceId, processInstance.getEndTime(), processInstance.getDeleteReason());
            }

            return processInstance;
        } catch (Exception e) {
            logger.error("Error getting process history: {}", processInstanceId, e);
            return null;
        }
    }

    /**
     * 修复存量流程实例：为缺少 secondLevelApprover 变量的 SERIAL_AFTER_PARALLEL 流程补充变量
     *
     * @param approval 审批记录
     * @return 是否修复成功
     */
    public boolean fixMissingSecondLevelApprover(Approval approval) {
        if (approval == null || approval.getProcessInstanceId() == null) {
            logger.warn("无法修复：审批记录或流程实例ID为空");
            return false;
        }

        String processInstanceId = approval.getProcessInstanceId();
        
        try {
            // 1. 检查流程是否还在运行
            org.flowable.engine.runtime.ProcessInstance processInstance = 
                runtimeService.createProcessInstanceQuery()
                    .processInstanceId(processInstanceId)
                    .singleResult();
            
            if (processInstance == null) {
                logger.warn("流程实例已结束，无需修复: processInstanceId={}", processInstanceId);
                return false;
            }

            // 2. 获取流程变量
            Map<String, Object> variables = runtimeService.getVariables(processInstanceId);
            String signType = (String) variables.get("signType");

            if (!"SERIAL_AFTER_PARALLEL".equals(signType)) {
                logger.info("流程不是 SERIAL_AFTER_PARALLEL 类型，无需修复: signType={}", signType);
                return false;
            }

            // 3. 检查是否已有 secondLevelApprover 变量
            if (variables.containsKey("secondLevelApprover")) {
                logger.info("流程已有 secondLevelApprover 变量，无需修复: secondLevelApprover={}", 
                    variables.get("secondLevelApprover"));
                return false;
            }

            // 4. 重新评估规则并解析审批人
            logger.info("开始修复流程实例: processInstanceId={}, approvalId={}", processInstanceId, approval.getId());
            
            ApprovalRule rule = approvalRuleEvaluator.evaluateRule(
                approval.getApprovalType(),
                approval.getAmount(),
                null // 不需要重新计算天数，直接用已有规则
            );

            String[] roles = rule.getRequiredRoles().split(",");
            if (roles.length <= 1) {
                logger.warn("规则中角色数量不足，无法解析第二级审批人: requiredRoles={}", rule.getRequiredRoles());
                return false;
            }

            String secondLevelRole = roles[roles.length - 1].trim();
            
            // 5. 解析第二级审批人
            List<Integer> secondLevelApproverIds = approvalRoleResolver.resolveApprovers(
                secondLevelRole, approval.getApplicantId(), approval.getApprovalType());
            
            String secondLevelApprover;
            if (!secondLevelApproverIds.isEmpty()) {
                secondLevelApprover = String.valueOf(secondLevelApproverIds.get(0));
            } else {
                logger.warn("无法解析第二级审批人（角色={}），使用系统管理员兜底", secondLevelRole);
                secondLevelApprover = "1"; // 系统管理员ID
            }

            // 6. 设置流程变量
            runtimeService.setVariable(processInstanceId, "secondLevelApprover", secondLevelApprover);
            logger.info("✓ 流程变量修复成功: processInstanceId={}, secondLevelApprover={}", 
                processInstanceId, secondLevelApprover);
            
            return true;
        } catch (Exception e) {
            logger.error("修复流程实例失败: processInstanceId={}, error={}", processInstanceId, e.getMessage(), e);
            return false;
        }
    }

    /**
     * 获取TaskService（供外部调用）
     */
    public TaskService getTaskService() {
        return taskService;
    }

    /**
     * 修复所有存量的 SERIAL_AFTER_PARALLEL 流程实例
     *
     * @param pendingApprovals 待审批的审批记录列表
     * @return 修复成功的数量
     */
    public int fixAllMissingSecondLevelApprovers(List<Approval> pendingApprovals) {
        int fixedCount = 0;
        for (Approval approval : pendingApprovals) {
            if (fixMissingSecondLevelApprover(approval)) {
                fixedCount++;
            }
        }
        return fixedCount;
    }
}