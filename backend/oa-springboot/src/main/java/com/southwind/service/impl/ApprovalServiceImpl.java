package com.southwind.service.impl;

import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.southwind.entity.Approval;
import com.southwind.mapper.ApprovalMapper;
import com.southwind.service.ApprovalService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * 审批服务实现类
 */
@Service
public class ApprovalServiceImpl extends ServiceImpl<ApprovalMapper, Approval> implements ApprovalService {

    private static final String STATUS_PENDING = "待审批";
    private static final String STATUS_WITHDRAWN = "已撤回";

    @Override
    public int updateApprovalIfPending(Approval approval) {
        UpdateWrapper<Approval> updateWrapper = new UpdateWrapper<>();
        updateWrapper.eq("id", approval.getId()).eq("status", STATUS_PENDING);
        return this.baseMapper.update(approval, updateWrapper);
    }

    @Override
    public int withdrawIfApplicantPending(Integer id, Integer applicantId, LocalDateTime updateTime) {
        Approval updateApproval = new Approval();
        updateApproval.setStatus(STATUS_WITHDRAWN);
        updateApproval.setUpdateTime(updateTime);

        UpdateWrapper<Approval> updateWrapper = new UpdateWrapper<>();
        updateWrapper.eq("id", id)
                .eq("applicant_id", applicantId)
                .eq("status", STATUS_PENDING);
        return this.baseMapper.update(updateApproval, updateWrapper);
    }
}