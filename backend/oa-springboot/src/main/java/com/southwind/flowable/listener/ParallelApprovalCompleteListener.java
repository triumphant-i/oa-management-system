package com.southwind.flowable.listener;

import org.flowable.engine.delegate.TaskListener;
import org.flowable.task.service.delegate.DelegateTask;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * 并行会签完成监听器
 * 监听多实例用户任务的 complete 事件。
 *
 * 职责（精简后）：
 * - REJECT：维护 nrOfRejectedInstances 计数，并确保 isAllApproved=false 写入流程实例级变量，
 *   触发 completionCondition 提前终止会签。
 * - APPROVE：仅记录日志。"全部同意"的判定和 isAllApproved=true 的写入已由
 *   ApprovalFlowableService.approveTask() 在 taskService.complete() 的 variables 中显式完成，
 *   不再依赖本监听器在 complete 事件中判断（因为 TaskListener(complete) 触发时
 *   nrOfCompletedInstances 尚未递增，无法正确判断"是否最后一人"）。
 *
 * 多实例任务的完成条件在 BPMN 中定义：
 * ${nrOfCompletedInstances == nrOfInstances or (isAllApproved != null and isAllApproved == false)}
 */
@Component("parallelApprovalCompleteListener")
public class ParallelApprovalCompleteListener implements TaskListener {

    private static final Logger logger = LoggerFactory.getLogger(ParallelApprovalCompleteListener.class);

    @Override
    public void notify(DelegateTask delegateTask) {
        try {
            String taskId = delegateTask.getId();
            String taskName = delegateTask.getName();
            String assignee = delegateTask.getAssignee();

            // 安全读取多实例内置变量（避免 ClassCastException：某些场景下可能是 Long）
            int loopCounter = toInt(delegateTask.getVariable("loopCounter"), -1);
            int nrOfInstances = toInt(delegateTask.getVariable("nrOfInstances"), -1);
            int nrOfCompletedInstances = toInt(delegateTask.getVariable("nrOfCompletedInstances"), -1);

            String outcome = (String) delegateTask.getVariable("outcome");

            logger.info("会签任务完成: taskId={}, taskName={}, assignee={}, outcome={}",
                    taskId, taskName, assignee, outcome);
            logger.info("多实例状态: 当前第{}个, 总计{}, 已完成(引擎计数,尚未+1)={}",
                    loopCounter, nrOfInstances, nrOfCompletedInstances);

            if ("REJECT".equals(outcome)) {
                // 任一人驳回：维护驳回计数，确保 isAllApproved=false 在流程实例级可见
                int nrOfRejectedInstances = toInt(delegateTask.getVariable("nrOfRejectedInstances"), 0);
                nrOfRejectedInstances++;
                delegateTask.setVariable("nrOfRejectedInstances", nrOfRejectedInstances);
                delegateTask.setVariable("isAllApproved", false);
                logger.info("✗ 会签驳回: 审批人 {} 选择拒绝 (累计驳回{}人)，触发会签提前终止",
                        assignee, nrOfRejectedInstances);

            } else if ("APPROVE".equals(outcome)) {
                // 同意：isAllApproved=true 已由 approveTask() 通过 taskService.complete() 的
                // variables 参数显式写入流程实例级变量，此处无需重复设置。
                // 多实例结束由 completionCondition 的 nrOfCompletedInstances==nrOfInstances 触发。
                logger.info("✓ 会签同意: 审批人 {} 选择同意，等待引擎完成条件判定", assignee);

            } else {
                logger.warn("⚠ 未知的审批结果: outcome={}, taskId={}", outcome, taskId);
            }

        } catch (Exception e) {
            logger.error("❌ ParallelApprovalCompleteListener 执行异常: {}", e.getMessage(), e);
        }
    }

    /**
     * 安全地将变量转为 int，避免 Integer/Long 类型不一致导致 ClassCastException
     */
    private int toInt(Object value, int defaultValue) {
        if (value == null) {
            return defaultValue;
        }
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        try {
            return Integer.parseInt(value.toString());
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }
}