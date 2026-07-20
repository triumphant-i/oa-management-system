package com.southwind.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.southwind.entity.Approval;
import com.southwind.entity.Message;
import com.southwind.service.ApprovalService;
import com.southwind.service.EmployeeService;
import com.southwind.service.MessageService;
import com.southwind.util.ResultVOUtil;
import com.southwind.vo.PageVO;
import com.southwind.vo.ResultVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 审批控制器
 */
@RestController
@RequestMapping("/approval")
public class ApprovalController {

    @Autowired
    private ApprovalService approvalService;

    @Autowired
    private MessageService messageService;

    @Autowired
    private EmployeeService employeeService;

    /**
     * 提交申请
     */
    @PostMapping("/submit")
    public ResultVO submit(@RequestBody Approval approval) {
        approval.setStatus("待审批");
        approval.setCreateTime(LocalDateTime.now());
        boolean save = approvalService.save(approval);
        if (!save) return ResultVOUtil.fail("提交失败");

        Message message = new Message();
        message.setSenderId(approval.getApplicantId());
        message.setSenderName(approval.getApplicantName());
        message.setReceiverId(approval.getApplicantId());
        message.setReceiverName(approval.getApplicantName());
        message.setTitle("申请提交成功");
        message.setContent("您的" + getApprovalTypeName(approval.getApprovalType()) + "已提交，等待审批中。");
        message.setMsgType("APPROVAL");
        message.setRelatedId(approval.getId());
        message.setIsRead(0);
        message.setIsTop(0);
        messageService.sendMessage(message);

        return ResultVOUtil.success(approval.getId());
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

    /**
     * 查询我的申请
     */
    @GetMapping("/myApplications/{applicantId}")
    public ResultVO myApplications(@PathVariable("applicantId") Integer applicantId) {
        QueryWrapper<Approval> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("applicant_id", applicantId);
        queryWrapper.orderByDesc("create_time");
        List<Approval> list = approvalService.list(queryWrapper);
        return ResultVOUtil.success(list);
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
        
        QueryWrapper<Approval> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("status", "待审批");
        
        if (role != null && "DEPARTMENT_MANAGER".equals(role) && approverId != null) {
            com.southwind.entity.Employee employee = employeeService.getById(approverId);
            if (employee != null && employee.getDepartmentId() != null) {
                queryWrapper.eq("applicant_department_id", employee.getDepartmentId());
            }
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
        
        QueryWrapper<Approval> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("status", "待审批");
        
        if (role != null && "DEPARTMENT_MANAGER".equals(role) && approverId != null) {
            com.southwind.entity.Employee employee = employeeService.getById(approverId);
            if (employee != null && employee.getDepartmentId() != null) {
                queryWrapper.eq("applicant_department_id", employee.getDepartmentId());
            }
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
        // 1. 验证申请是否存在
        Approval existing = approvalService.getById(approval.getId());
        if (existing == null) {
            return ResultVOUtil.fail("申请不存在");
        }

        // 2. 验证状态是否为"待审批"
        if (!"待审批".equals(existing.getStatus())) {
            return ResultVOUtil.fail("只能处理待审批的申请");
        }

        // 3. 更新审批信息
        existing.setStatus(approval.getStatus()); // "已通过" 或 "已拒绝"
        existing.setApproverId(approval.getApproverId());
        existing.setApproverName(approval.getApproverName());
        existing.setApproveReason(approval.getApproveReason());
        existing.setApproveTime(LocalDateTime.now());
        existing.setUpdateTime(LocalDateTime.now());

        // 4. 保存
        boolean update = approvalService.updateById(existing);
        if (!update) {
            return ResultVOUtil.fail("审批失败");
        }

        // 5. 发送审批结果通知
        Message message = new Message();
        message.setSenderId(approval.getApproverId());
        message.setSenderName(approval.getApproverName());
        message.setReceiverId(existing.getApplicantId());
        message.setReceiverName(existing.getApplicantName());
        
        if ("已通过".equals(approval.getStatus())) {
            message.setTitle("申请已通过");
            message.setContent("您的" + getApprovalTypeName(existing.getApprovalType()) + "已由" + approval.getApproverName() + "审批通过。");
        } else {
            message.setTitle("申请已拒绝");
            String reason = approval.getApproveReason() != null && !approval.getApproveReason().isEmpty() 
                ? "，理由：" + approval.getApproveReason() 
                : "";
            message.setContent("您的" + getApprovalTypeName(existing.getApprovalType()) + "已由" + approval.getApproverName() + "拒绝" + reason + "。");
        }
        message.setMsgType("APPROVAL");
        message.setRelatedId(existing.getId());
        message.setIsRead(0);
        message.setIsTop(0);
        messageService.sendMessage(message);

        return ResultVOUtil.success(null);
    }

    /**
     * 分页查询所有申请
     */
    @GetMapping("/list/{page}/{size}")
    public ResultVO list(@PathVariable("page") Integer page, @PathVariable("size") Integer size) {
        Page<Approval> approvalPage = new Page<>(page, size);
        QueryWrapper<Approval> queryWrapper = new QueryWrapper<>();
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
     * 根据状态查询
     */
    @GetMapping("/findByStatus/{status}")
    public ResultVO findByStatus(@PathVariable("status") String status) {
        QueryWrapper<Approval> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("status", status);
        queryWrapper.orderByDesc("create_time");
        List<Approval> list = approvalService.list(queryWrapper);
        return ResultVOUtil.success(list);
    }

    /**
     * 根据类型查询
     */
    @GetMapping("/findByType/{type}")
    public ResultVO findByType(@PathVariable("type") String type) {
        QueryWrapper<Approval> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("approval_type", type);
        queryWrapper.orderByDesc("create_time");
        List<Approval> list = approvalService.list(queryWrapper);
        return ResultVOUtil.success(list);
    }

    /**
     * 撤回申请
     */
    @PutMapping("/withdraw/{id}")
    public ResultVO withdraw(@PathVariable("id") Integer id) {
        Approval approval = approvalService.getById(id);
        if (approval == null) {
            return ResultVOUtil.fail("申请不存在");
        }
        if (!"待审批".equals(approval.getStatus())) {
            return ResultVOUtil.fail("只能撤回待审批的申请");
        }

        Approval updateApproval = new Approval();
        updateApproval.setId(id);
        updateApproval.setStatus("已撤回");
        updateApproval.setUpdateTime(LocalDateTime.now());

        boolean update = approvalService.updateById(updateApproval);
        if (!update) return ResultVOUtil.fail("撤回失败");
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
}