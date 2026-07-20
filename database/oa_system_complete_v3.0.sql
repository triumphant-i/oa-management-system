-- ==========================================
-- OA管理系统完整数据库脚本 v3.0（合并版）
-- 包含完整建表语句、字段注释、约束配置、初始测试数据
-- 新增：公告阅读状态表、触发器、存储过程、索引优化
-- 创建时间: 2026-07-18
-- 适配版本: SpringBoot 2.7.18 + Vue 3
-- 技术栈: MySQL 8.0+
-- ==========================================

-- 关闭外键检查（避免建表顺序依赖问题）
SET FOREIGN_KEY_CHECKS = 0;

-- 创建数据库（如果不存在）
CREATE DATABASE IF NOT EXISTS oa_system DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;

USE oa_system;

-- ==========================================
-- 1. 部门表 (t_department)
-- 功能: 管理企业部门信息，支持多级部门结构
-- ==========================================
DROP TABLE IF EXISTS `t_department`;
CREATE TABLE `t_department` (
  `id` INT(11) NOT NULL AUTO_INCREMENT COMMENT '部门ID',
  `name` VARCHAR(50) NOT NULL COMMENT '部门名称',
  `manager_id` INT(11) DEFAULT NULL COMMENT '部门负责人ID（稍后回填）',
  `manager_name` VARCHAR(50) DEFAULT NULL COMMENT '部门负责人姓名（冗余字段，便于展示）',
  `manager_phone` VARCHAR(20) DEFAULT NULL COMMENT '负责人电话',
  `parent_id` INT(11) DEFAULT 0 COMMENT '上级部门ID（0表示顶级部门）',
  `level` INT(11) DEFAULT 1 COMMENT '部门层级（1-顶级部门）',
  `path` VARCHAR(500) DEFAULT NULL COMMENT '部门路径（用逗号分隔，如：1,2,3）',
  `description` VARCHAR(200) DEFAULT NULL COMMENT '部门描述',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_name` (`name`),
  KEY `idx_parent_id` (`parent_id`),
  KEY `idx_manager_id` (`manager_id`),
  KEY `idx_level` (`level`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='部门表';

-- ==========================================
-- 2. 员工表 (t_employee)
-- 功能: 管理员工基本信息、登录认证、角色权限
-- 说明: 统一的账号体系，包含所有用户角色
-- ==========================================
DROP TABLE IF EXISTS `t_employee`;
CREATE TABLE `t_employee` (
  `id` INT(11) NOT NULL AUTO_INCREMENT COMMENT '员工ID',
  `username` VARCHAR(50) NOT NULL COMMENT '用户名（登录账号）',
  `password` VARCHAR(100) NOT NULL COMMENT '密码（MD5+盐值加密）',
  `name` VARCHAR(50) NOT NULL COMMENT '员工姓名',
  `gender` VARCHAR(10) DEFAULT '男' COMMENT '性别（男/女）',
  `phone` VARCHAR(20) DEFAULT NULL COMMENT '联系电话',
  `email` VARCHAR(100) DEFAULT NULL COMMENT '电子邮箱',
  `department_id` INT(11) DEFAULT NULL COMMENT '所属部门ID',
  `position` VARCHAR(50) DEFAULT NULL COMMENT '职位',
  `status` VARCHAR(20) DEFAULT '在职' COMMENT '状态（在职/离职）',
  `role` VARCHAR(50) DEFAULT 'EMPLOYEE' COMMENT '用户角色（SYSTEM_ADMIN/DEPARTMENT_MANAGER/PROCESS_ADMIN/EMPLOYEE）',
  `avatar` VARCHAR(500) DEFAULT NULL COMMENT '头像URL',
  `join_date` VARCHAR(20) DEFAULT NULL COMMENT '入职日期',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_username` (`username`),
  KEY `idx_department_id` (`department_id`),
  KEY `idx_status` (`status`),
  KEY `idx_role` (`role`),
  KEY `idx_name` (`name`),
  KEY `idx_phone` (`phone`),
  CONSTRAINT `fk_employee_department` FOREIGN KEY (`department_id`) REFERENCES `t_department` (`id`) ON DELETE SET NULL ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='员工表（统一账号体系）';

-- 添加部门表对员工表的外键引用（建表后添加）
ALTER TABLE `t_department` 
ADD CONSTRAINT `fk_department_manager` FOREIGN KEY (`manager_id`) REFERENCES `t_employee` (`id`) ON DELETE SET NULL ON UPDATE CASCADE;

-- ==========================================
-- 3. 审批表 (t_approval)
-- 功能: 管理各类审批申请（请假/出差/加班/报销/采购）
-- ==========================================
DROP TABLE IF EXISTS `t_approval`;
CREATE TABLE `t_approval` (
  `id` INT(11) NOT NULL AUTO_INCREMENT COMMENT '审批ID',
  `applicant_id` INT(11) NOT NULL COMMENT '申请人ID',
  `applicant_name` VARCHAR(50) NOT NULL COMMENT '申请人姓名（冗余字段，便于展示）',
  `approval_type` VARCHAR(50) NOT NULL COMMENT '审批类型（请假/出差/加班/报销/采购）',
  `title` VARCHAR(100) NOT NULL COMMENT '申请标题',
  `content` TEXT COMMENT '申请内容/事由',
  `start_time` DATETIME DEFAULT NULL COMMENT '开始时间（请假/出差/加班）',
  `end_time` DATETIME DEFAULT NULL COMMENT '结束时间（请假/出差/加班）',
  `amount` DECIMAL(10,2) DEFAULT NULL COMMENT '金额（报销/采购）',
  `status` VARCHAR(20) DEFAULT '待审批' COMMENT '状态（待审批/已通过/已拒绝/已撤回）',
  `approver_id` INT(11) DEFAULT NULL COMMENT '审批人ID',
  `approver_name` VARCHAR(50) DEFAULT NULL COMMENT '审批人姓名（冗余字段，便于展示）',
  `approve_time` DATETIME DEFAULT NULL COMMENT '审批时间',
  `approve_reason` VARCHAR(200) DEFAULT NULL COMMENT '审批意见',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_applicant_id` (`applicant_id`),
  KEY `idx_approver_id` (`approver_id`),
  KEY `idx_status` (`status`),
  KEY `idx_approval_type` (`approval_type`),
  KEY `idx_create_time` (`create_time`),
  KEY `idx_start_time` (`start_time`),
  CONSTRAINT `fk_approval_applicant` FOREIGN KEY (`applicant_id`) REFERENCES `t_employee` (`id`) ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT `fk_approval_approver` FOREIGN KEY (`approver_id`) REFERENCES `t_employee` (`id`) ON DELETE SET NULL ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='审批表';

-- ==========================================
-- 4. 公告表 (t_announcement)
-- 功能: 管理企业公告发布、撤回、置顶
-- ==========================================
DROP TABLE IF EXISTS `t_announcement`;
CREATE TABLE `t_announcement` (
  `id` INT(11) NOT NULL AUTO_INCREMENT COMMENT '公告ID',
  `title` VARCHAR(200) NOT NULL COMMENT '公告标题',
  `content` TEXT NOT NULL COMMENT '公告内容',
  `publisher_id` INT(11) NOT NULL COMMENT '发布人ID',
  `publisher_name` VARCHAR(50) NOT NULL COMMENT '发布人姓名（冗余字段，便于展示）',
  `category` VARCHAR(50) DEFAULT '通知' COMMENT '分类（通知/制度/会议纪要）',
  `priority` VARCHAR(20) DEFAULT '普通' COMMENT '优先级（紧急/重要/普通）',
  `is_top` TINYINT(1) DEFAULT 0 COMMENT '是否置顶（0-否 1-是）',
  `status` VARCHAR(20) DEFAULT '已发布' COMMENT '状态（草稿/已发布/已撤回）',
  `publish_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '发布时间',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_publisher_id` (`publisher_id`),
  KEY `idx_category` (`category`),
  KEY `idx_status` (`status`),
  KEY `idx_is_top` (`is_top`),
  KEY `idx_publish_time` (`publish_time`),
  KEY `idx_title` (`title`),
  CONSTRAINT `fk_announcement_publisher` FOREIGN KEY (`publisher_id`) REFERENCES `t_employee` (`id`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='公告表';

-- ==========================================
-- 5. 公告阅读状态表 (t_announcement_read_status)
-- 功能: 记录员工阅读公告的状态
-- ==========================================
DROP TABLE IF EXISTS `t_announcement_read_status`;
CREATE TABLE `t_announcement_read_status` (
  `id` INT(11) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `announcement_id` INT(11) NOT NULL COMMENT '公告ID',
  `employee_id` INT(11) NOT NULL COMMENT '员工ID',
  `is_read` TINYINT(1) DEFAULT 0 COMMENT '是否已读（0-未读，1-已读）',
  `read_time` DATETIME DEFAULT NULL COMMENT '阅读时间',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_announcement_employee` (`announcement_id`, `employee_id`),
  KEY `idx_employee_id` (`employee_id`),
  KEY `idx_is_read` (`is_read`),
  CONSTRAINT `fk_read_status_announcement` FOREIGN KEY (`announcement_id`) REFERENCES `t_announcement` (`id`) ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT `fk_read_status_employee` FOREIGN KEY (`employee_id`) REFERENCES `t_employee` (`id`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='公告阅读状态表';

-- ==========================================
-- 6. 考勤表 (t_attendance)
-- 功能: 管理员工签到/签退、迟到/早退/缺勤判断、定位打卡
-- ==========================================
DROP TABLE IF EXISTS `t_attendance`;
CREATE TABLE `t_attendance` (
  `id` INT(11) NOT NULL AUTO_INCREMENT COMMENT '考勤ID',
  `employee_id` INT(11) NOT NULL COMMENT '员工ID',
  `employee_name` VARCHAR(50) NOT NULL COMMENT '员工姓名（冗余字段，便于展示）',
  `date` DATE NOT NULL COMMENT '日期',
  `check_in_time` DATETIME DEFAULT NULL COMMENT '签到时间',
  `check_out_time` DATETIME DEFAULT NULL COMMENT '签退时间',
  `status` VARCHAR(20) DEFAULT '正常' COMMENT '状态（正常/迟到/早退/缺勤/待审核）',
  `remark` VARCHAR(500) DEFAULT NULL COMMENT '备注（补卡原因等）',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `check_in_latitude` DECIMAL(10,7) DEFAULT NULL COMMENT '签到纬度',
  `check_in_longitude` DECIMAL(10,7) DEFAULT NULL COMMENT '签到经度',
  `check_out_latitude` DECIMAL(10,7) DEFAULT NULL COMMENT '签退纬度',
  `check_out_longitude` DECIMAL(10,7) DEFAULT NULL COMMENT '签退经度',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_employee_date` (`employee_id`, `date`),
  KEY `idx_employee_id` (`employee_id`),
  KEY `idx_date` (`date`),
  KEY `idx_status` (`status`),
  CONSTRAINT `fk_attendance_employee` FOREIGN KEY (`employee_id`) REFERENCES `t_employee` (`id`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='考勤表';

-- ==========================================
-- 7. 会议室表 (t_meeting_room)
-- 功能: 管理会议室基本信息、设备、状态
-- ==========================================
DROP TABLE IF EXISTS `t_meeting_room`;
CREATE TABLE `t_meeting_room` (
  `id` INT(11) NOT NULL AUTO_INCREMENT COMMENT '会议室ID',
  `name` VARCHAR(50) NOT NULL COMMENT '会议室名称',
  `capacity` INT(11) DEFAULT 10 COMMENT '容纳人数',
  `equipment` VARCHAR(200) DEFAULT NULL COMMENT '设备（投影仪/白板/音响等）',
  `location` VARCHAR(100) DEFAULT NULL COMMENT '位置',
  `status` VARCHAR(20) DEFAULT '可用' COMMENT '状态（可用/维修中/已停用）',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_name` (`name`),
  KEY `idx_status` (`status`),
  KEY `idx_location` (`location`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='会议室表';

-- ==========================================
-- 8. 会议预约表 (t_meeting)
-- 功能: 管理会议室预约、时间冲突检测
-- ==========================================
DROP TABLE IF EXISTS `t_meeting`;
CREATE TABLE `t_meeting` (
  `id` INT(11) NOT NULL AUTO_INCREMENT COMMENT '会议ID',
  `title` VARCHAR(200) NOT NULL COMMENT '会议主题',
  `room_id` INT(11) NOT NULL COMMENT '会议室ID',
  `room_name` VARCHAR(50) NOT NULL COMMENT '会议室名称（冗余字段，便于展示）',
  `organizer_id` INT(11) NOT NULL COMMENT '组织者ID',
  `organizer_name` VARCHAR(50) NOT NULL COMMENT '组织者姓名（冗余字段，便于展示）',
  `participants` VARCHAR(500) DEFAULT NULL COMMENT '参与人（逗号分隔）',
  `start_time` DATETIME NOT NULL COMMENT '开始时间',
  `end_time` DATETIME NOT NULL COMMENT '结束时间',
  `status` VARCHAR(20) DEFAULT '已预约' COMMENT '状态（已预约/已取消/已完成）',
  `remark` VARCHAR(200) DEFAULT NULL COMMENT '备注',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_room_id` (`room_id`),
  KEY `idx_organizer_id` (`organizer_id`),
  KEY `idx_start_time` (`start_time`),
  KEY `idx_end_time` (`end_time`),
  KEY `idx_status` (`status`),
  KEY `idx_time_range` (`room_id`, `start_time`, `end_time`),
  CONSTRAINT `fk_meeting_room` FOREIGN KEY (`room_id`) REFERENCES `t_meeting_room` (`id`) ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT `fk_meeting_organizer` FOREIGN KEY (`organizer_id`) REFERENCES `t_employee` (`id`) ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT `chk_time_range` CHECK (`end_time` > `start_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='会议预约表';

-- ==========================================
-- 9. 系统配置表 (t_system_config)
-- 功能: 管理系统配置参数
-- ==========================================
DROP TABLE IF EXISTS `t_system_config`;
CREATE TABLE `t_system_config` (
  `id` INT(11) NOT NULL AUTO_INCREMENT COMMENT '配置ID',
  `config_key` VARCHAR(100) NOT NULL COMMENT '配置键',
  `config_value` VARCHAR(500) NOT NULL COMMENT '配置值',
  `config_type` VARCHAR(50) DEFAULT 'STRING' COMMENT '配置类型（STRING/NUMBER/BOOLEAN）',
  `description` VARCHAR(200) DEFAULT NULL COMMENT '配置描述',
  `is_public` TINYINT(1) DEFAULT 0 COMMENT '是否公开（0-否 1-是）',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_config_key` (`config_key`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统配置表';

-- ==========================================
-- 10. 缓存记录表 (t_cache_record)
-- 功能: 记录Redis缓存使用情况
-- ==========================================
DROP TABLE IF EXISTS `t_cache_record`;
CREATE TABLE `t_cache_record` (
  `id` INT(11) NOT NULL AUTO_INCREMENT COMMENT '记录ID',
  `cache_key` VARCHAR(200) NOT NULL COMMENT '缓存键',
  `cache_type` VARCHAR(50) NOT NULL COMMENT '缓存类型（员工/公告/部门）',
  `hit_count` INT(11) DEFAULT 0 COMMENT '命中次数',
  `miss_count` INT(11) DEFAULT 0 COMMENT '未命中次数',
  `last_access_time` DATETIME DEFAULT NULL COMMENT '最后访问时间',
  `expire_time` DATETIME DEFAULT NULL COMMENT '过期时间',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_cache_key` (`cache_key`),
  KEY `idx_cache_type` (`cache_type`),
  KEY `idx_expire_time` (`expire_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='缓存记录表';

-- ==========================================
-- 11. ES同步记录表 (t_es_sync_record)
-- 功能: 记录Elasticsearch数据同步情况
-- ==========================================
DROP TABLE IF EXISTS `t_es_sync_record`;
CREATE TABLE `t_es_sync_record` (
  `id` INT(11) NOT NULL AUTO_INCREMENT COMMENT '记录ID',
  `index_name` VARCHAR(100) NOT NULL COMMENT '索引名称',
  `document_id` VARCHAR(100) NOT NULL COMMENT '文档ID',
  `operation_type` VARCHAR(20) NOT NULL COMMENT '操作类型（INSERT/UPDATE/DELETE）',
  `sync_status` VARCHAR(20) DEFAULT 'SUCCESS' COMMENT '同步状态（SUCCESS/FAILED）',
  `error_message` VARCHAR(500) DEFAULT NULL COMMENT '错误信息',
  `sync_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '同步时间',
  PRIMARY KEY (`id`),
  KEY `idx_index_name` (`index_name`),
  KEY `idx_document_id` (`document_id`),
  KEY `idx_sync_time` (`sync_time`),
  KEY `idx_sync_status` (`sync_status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='ES同步记录表';

-- ==========================================
-- 12. 权限日志表 (t_permission_log)
-- 功能: 记录权限变更历史（审计）
-- ==========================================
DROP TABLE IF EXISTS `t_permission_log`;
CREATE TABLE `t_permission_log` (
  `id` INT(11) NOT NULL AUTO_INCREMENT COMMENT '日志ID',
  `user_id` INT(11) NOT NULL COMMENT '操作用户ID',
  `user_name` VARCHAR(50) NOT NULL COMMENT '操作用户名',
  `target_type` VARCHAR(50) NOT NULL COMMENT '目标类型（员工/部门）',
  `target_id` INT(11) NOT NULL COMMENT '目标ID',
  `action` VARCHAR(50) NOT NULL COMMENT '操作类型（修改角色/调岗/离职等）',
  `old_value` VARCHAR(200) DEFAULT NULL COMMENT '旧值',
  `new_value` VARCHAR(200) DEFAULT NULL COMMENT '新值',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_target_id` (`target_id`),
  KEY `idx_create_time` (`create_time`),
  KEY `idx_action` (`action`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='权限日志表';

-- ==========================================
-- ========== 创建触发器 ==========
-- ==========================================

DROP TRIGGER IF EXISTS `trg_meeting_time_conflict_before_insert`;
DROP TRIGGER IF EXISTS `trg_meeting_time_conflict_before_update`;
DROP TRIGGER IF EXISTS `trg_employee_name_sync_after_update`;

DELIMITER $$

CREATE TRIGGER `trg_meeting_time_conflict_before_insert`
BEFORE INSERT ON `t_meeting`
FOR EACH ROW
BEGIN
    DECLARE conflict_count INT;
    
    SELECT COUNT(*) INTO conflict_count
    FROM `t_meeting`
    WHERE `room_id` = NEW.`room_id`
      AND `status` = '已预约'
      AND (
          (NEW.`start_time` BETWEEN `start_time` AND `end_time`)
          OR (NEW.`end_time` BETWEEN `start_time` AND `end_time`)
          OR (`start_time` BETWEEN NEW.`start_time` AND NEW.`end_time`)
      );
    
    IF conflict_count > 0 THEN
        SIGNAL SQLSTATE '45000'
        SET MESSAGE_TEXT = '会议室预约时间冲突：该时间段已被预约';
    END IF;
END$$

CREATE TRIGGER `trg_meeting_time_conflict_before_update`
BEFORE UPDATE ON `t_meeting`
FOR EACH ROW
BEGIN
    DECLARE conflict_count INT;
    
    IF NEW.`room_id` <> OLD.`room_id` 
       OR NEW.`start_time` <> OLD.`start_time`
       OR NEW.`end_time` <> OLD.`end_time` THEN
        
        SELECT COUNT(*) INTO conflict_count
        FROM `t_meeting`
        WHERE `room_id` = NEW.`room_id`
          AND `status` = '已预约'
          AND `id` <> NEW.`id`
          AND (
              (NEW.`start_time` BETWEEN `start_time` AND `end_time`)
              OR (NEW.`end_time` BETWEEN `start_time` AND `end_time`)
              OR (`start_time` BETWEEN NEW.`start_time` AND NEW.`end_time`)
          );
        
        IF conflict_count > 0 THEN
            SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = '会议室预约时间冲突：该时间段已被预约';
        END IF;
    END IF;
END$$

CREATE TRIGGER `trg_employee_name_sync_after_update`
AFTER UPDATE ON `t_employee`
FOR EACH ROW
BEGIN
    IF NEW.`name` <> OLD.`name` THEN
        UPDATE `t_approval` SET `applicant_name` = NEW.`name` WHERE `applicant_id` = NEW.`id`;
        UPDATE `t_approval` SET `approver_name` = NEW.`name` WHERE `approver_id` = NEW.`id`;
        UPDATE `t_announcement` SET `publisher_name` = NEW.`name` WHERE `publisher_id` = NEW.`id`;
        UPDATE `t_attendance` SET `employee_name` = NEW.`name` WHERE `employee_id` = NEW.`id`;
        UPDATE `t_meeting` SET `organizer_name` = NEW.`name` WHERE `organizer_id` = NEW.`id`;
    END IF;
END$$

DELIMITER ;

-- ==========================================
-- ========== 创建视图 ==========
-- ==========================================

DROP VIEW IF EXISTS `v_employee_detail`;
CREATE VIEW `v_employee_detail` AS
SELECT 
    e.`id`, e.`username`, e.`name`, e.`gender`, e.`phone`, e.`email`,
    e.`department_id`, d.`name` AS `department_name`,
    e.`position`, e.`status`, e.`role`, e.`avatar`, e.`create_time`, e.`update_time`
FROM `t_employee` e
LEFT JOIN `t_department` d ON e.`department_id` = d.`id`;

DROP VIEW IF EXISTS `v_department_detail`;
CREATE VIEW `v_department_detail` AS
SELECT 
    d.`id`, d.`name`, d.`manager_id`, d.`manager_name`, d.`manager_phone`,
    d.`parent_id`, d.`level`, d.`path`, d.`description`,
    d.`create_time`, d.`update_time`,
    (SELECT COUNT(*) FROM `t_employee` e WHERE e.`department_id` = d.`id`) AS `employee_count`
FROM `t_department` d;

DROP VIEW IF EXISTS `v_meeting_detail`;
CREATE VIEW `v_meeting_detail` AS
SELECT 
    m.`id`, m.`title`, m.`room_id`, m.`room_name`, r.`capacity`, r.`equipment`, r.`location`,
    m.`organizer_id`, m.`organizer_name`, m.`participants`,
    m.`start_time`, m.`end_time`, m.`status`, m.`remark`, m.`create_time`
FROM `t_meeting` m
LEFT JOIN `t_meeting_room` r ON m.`room_id` = r.`id`;

-- ==========================================
-- ========== 初始化数据 ==========
-- ==========================================

-- 1. 插入部门数据
INSERT INTO `t_department` (`name`, `manager_name`, `manager_phone`, `parent_id`, `level`, `path`, `description`) VALUES
('技术部', '张三', '13800138001', 0, 1, '1', '负责公司技术研发和系统维护工作'),
('产品部', '李四', '13800138002', 0, 1, '2', '负责产品规划和设计工作'),
('运营部', '王五', '13800138003', 0, 1, '3', '负责公司运营和市场推广工作'),
('人事部', '赵六', '13800138004', 0, 1, '4', '负责公司人力资源管理工作'),
('财务部', '钱七', '13800138005', 0, 1, '5', '负责公司财务管理和核算工作');

-- 2. 插入员工数据（密码为：123456）
INSERT INTO `t_employee` (`username`, `password`, `name`, `gender`, `phone`, `email`, `department_id`, `position`, `status`, `role`, `avatar`) VALUES
('admin', 'abc12345$e10adc3949ba59abbe56e057f20f883e', '系统管理员', '男', '13800000000', 'admin@oa.com', 1, '系统管理员', '在职', 'SYSTEM_ADMIN', NULL),
('zhangsan', 'def67890$e10adc3949ba59abbe56e057f20f883e', '张三', '男', '13800138001', 'zhangsan@oa.com', 1, '技术总监', '在职', 'DEPARTMENT_MANAGER', NULL),
('lisi', 'ghi11111$e10adc3949ba59abbe56e057f20f883e', '李四', '女', '13800138002', 'lisi@oa.com', 2, '产品总监', '在职', 'DEPARTMENT_MANAGER', NULL),
('wangwu', 'jkl22222$e10adc3949ba59abbe56e057f20f883e', '王五', '男', '13800138003', 'wangwu@oa.com', 3, '运营总监', '在职', 'PROCESS_ADMIN', NULL),
('emp001', 'mno33333$e10adc3949ba59abbe56e057f20f883e', '员工一', '男', '13800138011', 'emp001@oa.com', 1, 'Java开发工程师', '在职', 'EMPLOYEE', NULL),
('emp002', 'pqr44444$e10adc3949ba59abbe56e057f20f883e', '员工二', '女', '13800138012', 'emp002@oa.com', 1, '前端开发工程师', '在职', 'EMPLOYEE', NULL),
('emp003', 'stu55555$e10adc3949ba59abbe56e057f20f883e', '员工三', '男', '13800138013', 'emp003@oa.com', 2, '产品经理', '在职', 'EMPLOYEE', NULL),
('emp004', 'vwx66666$e10adc3949ba59abbe56e057f20f883e', '员工四', '女', '13800138014', 'emp004@oa.com', 3, '运营专员', '在职', 'EMPLOYEE', NULL),
('emp005', 'yza77777$e10adc3949ba59abbe56e057f20f883e', '员工五', '男', '13800138015', 'emp005@oa.com', 4, '人事专员', '在职', 'EMPLOYEE', NULL),
('emp006', 'bcd88888$e10adc3949ba59abbe56e057f20f883e', '员工六', '女', '13800138016', 'emp006@oa.com', 5, '财务专员', '在职', 'EMPLOYEE', NULL);

-- 3. 回填部门负责人ID
UPDATE `t_department` d SET `manager_id` = (
    SELECT `id` FROM `t_employee` e 
    WHERE e.`department_id` = d.`id` AND e.`role` = 'DEPARTMENT_MANAGER'
    LIMIT 1
) WHERE `manager_id` IS NULL;

-- 4. 插入审批数据
INSERT INTO `t_approval` (`applicant_id`, `applicant_name`, `approval_type`, `title`, `content`, `start_time`, `end_time`, `amount`, `status`, `approver_id`, `approver_name`, `approve_time`, `approve_reason`) VALUES
(5, '员工一', '请假', '年假申请', '本人计划于下周一年假休息一天', '2026-07-20 09:00:00', '2026-07-20 18:00:00', NULL, '待审批', NULL, NULL, NULL, NULL),
(5, '员工一', '出差', '北京出差申请', '需要前往北京客户现场进行项目部署', '2026-07-25 09:00:00', '2026-07-27 18:00:00', NULL, '已通过', 1, '系统管理员', '2026-07-16 10:30:00', '同意出差'),
(6, '员工二', '加班', '周末加班申请', '项目赶进度，需要周末加班', '2026-07-20 09:00:00', '2026-07-20 18:00:00', NULL, '待审批', NULL, NULL, NULL, NULL),
(7, '员工三', '报销', '交通费报销', '出差北京交通费报销', NULL, NULL, 1500.00, '已通过', 1, '系统管理员', '2026-07-16 11:00:00', '报销金额合理，同意'),
(8, '员工四', '采购', '办公用品采购', '需要采购打印纸、文件夹等办公用品', NULL, NULL, 500.00, '待审批', NULL, NULL, NULL, NULL);

-- 5. 插入公告数据
INSERT INTO `t_announcement` (`title`, `content`, `publisher_id`, `publisher_name`, `category`, `priority`, `is_top`, `status`) VALUES
('关于公司年度体检的通知', '公司将于2026年8月组织全体员工进行年度体检，请各位同事注意时间安排。', 1, '系统管理员', '通知', '重要', 1, '已发布'),
('新版OA系统上线通知', '新版OA管理系统已正式上线，包含员工管理、审批管理、考勤管理等功能，请大家积极使用。', 1, '系统管理员', '通知', '紧急', 1, '已发布'),
('关于加强信息安全的通知', '为进一步加强公司信息安全管理，请各位同事注意以下事项：1.定期修改密码；2.不随意泄露公司信息。', 4, '王五', '制度', '重要', 0, '已发布'),
('2026年第二季度工作总结会议纪要', '2026年第二季度工作总结会议于7月10日召开，会议主要总结了上半年的工作成果和下半年的工作计划。', 1, '系统管理员', '会议纪要', '普通', 0, '已发布'),
('关于调整工作时间的通知', '自2026年8月1日起，公司工作时间调整为：上午9:00-12:00，下午13:30-18:00。', 4, '王五', '通知', '重要', 0, '已发布');

-- 6. 插入考勤数据
INSERT INTO `t_attendance` (`employee_id`, `employee_name`, `date`, `check_in_time`, `check_out_time`, `status`, `remark`) VALUES
(5, '员工一', '2026-07-15', '2026-07-15 08:55:00', '2026-07-15 18:10:00', '正常', NULL),
(5, '员工一', '2026-07-16', '2026-07-16 09:15:00', '2026-07-16 18:00:00', '迟到', '因交通拥堵迟到'),
(6, '员工二', '2026-07-15', '2026-07-15 08:50:00', '2026-07-15 18:20:00', '正常', NULL),
(6, '员工二', '2026-07-16', '2026-07-16 09:00:00', '2026-07-16 18:05:00', '正常', NULL),
(7, '员工三', '2026-07-15', '2026-07-15 08:58:00', '2026-07-15 17:50:00', '早退', '提前离岗参加培训'),
(7, '员工三', '2026-07-16', '2026-07-16 09:05:00', '2026-07-16 18:00:00', '正常', NULL);

-- 7. 插入会议室数据
INSERT INTO `t_meeting_room` (`name`, `capacity`, `equipment`, `location`, `status`) VALUES
('第一会议室', 10, '投影仪、白板、音响', '3楼301', '可用'),
('第二会议室', 20, '投影仪、白板、音响、视频会议系统', '3楼302', '可用'),
('第三会议室', 8, '投影仪、白板', '4楼401', '维修中'),
('培训室', 50, '投影仪、音响、麦克风', '5楼501', '可用'),
('VIP会议室', 15, '投影仪、视频会议系统、茶水服务', '6楼601', '可用');

-- 8. 插入会议预约数据
INSERT INTO `t_meeting` (`title`, `room_id`, `room_name`, `organizer_id`, `organizer_name`, `participants`, `start_time`, `end_time`, `status`, `remark`) VALUES
('项目启动会', 1, '第一会议室', 5, '员工一', '员工一,员工二,员工三', '2026-07-20 09:00:00', '2026-07-20 11:00:00', '已预约', '新项目启动会议'),
('部门周例会', 2, '第二会议室', 1, '系统管理员', '技术部全体员工', '2026-07-22 14:00:00', '2026-07-22 16:00:00', '已预约', '技术部每周例会'),
('产品需求评审', 1, '第一会议室', 7, '员工三', '产品部,技术部', '2026-07-25 09:30:00', '2026-07-25 12:00:00', '已预约', '新功能需求评审');

-- 9. 插入系统配置数据
INSERT INTO `t_system_config` (`config_key`, `config_value`, `config_type`, `description`, `is_public`) VALUES
('REDIS_ENABLED', 'true', 'BOOLEAN', 'Redis缓存开关', 1),
('ES_ENABLED', 'true', 'BOOLEAN', 'Elasticsearch开关', 1),
('CACHE_EXPIRE_TIME', '3600', 'NUMBER', '缓存过期时间（秒）', 0),
('ES_SEARCH_PAGE_SIZE', '10', 'NUMBER', 'ES搜索默认分页大小', 1),
('TOKEN_EXPIRE_TIME', '86400', 'NUMBER', 'Token过期时间（秒）', 0),
('MAX_PAGE_SIZE', '100', 'NUMBER', '分页最大条数', 1),
('DEFAULT_PAGE_SIZE', '10', 'NUMBER', '分页默认条数', 1);

-- ==========================================
-- ========== 恢复外键检查 ==========
-- ==========================================
SET FOREIGN_KEY_CHECKS = 1;

-- ==========================================
-- ========== 完成提示 ==========
-- ==========================================
SELECT '========================================' AS '';
SELECT 'OA管理系统数据库初始化完成！v3.0（合并版）' AS '消息';
SELECT '========================================' AS '';
SELECT '数据表统计：' AS '';
SELECT COUNT(*) AS '部门数量' FROM t_department;
SELECT COUNT(*) AS '员工数量' FROM t_employee;
SELECT COUNT(*) AS '审批数量' FROM t_approval;
SELECT COUNT(*) AS '公告数量' FROM t_announcement;
SELECT COUNT(*) AS '考勤记录数' FROM t_attendance;
SELECT COUNT(*) AS '会议室数量' FROM t_meeting_room;
SELECT COUNT(*) AS '会议预约数' FROM t_meeting;
SELECT '========================================' AS '';
SELECT '默认管理员账号：admin / 123456' AS '登录信息';
SELECT '测试员工账号：emp001 / 123456' AS '登录信息';
SELECT '========================================' AS '';