package com.southwind.service;

import com.southwind.document.EmployeeDocument;
import com.southwind.repository.EmployeeDocumentRepository;
import org.elasticsearch.index.query.MultiMatchQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.core.ElasticsearchRestTemplate;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.query.NativeSearchQuery;
import org.springframework.data.elasticsearch.core.query.NativeSearchQueryBuilder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 员工ES全文检索服务
 * 使用Elasticsearch实现员工搜索功能
 * 
 * 启用条件：spring.elasticsearch.uris 配置存在
 */
@Service
@ConditionalOnProperty(name = "spring.elasticsearch.uris")
public class EmployeeSearchService {

    private static final Logger log = LoggerFactory.getLogger(EmployeeSearchService.class);

    @Autowired
    private ElasticsearchRestTemplate elasticsearchTemplate;

    @Autowired
    private EmployeeDocumentRepository employeeDocumentRepository;

    /**
     * 全文检索员工（关键词搜索）
     * 在姓名、用户名、邮箱、职位中搜索
     */
    public List<EmployeeDocument> searchByKeyword(String keyword) {
        try {
            // 构建多字段匹配查询
            MultiMatchQueryBuilder queryBuilder = QueryBuilders.multiMatchQuery(keyword)
                    .field("name", 3.0f)       // 姓名权重最高
                    .field("username", 2.0f)   // 用户名权重第二
                    .field("position", 1.5f)   // 职位权重第三
                    .field("email", 1.0f)      // 邮箱权重最低
                    .type(MultiMatchQueryBuilder.Type.BEST_FIELDS);

            NativeSearchQuery searchQuery = new NativeSearchQueryBuilder()
                    .withQuery(queryBuilder)
                    .build();

            SearchHits<EmployeeDocument> searchHits = elasticsearchTemplate.search(searchQuery, EmployeeDocument.class);

            List<EmployeeDocument> results = searchHits.stream()
                    .map(SearchHit::getContent)
                    .collect(Collectors.toList());

            log.info("ES全文检索员工成功，关键词: {}，结果数: {}", keyword, results.size());
            return results;
        } catch (Exception e) {
            log.error("ES全文检索员工失败，关键词: {}", keyword, e);
            return List.of();
        }
    }

    /**
     * 分页搜索员工（关键词 + 分页）
     */
    public Page<EmployeeDocument> searchByKeywordWithPaging(String keyword, int page, int size) {
        try {
            Pageable pageable = PageRequest.of(page - 1, size);

            NativeSearchQuery searchQuery = new NativeSearchQueryBuilder()
                    .withQuery(QueryBuilders.multiMatchQuery(keyword)
                            .field("name", 3.0f)
                            .field("username", 2.0f)
                            .field("position", 1.5f)
                            .type(MultiMatchQueryBuilder.Type.BEST_FIELDS))
                    .withPageable(pageable)
                    .build();

            SearchHits<EmployeeDocument> searchHits = elasticsearchTemplate.search(searchQuery, EmployeeDocument.class);

            // 转换为Page对象（简化处理）
            List<EmployeeDocument> content = searchHits.stream()
                    .map(SearchHit::getContent)
                    .collect(Collectors.toList());

            return new org.springframework.data.domain.PageImpl<>(content, pageable, searchHits.getTotalHits());
        } catch (Exception e) {
            log.error("ES分页搜索员工失败，关键词: {}", keyword, e);
            return Page.empty();
        }
    }

    /**
     * 根据部门ID搜索员工
     */
    public List<EmployeeDocument> searchByDepartmentId(Integer departmentId) {
        try {
            NativeSearchQuery searchQuery = new NativeSearchQueryBuilder()
                    .withQuery(QueryBuilders.termQuery("departmentId", departmentId))
                    .build();

            SearchHits<EmployeeDocument> searchHits = elasticsearchTemplate.search(searchQuery, EmployeeDocument.class);

            List<EmployeeDocument> results = searchHits.stream()
                    .map(SearchHit::getContent)
                    .collect(Collectors.toList());

            log.info("ES按部门搜索员工成功，部门ID: {}，结果数: {}", departmentId, results.size());
            return results;
        } catch (Exception e) {
            log.error("ES按部门搜索员工失败，部门ID: {}", departmentId, e);
            return List.of();
        }
    }

    /**
     * 同步员工数据到ES
     */
    public void syncEmployeeToEs(EmployeeDocument employeeDocument) {
        try {
            employeeDocumentRepository.save(employeeDocument);
            log.info("同步员工数据到ES成功，员工ID: {}", employeeDocument.getId());
        } catch (Exception e) {
            log.error("同步员工数据到ES失败，员工ID: {}", employeeDocument.getId(), e);
        }
    }

    /**
     * 批量同步员工数据到ES
     */
    public void syncEmployeesToEs(List<EmployeeDocument> employeeDocuments) {
        try {
            employeeDocumentRepository.saveAll(employeeDocuments);
            log.info("批量同步员工数据到ES成功，数量: {}", employeeDocuments.size());
        } catch (Exception e) {
            log.error("批量同步员工数据到ES失败", e);
        }
    }

    /**
     * 从ES中删除员工数据
     */
    public void deleteEmployeeFromEs(Integer employeeId) {
        try {
            employeeDocumentRepository.deleteById(employeeId);
            log.info("从ES中删除员工数据成功，员工ID: {}", employeeId);
        } catch (Exception e) {
            log.error("从ES中删除员工数据失败，员工ID: {}", employeeId, e);
        }
    }

    /**
     * 根据部门搜索员工
     */
    public List<EmployeeDocument> searchByDepartment(Integer departmentId) {
        return searchByDepartmentId(departmentId);
    }

    /**
     * 根据状态搜索员工
     */
    public List<EmployeeDocument> searchByStatus(String status) {
        try {
            NativeSearchQuery searchQuery = new NativeSearchQueryBuilder()
                    .withQuery(QueryBuilders.termQuery("status", status))
                    .build();

            SearchHits<EmployeeDocument> searchHits = elasticsearchTemplate.search(searchQuery, EmployeeDocument.class);

            List<EmployeeDocument> results = searchHits.stream()
                    .map(SearchHit::getContent)
                    .collect(Collectors.toList());

            log.info("ES按状态搜索员工成功，状态: {}，结果数: {}", status, results.size());
            return results;
        } catch (Exception e) {
            log.error("ES按状态搜索员工失败，状态: {}", status, e);
            return List.of();
        }
    }
}