package com.southwind.repository;

import com.southwind.document.EmployeeDocument;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

import java.util.List;

/**
 * 员工ES Repository
 * 用于ES数据访问
 */
public interface EmployeeDocumentRepository extends ElasticsearchRepository<EmployeeDocument, Integer> {

    /**
     * 根据部门ID查询
     */
    List<EmployeeDocument> findByDepartmentId(Integer departmentId);

    /**
     * 根据状态查询
     */
    List<EmployeeDocument> findByStatus(String status);

    /**
     * 根据角色查询
     */
    List<EmployeeDocument> findByRole(String role);
}