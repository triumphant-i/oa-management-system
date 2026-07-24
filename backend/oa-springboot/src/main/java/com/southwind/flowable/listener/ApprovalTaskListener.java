package com.southwind.flowable.listener;

import org.flowable.engine.delegate.TaskListener;
import org.flowable.task.service.delegate.DelegateTask;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * 审批任务监听器
 * 监听用户任务的创建事件，用于：
 * 1. 记录任务分配信息
 * 2. 空值兜底：如果 assignee 为空，则记录警告（不自动完成，应该返回错误）
 */
@Component("approvalTaskListener")
public class ApprovalTaskListener implements TaskListener {

    private static final Logger logger = LoggerFactory.getLogger(ApprovalTaskListener.class);

    @Override
    public void notify(DelegateTask delegateTask) {
        try {
            String taskId = delegateTask.getId();
            String taskName = delegateTask.getName();
            String processInstanceId = delegateTask.getProcessInstanceId();
            String assignee = delegateTask.getAssignee();
            
            logger.info("============ 审批任务创建 ============");
            logger.info("taskId={}, taskName={}", taskId, taskName);
            logger.info("processInstanceId={}, assignee={}", processInstanceId, assignee);

            // 检查 assignee 是否为空
            if (assignee == null || assignee.trim().isEmpty()) {
                logger.warn("❌ 任务分配人为空!");
                logger.warn("   taskId={}, taskName={}, processInstanceId={}", 
                        taskId, taskName, processInstanceId);
                logger.warn("   这表示审批规则解析可能有问题，该级别没有合适的审批人");
                // 不自动完成，应该由前端/管理员处理
            } else {
                logger.info("✓ 任务已分配: taskId={}, assignee={}", taskId, assignee);
            }

        } catch (Exception e) {
            logger.error("❌ ApprovalTaskListener 执行异常: {}", e.getMessage(), e);
            // 不影响流程继续流转
        }
    }
}