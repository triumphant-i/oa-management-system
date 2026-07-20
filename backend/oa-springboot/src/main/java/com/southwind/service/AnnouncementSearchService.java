package com.southwind.service;

import com.southwind.document.AnnouncementDocument;
import com.southwind.repository.AnnouncementDocumentRepository;
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
 * 公告ES全文检索服务
 * 使用Elasticsearch实现公告搜索功能
 * 
 * 启用条件：spring.elasticsearch.uris 配置存在
 */
@Service
@ConditionalOnProperty(name = "spring.elasticsearch.uris")
public class AnnouncementSearchService {

    private static final Logger log = LoggerFactory.getLogger(AnnouncementSearchService.class);

    @Autowired
    private ElasticsearchRestTemplate elasticsearchTemplate;

    @Autowired
    private AnnouncementDocumentRepository announcementDocumentRepository;

    /**
     * 全文检索公告（关键词搜索）
     * 在标题和内容中搜索
     */
    public List<AnnouncementDocument> searchByKeyword(String keyword) {
        try {
            // 构建多字段匹配查询
            MultiMatchQueryBuilder queryBuilder = QueryBuilders.multiMatchQuery(keyword)
                    .field("title", 3.0f)      // 标题权重最高
                    .field("content", 2.0f)    // 内容权重第二
                    .field("publisherName", 1.0f) // 发布者权重最低
                    .type(MultiMatchQueryBuilder.Type.BEST_FIELDS);

            NativeSearchQuery searchQuery = new NativeSearchQueryBuilder()
                    .withQuery(queryBuilder)
                    .build();

            SearchHits<AnnouncementDocument> searchHits = elasticsearchTemplate.search(searchQuery, AnnouncementDocument.class);

            List<AnnouncementDocument> results = searchHits.stream()
                    .map(SearchHit::getContent)
                    .collect(Collectors.toList());

            log.info("ES全文检索公告成功，关键词: {}，结果数: {}", keyword, results.size());
            return results;
        } catch (Exception e) {
            log.error("ES全文检索公告失败，关键词: {}", keyword, e);
            return List.of();
        }
    }

    /**
     * 分页搜索公告（关键词 + 分页）
     */
    public Page<AnnouncementDocument> searchByKeywordWithPaging(String keyword, int page, int size) {
        try {
            Pageable pageable = PageRequest.of(page - 1, size);

            NativeSearchQuery searchQuery = new NativeSearchQueryBuilder()
                    .withQuery(QueryBuilders.multiMatchQuery(keyword)
                            .field("title", 3.0f)
                            .field("content", 2.0f)
                            .type(MultiMatchQueryBuilder.Type.BEST_FIELDS))
                    .withPageable(pageable)
                    .build();

            SearchHits<AnnouncementDocument> searchHits = elasticsearchTemplate.search(searchQuery, AnnouncementDocument.class);

            List<AnnouncementDocument> content = searchHits.stream()
                    .map(SearchHit::getContent)
                    .collect(Collectors.toList());

            return new org.springframework.data.domain.PageImpl<>(content, pageable, searchHits.getTotalHits());
        } catch (Exception e) {
            log.error("ES分页搜索公告失败，关键词: {}", keyword, e);
            return Page.empty();
        }
    }

    /**
     * 根据分类搜索公告
     */
    public List<AnnouncementDocument> searchByCategory(String category) {
        return announcementDocumentRepository.findByCategory(category);
    }

    /**
     * 根据状态搜索公告
     */
    public List<AnnouncementDocument> searchByStatus(String status) {
        return announcementDocumentRepository.findByStatus(status);
    }
}