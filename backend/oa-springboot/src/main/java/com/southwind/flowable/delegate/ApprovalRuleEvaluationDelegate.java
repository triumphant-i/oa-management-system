package com.southwind.flowable.delegate;

import com.southwind.entity.ApprovalRule;
import com.southwind.service.flowable.ApprovalRuleEvaluator;
import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.delegate.JavaDelegate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * 审批规则评估委托
 * 由BPMN流程中的 RuleEvaluationTask 调用
 * 输出：requiredRoles、signType、notifySuperiorOnReject、notifyAdminOnReject
 *
 * 注意：只能从流程变量读取 approvalType / applicantId / amount / leaveDays / businessDays，
 * 不可读取 requiredRoles / signType（它们就是本任务要设定的变量）。
 */
@Component("approvalRuleEvaluationDelegate")
public class ApprovalRuleEvaluationDelegate implements JavaDelegate {

    private static final Logger logger = LoggerFactory.getLogger(ApprovalRuleEvaluationDelegate.class);

    @Autowired
    private ApprovalRuleEvaluator approvalRuleEvaluator;

    @Override
    public void execute(DelegateExecution execution) {
        try {
            logger.info("=== 审批规则评估（Flowable 驱动） ===");

            // 1. 读取流程变量（原始输入）
            String approvalType = (String) execution.getVariable("approvalType");
            Integer applicantId = ((Number) execution.getVariable("applicantId")).intValue();
            Double amount = null;
            Integer leaveDays = null;
            Integer businessDays = null;

            if (execution.getVariable("amount") != null) {
                amount = ((Number) execution.getVariable("amount")).doubleValue();
            }
            if (execution.getVariable("leaveDays") != null) {
                leaveDays = ((Number) execution.getVariable("leaveDays")).intValue();
            }
            if (execution.getVariable("businessDays") != null) {
                businessDays = ((Number) execution.getVariable("businessDays")).intValue();
            }

            logger.info("流程参数: approvalType={}, applicantId={}, amount={}, leaveDays={}, businessDays={}",
                    approvalType, applicantId, amount, leaveDays, businessDays);

            // 2. 评估规则（读取 t_approval_rule 表）
            ApprovalRule rule = approvalRuleEvaluator.evaluateRule(
                    approvalType,
                    amount != null ? BigDecimal.valueOf(amount) : null,
                    leaveDays != null ? leaveDays : businessDays
            );

            logger.info("规则评估结果: requiredRoles={}, signType={}",
                    rule.getRequiredRoles(), rule.getSignType());

            // 3. 写入流程实例变量
            execution.setVariable("requiredRoles", rule.getRequiredRoles());
            execution.setVariable("signType", rule.getSignType());
            execution.setVariable("notifySuperiorOnReject", Boolean.TRUE.equals(rule.getNotifySuperiorOnReject()));
            execution.setVariable("notifyAdminOnReject", Boolean.TRUE.equals(rule.getNotifyAdminOnReject()));

            logger.info("=== 审批规则评估完成 ===");

        } catch (Exception e) {
            logger.error("❌ 审批规则评估失败: {}", e.getMessage(), e);
            throw new RuntimeException("审批规则评估失败: " + e.getMessage(), e);
        }
    }
}