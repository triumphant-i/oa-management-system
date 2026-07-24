package com.southwind.service;

import com.southwind.entity.Approval;
import com.baomidou.mybatisplus.extension.service.IService;

import java.time.LocalDateTime;

/**
 * 审批服务类
 */
public interface ApprovalService extends IService<Approval> {

    /**
     * 仅当状态为"待审批"时更新审批结果，返回受影响行数
     */
    int updateApprovalIfPending(Approval approval);

    /**
     * 仅当申请人为本人且状态为"待审批"时撤回，返回受影响行数
     */
    int withdrawIfApplicantPending(Integer id, Integer applicantId, LocalDateTime updateTime);

    /**
     * 提交审批申请（包含事件发布）
     * 自动查询第一级审批人（部门主管），发布 SUBMITTED 事件
     */
    Approval submitApproval(Approval approval);

    /**
     * 审批处理（通过/拒绝）（包含事件发布）
     * 通过时发布 APPROVED_FINAL 事件（暂时；后续工作流多级后改为区分APPROVED_NODE）
     * 拒绝时发布 REJECTED 事件
     */
    int approveApproval(Approval approval, boolean approved);

    /**
     * 撤回审批申请（包含事件发布）
     * 发布 WITHDRAWN 事件，通知当前审批人
     */
    int withdrawApproval(Integer id, Integer applicantId, LocalDateTime updateTime, Integer approverId, String approverName);

    /**
     * 查询我的审批列表（分页）
     */
    com.southwind.vo.PageVO listMyApprovals(Integer applicantId, String status, int page, int size);
}