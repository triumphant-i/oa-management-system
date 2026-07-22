package com.southwind.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.southwind.common.UserContext;
import com.southwind.entity.Approval;
import com.southwind.entity.Attendance;
import com.southwind.entity.Message;
import com.southwind.enums.RoleType;
import com.southwind.service.ApprovalService;
import com.southwind.service.AttendanceService;
import com.southwind.service.MessageService;
import com.southwind.util.ResultVOUtil;
import com.southwind.vo.PageVO;
import com.southwind.vo.ResultVO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * 审批控制器
 */
@RestController
@RequestMapping("/approval")
public class ApprovalController {

    private static final Logger logger = LoggerFactory.getLogger(ApprovalController.class);
    private static final String STATUS_PENDING = "待审批";
    private static final String STATUS_APPROVED = "已通过";
    private static final String STATUS_REJECTED = "已拒绝";
    private static final Set<String> APPROVAL_TYPE_WHITELIST = Set.of("leave", "business", "overtime", "reimburse", "purchase", "card");

    @Autowired
    private ApprovalService approvalService;

    @Autowired
    private MessageService messageService;

    @Autowired
    private AttendanceService attendanceService;

    /**
     * 提交申请
     */
    @PostMapping("/submit")
    public ResultVO submit(@RequestBody Approval approval) {
        UserContext.UserInfo currentUser = UserContext.getCurrentUser();
        if (currentUser == null) {
            return ResultVOUtil.fail("未登录");
        }

        if (approval.getApprovalType() == null || !APPROVAL_TYPE_WHITELIST.contains(approval.getApprovalType())) {
            return ResultVOUtil.fail("审批类型不合法");
        }
        String typeValidationMessage = validateByType(approval);
        if (typeValidationMessage != null) {
            return ResultVOUtil.fail(typeValidationMessage);
        }

        approval.setApplicantId(currentUser.getUserId());
        approval.setApplicantName(currentUser.getName() != null ? currentUser.getName() : currentUser.getUsername());
        approval.setApplicantDepartmentId(currentUser.getDepartmentId());
        approval.setStatus(STATUS_PENDING);
        approval.setCreateTime(LocalDateTime.now());
        approval.setUpdateTime(LocalDateTime.now());
        
        // 调用新的 Service 方法，自动发布 SUBMITTED 事件
        Approval result = approvalService.submitApproval(approval);
        if (result == null || result.getId() == null) {
            return ResultVOUtil.fail("提交失败");
        }

        return ResultVOUtil.success(result.getId());
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

    private void processCardApproval(Approval approval) {
        try {
            String cardDateStr = approval.getCardDate();
            String cardTimeStr = approval.getCardTime();
            String cardType = approval.getCardType();
            
            logger.info("=== processCardApproval ===");
            logger.info("approvalId={}, applicantId={}", approval.getId(), approval.getApplicantId());
            logger.info("cardDate={}, cardTime={}, cardType={}", cardDateStr, cardTimeStr, cardType);
            
            if (cardDateStr == null || cardTimeStr == null || cardType == null) {
                logger.warn("processCardApproval: one of cardDate/cardTime/cardType is null, returning");
                return;
            }
            
            LocalDate date = LocalDate.parse(cardDateStr);
            LocalTime time = LocalTime.parse(cardTimeStr);
            LocalDateTime checkTime = LocalDateTime.of(date, time);
            
            QueryWrapper<Attendance> queryWrapper = new QueryWrapper<>();
            queryWrapper.eq("employee_id", approval.getApplicantId());
            queryWrapper.eq("date", date);
            Attendance existing = attendanceService.getOne(queryWrapper);
            
            Attendance attendance;
            if (existing != null) {
                attendance = existing;
            } else {
                attendance = new Attendance();
                attendance.setEmployeeId(approval.getApplicantId());
                attendance.setEmployeeName(approval.getApplicantName());
                attendance.setDate(date);
            }
            
            if ("late".equals(cardType) || "miss".equals(cardType)) {
                attendance.setCheckInTime(checkTime);
            }
            if ("early".equals(cardType)) {
                attendance.setCheckOutTime(checkTime);
            }
            
            attendance.setStatus("正常");
            attendance.setRemark("补卡通过");
            attendance.setUpdateTime(LocalDateTime.now());
            
            if (existing != null) {
                boolean updated = attendanceService.updateById(attendance);
                logger.info("processCardApproval: updated existing attendance, result={}", updated);
            } else {
                attendance.setCreateTime(LocalDateTime.now());
                boolean saved = attendanceService.save(attendance);
                logger.info("processCardApproval: saved new attendance, result={}", saved);
            }
        } catch (Exception e) {
            logger.error("processCardApproval: exception={}", e.getMessage(), e);
        }
    }

    /**
     * 查询我的申请
     */
    @GetMapping("/myApplications/{applicantId}")
    public ResultVO myApplications(@PathVariable("applicantId") Integer applicantId,
                                   @RequestParam(value = "page", required = false) Integer page,
                                   @RequestParam(value = "size", required = false) Integer size,
                                   @RequestParam(value = "approvalType", required = false) String approvalType,
                                   @RequestParam(value = "status", required = false) String status) {
        UserContext.UserInfo currentUser = UserContext.getCurrentUser();
        if (currentUser == null) {
            return ResultVOUtil.fail("未登录");
        }
        if (!Objects.equals(currentUser.getUserId(), applicantId)) {
            return ResultVOUtil.fail("无权查看他人申请");
        }

        QueryWrapper<Approval> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("applicant_id", applicantId);
        if (approvalType != null && !approvalType.trim().isEmpty()) {
            queryWrapper.eq("approval_type", approvalType.trim());
        }
        if (status != null && !status.trim().isEmpty()) {
            queryWrapper.eq("status", normalizeStatus(status.trim()));
        }
        queryWrapper.orderByDesc("create_time");

        if (page == null || size == null || page <= 0 || size <= 0) {
            List<Approval> list = approvalService.list(queryWrapper);
            return ResultVOUtil.success(list);
        }

        Page<Approval> approvalPage = new Page<>(page, size);
        Page<Approval> resultPage = approvalService.page(approvalPage, queryWrapper);
        PageVO pageVO = new PageVO();
        pageVO.setTotal(resultPage.getTotal());
        pageVO.setRecords(resultPage.getRecords());
        pageVO.setCurrent(resultPage.getCurrent());
        pageVO.setSize(resultPage.getSize());
        return ResultVOUtil.success(pageVO);
    }

    /**
     * 待审批列表（按角色过滤）
     * 系统管理员：查看所有待审批申请
     * 部门主管：只查看自己部门的待审批申请
     */
    @GetMapping("/pendingList")
    public ResultVO pendingList(
            @RequestParam(value = "approverId", required = false) Integer approverId,
            @RequestParam(value = "role", required = false) String role) {
        UserContext.UserInfo currentUser = UserContext.getCurrentUser();
        if (currentUser == null) {
            return ResultVOUtil.fail("未登录");
        }

        if (currentUser.getRole() == RoleType.EMPLOYEE) {
            return ResultVOUtil.success(Collections.emptyList());
        }

        QueryWrapper<Approval> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("status", STATUS_PENDING);

        if (currentUser.getRole() == RoleType.DEPARTMENT_MANAGER) {
            if (currentUser.getDepartmentId() == null) {
                return ResultVOUtil.success(Collections.emptyList());
            }
            queryWrapper.eq("applicant_department_id", currentUser.getDepartmentId());
        } else if (currentUser.getRole() != RoleType.SYSTEM_ADMIN && currentUser.getRole() != RoleType.PROCESS_ADMIN) {
            return ResultVOUtil.success(Collections.emptyList());
        }

        queryWrapper.orderByAsc("create_time");
        List<Approval> list = approvalService.list(queryWrapper);
        return ResultVOUtil.success(list);
    }

    /**
     * 获取待审批数量（用于红点提示）
     */
    @GetMapping("/pendingCount")
    public ResultVO pendingCount(
            @RequestParam(value = "approverId", required = false) Integer approverId,
            @RequestParam(value = "role", required = false) String role) {
        UserContext.UserInfo currentUser = UserContext.getCurrentUser();
        if (currentUser == null) {
            return ResultVOUtil.fail("未登录");
        }

        if (currentUser.getRole() == RoleType.EMPLOYEE) {
            return ResultVOUtil.success(0);
        }

        QueryWrapper<Approval> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("status", STATUS_PENDING);

        if (currentUser.getRole() == RoleType.DEPARTMENT_MANAGER) {
            if (currentUser.getDepartmentId() == null) {
                return ResultVOUtil.success(0);
            }
            queryWrapper.eq("applicant_department_id", currentUser.getDepartmentId());
        } else if (currentUser.getRole() != RoleType.SYSTEM_ADMIN && currentUser.getRole() != RoleType.PROCESS_ADMIN) {
            return ResultVOUtil.success(0);
        }

        long count = approvalService.count(queryWrapper);
        return ResultVOUtil.success(count);
    }

    /**
     * 审批处理（同意/拒绝）
     * 前端传入：{ id, status, approverId, approverName, approveReason }
     */
    @PutMapping("/approve")
    public ResultVO approve(@RequestBody Approval approval) {
        UserContext.UserInfo currentUser = UserContext.getCurrentUser();
        if (currentUser == null) {
            return ResultVOUtil.fail("未登录");
        }
        if (approval.getId() == null) {
            return ResultVOUtil.fail("申请ID不能为空");
        }
        String targetStatus = normalizeApproveStatus(approval.getStatus());
        if (targetStatus == null) {
            return ResultVOUtil.fail("审批状态不合法");
        }

        // 1. 验证申请是否存在
        Approval existing = approvalService.getById(approval.getId());
        if (existing == null) {
            return ResultVOUtil.fail("申请不存在");
        }

        // 2. 验证状态是否为"待审批"
        if (!STATUS_PENDING.equals(existing.getStatus())) {
            return ResultVOUtil.fail("只能处理待审批的申请");
        }
        if (!canApprove(currentUser, existing)) {
            return ResultVOUtil.fail("无权限审批该申请");
        }

        // 3. 带状态条件更新，防止重复提交覆盖
        Approval updateApproval = new Approval();
        updateApproval.setId(existing.getId());
        updateApproval.setStatus(targetStatus);
        updateApproval.setApproverId(currentUser.getUserId());
        updateApproval.setApproverName(currentUser.getName() != null ? currentUser.getName() : currentUser.getUsername());
        updateApproval.setApproveReason(approval.getApproveReason());
        updateApproval.setApproveTime(LocalDateTime.now());
        updateApproval.setUpdateTime(LocalDateTime.now());

        int affected = approvalService.approveApproval(updateApproval, STATUS_APPROVED.equals(targetStatus));
        if (affected == 0) {
            return ResultVOUtil.fail("申请状态已变更，请刷新后重试");
        }

        // 5. 如果是补卡申请且审批通过，更新考勤记录
        logger.info("approve: status={}, approvalType={}, equalsCheck={}, cardEquals={}", 
            targetStatus, existing.getApprovalType(),
            STATUS_APPROVED.equals(targetStatus), "card".equals(existing.getApprovalType()));
        if (STATUS_APPROVED.equals(targetStatus) && "card".equals(existing.getApprovalType())) {
            logger.info("approve: calling processCardApproval");
            processCardApproval(existing);
        }

        // 注意：事件发布已在 Service 层的 approveApproval 方法中处理，这里无需再发送消息
        return ResultVOUtil.success(null);
    }

    /**
     * 分页查询所有申请（权限控制）
     * 系统管理员：查看所有申请
     * 部门主管：只查看本部门申请
     * 普通员工：只能查看自己的申请
     */
    @GetMapping("/list/{page}/{size}")
    public ResultVO list(@PathVariable("page") Integer page, @PathVariable("size") Integer size) {
        UserContext.UserInfo currentUser = UserContext.getCurrentUser();
        if (currentUser == null) {
            return ResultVOUtil.fail("未登录");
        }

        Page<Approval> approvalPage = new Page<>(page, size);
        QueryWrapper<Approval> queryWrapper = new QueryWrapper<>();

        if (currentUser.getRole() == RoleType.EMPLOYEE) {
            queryWrapper.eq("applicant_id", currentUser.getUserId());
        } else if (currentUser.getRole() == RoleType.DEPARTMENT_MANAGER) {
            queryWrapper.eq("applicant_department_id", currentUser.getDepartmentId());
        }

        queryWrapper.orderByDesc("create_time");
        Page<Approval> resultPage = approvalService.page(approvalPage, queryWrapper);

        PageVO pageVO = new PageVO();
        pageVO.setTotal(resultPage.getTotal());
        pageVO.setRecords(resultPage.getRecords());
        pageVO.setCurrent(resultPage.getCurrent());
        pageVO.setSize(resultPage.getSize());
        return ResultVOUtil.success(pageVO);
    }

    /**
     * 根据状态查询（权限控制）
     */
    @GetMapping("/findByStatus/{status}")
    public ResultVO findByStatus(@PathVariable("status") String status) {
        UserContext.UserInfo currentUser = UserContext.getCurrentUser();
        if (currentUser == null) {
            return ResultVOUtil.fail("未登录");
        }

        QueryWrapper<Approval> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("status", status);

        if (currentUser.getRole() == RoleType.EMPLOYEE) {
            queryWrapper.eq("applicant_id", currentUser.getUserId());
        } else if (currentUser.getRole() == RoleType.DEPARTMENT_MANAGER) {
            queryWrapper.eq("applicant_department_id", currentUser.getDepartmentId());
        }

        queryWrapper.orderByDesc("create_time");
        List<Approval> list = approvalService.list(queryWrapper);
        return ResultVOUtil.success(list);
    }

    /**
     * 根据类型查询（权限控制）
     */
    @GetMapping("/findByType/{type}")
    public ResultVO findByType(@PathVariable("type") String type) {
        UserContext.UserInfo currentUser = UserContext.getCurrentUser();
        if (currentUser == null) {
            return ResultVOUtil.fail("未登录");
        }

        QueryWrapper<Approval> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("approval_type", type);

        if (currentUser.getRole() == RoleType.EMPLOYEE) {
            queryWrapper.eq("applicant_id", currentUser.getUserId());
        } else if (currentUser.getRole() == RoleType.DEPARTMENT_MANAGER) {
            queryWrapper.eq("applicant_department_id", currentUser.getDepartmentId());
        }

        queryWrapper.orderByDesc("create_time");
        List<Approval> list = approvalService.list(queryWrapper);
        return ResultVOUtil.success(list);
    }

    /**
     * 撤回申请
     */
    @PutMapping("/withdraw/{id}")
    public ResultVO withdraw(@PathVariable("id") Integer id) {
        UserContext.UserInfo currentUser = UserContext.getCurrentUser();
        if (currentUser == null) {
            return ResultVOUtil.fail("未登录");
        }

        Approval approval = approvalService.getById(id);
        if (approval == null) {
            return ResultVOUtil.fail("申请不存在");
        }
        if (!Objects.equals(approval.getApplicantId(), currentUser.getUserId())) {
            return ResultVOUtil.fail("只能撤回自己的申请");
        }
        if (!STATUS_PENDING.equals(approval.getStatus())) {
            return ResultVOUtil.fail("只能撤回待审批的申请");
        }

        // 调用新的 Service 方法，自动发布 WITHDRAWN 事件
        int affected = approvalService.withdrawApproval(
            id, 
            currentUser.getUserId(), 
            LocalDateTime.now(),
            approval.getApproverId(),  // 当前审批人
            approval.getApproverName()  // 当前审批人名称
        );
        if (affected == 0) {
            return ResultVOUtil.fail("申请状态已变更，请刷新后重试");
        }
        return ResultVOUtil.success(null);
    }

    /**
     * ⭐ 获取申请详情（按ID查询）
     * 前端调用：GET /approval/detail/{id}
     */
    @GetMapping("/detail/{id}")
    public ResultVO detail(@PathVariable("id") Integer id) {
        Approval approval = approvalService.getById(id);
        if (approval == null) {
            return ResultVOUtil.fail("申请不存在");
        }
        return ResultVOUtil.success(approval);
    }

    private boolean canApprove(UserContext.UserInfo currentUser, Approval approval) {
        RoleType role = currentUser.getRole();
        if (role == RoleType.SYSTEM_ADMIN || role == RoleType.PROCESS_ADMIN) {
            return true;
        }
        if (role == RoleType.DEPARTMENT_MANAGER) {
            return currentUser.getDepartmentId() != null
                    && currentUser.getDepartmentId().equals(approval.getApplicantDepartmentId());
        }
        return false;
    }

    private String normalizeApproveStatus(String status) {
        if ("approved".equals(status) || STATUS_APPROVED.equals(status)) {
            return STATUS_APPROVED;
        }
        if ("rejected".equals(status) || STATUS_REJECTED.equals(status)) {
            return STATUS_REJECTED;
        }
        return null;
    }

    private String normalizeStatus(String status) {
        if ("pending".equals(status)) {
            return STATUS_PENDING;
        }
        if ("approved".equals(status)) {
            return STATUS_APPROVED;
        }
        if ("rejected".equals(status)) {
            return STATUS_REJECTED;
        }
        if ("withdrawn".equals(status)) {
            return "已撤回";
        }
        return status;
    }

    private String validateByType(Approval approval) {
        if (approval.getTitle() == null || approval.getTitle().trim().isEmpty()) {
            return "申请标题不能为空";
        }
        switch (approval.getApprovalType()) {
            case "leave":
                if (approval.getStartTime() == null || approval.getEndTime() == null) {
                    return "请假申请需填写开始时间和结束时间";
                }
                if (approval.getEndTime().isBefore(approval.getStartTime())) {
                    return "结束时间不能早于开始时间";
                }
                return null;
            case "business":
                if (approval.getStartTime() == null || approval.getEndTime() == null) {
                    return "出差申请需填写开始时间和结束时间";
                }
                if (approval.getEndTime().isBefore(approval.getStartTime())) {
                    return "结束时间不能早于开始时间";
                }
                if (approval.getDestCity() == null || approval.getDestCity().trim().isEmpty()) {
                    return "出差申请需填写出差城市";
                }
                return null;
            case "overtime":
                if (approval.getWorkDate() == null || approval.getWorkDate().trim().isEmpty()
                        || approval.getStartTimeOnly() == null || approval.getStartTimeOnly().trim().isEmpty()
                        || approval.getEndTimeOnly() == null || approval.getEndTimeOnly().trim().isEmpty()) {
                    return "加班申请需填写日期与起止时间";
                }
                return null;
            case "reimburse":
                if (approval.getAmount() == null || approval.getAmount().signum() <= 0) {
                    return "报销申请金额必须大于0";
                }
                return null;
            case "purchase":
                if (approval.getQuantity() == null || approval.getQuantity() <= 0
                        || approval.getUnitPrice() == null || approval.getUnitPrice().signum() <= 0) {
                    return "采购申请数量和单价必须大于0";
                }
                return null;
            case "card":
                if (approval.getCardDate() == null || approval.getCardDate().trim().isEmpty()
                        || approval.getCardTime() == null || approval.getCardTime().trim().isEmpty()
                        || approval.getCardType() == null || approval.getCardType().trim().isEmpty()) {
                    return "补卡申请需填写补卡日期、时间和类型";
                }
                return null;
            default:
                return "审批类型不合法";
        }
    }
}