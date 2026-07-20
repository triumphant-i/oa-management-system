package com.southwind.controller;

import com.southwind.annotation.RequirePermission;
import com.southwind.common.UserContext;
import com.southwind.constant.SystemConstants;
import com.southwind.document.AnnouncementDocument;
import com.southwind.document.EmployeeDocument;
import com.southwind.enums.RoleType;
import com.southwind.service.AnnouncementSearchService;
import com.southwind.service.EmployeeSearchService;
import com.southwind.util.RedisCacheUtil;
import com.southwind.util.ResultVOUtil;
import com.southwind.vo.PageVO;
import com.southwind.vo.ResultVO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 搜索控制器
 * 使用Elasticsearch实现全文检索
 * 使用Redis实现缓存优化
 * 
 * 启用条件：spring.elasticsearch.uris 配置存在
 */
@RestController
@RequestMapping("/search")
@ConditionalOnProperty(name = "spring.elasticsearch.uris")
public class SearchController {

    private static final Logger log = LoggerFactory.getLogger(SearchController.class);

    @Autowired
    private EmployeeSearchService employeeSearchService;

    @Autowired
    private AnnouncementSearchService announcementSearchService;

    @Autowired
    private RedisCacheUtil redisCacheUtil;

    /**
     * 员工全文检索（ES）
     * 使用Elasticsearch替代MySQL模糊查询
     * 支持在姓名、用户名、邮箱、职位中搜索
     */
    @GetMapping("/employee")
    public ResultVO searchEmployee(
            @RequestParam String keyword,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size) {

        try {
            // 权限校验
            UserContext.UserInfo currentUser = UserContext.getCurrentUser();
            if (currentUser == null) {
                return ResultVOUtil.fail(SystemConstants.MSG_NOT_LOGIN);
            }

            // 只有系统管理员和部门主管可以搜索
            if (currentUser.getRole() != RoleType.SYSTEM_ADMIN &&
                currentUser.getRole() != RoleType.DEPARTMENT_MANAGER) {
                return ResultVOUtil.fail("无权搜索员工");
            }

            // 使用ES全文检索
            List<EmployeeDocument> results = employeeSearchService.searchByKeyword(keyword);

            // 部门主管只能查看本部门员工
            if (currentUser.getRole() == RoleType.DEPARTMENT_MANAGER) {
                results = results.stream()
                        .filter(emp -> emp.getDepartmentId().equals(currentUser.getDepartmentId()))
                        .toList();
            }

            PageVO pageVO = new PageVO();
            pageVO.setData(results);
            pageVO.setTotal((long) results.size());

            log.info("ES全文检索员工: 关键词={}, 结果数={}", keyword, results.size());
            return ResultVOUtil.success(pageVO);
        } catch (Exception e) {
            log.error("ES全文检索员工失败", e);
            return ResultVOUtil.fail("搜索失败: " + e.getMessage());
        }
    }

    /**
     * 公告全文检索（ES）
     * 使用Elasticsearch替代MySQL模糊查询
     * 支持在标题、内容中搜索
     */
    @GetMapping("/announcement")
    public ResultVO searchAnnouncement(
            @RequestParam String keyword,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size) {

        try {
            // 构建缓存key
            String cacheKey = "search:announcement:" + keyword + ":" + page + ":" + size;

            // 先从Redis缓存获取
            Object cachedResult = redisCacheUtil.get(cacheKey);
            if (cachedResult != null) {
                log.info("从Redis缓存获取公告搜索结果: {}", cacheKey);
                return ResultVOUtil.success(cachedResult);
            }

            // 使用ES全文检索
            List<AnnouncementDocument> results = announcementSearchService.searchByKeyword(keyword);

            // 只返回已发布的公告
            results = results.stream()
                    .filter(ann -> "已发布".equals(ann.getStatus()))
                    .toList();

            PageVO pageVO = new PageVO();
            pageVO.setData(results);
            pageVO.setTotal((long) results.size());

            // 缓存搜索结果（10分钟）
            redisCacheUtil.set(cacheKey, pageVO, 10, TimeUnit.MINUTES);

            log.info("ES全文检索公告: 关键词={}, 结果数={}", keyword, results.size());
            return ResultVOUtil.success(pageVO);
        } catch (Exception e) {
            log.error("ES全文检索公告失败", e);
            return ResultVOUtil.fail("搜索失败: " + e.getMessage());
        }
    }

    /**
     * 按部门搜索员工（ES + Redis缓存）
     */
    @GetMapping("/employee/department/{departmentId}")
    @Cacheable(value = "employee:department", key = "#departmentId", unless = "#result == null")
    public ResultVO searchEmployeeByDepartment(@PathVariable Integer departmentId) {
        try {
            List<EmployeeDocument> results = employeeSearchService.searchByDepartment(departmentId);

            PageVO pageVO = new PageVO();
            pageVO.setData(results);
            pageVO.setTotal((long) results.size());

            log.info("ES按部门搜索员工: 部门ID={}, 结果数={}", departmentId, results.size());
            return ResultVOUtil.success(pageVO);
        } catch (Exception e) {
            log.error("ES按部门搜索员工失败", e);
            return ResultVOUtil.fail("搜索失败: " + e.getMessage());
        }
    }

    /**
     * 按状态搜索员工（ES + Redis缓存）
     */
    @GetMapping("/employee/status/{status}")
    @Cacheable(value = "employee:status", key = "#status", unless = "#result == null")
    public ResultVO searchEmployeeByStatus(@PathVariable String status) {
        try {
            List<EmployeeDocument> results = employeeSearchService.searchByStatus(status);

            PageVO pageVO = new PageVO();
            pageVO.setData(results);
            pageVO.setTotal((long) results.size());

            log.info("ES按状态搜索员工: 状态={}, 结果数={}", status, results.size());
            return ResultVOUtil.success(pageVO);
        } catch (Exception e) {
            log.error("ES按状态搜索员工失败", e);
            return ResultVOUtil.fail("搜索失败: " + e.getMessage());
        }
    }

    /**
     * 清除员工搜索缓存
     */
    @DeleteMapping("/cache/employee")
    @RequirePermission(roles = {RoleType.SYSTEM_ADMIN}, description = "清除员工搜索缓存")
    public ResultVO clearEmployeeCache() {
        try {
            // 清除所有员工相关缓存
            Long deleted1 = redisCacheUtil.deleteByPattern("employee:department:*");
            Long deleted2 = redisCacheUtil.deleteByPattern("employee:status:*");
            log.info("清除员工搜索缓存成功，删除{}条缓存", deleted1 + deleted2);
            return ResultVOUtil.success("缓存已清除");
        } catch (Exception e) {
            log.error("清除员工搜索缓存失败", e);
            return ResultVOUtil.fail("清除缓存失败");
        }
    }
}