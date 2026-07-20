package com.southwind.service;

import com.southwind.document.AnnouncementDocument;
import com.southwind.document.EmployeeDocument;
import com.southwind.entity.Announcement;
import com.southwind.entity.Employee;
import com.southwind.repository.AnnouncementDocumentRepository;
import com.southwind.repository.EmployeeDocumentRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Elasticsearch数据同步服务
 * 负责将MySQL数据同步到ES索引
 * 
 * 启用条件：spring.elasticsearch.uris 配置存在
 */
@Service
@ConditionalOnProperty(name = "spring.elasticsearch.uris")
public class ElasticsearchSyncService {

    private static final Logger log = LoggerFactory.getLogger(ElasticsearchSyncService.class);

    @Autowired
    private EmployeeDocumentRepository employeeDocumentRepository;

    @Autowired
    private AnnouncementDocumentRepository announcementDocumentRepository;

    @Autowired
    private EmployeeService employeeService;

    @Autowired
    private AnnouncementService announcementService;

    /**
     * 同步单个员工数据到ES
     */
    public void syncEmployee(Employee employee, String departmentName) {
        try {
            EmployeeDocument document = new EmployeeDocument();
            BeanUtils.copyProperties(employee, document);
            document.setDepartmentName(departmentName);
            employeeDocumentRepository.save(document);
            log.info("同步员工数据到ES成功: {}", employee.getName());
        } catch (Exception e) {
            log.error("同步员工数据到ES失败: {}", employee.getName(), e);
        }
    }

    /**
     * 批量同步员工数据到ES
     */
    public void syncAllEmployees() {
        try {
            List<Employee> employees = employeeService.list();
            List<EmployeeDocument> documents = employees.stream()
                    .map(emp -> {
                        EmployeeDocument doc = new EmployeeDocument();
                        BeanUtils.copyProperties(emp, doc);
                        // TODO: 查询部门名称并设置
                        return doc;
                    })
                    .collect(Collectors.toList());
            employeeDocumentRepository.saveAll(documents);
            log.info("批量同步员工数据到ES成功，共{}条", documents.size());
        } catch (Exception e) {
            log.error("批量同步员工数据到ES失败", e);
        }
    }

    /**
     * 同步单个公告数据到ES
     */
    public void syncAnnouncement(Announcement announcement) {
        try {
            AnnouncementDocument document = new AnnouncementDocument();
            BeanUtils.copyProperties(announcement, document);
            announcementDocumentRepository.save(document);
            log.info("同步公告数据到ES成功: {}", announcement.getTitle());
        } catch (Exception e) {
            log.error("同步公告数据到ES失败: {}", announcement.getTitle(), e);
        }
    }

    /**
     * 批量同步公告数据到ES
     */
    public void syncAllAnnouncements() {
        try {
            List<Announcement> announcements = announcementService.list();
            List<AnnouncementDocument> documents = announcements.stream()
                    .map(ann -> {
                        AnnouncementDocument doc = new AnnouncementDocument();
                        BeanUtils.copyProperties(ann, doc);
                        return doc;
                    })
                    .collect(Collectors.toList());
            announcementDocumentRepository.saveAll(documents);
            log.info("批量同步公告数据到ES成功，共{}条", documents.size());
        } catch (Exception e) {
            log.error("批量同步公告数据到ES失败", e);
        }
    }

    /**
     * 删除员工ES文档
     */
    public void deleteEmployee(Integer employeeId) {
        try {
            employeeDocumentRepository.deleteById(employeeId);
            log.info("删除员工ES文档成功: {}", employeeId);
        } catch (Exception e) {
            log.error("删除员工ES文档失败: {}", employeeId, e);
        }
    }

    /**
     * 删除公告ES文档
     */
    public void deleteAnnouncement(Integer announcementId) {
        try {
            announcementDocumentRepository.deleteById(announcementId);
            log.info("删除公告ES文档成功: {}", announcementId);
        } catch (Exception e) {
            log.error("删除公告ES文档失败: {}", announcementId, e);
        }
    }
}