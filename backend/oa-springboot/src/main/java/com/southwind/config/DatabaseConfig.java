package com.southwind.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * 数据库版本配置
 * 当前使用数据库版本：v2.1
 * 
 * v2.1新增功能：
 * 1. 删除t_admin表，统一使用t_employee管理所有用户
 * 2. 新增3个触发器（会议室时间冲突检测、员工姓名自动同步）
 * 3. 新增4个存储过程（会议室检查、ES同步、考勤统计、审批报表）
 * 4. 新增5个视图（员工详情、部门详情、审批统计、技术监控、会议详情）
 * 5. 优化索引设计（新增10+个索引）
 * 
 * 后端代码适配：
 * 1. 新增StoredProceduresMapper - 存储过程调用
 * 2. 新增DatabaseProcedureService - 存储过程服务
 * 3. 新增MeetingConflictException - 会议室冲突异常
 * 4. 更新MeetingServiceImpl - 时间冲突检测、冗余字段同步
 * 5. 新增DatabaseProcedureController - 存储过程API接口
 */
@Configuration
public class DatabaseConfig implements WebMvcConfigurer {

    /**
     * 数据库版本信息
     */
    public static final String DATABASE_VERSION = "v2.1";
    public static final String DATABASE_CREATE_TIME = "2026-07-16";
    
    /**
     * 数据库特性
     */
    public static final boolean SUPPORTS_STORED_PROCEDURES = true;
    public static final boolean SUPPORTS_TRIGGERS = true;
    public static final boolean SUPPORTS_VIEWS = true;
}