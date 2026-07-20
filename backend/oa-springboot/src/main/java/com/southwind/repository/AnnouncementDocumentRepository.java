package com.southwind.repository;

import com.southwind.document.AnnouncementDocument;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

import java.util.List;

/**
 * 公告ES Repository
 * 用于ES数据访问
 */
public interface AnnouncementDocumentRepository extends ElasticsearchRepository<AnnouncementDocument, Integer> {

    /**
     * 根据状态查询
     */
    List<AnnouncementDocument> findByStatus(String status);

    /**
     * 根据分类查询
     */
    List<AnnouncementDocument> findByCategory(String category);

    /**
     * 根据发布者ID查询
     */
    List<AnnouncementDocument> findByPublisherId(Integer publisherId);
}