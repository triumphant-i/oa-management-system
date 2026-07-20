package com.southwind.service.impl;

import com.southwind.entity.OperationLog;
import com.southwind.mapper.OperationLogMapper;
import com.southwind.service.OperationLogService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

@Service
public class OperationLogServiceImpl extends ServiceImpl<OperationLogMapper, OperationLog> implements OperationLogService {
}