package com.southwind.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.southwind.entity.Employee;
import com.southwind.event.ApprovalEvent;
import com.southwind.mapper.EmployeeMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/** 兜底不能静默：保留可用性，同时把异常上下文实时通知流程/系统管理员。 */
@Service
public class WorkflowAlertService {
    @Autowired private EmployeeMapper employeeMapper;
    @Autowired private ApplicationEventPublisher eventPublisher;

    public void approverResolutionFailed(Integer approvalId, String approvalType, String failedRole, String detail) {
        List<Employee> admins = employeeMapper.selectList(new QueryWrapper<Employee>()
                .in("role", "SYSTEM_ADMIN", "PROCESS_ADMIN").eq("status", "在职"));
        List<Integer> receiverIds = admins.stream().map(Employee::getId).collect(Collectors.toList());
        if (receiverIds.isEmpty()) receiverIds.add(1); // 最终告警接收人，绝不静默
        String content = "审批人解析失败：审批类型=" + approvalType + "，规则/角色=" + failedRole
                + "，详情=" + detail + "。系统已临时分配给系统管理员，请立即修复规则或角色配置。";
        eventPublisher.publishEvent(ApprovalEvent.builder()
                .eventType(ApprovalEvent.EventType.CC_ADDED).bizType(ApprovalEvent.BizType.WORKFLOW)
                .bizId(approvalId).receiverIds(receiverIds).title("【告警】审批人解析失败")
                .content(content).jumpUrl(approvalId == null ? "/workflow" : "/approval/detail/" + approvalId).build());
    }
}