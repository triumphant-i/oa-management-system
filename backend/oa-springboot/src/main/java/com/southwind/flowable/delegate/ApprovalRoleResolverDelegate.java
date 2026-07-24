package com.southwind.flowable.delegate;

import com.southwind.service.ApprovalRoleResolver;
import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.delegate.JavaDelegate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.ArrayList;
import java.util.List;

/**
 * 审批角色解析执行监听器
 * 在流程到达用户任务前，解析出审批人列表
 */
@Component("approvalRoleResolverDelegate")
public class ApprovalRoleResolverDelegate implements JavaDelegate {

    private static final Logger logger = LoggerFactory.getLogger(ApprovalRoleResolverDelegate.class);

    @Autowired
    private ApprovalRoleResolver approvalRoleResolver;

    @Override
    public void execute(DelegateExecution execution) {
        try {
            String requiredRoles = (String) execution.getVariable("requiredRoles");
            String signType = (String) execution.getVariable("signType");
            Integer applicantId = (Integer) execution.getVariable("applicantId");
            String approvalType = (String) execution.getVariable("approvalType");

            logger.info("Resolving approval roles: requiredRoles={}, signType={}, applicantId={}, approvalType={}", 
                    requiredRoles, signType, applicantId, approvalType);

            if (requiredRoles != null && applicantId != null) {
                String[] roles = requiredRoles.split(",");
                List<String> approverList = new ArrayList<>();
                
                if ("SERIAL_AFTER_PARALLEL".equals(signType) && roles.length > 1) {
                    // 会签后串签场景
                    String secondLevelRole = roles[roles.length - 1].trim();
                    String[] firstLevelRoles = Arrays.copyOf(roles, roles.length - 1);
                    
                    // 解析第一级审批人
                    String firstLevelRolesStr = String.join(",", firstLevelRoles);
                    List<Integer> firstLevelApproverIds = approvalRoleResolver.resolveApprovers(
                            firstLevelRolesStr, applicantId, approvalType);
                    for (Integer approverId : firstLevelApproverIds) {
                        approverList.add(String.valueOf(approverId));
                    }
                    
                    // 解析第二级审批人
                    List<Integer> secondLevelApproverIds = approvalRoleResolver.resolveApprovers(
                            secondLevelRole, applicantId, approvalType);
                    if (!secondLevelApproverIds.isEmpty()) {
                        execution.setVariable("secondLevelApprover", String.valueOf(secondLevelApproverIds.get(0)));
                    } else {
                        logger.error("❌ 无法解析第二级审批人（角色={}），使用系统管理员兜底！", secondLevelRole);
                        execution.setVariable("secondLevelApprover", "1");
                    }
                    
                    logger.info("SERIAL_AFTER_PARALLEL场景: 第一级审批人={}, 第二级审批人={}", 
                            approverList, execution.getVariable("secondLevelApprover"));
                } else {
                    // SINGLE 或 PARALLEL 场景
                    List<Integer> approverIds = approvalRoleResolver.resolveApprovers(
                            requiredRoles, applicantId, approvalType);
                    for (Integer approverId : approverIds) {
                        approverList.add(String.valueOf(approverId));
                    }
                    logger.info("SINGLE/PARALLEL场景: 审批人={}", approverList);
                }
                
                execution.setVariable("approverList", approverList);
            }
        } catch (Exception e) {
            logger.error("Error resolving approval roles: {}", e.getMessage(), e);
        }
    }
}