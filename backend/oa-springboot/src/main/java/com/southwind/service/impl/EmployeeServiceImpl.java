package com.southwind.service.impl;

import com.southwind.entity.Employee;
import com.southwind.mapper.EmployeeMapper;
import com.southwind.service.EmployeeService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * 员工服务实现类
 */
@Service
public class EmployeeServiceImpl extends ServiceImpl<EmployeeMapper, Employee> implements EmployeeService {

    private static final Logger log = LoggerFactory.getLogger(EmployeeServiceImpl.class);

    @Override
    public int importEmployees(java.util.List<Employee> employees) {
        if (employees == null || employees.isEmpty()) {
            return 0;
        }
        int successCount = 0;
        for (Employee employee : employees) {
            try {
                save(employee);
                successCount++;
            } catch (Exception e) {
                log.warn("导入员工失败: " + employee.getName(), e);
            }
        }
        return successCount;
    }

}