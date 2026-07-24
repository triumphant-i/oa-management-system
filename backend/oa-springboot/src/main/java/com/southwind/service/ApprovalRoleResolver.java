package com.southwind.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.southwind.entity.ApprovalDelegate;
import com.southwind.entity.ApprovalRole;
import com.southwind.entity.Department;
import com.southwind.entity.Employee;
import com.southwind.mapper.ApprovalDelegateMapper;
import com.southwind.mapper.ApprovalRoleMapper;
import com.southwind.mapper.DepartmentMapper;
import com.southwind.mapper.EmployeeMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 审批角色解析服务
 * 根据角色编码和发起人信息，解析出实际的审批人员工ID列表
 */
@Service
public class ApprovalRoleResolver {

    private static final Logger logger = LoggerFactory.getLogger(ApprovalRoleResolver.class);

    @Autowired
    private ApprovalRoleMapper approvalRoleMapper;

    @Autowired
    private DepartmentMapper departmentMapper;

    @Autowired
    private EmployeeMapper employeeMapper;

    @Autowired
    private ApprovalDelegateMapper approvalDelegateMapper;

    @Autowired
    private WorkflowAlertService workflowAlertService;

    /**
     * 根据需要的审批角色列表和发起人信息，解析出实际的审批人员工ID
     *
     * @param requiredRoles 需要的审批角色编码列表（逗号分隔，如 "DEPT_MANAGER,FINANCE_STAFF"）
     * @param applicantId   申请人ID
     * @param approvalType  审批类型（用于检查委托记录）
     * @return 审批人员工ID列表（去重）
     */
    public List<Integer> resolveApprovers(String requiredRoles, Integer applicantId, String approvalType) {
        if (requiredRoles == null || requiredRoles.trim().isEmpty() || applicantId == null) {
            logger.warn("Invalid input: requiredRoles={}, applicantId={}", requiredRoles, applicantId);
            return new ArrayList<>();
        }

        // 分解角色编码
        String[] roleCodes = requiredRoles.split(",");
        Set<Integer> approverIds = new LinkedHashSet<>();

        // 获取申请人信息（用于OWN_DEPT_MANAGER策略）
        Employee applicant = employeeMapper.selectById(applicantId);
        if (applicant == null) {
            logger.error("Applicant not found: applicantId={}", applicantId);
            return new ArrayList<>();
        }

        // 对每个角色编码进行解析
        for (String roleCode : roleCodes) {
            roleCode = roleCode.trim();
            try {
                List<Integer> resolvedIds = resolveByRole(roleCode, applicant, approvalType);
                if (resolvedIds != null && !resolvedIds.isEmpty()) {
                    approverIds.addAll(resolvedIds);
                } else {
                    logger.warn("⚠ No approvers resolved for role: {}", roleCode);
                }
            } catch (Exception e) {
                logger.error("❌ Error resolving role: {}", roleCode, e);
            }
        }

        // ===== 关键改进：兜底逻辑 =====
        // 如果所有角色都找不到审批人，使用系统管理员作为兜底
        if (approverIds.isEmpty()) {
            logger.error("No approvers found for roles={}, applicantId={}, type={}; explicit fallback to system admin (id=1)",
                    requiredRoles, applicantId, approvalType);
            workflowAlertService.approverResolutionFailed(null, approvalType, requiredRoles,
                    "未解析到任何审批人，已分配系统管理员 ID=1");
            approverIds.add(1);  // 系统管理员 ID
        }

        return new ArrayList<>(approverIds);
    }

