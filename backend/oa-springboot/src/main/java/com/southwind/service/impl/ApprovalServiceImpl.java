package com.southwind.service.impl;

import com.southwind.entity.Approval;
import com.southwind.mapper.ApprovalMapper;
import com.southwind.service.ApprovalService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

/**
 * 审批服务实现类
 */
@Service
public class ApprovalServiceImpl extends ServiceImpl<ApprovalMapper, Approval> implements ApprovalService {

}