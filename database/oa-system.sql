-- ==========================================
-- OA管理系统完整数据库脚本 v3.1（修正版）
-- 修复：t_approval 表缺少 invoice_number 和 invoice_image 字段
-- 创建时间: 2026-07-18
-- ==========================================

-- 关闭外键检查
SET FOREIGN_KEY_CHECKS = 0;

-- 删除已存在的数据库（谨慎使用）
DROP DATABASE IF EXISTS oa_system;

-- 创建数据库
CREATE DATABASE IF NOT EXISTS oa_system DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;

USE oa_system;

-- ==========================================
-- 1. 部门表 (t_department)
-- ==========================================
DROP TABLE IF EXISTS `t_department`;
CREATE TABLE `t_department` (
  `id` INT NOT NULL AUTO_INCREMENT COMMENT '部门ID',
  `name` VARCHAR(50) NOT NULL COMMENT '部门名称',
  `manager_id` INT DEFAULT NULL COMMENT '部门负责人ID',
  `manager_name` VARCHAR(50) DEFAULT NULL COMMENT '部门负责人姓名',
  `manager_phone` VARCHAR(20) DEFAULT NULL COMMENT '负责人电话',
  `parent_id` INT DEFAULT 0 COMMENT '上级部门ID',
  `level` INT DEFAULT 1 COMMENT '部门层级',
  `path` VARCHAR(500) DEFAULT NULL COMMENT '部门路径',
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
-- ==========================================
DROP TABLE IF EXISTS `t_employee`;
CREATE TABLE `t_employee` (
  `id` INT NOT NULL AUTO_INCREMENT COMMENT '员工ID',
  `username` VARCHAR(50) NOT NULL COMMENT '用户名',
  `password` VARCHAR(100) NOT NULL COMMENT '密码',
  `name` VARCHAR(50) NOT NULL COMMENT '员工姓名',
  `gender` VARCHAR(10) DEFAULT '男' COMMENT '性别',
  `phone` VARCHAR(20) DEFAULT NULL COMMENT '联系电话',
  `email` VARCHAR(100) DEFAULT NULL COMMENT '电子邮箱',
  `department_id` INT DEFAULT NULL COMMENT '所属部门ID',
  `position` VARCHAR(50) DEFAULT NULL COMMENT '职位',
  `status` VARCHAR(20) DEFAULT '在职' COMMENT '状态',
  `role` VARCHAR(50) DEFAULT 'EMPLOYEE' COMMENT '用户角色',
  `avatar` VARCHAR(500) DEFAULT NULL COMMENT '头像URL',
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
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='员工表';

-- ==========================================
-- 3. 审批表 (t_approval) - 完整版（包含所有字段）
-- ==========================================
DROP TABLE IF EXISTS `t_approval`;
CREATE TABLE `t_approval` (
  `id` INT NOT NULL AUTO_INCREMENT COMMENT '审批ID',
  
  -- 基础信息
  `applicant_id` INT NOT NULL COMMENT '申请人ID',
  `applicant_name` VARCHAR(50) NOT NULL COMMENT '申请人姓名',
  `department_id` INT DEFAULT NULL COMMENT '申请人部门ID',
  `department_name` VARCHAR(50) DEFAULT NULL COMMENT '申请人部门名称',
  `approval_type` VARCHAR(20) NOT NULL COMMENT '审批类型：leave/business/overtime/reimburse/purchase/card',
  `title` VARCHAR(100) NOT NULL COMMENT '申请标题',
  `content` TEXT COMMENT '申请内容/事由',
  `status` VARCHAR(20) DEFAULT '待审批' COMMENT '状态：待审批/已通过/已拒绝/已撤回/草稿',
  `priority` VARCHAR(20) DEFAULT '普通' COMMENT '优先级：紧急/重要/普通',
  
  -- 审批人信息
  `approver_id` INT DEFAULT NULL COMMENT '当前审批人ID',
  `approver_name` VARCHAR(50) DEFAULT NULL COMMENT '当前审批人姓名',
  `approve_time` DATETIME DEFAULT NULL COMMENT '审批时间',
  `approve_reason` VARCHAR(500) DEFAULT NULL COMMENT '审批意见',
  
  -- 请假/出差/加班 公共字段
  `start_time` DATETIME DEFAULT NULL COMMENT '开始时间',
  `end_time` DATETIME DEFAULT NULL COMMENT '结束时间',
  
  -- 请假特有
  `leave_type` VARCHAR(20) DEFAULT NULL COMMENT '请假类型',
  `total_days` DECIMAL(5,1) DEFAULT NULL COMMENT '总天数',
  
  -- 出差特有
  `dest_city` VARCHAR(50) DEFAULT NULL COMMENT '出差城市',
  `dest_address` VARCHAR(200) DEFAULT NULL COMMENT '详细地址',
  `companion` VARCHAR(200) DEFAULT NULL COMMENT '同行人员',
  
  -- 加班特有
  `work_date` DATE DEFAULT NULL COMMENT '加班日期',
  `start_time_only` TIME DEFAULT NULL COMMENT '开始时间',
  `end_time_only` TIME DEFAULT NULL COMMENT '结束时间',
  `total_hours` DECIMAL(5,1) DEFAULT NULL COMMENT '总小时数',
  
  -- 报销特有
  `expense_type` VARCHAR(20) DEFAULT NULL COMMENT '报销类型',
  `amount` DECIMAL(12,2) DEFAULT NULL COMMENT '报销金额',
  `expense_date` DATE DEFAULT NULL COMMENT '费用日期',
  `invoice_number` VARCHAR(50) DEFAULT NULL COMMENT '发票号码',
  `invoice_image` VARCHAR(500) DEFAULT NULL COMMENT '发票图片',
  
  -- 采购特有
  `goods_name` VARCHAR(100) DEFAULT NULL COMMENT '物品名称',
  `quantity` INT DEFAULT NULL COMMENT '采购数量',
  `unit_price` DECIMAL(12,2) DEFAULT NULL COMMENT '单价',
  `total_amount` DECIMAL(12,2) DEFAULT NULL COMMENT '总金额',
  `supplier` VARCHAR(100) DEFAULT NULL COMMENT '供应商',
  
  -- 补卡特有
  `card_date` DATE DEFAULT NULL COMMENT '补卡日期',
  `card_time` TIME DEFAULT NULL COMMENT '补卡时间',
  `card_type` VARCHAR(20) DEFAULT NULL COMMENT '补卡类型',
  
  -- 附件和扩展
  `attachments` JSON DEFAULT NULL COMMENT '附件列表',
  `ext_data` JSON DEFAULT NULL COMMENT '扩展数据',
  `ext1` VARCHAR(200) DEFAULT NULL COMMENT '扩展字段1',
  `ext2` VARCHAR(200) DEFAULT NULL COMMENT '扩展字段2',
  `ext3` VARCHAR(200) DEFAULT NULL COMMENT '扩展字段3',
  
  -- 流程相关
  `current_step` INT DEFAULT 1 COMMENT '当前流程步骤',
  `total_steps` INT DEFAULT 3 COMMENT '总流程步骤',
  `flow_instance_id` VARCHAR(50) DEFAULT NULL COMMENT '工作流实例ID',
  
  -- 系统字段
  `draft_saved` TINYINT(1) DEFAULT 0 COMMENT '是否保存草稿',
  `is_deleted` TINYINT(1) DEFAULT 0 COMMENT '逻辑删除',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  
  PRIMARY KEY (`id`),
  KEY `idx_applicant_id` (`applicant_id`),
  KEY `idx_approver_id` (`approver_id`),
  KEY `idx_status` (`status`),
  KEY `idx_approval_type` (`approval_type`),
  KEY `idx_create_time` (`create_time`),
  KEY `idx_start_time` (`start_time`),
  KEY `idx_department_id` (`department_id`),
  KEY `idx_leave_type` (`leave_type`),
  KEY `idx_expense_type` (`expense_type`),
  KEY `idx_card_type` (`card_type`),
  KEY `idx_priority` (`priority`),
  KEY `idx_draft_saved` (`draft_saved`),
  KEY `idx_is_deleted` (`is_deleted`),
  
  CONSTRAINT `fk_approval_applicant` FOREIGN KEY (`applicant_id`) REFERENCES `t_employee` (`id`) ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT `fk_approval_approver` FOREIGN KEY (`approver_id`) REFERENCES `t_employee` (`id`) ON DELETE SET NULL ON UPDATE CASCADE,
  CONSTRAINT `fk_approval_department` FOREIGN KEY (`department_id`) REFERENCES `t_department` (`id`) ON DELETE SET NULL ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='审批表（完整版）';

-- ==========================================
-- 4. 公告表 (t_announcement)
-- ==========================================
DROP TABLE IF EXISTS `t_announcement`;
CREATE TABLE `t_announcement` (
  `id` INT NOT NULL AUTO_INCREMENT COMMENT '公告ID',
  `title` VARCHAR(200) NOT NULL COMMENT '公告标题',
  `content` TEXT NOT NULL COMMENT '公告内容',
  `publisher_id` INT NOT NULL COMMENT '发布人ID',
  `publisher_name` VARCHAR(50) NOT NULL COMMENT '发布人姓名',
  `category` VARCHAR(50) DEFAULT '通知' COMMENT '分类',
  `priority` VARCHAR(20) DEFAULT '普通' COMMENT '优先级',
  `is_top` TINYINT(1) DEFAULT 0 COMMENT '是否置顶',
  `status` VARCHAR(20) DEFAULT '已发布' COMMENT '状态',
  `publish_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '发布时间',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_publisher_id` (`publisher_id`),
  KEY `idx_category` (`category`),
  KEY `idx_status` (`status`),
  KEY `idx_is_top` (`is_top`),
  KEY `idx_publish_time` (`publish_time`),
  CONSTRAINT `fk_announcement_publisher` FOREIGN KEY (`publisher_id`) REFERENCES `t_employee` (`id`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='公告表';

-- ==========================================
-- 5. 考勤表 (t_attendance)
-- ==========================================
DROP TABLE IF EXISTS `t_attendance`;
CREATE TABLE `t_attendance` (
  `id` INT NOT NULL AUTO_INCREMENT COMMENT '考勤ID',
  `employee_id` INT NOT NULL COMMENT '员工ID',
  `employee_name` VARCHAR(50) NOT NULL COMMENT '员工姓名',
  `date` DATE NOT NULL COMMENT '日期',
  `check_in_time` DATETIME DEFAULT NULL COMMENT '签到时间',
  `check_out_time` DATETIME DEFAULT NULL COMMENT '签退时间',
  `status` VARCHAR(20) DEFAULT '正常' COMMENT '状态',
  `remark` VARCHAR(200) DEFAULT NULL COMMENT '备注',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_employee_date` (`employee_id`, `date`),
  KEY `idx_employee_id` (`employee_id`),
  KEY `idx_date` (`date`),
  KEY `idx_status` (`status`),
  CONSTRAINT `fk_attendance_employee` FOREIGN KEY (`employee_id`) REFERENCES `t_employee` (`id`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='考勤表';

-- ==========================================
-- 6. 会议室表 (t_meeting_room)
-- ==========================================
DROP TABLE IF EXISTS `t_meeting_room`;
CREATE TABLE `t_meeting_room` (
  `id` INT NOT NULL AUTO_INCREMENT COMMENT '会议室ID',
  `name` VARCHAR(50) NOT NULL COMMENT '会议室名称',
  `capacity` INT DEFAULT 10 COMMENT '容纳人数',
  `equipment` VARCHAR(200) DEFAULT NULL COMMENT '设备',
  `location` VARCHAR(100) DEFAULT NULL COMMENT '位置',
  `status` VARCHAR(20) DEFAULT '可用' COMMENT '状态',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_name` (`name`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='会议室表';

-- ==========================================
-- 7. 会议预约表 (t_meeting)
-- ==========================================
DROP TABLE IF EXISTS `t_meeting`;
CREATE TABLE `t_meeting` (
  `id` INT NOT NULL AUTO_INCREMENT COMMENT '会议ID',
  `title` VARCHAR(200) NOT NULL COMMENT '会议主题',
  `room_id` INT NOT NULL COMMENT '会议室ID',
  `room_name` VARCHAR(50) NOT NULL COMMENT '会议室名称',
  `organizer_id` INT NOT NULL COMMENT '组织者ID',
  `organizer_name` VARCHAR(50) NOT NULL COMMENT '组织者姓名',
  `participants` VARCHAR(500) DEFAULT NULL COMMENT '参与人',
  `start_time` DATETIME NOT NULL COMMENT '开始时间',
  `end_time` DATETIME NOT NULL COMMENT '结束时间',
  `status` VARCHAR(20) DEFAULT '已预约' COMMENT '状态',
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
-- 8. 系统配置表 (t_system_config)
-- ==========================================
DROP TABLE IF EXISTS `t_system_config`;
CREATE TABLE `t_system_config` (
  `id` INT NOT NULL AUTO_INCREMENT COMMENT '配置ID',
  `config_key` VARCHAR(100) NOT NULL COMMENT '配置键',
  `config_value` VARCHAR(500) NOT NULL COMMENT '配置值',
  `config_type` VARCHAR(50) DEFAULT 'STRING' COMMENT '配置类型',
  `description` VARCHAR(200) DEFAULT NULL COMMENT '配置描述',
  `is_public` TINYINT(1) DEFAULT 0 COMMENT '是否公开',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_config_key` (`config_key`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统配置表';

-- ==========================================
-- 9. 通知消息表 (t_notification)
-- ==========================================
DROP TABLE IF EXISTS `t_notification`;
CREATE TABLE `t_notification` (
  `id` INT NOT NULL AUTO_INCREMENT COMMENT '通知ID',
  `user_id` INT NOT NULL COMMENT '接收人ID',
  `type` VARCHAR(20) NOT NULL COMMENT '通知类型：APPROVAL/MEETING/SYSTEM',
  `title` VARCHAR(200) NOT NULL COMMENT '通知标题',
  `content` TEXT NOT NULL COMMENT '通知内容',
  `source_id` INT DEFAULT NULL COMMENT '来源ID',
  `source_type` VARCHAR(50) DEFAULT NULL COMMENT '来源类型',
  `is_read` TINYINT(1) DEFAULT 0 COMMENT '是否已读',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_is_read` (`is_read`),
  KEY `idx_create_time` (`create_time`),
  CONSTRAINT `fk_notification_user` FOREIGN KEY (`user_id`) REFERENCES `t_employee` (`id`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='通知消息表';

-- ==========================================
-- 10. 缓存记录表 (t_cache_record)
-- ==========================================
DROP TABLE IF EXISTS `t_cache_record`;
CREATE TABLE `t_cache_record` (
  `id` INT NOT NULL AUTO_INCREMENT COMMENT '记录ID',
  `cache_key` VARCHAR(200) NOT NULL COMMENT '缓存键',
  `cache_type` VARCHAR(50) NOT NULL COMMENT '缓存类型',
  `hit_count` INT DEFAULT 0 COMMENT '命中次数',
  `miss_count` INT DEFAULT 0 COMMENT '未命中次数',
  `last_access_time` DATETIME DEFAULT NULL COMMENT '最后访问时间',
  `expire_time` DATETIME DEFAULT NULL COMMENT '过期时间',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_cache_key` (`cache_key`),
  KEY `idx_cache_type` (`cache_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='缓存记录表';

-- ==========================================
-- 11. ES同步记录表 (t_es_sync_record)
-- ==========================================
DROP TABLE IF EXISTS `t_es_sync_record`;
CREATE TABLE `t_es_sync_record` (
  `id` INT NOT NULL AUTO_INCREMENT COMMENT '记录ID',
  `index_name` VARCHAR(100) NOT NULL COMMENT '索引名称',
  `document_id` VARCHAR(100) NOT NULL COMMENT '文档ID',
  `operation_type` VARCHAR(20) NOT NULL COMMENT '操作类型',
  `sync_status` VARCHAR(20) DEFAULT 'SUCCESS' COMMENT '同步状态',
  `error_message` VARCHAR(500) DEFAULT NULL COMMENT '错误信息',
  `sync_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '同步时间',
  PRIMARY KEY (`id`),
  KEY `idx_index_name` (`index_name`),
  KEY `idx_sync_status` (`sync_status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='ES同步记录表';

-- ==========================================
-- 12. 权限日志表 (t_permission_log)
-- ==========================================
DROP TABLE IF EXISTS `t_permission_log`;
CREATE TABLE `t_permission_log` (
  `id` INT NOT NULL AUTO_INCREMENT COMMENT '日志ID',
  `user_id` INT NOT NULL COMMENT '操作用户ID',
  `user_name` VARCHAR(50) NOT NULL COMMENT '操作用户名',
  `target_type` VARCHAR(50) NOT NULL COMMENT '目标类型',
  `target_id` INT NOT NULL COMMENT '目标ID',
  `action` VARCHAR(50) NOT NULL COMMENT '操作类型',
  `old_value` VARCHAR(200) DEFAULT NULL COMMENT '旧值',
  `new_value` VARCHAR(200) DEFAULT NULL COMMENT '新值',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='权限日志表';

-- ==========================================
-- 添加部门表外键（解决循环引用）
-- ==========================================
ALTER TABLE `t_department` 
ADD CONSTRAINT `fk_department_manager` 
FOREIGN KEY (`manager_id`) REFERENCES `t_employee` (`id`) ON DELETE SET NULL ON UPDATE CASCADE;

-- ==========================================
-- 创建触发器
-- ==========================================

DROP TRIGGER IF EXISTS `trg_meeting_time_conflict_before_insert`;
DROP TRIGGER IF EXISTS `trg_meeting_time_conflict_before_update`;
DROP TRIGGER IF EXISTS `trg_employee_name_sync_after_update`;
DROP TRIGGER IF EXISTS `trg_approval_notification`;

DELIMITER $$

-- 触发器1: 会议时间冲突检测（插入）
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

-- 触发器2: 会议时间冲突检测（更新）
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

-- 触发器3: 员工姓名同步
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

-- 触发器4: 审批通知自动创建
CREATE TRIGGER `trg_approval_notification`
AFTER UPDATE ON `t_approval`
FOR EACH ROW
BEGIN
    IF NEW.`status` IN ('已通过', '已拒绝') AND OLD.`status` = '待审批' THEN
        INSERT INTO `t_notification` 
        (`user_id`, `type`, `title`, `content`, `source_id`, `source_type`)
        VALUES (
            NEW.`applicant_id`,
            'APPROVAL',
            CONCAT('审批', CASE WHEN NEW.`status` = '已通过' THEN '通过' ELSE '拒绝' END),
            CONCAT('您申请的【', NEW.`title`, '】已', CASE WHEN NEW.`status` = '已通过' THEN '通过' ELSE '拒绝' END,
                   IF(NEW.`approve_reason` IS NOT NULL, CONCAT('，意见：', NEW.`approve_reason`), '')),
            NEW.`id`,
            'approval'
        );
    END IF;
END$$

DELIMITER ;

-- ==========================================
-- 初始化数据
-- ==========================================

-- 1. 插入部门数据
INSERT INTO `t_department` (`name`, `manager_name`, `manager_phone`, `parent_id`, `level`, `path`, `description`) VALUES
('技术部', '张三', '13800138001', 0, 1, '1', '负责公司技术研发和系统维护工作'),
('产品部', '李四', '13800138002', 0, 1, '2', '负责产品规划和设计工作'),
('运营部', '王五', '13800138003', 0, 1, '3', '负责公司运营和市场推广工作'),
('人事部', '赵六', '13800138004', 0, 1, '4', '负责公司人力资源管理工作'),
('财务部', '钱七', '13800138005', 0, 1, '5', '负责公司财务管理和核算工作');

-- 2. 插入员工数据（密码：123456）
INSERT INTO `t_employee` (`username`, `password`, `name`, `gender`, `phone`, `email`, `department_id`, `position`, `status`, `role`) VALUES
('admin', 'abc12345$e10adc3949ba59abbe56e057f20f883e', '系统管理员', '男', '13800000000', 'admin@oa.com', 1, '系统管理员', '在职', 'SYSTEM_ADMIN'),
('zhangsan', 'def67890$e10adc3949ba59abbe56e057f20f883e', '张三', '男', '13800138001', 'zhangsan@oa.com', 1, '技术总监', '在职', 'DEPARTMENT_MANAGER'),
('lisi', 'ghi11111$e10adc3949ba59abbe56e057f20f883e', '李四', '女', '13800138002', 'lisi@oa.com', 2, '产品总监', '在职', 'DEPARTMENT_MANAGER'),
('wangwu', 'jkl22222$e10adc3949ba59abbe56e057f20f883e', '王五', '男', '13800138003', 'wangwu@oa.com', 3, '运营总监', '在职', 'PROCESS_ADMIN'),
('emp001', 'mno33333$e10adc3949ba59abbe56e057f20f883e', '员工一', '男', '13800138011', 'emp001@oa.com', 1, 'Java开发工程师', '在职', 'EMPLOYEE'),
('emp002', 'pqr44444$e10adc3949ba59abbe56e057f20f883e', '员工二', '女', '13800138012', 'emp002@oa.com', 1, '前端开发工程师', '在职', 'EMPLOYEE'),
('emp003', 'stu55555$e10adc3949ba59abbe56e057f20f883e', '员工三', '男', '13800138013', 'emp003@oa.com', 2, '产品经理', '在职', 'EMPLOYEE'),
('emp004', 'vwx66666$e10adc3949ba59abbe56e057f20f883e', '员工四', '女', '13800138014', 'emp004@oa.com', 3, '运营专员', '在职', 'EMPLOYEE'),
('emp005', 'yza77777$e10adc3949ba59abbe56e057f20f883e', '员工五', '男', '13800138015', 'emp005@oa.com', 4, '人事专员', '在职', 'EMPLOYEE'),
('emp006', 'bcd88888$e10adc3949ba59abbe56e057f20f883e', '员工六', '女', '13800138016', 'emp006@oa.com', 5, '财务专员', '在职', 'EMPLOYEE');

-- 3. 回填部门负责人ID
UPDATE `t_department` d SET `manager_id` = (
    SELECT `id` FROM `t_employee` e 
    WHERE e.`department_id` = d.`id` AND e.`role` = 'DEPARTMENT_MANAGER'
    LIMIT 1
) WHERE `manager_id` IS NULL;

-- 4. 插入审批数据（6种类型完整示例）
INSERT INTO `t_approval` (
  `applicant_id`, `applicant_name`, `department_id`, `department_name`,
  `approval_type`, `title`, `content`, `status`, `priority`,
  `leave_type`, `start_time`, `end_time`, `total_days`,
  `expense_type`, `amount`, `expense_date`, `invoice_number`, `invoice_image`,
  `goods_name`, `quantity`, `unit_price`, `total_amount`, `supplier`,
  `work_date`, `start_time_only`, `end_time_only`, `total_hours`,
  `card_date`, `card_time`, `card_type`,
  `create_time`
) VALUES
-- 1. 请假申请
(5, '员工一', 1, '技术部', 'leave', '2026年7月年假申请', '本人计划于下周一年假休息一天', '待审批', '普通',
 '年假', '2026-07-20 09:00:00', '2026-07-20 18:00:00', 1.0,
 NULL, NULL, NULL, NULL, NULL,
 NULL, NULL, NULL, NULL, NULL,
 NULL, NULL, NULL, NULL,
 NULL, NULL, NULL,
 '2026-07-16 09:00:00'),

-- 2. 出差申请
(5, '员工一', 1, '技术部', 'business', '北京出差申请', '需要前往北京客户现场进行项目部署', '已通过', '重要',
 NULL, '2026-07-25 09:00:00', '2026-07-27 18:00:00', 3.0,
 NULL, NULL, NULL, NULL, NULL,
 NULL, NULL, NULL, NULL, NULL,
 NULL, NULL, NULL, NULL,
 NULL, NULL, NULL,
 '2026-07-16 10:00:00'),

-- 3. 加班申请
(6, '员工二', 1, '技术部', 'overtime', '周末加班申请', '项目赶进度，需要周末加班', '待审批', '紧急',
 NULL, NULL, NULL, NULL,
 NULL, NULL, NULL, NULL, NULL,
 NULL, NULL, NULL, NULL, NULL,
 '2026-07-20', '09:00', '18:00', 9.0,
 NULL, NULL, NULL,
 '2026-07-16 11:00:00'),

-- 4. 报销申请
(7, '员工三', 2, '产品部', 'reimburse', '交通费报销', '出差北京交通费报销', '已通过', '普通',
 NULL, NULL, NULL, NULL,
 '交通费', 1500.00, '2026-07-15', 'INV-2026-001', '/uploads/invoice-001.jpg',
 NULL, NULL, NULL, NULL, NULL,
 NULL, NULL, NULL, NULL,
 NULL, NULL, NULL,
 '2026-07-16 14:00:00'),

-- 5. 采购申请
(8, '员工四', 3, '运营部', 'purchase', '办公用品采购', '需要采购打印纸、文件夹等办公用品', '待审批', '普通',
 NULL, NULL, NULL, NULL,
 NULL, NULL, NULL, NULL, NULL,
 '打印纸', 10, 50.00, 500.00, '得力文具',
 NULL, NULL, NULL, NULL,
 NULL, NULL, NULL,
 '2026-07-16 15:00:00'),

-- 6. 补卡申请
(5, '员工一', 1, '技术部', 'card', '上班补卡申请', '2026年7月16日上班忘记打卡', '待审批', '普通',
 NULL, NULL, NULL, NULL,
 NULL, NULL, NULL, NULL, NULL,
 NULL, NULL, NULL, NULL, NULL,
 NULL, NULL, NULL, NULL,
 '2026-07-16', '09:00', '上班签到',
 '2026-07-16 16:00:00');

-- 5. 插入公告数据
INSERT INTO `t_announcement` (`title`, `content`, `publisher_id`, `publisher_name`, `category`, `priority`, `is_top`, `status`) VALUES
('关于公司年度体检的通知', '公司将于2026年8月组织全体员工进行年度体检，请各位同事注意时间安排。', 1, '系统管理员', '通知', '重要', 1, '已发布'),
('新版OA系统上线通知', '新版OA管理系统已正式上线，包含员工管理、审批管理、考勤管理等功能。', 1, '系统管理员', '通知', '紧急', 1, '已发布');

-- 6. 插入考勤数据
INSERT INTO `t_attendance` (`employee_id`, `employee_name`, `date`, `check_in_time`, `check_out_time`, `status`) VALUES
(5, '员工一', '2026-07-15', '2026-07-15 08:55:00', '2026-07-15 18:10:00', '正常'),
(5, '员工一', '2026-07-16', '2026-07-16 09:15:00', '2026-07-16 18:00:00', '迟到'),
(6, '员工二', '2026-07-15', '2026-07-15 08:50:00', '2026-07-15 18:20:00', '正常');

-- 7. 插入会议室数据
INSERT INTO `t_meeting_room` (`name`, `capacity`, `equipment`, `location`, `status`) VALUES
('第一会议室', 10, '投影仪、白板、音响', '3楼301', '可用'),
('第二会议室', 20, '投影仪、白板、音响、视频会议系统', '3楼302', '可用'),
('第三会议室', 8, '投影仪、白板', '4楼401', '维修中');

-- 8. 插入会议预约数据
INSERT INTO `t_meeting` (`title`, `room_id`, `room_name`, `organizer_id`, `organizer_name`, `participants`, `start_time`, `end_time`, `status`) VALUES
('项目启动会', 1, '第一会议室', 5, '员工一', '员工一,员工二,员工三', '2026-07-20 09:00:00', '2026-07-20 11:00:00', '已预约'),
('部门周例会', 2, '第二会议室', 1, '系统管理员', '技术部全体员工', '2026-07-22 14:00:00', '2026-07-22 16:00:00', '已预约');

-- 9. 插入系统配置数据
INSERT INTO `t_system_config` (`config_key`, `config_value`, `config_type`, `description`, `is_public`) VALUES
('REDIS_ENABLED', 'true', 'BOOLEAN', 'Redis缓存开关', 1),
('ES_ENABLED', 'true', 'BOOLEAN', 'Elasticsearch开关', 1),
('TOKEN_EXPIRE_TIME', '86400', 'NUMBER', 'Token过期时间（秒）', 0),
('DEFAULT_PAGE_SIZE', '10', 'NUMBER', '分页默认条数', 1);

-- 10. 插入通知数据
INSERT INTO `t_notification` (`user_id`, `type`, `title`, `content`, `source_id`, `source_type`, `is_read`) VALUES
(5, 'APPROVAL', '✅ 年假申请已通过', '您申请的【2026年7月年假申请】已通过', 1, 'approval', 0),
(5, 'APPROVAL', '❌ 加班申请已拒绝', '您申请的【周末加班申请】已拒绝', 3, 'approval', 1);

-- ==========================================
-- 验证数据
-- ==========================================
SELECT '========================================' AS '';
SELECT 'OA管理系统数据库初始化完成！v3.1' AS '消息';
SELECT '========================================' AS '';
SELECT COUNT(*) AS '部门数量' FROM t_department;
SELECT COUNT(*) AS '员工数量' FROM t_employee;
SELECT COUNT(*) AS '审批数量' FROM t_approval;
SELECT COUNT(*) AS '公告数量' FROM t_announcement;
SELECT COUNT(*) AS '考勤记录数' FROM t_attendance;
SELECT COUNT(*) AS '会议室数量' FROM t_meeting_room;
SELECT COUNT(*) AS '会议预约数' FROM t_meeting;
SELECT COUNT(*) AS '通知数量' FROM t_notification;
SELECT '========================================' AS '';
SELECT '默认管理员账号：admin / 123456' AS '登录信息';
SELECT '测试员工账号：emp001 / 123456' AS '登录信息';
SELECT '========================================' AS '';

-- 恢复外键检查
SET FOREIGN_KEY_CHECKS = 1;