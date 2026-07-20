package com.southwind.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.stereotype.Component;

/**
 * Nacos动态配置监听器
 * 用于动态刷新配置
 * 
 * 启用条件：spring.cloud.nacos.config.server-addr 配置存在
 */
@Component
@RefreshScope
@ConditionalOnProperty(name = "spring.cloud.nacos.config.server-addr")
public class NacosConfigListener {

    private static final Logger log = LoggerFactory.getLogger(NacosConfigListener.class);

    // 这里可以添加需要动态刷新的配置属性
    // 例如：
    // @Value("${custom.config.value}")
    // private String customValue;

    /**
     * 监听配置变更
     * 使用@RefreshScope注解，当Nacos配置变更时自动刷新
     */
    public void onConfigChange() {
        log.info("Nacos配置已动态刷新");
    }
}