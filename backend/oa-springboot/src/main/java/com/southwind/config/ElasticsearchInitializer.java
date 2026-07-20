package com.southwind.config;

import com.southwind.service.ElasticsearchSyncService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Elasticsearch初始化器
 * 应用启动时自动同步MySQL数据到ES
 * 
 * 启用条件：spring.elasticsearch.uris 配置存在
 */
@Component
@ConditionalOnProperty(name = "spring.elasticsearch.uris")
public class ElasticsearchInitializer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(ElasticsearchInitializer.class);

    @Autowired
    private ElasticsearchSyncService elasticsearchSyncService;

    @Override
    public void run(ApplicationArguments args) throws Exception {
        log.info("========== 开始初始化Elasticsearch数据 ==========");

        try {
            // 同步员工数据
            log.info("正在同步员工数据到ES...");
            elasticsearchSyncService.syncAllEmployees();
            log.info("员工数据同步完成");

            // 同步公告数据
            log.info("正在同步公告数据到ES...");
            elasticsearchSyncService.syncAllAnnouncements();
            log.info("公告数据同步完成");

            log.info("========== Elasticsearch数据初始化完成 ==========");
        } catch (Exception e) {
            log.error("Elasticsearch数据初始化失败", e);
            // 不抛出异常，允许应用继续启动
        }
    }
}