    /**
     * 根据角色编码解析出实际的审批人
     *
     * @param roleCode      角色编码
     * @param applicant     申请人信息
     * @param approvalType  审批类型
     * @return 审批人ID列表（通常是1人，除非支持多人分配）
     */
    private List<Integer> resolveByRole(String roleCode, Employee applicant, String approvalType) {
        // 从数据库查询审批角色配置
        QueryWrapper<ApprovalRole> roleQuery = new QueryWrapper<>();
        roleQuery.eq("role_code", roleCode);
        ApprovalRole role = approvalRoleMapper.selectOne(roleQuery);

        if (role == null) {
            logger.warn("❌ ApprovalRole not found in database: roleCode={}", roleCode);
            return new ArrayList<>();
        }

        List<Integer> approverIds = new ArrayList<>();

        String strategy = role.getResolveStrategy();

        // 根据解析策略进行不同的处理
        if ("OWN_DEPT_MANAGER".equals(strategy)) {
            // 查询申请人所在部门的负责人
            if (applicant.getDepartmentId() != null) {
                Department dept = departmentMapper.selectById(applicant.getDepartmentId());
                if (dept != null && dept.getManagerId() != null) {
                    approverIds.add(dept.getManagerId());
                    logger.info("✓ Resolved OWN_DEPT_MANAGER: departmentId={}, managerId={}", 
                        applicant.getDepartmentId(), dept.getManagerId());
                } else {
                    logger.warn("⚠ OWN_DEPT_MANAGER: Department {} has no manager", applicant.getDepartmentId());
                }
            } else {
                logger.warn("⚠ OWN_DEPT_MANAGER: Applicant has no department assigned");
            }
        } else if ("FIXED_DEPT_MANAGER".equals(strategy)) {
            // 查询固定部门的当前负责人（运行时动态查询）
            if (role.getFixedDepartmentId() != null) {
                Department dept = departmentMapper.selectById(role.getFixedDepartmentId());
                if (dept != null && dept.getManagerId() != null) {
                    approverIds.add(dept.getManagerId());
                    logger.info("✓ Resolved FIXED_DEPT_MANAGER: fixedDepartmentId={}, managerId={}", 
                        role.getFixedDepartmentId(), dept.getManagerId());
                } else {
                    logger.warn("⚠ FIXED_DEPT_MANAGER: Department {} has no manager", role.getFixedDepartmentId());
                }
            } else {
                logger.warn("⚠ FIXED_DEPT_MANAGER: Role {} has no fixed department configured", roleCode);
            }
        } else if ("FIXED_EMPLOYEE".equals(strategy)) {
            // 直接返回固定的员工ID
            if (role.getFixedEmployeeId() != null) {
                approverIds.add(role.getFixedEmployeeId());
                logger.info("✓ Resolved FIXED_EMPLOYEE: fixedEmployeeId={}", role.getFixedEmployeeId());
            } else {
                logger.warn("⚠ FIXED_EMPLOYEE: Role {} has no fixed employee configured", roleCode);
            }
        } else {
            logger.warn("⚠ Unknown strategy for role {}: {}", roleCode, strategy);
        }

        // 对解析出的每个审批人，检查是否有生效中的委托记录
        List<Integer> finalApprovers = new ArrayList<>();
        for (Integer approverId : approverIds) {
            Integer delegateId = checkAndApplyDelegate(approverId, approvalType);
            finalApprovers.add(delegateId);
        }

        return finalApprovers;
    }

    /**
     * 检查审批人是否有生效中的委托记录，如有则返回代理人ID，否则返回原审批人ID
     *
     * @param approverId   审批人ID
     * @param approvalType 审批类型
     * @return 最终的审批人ID（可能是委托的代理人）
     */
    private Integer checkAndApplyDelegate(Integer approverId, String approvalType) {
        LocalDateTime now = LocalDateTime.now();

        // 查询该审批人的生效中的委托记录
        QueryWrapper<ApprovalDelegate> delegateQuery = new QueryWrapper<>();
        delegateQuery.eq("delegator_id", approverId)
                     .eq("status", "active")
                     .le("start_time", now)
                     .ge("end_time", now);

        // 首先尝试查找与审批类型完全匹配的委托记录
        delegateQuery.clone().eq("approval_type", approvalType);
        ApprovalDelegate delegate = approvalDelegateMapper.selectOne(delegateQuery);

        // 如果没有找到特定类型的委托，查找通用委托记录（approval_type为NULL）
        if (delegate == null) {
            QueryWrapper<ApprovalDelegate> generalDelegateQuery = new QueryWrapper<>();
            generalDelegateQuery.eq("delegator_id", approverId)
                                .eq("status", "active")
                                .isNull("approval_type")
                                .le("start_time", now)
                                .ge("end_time", now);
            delegate = approvalDelegateMapper.selectOne(generalDelegateQuery);
        }

        if (delegate != null) {
            logger.info("Delegate found for approverId={}, delegating to delegateId={}, approvalType={}", 
                approverId, delegate.getDelegateId(), approvalType);
            return delegate.getDelegateId();
        }

        return approverId;
    }

    /**
     * 批量解析多个角色为审批人列表（供会签场景使用）
     *
     * @param requiredRoles 需要的审批角色编码列表
     * @param applicantId   申请人ID
     * @param approvalType  审批类型
     * @return 按角色分组的审批人ID映射表，key为角色编码，value为审批人ID列表
     */
    public Map<String, List<Integer>> resolveApproversByRole(String requiredRoles, Integer applicantId, String approvalType) {
        Map<String, List<Integer>> result = new LinkedHashMap<>();

        if (requiredRoles == null || requiredRoles.trim().isEmpty() || applicantId == null) {
            return result;
        }

        String[] roleCodes = requiredRoles.split(",");
        Employee applicant = employeeMapper.selectById(applicantId);

        if (applicant == null) {
            logger.error("Applicant not found: applicantId={}", applicantId);
            return result;
        }

        for (String roleCode : roleCodes) {
            roleCode = roleCode.trim();
            List<Integer> resolvedIds = resolveByRole(roleCode, applicant, approvalType);
            result.put(roleCode, resolvedIds);
        }

        return result;
    }
}