package com.southwind.service;

import com.southwind.entity.Employee;
import com.baomidou.mybatisplus.extension.service.IService;

/**
 * 员工服务类
 */
public interface EmployeeService extends IService<Employee> {

    int importEmployees(java.util.List<Employee> employees);

}