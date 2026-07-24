package com.southwind.service.flowable;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.southwind.entity.ApprovalRule;
import com.southwind.mapper.ApprovalRuleMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/** 仅在新流程启动时评估；评估结果会写入 Flowable 变量，因此规则修改不影响存量实例。 */
@Service
public class ApprovalRuleEvaluator {
    private static final Logger logger = LoggerFactory.getLogger(ApprovalRuleEvaluator.class);
    private final Map<String, List<ApprovalRule>> cache = new ConcurrentHashMap<>();

    @Autowired
    private ApprovalRuleMapper approvalRuleMapper;

    public ApprovalRule evaluateRule(String approvalType, Object amount, Integer days) {
        String field = needsDays(approvalType) ? "DAYS" : needsAmount(approvalType) ? "AMOUNT" : "NONE";
        BigDecimal value = "DAYS".equals(field) ? BigDecimal.valueOf(days == null ? 0 : days) : toDecimal(amount);
        for (ApprovalRule rule : rulesFor(approvalType)) {
            if (("NONE".equals(rule.getConditionField()) || field.equals(rule.getConditionField())) && matches(rule, value)) {
                return rule;
            }
        }
        logger.error("Approval rule missing: type={}, field={}, value={}; falling back to DEPT_MANAGER/SINGLE", approvalType, field, value);
        return fallback();
    }

    public void evictCache() { cache.clear(); }

    private List<ApprovalRule> rulesFor(String approvalType) {
        return cache.computeIfAbsent(approvalType, type -> {
            List<ApprovalRule> rules = approvalRuleMapper.selectList(new QueryWrapper<ApprovalRule>()
                    .eq("approval_type", type).eq("status", 1).orderByAsc("priority").orderByAsc("id"));
            return rules == null ? Collections.emptyList() : rules;
        });
    }

    private boolean matches(ApprovalRule rule, BigDecimal value) {
        return (rule.getMinValue() == null || value.compareTo(rule.getMinValue()) > 0)
                && (rule.getMaxValue() == null || value.compareTo(rule.getMaxValue()) <= 0);
    }
    private boolean needsDays(String type) { return "leave".equals(type) || "business".equals(type); }
    private boolean needsAmount(String type) { return "reimburse".equals(type) || "purchase".equals(type); }
    private BigDecimal toDecimal(Object value) {
        if (value instanceof BigDecimal) return (BigDecimal) value;
        if (value instanceof Number) return BigDecimal.valueOf(((Number) value).doubleValue());
        try { return value == null ? BigDecimal.ZERO : new BigDecimal(String.valueOf(value)); }
        catch (NumberFormatException ex) { return BigDecimal.ZERO; }
    }
    private ApprovalRule fallback() {
        ApprovalRule rule = new ApprovalRule();
        rule.setApprovalType("fallback"); rule.setConditionField("NONE"); rule.setRequiredRoles("DEPT_MANAGER");
        rule.setSignType("SINGLE"); rule.setNotifyAdminOnReject(false); rule.setNotifySuperiorOnReject(false); rule.setStatus(1);
        return rule;
    }
}