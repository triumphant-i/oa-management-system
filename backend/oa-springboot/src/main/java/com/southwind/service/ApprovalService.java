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
}