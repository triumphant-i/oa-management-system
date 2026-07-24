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
import java.util.*;

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

    @Autowired(required = false)
    private org.flowable.engine.TaskService flowableTaskService;

    @Autowired(required = false)
    private org.flowable.engine.HistoryService flowableHistoryService;

    @Autowired
    private com.southwind.service.flowable.ApprovalFlowableService approvalFlowableService;
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
     * 待审批列表（基于 Flowable 任务数据为准）
     * 系统管理员：查看所有待审批任务对应的申请
     * 部门主管：查看分配给自己且属于自己部门的待审批申请
     * 其他用户：查看分配给自己的待审批申请
     *
     * ===== 关键改进 =====
     * 1. 不再仅依赖 t_approval 的部门ID字段过滤
     * 2. 优先查询 Flowable TaskService 中分配给当前用户的任务
     * 3. 根据这些任务的流程实例 ID，反查对应的审批记录
     * 4. 这样可以确保"待审批列表"和"Flowable 真实分配"保持一致
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

        List<Approval> resultList = new ArrayList<>();

        try {
            // 系统管理员：查看所有待审批任务
            if (currentUser.getRole() == RoleType.SYSTEM_ADMIN || currentUser.getRole() == RoleType.PROCESS_ADMIN) {
                // 直接从 DB 查询所有待审批的记录（保证准确性）
                QueryWrapper<Approval> queryWrapper = new QueryWrapper<>();
                queryWrapper.eq("status", STATUS_PENDING).orderByAsc("create_time");
                resultList = approvalService.list(queryWrapper);

                // 额外尝试从 Flowable 补充：如果 Flowable 有活跃任务但 DB 没有对应的待审批记录，
                // 说明部分记录的 status 可能不匹配，也把它们加入结果列表
                if (flowableTaskService != null) {
                    List<org.flowable.task.api.Task> allTasks = flowableTaskService.createTaskQuery()
                            .active().list();

                    Set<Integer> existingIds = new HashSet<>();
                    for (Approval a : resultList) existingIds.add(a.getId());

                    Set<Integer> flowableApprovalIds = new HashSet<>();
                    for (org.flowable.task.api.Task task : allTasks) {
                        try {
                            Object idObj = flowableTaskService.getVariable(task.getId(), "approvalId");
                            if (idObj != null) {
                                Integer aid = null;
                                if (idObj instanceof Integer) aid = (Integer) idObj;
                                else if (idObj instanceof Long) aid = ((Long) idObj).intValue();
                                else if (idObj instanceof String) aid = Integer.parseInt((String) idObj);
                                if (aid != null) flowableApprovalIds.add(aid);
                            }
                        } catch (Exception e) { /* ignore */ }
                    }

                    // 把 Flowable 中存在但 DB 主查询没找到的 approvalId 也查出来
                    for (Integer fid : flowableApprovalIds) {
                        if (!existingIds.contains(fid)) {
                            Approval extra = approvalService.getById(fid);
                            if (extra != null) {
                                resultList.add(extra);
                                logger.info("Added missing Flowable approval: id={}, status={}", fid, extra.getStatus());
                            }
                        }
                    }

                    logger.info("pendingList for SYSTEM_ADMIN: DB={}, Flowable tasks={}, combined={}",
                        resultList.size(), allTasks.size(), resultList.size());
                }
            }
            // 部门主管：查看分配给自己的待审批任务对应的申请
            else if (currentUser.getRole() == RoleType.DEPARTMENT_MANAGER) {
                if (currentUser.getDepartmentId() == null) {
                    return ResultVOUtil.success(Collections.emptyList());
                }

                // 直接从 DB 查询
                QueryWrapper<Approval> dbQuery = new QueryWrapper<>();
                dbQuery.eq("status", STATUS_PENDING)
                       .eq("applicant_department_id", currentUser.getDepartmentId())
                       .orderByAsc("create_time");
                resultList = approvalService.list(dbQuery);

                // 从 Flowable 补充
                if (flowableTaskService != null) {
                    String assignee = String.valueOf(currentUser.getUserId());
                    List<org.flowable.task.api.Task> myTasks = flowableTaskService.createTaskQuery()
                            .taskAssignee(assignee).active().list();

                    Set<Integer> existingIds = new HashSet<>();
                    for (Approval a : resultList) existingIds.add(a.getId());

                    for (org.flowable.task.api.Task task : myTasks) {
                        try {
                            Object idObj = flowableTaskService.getVariable(task.getId(), "approvalId");
                            if (idObj != null) {
                                Integer aid = null;
                                if (idObj instanceof Integer) aid = (Integer) idObj;
                                else if (idObj instanceof Long) aid = ((Long) idObj).intValue();
                                else if (idObj instanceof String) aid = Integer.parseInt((String) idObj);
                                if (aid != null && !existingIds.contains(aid)) {
                                    Approval extra = approvalService.getById(aid);
                                    if (extra != null) resultList.add(extra);
                                }
                            }
                        } catch (Exception e) { /* ignore */ }
                    }
                }
            }
        } catch (Exception e) {
            logger.error("Error querying pending list: {}", e.getMessage(), e);
            QueryWrapper<Approval> queryWrapper = new QueryWrapper<>();
            queryWrapper.eq("status", STATUS_PENDING);
            if (currentUser.getRole() == RoleType.DEPARTMENT_MANAGER && currentUser.getDepartmentId() != null) {
                queryWrapper.eq("applicant_department_id", currentUser.getDepartmentId());
            }
            queryWrapper.orderByAsc("create_time");
            resultList = approvalService.list(queryWrapper);
        }

        return ResultVOUtil.success(resultList);
    }

    /**
     * 获取待审批数量（用于红点提示）
     * 基于 Flowable 任务数据为准
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

        long count = 0;

        try {
            if (currentUser.getRole() == RoleType.SYSTEM_ADMIN || currentUser.getRole() == RoleType.PROCESS_ADMIN) {
                // 优先从 DB 查询，确保计数和列表一致
                QueryWrapper<Approval> queryWrapper = new QueryWrapper<>();
                queryWrapper.eq("status", STATUS_PENDING);
                count = approvalService.count(queryWrapper);
                logger.info("Pending count for SYSTEM_ADMIN (DB): {}", count);
            } else if (currentUser.getRole() == RoleType.DEPARTMENT_MANAGER) {
                if (currentUser.getDepartmentId() == null) {
                    return ResultVOUtil.success(0);
                }
                
                if (flowableTaskService != null) {
                    String assignee = String.valueOf(currentUser.getUserId());
                    count = flowableTaskService.createTaskQuery()
                            .taskAssignee(assignee)
                            .active()
                            .count();
                    logger.info("Pending task count for DEPARTMENT_MANAGER userId={}: {}", 
                        currentUser.getUserId(), count);
                } else {
                    QueryWrapper<Approval> queryWrapper = new QueryWrapper<>();
                    queryWrapper.eq("status", STATUS_PENDING)
                            .eq("applicant_department_id", currentUser.getDepartmentId());
                    count = approvalService.count(queryWrapper);
                }
            }
        } catch (Exception e) {
            logger.error("Error querying pending count: {}", e.getMessage(), e);
            // 异常时降级到传统查询
            QueryWrapper<Approval> queryWrapper = new QueryWrapper<>();
            queryWrapper.eq("status", STATUS_PENDING);
            if (currentUser.getRole() == RoleType.DEPARTMENT_MANAGER && currentUser.getDepartmentId() != null) {
                queryWrapper.eq("applicant_department_id", currentUser.getDepartmentId());
            }
            count = approvalService.count(queryWrapper);
        }

        return ResultVOUtil.success(count);
    }

    /**
     * 审批处理（同意/拒绝）
     * 前端传入：{ id, status, approverId, approverName, approveReason }
     * 
     * 整个流程：
     * 1. 检查申请是否存在且状态为"待审批"
     * 2. 检查当前用户权限（与Flowable流程集成）
     * 3. 完成Flowable任务（不更新t_approval状态）
     * 4. 由流程结束监听器在整个流程完成时更新t_approval的最终状态
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
            logger.warn("Approval not found: id={}", approval.getId());
            return ResultVOUtil.fail("申请不存在");
        }

        logger.info("Processing approval: id={}, approverId={}, targetStatus={}, processInstanceId={}", 
                approval.getId(), currentUser.getUserId(), targetStatus, existing.getProcessInstanceId());

        // 2. 验证状态是否为"待审批"
        if (!STATUS_PENDING.equals(existing.getStatus())) {
            logger.warn("Approval not in pending status: id={}, currentStatus={}", 
                    approval.getId(), existing.getStatus());
            return ResultVOUtil.fail("只能处理待审批的申请");
        }

        // 3. 检查权限（与Flowable流程集成）
        if (!canApprove(currentUser, existing)) {
            logger.warn("Permission denied for approval: id={}, userId={}, role={}, processInstanceId={}", 
                    approval.getId(), currentUser.getUserId(), currentUser.getRole(), existing.getProcessInstanceId());
            return ResultVOUtil.fail("无权限审批该申请");
        }

        // 4. 准备审批数据
        Approval updateApproval = new Approval();
        updateApproval.setId(existing.getId());
        updateApproval.setStatus(targetStatus);  // ⚠️ 临时设置，会被流程监听器覆盖
        updateApproval.setApproverId(currentUser.getUserId());
        updateApproval.setApproverName(currentUser.getName() != null ? currentUser.getName() : currentUser.getUsername());
        updateApproval.setApproveReason(approval.getApproveReason());
        updateApproval.setApproveTime(LocalDateTime.now());
        updateApproval.setUpdateTime(LocalDateTime.now());

        // 5. 调用Service处理审批（完成Flowable任务）
        int affected = approvalService.approveApproval(updateApproval, STATUS_APPROVED.equals(targetStatus));
        if (affected == 0) {
            logger.warn("Failed to process approval: id={}, userId={}", approval.getId(), currentUser.getUserId());
            return ResultVOUtil.fail("申请状态已变更，请刷新后重试");
        }

        logger.info("Approval processed successfully: id={}, userId={}, targetStatus={}", 
                approval.getId(), currentUser.getUserId(), targetStatus);

        // 6. 如果是补卡申请且审批通过，更新考勤记录
        if (STATUS_APPROVED.equals(targetStatus) && "card".equals(existing.getApprovalType())) {
            logger.info("Processing card approval: id={}", approval.getId());
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

    /**
     * ⭐ 查询我的申请列表
     * 前端调用：GET /approval/my?status=xxx&page=1&size=10
     * 
     * @param status 可选的状态过滤（例如：待审批、已通过、已拒绝）
     * @param page 页码，默认 1
     * @param size 每页数量，默认 10
     * @return 分页的申请列表
     */
    @GetMapping("/my")
    public ResultVO listMyApprovals(
            @RequestParam(value = "status", required = false) String status,
            @RequestParam(value = "page", defaultValue = "1") int page,
            @RequestParam(value = "size", defaultValue = "10") int size) {
        UserContext.UserInfo currentUser = UserContext.getCurrentUser();
        if (currentUser == null) {
            return ResultVOUtil.fail("未登录");
        }
        
        logger.info("查询我的申请: userId={}, status={}, page={}, size={}", 
            currentUser.getUserId(), status, page, size);
        
        // 调用服务层方法
        PageVO pageVO = approvalService.listMyApprovals(
            currentUser.getUserId(), 
            status, 
            page, 
            size
        );
        
        return ResultVOUtil.success(pageVO);
    }

    private boolean canApprove(UserContext.UserInfo currentUser, Approval approval) {
        RoleType role = currentUser.getRole();
        
        // 系统管理员和流程管理员可以审批任何申请
        if (role == RoleType.SYSTEM_ADMIN || role == RoleType.PROCESS_ADMIN) {
            return true;
        }

        // 对于其他角色，需要检查：
        // 1. 申请是否关联了Flowable流程
        // 2. 当前用户是否被分配了该流程的待办任务
        
        if (approval.getProcessInstanceId() == null || approval.getProcessInstanceId().isEmpty()) {
            // 如果没有Flowable流程，降级到传统的角色+部门权限检查
            if (role == RoleType.DEPARTMENT_MANAGER) {
                return currentUser.getDepartmentId() != null
                        && currentUser.getDepartmentId().equals(approval.getApplicantDepartmentId());
            }
            return false;
        }

        // 有Flowable流程：检查当前用户是否被分配了任务
        String assignee = String.valueOf(currentUser.getUserId());
        return hasFlowableTask(approval.getProcessInstanceId(), assignee);
    }

    /**
     * 检查当前用户是否在该流程中被分配了待办任务
     * @param processInstanceId 流程实例ID
     * @param assignee 任务分配人（用户ID字符串）
     * @return true 表示有待办任务
     */
    private boolean hasFlowableTask(String processInstanceId, String assignee) {
        if (flowableTaskService == null) {
            // 如果Flowable TaskService不可用，使用传统权限检查
            logger.warn("Flowable TaskService not available, using traditional permission check");
            return false;
        }

        try {
            // 查询当前用户在该流程中是否有待办任务
            long taskCount = flowableTaskService.createTaskQuery()
                    .taskAssignee(assignee)
                    .processInstanceId(processInstanceId)
                    .active()
                    .count();
            
            boolean hasTask = taskCount > 0;
            logger.info("Flowable task check: processInstanceId={}, assignee={}, hasTask={}", 
                    processInstanceId, assignee, hasTask);
            
            return hasTask;
        } catch (Exception e) {
            logger.error("Error checking Flowable task for processInstanceId={}, assignee={}: {}", 
                    processInstanceId, assignee, e.getMessage());
            return false;
        }
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

    /**
     * 临时接口：为没有流程实例的待审批记录重新启动流程
     * 仅限内部测试使用！
     */
    @GetMapping("/fixExistingRecords")
    public ResultVO fixExistingRecords() {
        logger.info("===== 开始修复现有审批记录 =====");
        try {
            QueryWrapper<Approval> queryWrapper = new QueryWrapper<>();
            queryWrapper.eq("status", STATUS_PENDING)
                       .isNull("process_instance_id");
            List<Approval> pendingApprovals = approvalService.list(queryWrapper);
            
            logger.info("找到 {} 条没有流程实例的待审批记录", pendingApprovals.size());
            
            int fixedCount = 0;
            for (Approval approval : pendingApprovals) {
                try {
                    logger.info("正在修复审批记录 ID={}, 类型={}, 申请人={}", 
                        approval.getId(), approval.getApprovalType(), approval.getApplicantName());
                    
                    String processInstanceId = approvalFlowableService.startApprovalProcess(approval);
                    
                    Approval updateApproval = new Approval();
                    updateApproval.setId(approval.getId());
                    updateApproval.setProcessInstanceId(processInstanceId);
                    approvalService.updateById(updateApproval);
                    
                    logger.info("✓ 审批记录 ID={} 修复成功，流程实例ID={}", 
                        approval.getId(), processInstanceId);
                    fixedCount++;
                } catch (Exception e) {
                    logger.error("✗ 修复审批记录 ID={} 失败: {}", approval.getId(), e.getMessage(), e);
                }
            }
            
            logger.info("===== 修复完成，共修复 {} 条记录 =====", fixedCount);
            return ResultVOUtil.success("修复完成，共修复 " + fixedCount + " 条记录");
        } catch (Exception e) {
            logger.error("修复失败: {}", e.getMessage(), e);
            return ResultVOUtil.fail("修复失败: " + e.getMessage());
        }
    }

    /**
     * 获取审批进度信息
     * 前端调用：GET /approval/progress/{id}
     */
    @GetMapping("/progress/{id}")
    public ResultVO getApprovalProgress(@PathVariable("id") Integer id) {
        Approval approval = approvalService.getById(id);
        if (approval == null) {
            return ResultVOUtil.fail("申请不存在");
        }
        
        Map<String, Object> progress = new HashMap<>();
        progress.put("approvalId", approval.getId());
        progress.put("status", approval.getStatus());
        progress.put("processInstanceId", approval.getProcessInstanceId());
        
        // 如果有流程实例，查询流程进度（使用历史任务查询获取完整信息）
        if (approval.getProcessInstanceId() != null) {
            try {
                // 使用历史任务查询，这样可以获取已完成任务的结束时间
                List<org.flowable.task.api.history.HistoricTaskInstance> historicTasks = 
                    flowableHistoryService.createHistoricTaskInstanceQuery()
                        .processInstanceId(approval.getProcessInstanceId())
                        .orderByTaskCreateTime().asc()
                        .list();
                
                List<Map<String, Object>> taskList = new ArrayList<>();
                for (org.flowable.task.api.history.HistoricTaskInstance task : historicTasks) {
                    Map<String, Object> taskInfo = new HashMap<>();
                    taskInfo.put("id", task.getId());
                    taskInfo.put("name", task.getName());
                    taskInfo.put("assignee", task.getAssignee());
                    taskInfo.put("createTime", task.getCreateTime());
                    taskInfo.put("endTime", task.getEndTime());
                    taskInfo.put("isCompleted", task.getEndTime() != null);
                    taskList.add(taskInfo);
                }
                progress.put("tasks", taskList);
            } catch (Exception e) {
                logger.error("Error getting approval progress: {}", e.getMessage());
                progress.put("tasks", Collections.emptyList());
            }
        } else {
            progress.put("tasks", Collections.emptyList());
        }
        
        return ResultVOUtil.success(progress);
    }

    /**
     * 临时接口：为缺少 secondLevelApprover 变量的 SERIAL_AFTER_PARALLEL 流程补充变量
     * 仅限内部测试使用！
     */
    @GetMapping("/fixSecondLevelApprover")
    public ResultVO fixSecondLevelApprover() {
        logger.info("===== 开始修复存量 SERIAL_AFTER_PARALLEL 流程 =====");
        try {
            QueryWrapper<Approval> queryWrapper = new QueryWrapper<>();
            queryWrapper.eq("status", STATUS_PENDING)
                       .isNotNull("process_instance_id");
            List<Approval> pendingApprovals = approvalService.list(queryWrapper);
            
            logger.info("找到 {} 条有流程实例的待审批记录", pendingApprovals.size());
            
            int fixedCount = approvalFlowableService.fixAllMissingSecondLevelApprovers(pendingApprovals);
            
            logger.info("===== 修复完成，共修复 {} 条 SERIAL_AFTER_PARALLEL 流程 =====", fixedCount);
            return ResultVOUtil.success("修复完成，共修复 " + fixedCount + " 条 SERIAL_AFTER_PARALLEL 流程");
        } catch (Exception e) {
            logger.error("修复失败: {}", e.getMessage(), e);
            return ResultVOUtil.fail("修复失败: " + e.getMessage());
        }
    }
}