package com.southwind.service.impl;

import com.southwind.entity.Department;
import com.southwind.mapper.DepartmentMapper;
import com.southwind.service.DepartmentService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

/**
 * 部门服务实现类
 */
@Service
public class DepartmentServiceImpl extends ServiceImpl<DepartmentMapper, Department> implements DepartmentService {

}