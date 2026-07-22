-- ==========================================
-- OA管理系统完整数据库脚本（最终版）
-- 整合add_oa_system.sql和oa_system_merged.sql
-- 创建时间: 2026-07-22
-- 适配版本: SpringBoot 2.7.18 + Vue 3
-- 技术栈: MySQL 8.0+
-- 密码统一为: 123456
-- ==========================================

SET FOREIGN_KEY_CHECKS = 0;

CREATE DATABASE IF NOT EXISTS oa_system DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;

USE oa_system;

-- ==========================================
-- 1. 部门表 (t_department)
-- ==========================================
DROP TABLE IF EXISTS `t_department`;
CREATE TABLE `t_department` (
  `id` INT(11) NOT NULL AUTO_INCREMENT COMMENT '部门ID',
  `name` VARCHAR(50) NOT NULL COMMENT '部门名称',
  `manager_id` INT(11) DEFAULT NULL COMMENT '部门负责人ID',
  `manager_name` VARCHAR(50) DEFAULT NULL COMMENT '部门负责人姓名（冗余字段）',
  `manager_phone` VARCHAR(20) DEFAULT NULL COMMENT '负责人电话',
  `parent_id` INT(11) DEFAULT 0 COMMENT '上级部门ID（0表示顶级部门）',
  `level` INT(11) DEFAULT 1 COMMENT '部门层级',
  `path` VARCHAR(500) DEFAULT NULL COMMENT '部门路径',
  `description` VARCHAR(200) DEFAULT NULL COMMENT '部门描述',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_name` (`name`),
  KEY `idx_parent_id` (`parent_id`),
  KEY `idx_manager_id` (`manager_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='部门表';

-- ==========================================
-- 2. 员工表 (t_employee)
-- ==========================================
DROP TABLE IF EXISTS `t_employee`;
CREATE TABLE `t_employee` (
  `id` INT(11) NOT NULL AUTO_INCREMENT COMMENT '员工ID',
  `username` VARCHAR(50) NOT NULL COMMENT '用户名（登录账号）',
  `password` VARCHAR(100) NOT NULL COMMENT '密码（MD5+salt格式）',
  `name` VARCHAR(50) NOT NULL COMMENT '员工姓名',
  `gender` VARCHAR(10) DEFAULT '男' COMMENT '性别（男/女）',
  `phone` VARCHAR(20) DEFAULT NULL COMMENT '联系电话',
  `email` VARCHAR(100) DEFAULT NULL COMMENT '电子邮箱',
  `department_id` INT(11) DEFAULT NULL COMMENT '所属部门ID',
  `position` VARCHAR(50) DEFAULT NULL COMMENT '职位',
  `status` VARCHAR(20) DEFAULT '在职' COMMENT '状态（在职/离职）',
  `role` VARCHAR(50) DEFAULT 'EMPLOYEE' COMMENT '用户角色（SYSTEM_ADMIN/DEPARTMENT_MANAGER/EMPLOYEE）',
  `avatar` VARCHAR(500) DEFAULT NULL COMMENT '头像URL',
  `join_date` VARCHAR(20) DEFAULT NULL COMMENT '入职日期',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_username` (`username`),
  KEY `idx_department_id` (`department_id`),
  KEY `idx_role` (`role`),
  CONSTRAINT `fk_employee_department` FOREIGN KEY (`department_id`) REFERENCES `t_department` (`id`) ON DELETE SET NULL ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='员工表';

ALTER TABLE `t_department` 
ADD CONSTRAINT `fk_department_manager` FOREIGN KEY (`manager_id`) REFERENCES `t_employee` (`id`) ON DELETE SET NULL ON UPDATE CASCADE;

-- ==========================================
-- 3. 审批表 (t_approval)
-- ==========================================
DROP TABLE IF EXISTS `t_approval`;
CREATE TABLE `t_approval` (
  `id` INT(11) NOT NULL AUTO_INCREMENT COMMENT '审批ID',
  `applicant_id` INT(11) NOT NULL COMMENT '申请人ID',
  `applicant_name` VARCHAR(50) NOT NULL COMMENT '申请人姓名',
  `applicant_department_id` INT(11) DEFAULT NULL COMMENT '申请人部门ID',
  `approval_type` VARCHAR(50) NOT NULL COMMENT '审批类型（leave/business/overtime/reimburse/purchase/card）',
  `title` VARCHAR(100) NOT NULL COMMENT '申请标题',
  `content` TEXT COMMENT '申请内容/事由',
  `start_time` DATETIME DEFAULT NULL COMMENT '开始时间',
  `end_time` DATETIME DEFAULT NULL COMMENT '结束时间',
  `amount` DECIMAL(10,2) DEFAULT NULL COMMENT '金额',
  `leave_type` VARCHAR(50) DEFAULT NULL COMMENT '请假类型',
  `dest_city` VARCHAR(100) DEFAULT NULL COMMENT '出差城市',
  `work_date` VARCHAR(20) DEFAULT NULL COMMENT '加班日期',
  `start_time_only` VARCHAR(20) DEFAULT NULL COMMENT '开始时间(仅时间)',
  `end_time_only` VARCHAR(20) DEFAULT NULL COMMENT '结束时间(仅时间)',
  `expense_type` VARCHAR(50) DEFAULT NULL COMMENT '报销类型',
  `expense_date` VARCHAR(20) DEFAULT NULL COMMENT '费用日期',
  `goods_name` VARCHAR(200) DEFAULT NULL COMMENT '物品名称',
  `quantity` INT(11) DEFAULT NULL COMMENT '采购数量',
  `unit_price` DECIMAL(10,2) DEFAULT NULL COMMENT '单价',
  `card_date` VARCHAR(20) DEFAULT NULL COMMENT '补卡日期',
  `card_time` VARCHAR(20) DEFAULT NULL COMMENT '补卡时间',
  `card_type` VARCHAR(50) DEFAULT NULL COMMENT '补卡类型（late/early/miss）',
  `status` VARCHAR(20) DEFAULT '待审批' COMMENT '状态（待审批/已通过/已拒绝/已撤回）',
  `approver_id` INT(11) DEFAULT NULL COMMENT '审批人ID',
  `approver_name` VARCHAR(50) DEFAULT NULL COMMENT '审批人姓名',
  `approve_time` DATETIME DEFAULT NULL COMMENT '审批时间',
  `approve_reason` VARCHAR(200) DEFAULT NULL COMMENT '审批意见',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_applicant_id` (`applicant_id`),
  KEY `idx_applicant_department_id` (`applicant_department_id`),
  KEY `idx_approver_id` (`approver_id`),
  KEY `idx_status` (`status`),
  KEY `idx_approval_type` (`approval_type`),
  KEY `idx_create_time` (`create_time`),
  CONSTRAINT `fk_approval_applicant` FOREIGN KEY (`applicant_id`) REFERENCES `t_employee` (`id`) ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT `fk_approval_approver` FOREIGN KEY (`approver_id`) REFERENCES `t_employee` (`id`) ON DELETE SET NULL ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='审批表';

-- ==========================================
-- 4. 公告表 (t_announcement)
-- ==========================================
DROP TABLE IF EXISTS `t_announcement`;
CREATE TABLE `t_announcement` (
  `id` INT(11) NOT NULL AUTO_INCREMENT COMMENT '公告ID',
  `title` VARCHAR(200) NOT NULL COMMENT '公告标题',
  `content` TEXT NOT NULL COMMENT '公告内容',
  `publisher_id` INT(11) NOT NULL COMMENT '发布人ID',
  `publisher_name` VARCHAR(50) NOT NULL COMMENT '发布人姓名',
  `category` VARCHAR(50) DEFAULT '通知' COMMENT '分类（通知/制度/会议纪要）',
  `priority` VARCHAR(20) DEFAULT '普通' COMMENT '优先级（紧急/重要/普通）',
  `is_top` TINYINT(1) DEFAULT 0 COMMENT '是否置顶',
  `status` VARCHAR(20) DEFAULT '已发布' COMMENT '状态（草稿/已发布/已撤回）',
  `publish_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '发布时间',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_publisher_id` (`publisher_id`),
  KEY `idx_status` (`status`),
  KEY `idx_is_top` (`is_top`),
  CONSTRAINT `fk_announcement_publisher` FOREIGN KEY (`publisher_id`) REFERENCES `t_employee` (`id`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='公告表';

-- ==========================================
-- 5. 公告阅读状态表 (t_announcement_read_status)
-- ==========================================
DROP TABLE IF EXISTS `t_announcement_read_status`;
CREATE TABLE `t_announcement_read_status` (
  `id` INT(11) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `announcement_id` INT(11) NOT NULL COMMENT '公告ID',
  `employee_id` INT(11) NOT NULL COMMENT '员工ID',
  `is_read` TINYINT(1) DEFAULT 0 COMMENT '是否已读',
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
-- ==========================================
DROP TABLE IF EXISTS `t_attendance`;
CREATE TABLE `t_attendance` (
  `id` INT(11) NOT NULL AUTO_INCREMENT COMMENT '考勤ID',
  `employee_id` INT(11) NOT NULL COMMENT '员工ID',
  `employee_name` VARCHAR(50) NOT NULL COMMENT '员工姓名',
  `date` DATE NOT NULL COMMENT '日期',
  `check_in_time` DATETIME DEFAULT NULL COMMENT '签到时间',
  `check_out_time` DATETIME DEFAULT NULL COMMENT '签退时间',
  `status` VARCHAR(20) DEFAULT '正常' COMMENT '状态',
  `remark` VARCHAR(500) DEFAULT NULL COMMENT '备注',
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
-- ==========================================
DROP TABLE IF EXISTS `t_meeting_room`;
CREATE TABLE `t_meeting_room` (
  `id` INT(11) NOT NULL AUTO_INCREMENT COMMENT '会议室ID',
  `name` VARCHAR(50) NOT NULL COMMENT '会议室名称',
  `capacity` INT(11) DEFAULT 10 COMMENT '容纳人数',
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
-- 8. 会议预约表 (t_meeting)
-- ==========================================
DROP TABLE IF EXISTS `t_meeting`;
CREATE TABLE `t_meeting` (
  `id` INT(11) NOT NULL AUTO_INCREMENT COMMENT '会议ID',
  `title` VARCHAR(200) NOT NULL COMMENT '会议主题',
  `room_id` INT(11) NOT NULL COMMENT '会议室ID',
  `room_name` VARCHAR(50) NOT NULL COMMENT '会议室名称',
  `organizer_id` INT(11) NOT NULL COMMENT '组织者ID',
  `organizer_name` VARCHAR(50) NOT NULL COMMENT '组织者姓名',
  `participants` VARCHAR(500) DEFAULT NULL COMMENT '参与人姓名（逗号分隔）',
  `participant_ids` VARCHAR(500) DEFAULT NULL COMMENT '参与人ID列表（逗号分隔）',
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
  CONSTRAINT `fk_meeting_room` FOREIGN KEY (`room_id`) REFERENCES `t_meeting_room` (`id`) ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT `fk_meeting_organizer` FOREIGN KEY (`organizer_id`) REFERENCES `t_employee` (`id`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='会议预约表';

-- ==========================================
-- 9. 消息表 (t_message)
-- ==========================================
DROP TABLE IF EXISTS `t_message`;
CREATE TABLE `t_message` (
  `id` INT(11) NOT NULL AUTO_INCREMENT COMMENT '消息ID',
  `sender_id` INT(11) DEFAULT NULL COMMENT '发送人ID',
  `sender_name` VARCHAR(50) DEFAULT NULL COMMENT '发送人姓名',
  `receiver_id` INT(11) NOT NULL COMMENT '接收人ID',
  `receiver_name` VARCHAR(50) DEFAULT NULL COMMENT '接收人姓名',
  `title` VARCHAR(200) DEFAULT NULL COMMENT '消息标题',
  `content` TEXT COMMENT '消息内容',
  `msg_type` VARCHAR(50) DEFAULT NULL COMMENT '消息类型',
  `event_type` VARCHAR(50) DEFAULT NULL COMMENT '事件类型（SUBMITTED/APPROVED_NODE/APPROVED_FINAL/REJECTED/WITHDRAWN/CC_ADDED等）',
  `biz_type` VARCHAR(50) DEFAULT 'approval' COMMENT '业务类型（approval/workflow/document等）',
  `biz_id` INT(11) DEFAULT NULL COMMENT '关联的业务单据ID（如审批单ID）',
  `related_id` INT(11) DEFAULT NULL COMMENT '关联ID（申请ID/会议ID等）',
  `is_read` TINYINT(1) DEFAULT 0 COMMENT '是否已读',
  `is_todo` TINYINT(1) DEFAULT 0 COMMENT '是否计入待办角标（1=是，0=否）',
  `jump_url` VARCHAR(200) DEFAULT NULL COMMENT '跳转标识（前端根据此路由跳转，如 /approval/detail/123）',
  `is_top` TINYINT(1) DEFAULT 0 COMMENT '是否置顶',
  `status` VARCHAR(20) DEFAULT '正常' COMMENT '状态',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `read_time` DATETIME DEFAULT NULL COMMENT '阅读时间',
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_receiver_id` (`receiver_id`),
  KEY `idx_is_read` (`is_read`),
  KEY `idx_msg_type` (`msg_type`),
  KEY `idx_create_time` (`create_time`),
  KEY `idx_receiver_is_read` (`receiver_id`, `is_read`),
  KEY `idx_receiver_is_todo` (`receiver_id`, `is_todo`),
  KEY `idx_receiver_biz_type` (`receiver_id`, `biz_type`),
  KEY `idx_event_create_time` (`event_type`, `create_time`),
  KEY `idx_biz_id` (`biz_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='消息表';

-- ==========================================
-- 10. 附件表 (t_attachment)
-- ==========================================
DROP TABLE IF EXISTS `t_attachment`;
CREATE TABLE `t_attachment` (
  `id` INT(11) NOT NULL AUTO_INCREMENT COMMENT '附件ID',
  `business_id` INT(11) NOT NULL COMMENT '业务ID',
  `business_type` VARCHAR(50) NOT NULL COMMENT '业务类型（approval/meeting/announcement）',
  `file_name` VARCHAR(200) NOT NULL COMMENT '文件名',
  `file_path` VARCHAR(500) NOT NULL COMMENT '文件路径',
  `file_size` BIGINT(20) DEFAULT NULL COMMENT '文件大小',
  `file_type` VARCHAR(50) DEFAULT NULL COMMENT '文件类型',
  `uploader_id` INT(11) DEFAULT NULL COMMENT '上传人ID',
  `uploader_name` VARCHAR(50) DEFAULT NULL COMMENT '上传人姓名',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_business_id` (`business_id`),
  KEY `idx_business_type` (`business_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='附件表';

-- ==========================================
-- 11. 字典表 (t_dict)
-- ==========================================
DROP TABLE IF EXISTS `t_dict`;
CREATE TABLE `t_dict` (
  `id` INT(11) NOT NULL AUTO_INCREMENT COMMENT '字典ID',
  `key` VARCHAR(50) NOT NULL COMMENT '字典键',
  `name` VARCHAR(50) NOT NULL COMMENT '字典名称',
  `options` TEXT DEFAULT NULL COMMENT '选项（JSON格式）',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_key` (`key`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='字典表';

-- ==========================================
-- 12. 操作日志表 (t_operation_log)
-- ==========================================
DROP TABLE IF EXISTS `t_operation_log`;
CREATE TABLE `t_operation_log` (
  `id` INT(11) NOT NULL AUTO_INCREMENT COMMENT '日志ID',
  `user_id` INT(11) DEFAULT NULL COMMENT '用户ID',
  `user` VARCHAR(50) DEFAULT NULL COMMENT '用户名',
  `action` VARCHAR(50) DEFAULT NULL COMMENT '操作类型',
  `desc` VARCHAR(500) DEFAULT NULL COMMENT '操作描述',
  `ip_address` VARCHAR(50) DEFAULT NULL COMMENT 'IP地址',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='操作日志表';

-- ==========================================
-- 13. 角色表 (t_role)
-- ==========================================
DROP TABLE IF EXISTS `t_role`;
CREATE TABLE `t_role` (
  `id` INT(11) NOT NULL AUTO_INCREMENT COMMENT '角色ID',
  `name` VARCHAR(50) NOT NULL COMMENT '角色名称',
  `code` VARCHAR(50) NOT NULL COMMENT '角色编码',
  `desc` VARCHAR(200) DEFAULT NULL COMMENT '角色描述',
  `icon` VARCHAR(50) DEFAULT NULL COMMENT '角色图标',
  `color` VARCHAR(20) DEFAULT NULL COMMENT '角色颜色',
  `permissions` TEXT DEFAULT NULL COMMENT '权限列表（JSON格式）',
  `user_count` INT(11) DEFAULT 0 COMMENT '用户数量',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_code` (`code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='角色表';

-- ==========================================
-- 触发器
-- ==========================================
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
        SET MESSAGE_TEXT = '会议室预约时间冲突';
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
            SET MESSAGE_TEXT = '会议室预约时间冲突';
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
-- 初始化数据
-- ==========================================

-- 1. 部门数据（manager_id已根据员工数据设置）
INSERT INTO `t_department` (`name`, `manager_id`, `manager_name`, `manager_phone`, `parent_id`, `level`, `path`, `description`) VALUES
('技术部', 2, '张三', '13800138001', 0, 1, '1', '负责技术研发'),
('产品部', 3, '李四', '13800138002', 0, 1, '2', '负责产品设计'),
('运营部', 4, '王五', '13800138003', 0, 1, '3', '负责运营推广'),
('人事部', 5, '赵六', '13800138004', 0, 1, '4', '负责人力资源'),
('财务部', 6, '钱七', '13800138005', 0, 1, '5', '负责财务管理');

-- 2. 员工数据（密码均为 123456，格式：salt$MD5(salt+password)）
INSERT INTO `t_employee` (`username`, `password`, `name`, `gender`, `phone`, `email`, `department_id`, `position`, `status`, `role`, `join_date`) VALUES
('admin', '117b95a6$51ca19c4df60f4d61ba99ef493ac1097', '系统管理员', '男', '13800000000', 'admin@oa.com', 1, '系统管理员', '在职', 'SYSTEM_ADMIN', '2026-01-01'),
('zhangsan', 'adb046e2$2dc92ba0cfdaf0a1ca7c4c0a5643bb0d', '张三', '男', '13800138001', 'zhangsan@oa.com', 1, '技术总监', '在职', 'DEPARTMENT_MANAGER', '2026-01-01'),
('lisi', '2a20fba1$c85ccc96e3613bfbc1a26553927ac308', '李四', '女', '13800138002', 'lisi@oa.com', 2, '产品总监', '在职', 'DEPARTMENT_MANAGER', '2026-01-01'),
('wangwu', 'f4c5a0f5$7311b2b39797f532ed34d0a677ba1ca0', '王五', '男', '13800138003', 'wangwu@oa.com', 3, '运营总监', '在职', 'EMPLOYEE', '2026-01-01'),
('zhaoliu', 'f6f8a323$ec58ad05e08b696a900d85e4e3993a10', '赵六', '女', '13800138004', 'zhaoliu@oa.com', 4, '人事主管', '在职', 'EMPLOYEE', '2026-01-01'),
('qianqi', '8f6af415$3a867c792bff55aa043720ec714f5aba', '钱七', '男', '13800138005', 'qianqi@oa.com', 5, '财务主管', '在职', 'EMPLOYEE', '2026-01-01'),
('emp001', 'f4ab09ca$587356ebf25fe7eaf41affe8b97e8a61', '员工一', '男', '13800138011', 'emp001@oa.com', 1, 'Java开发工程师', '在职', 'EMPLOYEE', '2026-03-01'),
('emp002', 'dcb68990$1a892bd3ca9e07b490b0fbc521cce252', '员工二', '女', '13800138012', 'emp002@oa.com', 1, '前端开发工程师', '在职', 'EMPLOYEE', '2026-03-01'),
('emp003', '530e80b1$74581ab20d887519370591843b3f874b', '员工三', '男', '13800138013', 'emp003@oa.com', 2, '产品经理', '在职', 'EMPLOYEE', '2026-03-01'),
('emp004', 'bcd88888$e772397f7bf3db6d9cc7ff5720cb39a2', '员工四', '女', '13800138014', 'emp004@oa.com', 3, '运营专员', '在职', 'EMPLOYEE', '2026-03-01');

-- 3. 审批数据（状态使用中文：待审批/已通过/已拒绝/已撤回，补卡类型使用英文：late/early/miss）
INSERT INTO `t_approval` (`applicant_id`, `applicant_name`, `applicant_department_id`, `approval_type`, `title`, `content`, `start_time`, `end_time`, `amount`, `leave_type`, `dest_city`, `work_date`, `start_time_only`, `end_time_only`, `expense_type`, `expense_date`, `goods_name`, `quantity`, `unit_price`, `card_date`, `card_time`, `card_type`, `status`, `approver_id`, `approver_name`, `approve_time`, `approve_reason`) VALUES
(7, '员工一', 1, 'leave', '年假申请', '计划休假一天', '2026-07-21 09:00:00', '2026-07-21 18:00:00', NULL, '年假', NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, '待审批', NULL, NULL, NULL, NULL),
(7, '员工一', 1, 'business', '北京出差', '前往北京客户现场', '2026-07-25 09:00:00', '2026-07-27 18:00:00', NULL, NULL, '北京', NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, '已通过', 1, '系统管理员', '2026-07-22 09:46:58', '同意出差'),
(8, '员工二', 1, 'overtime', '周末加班', '项目赶进度', NULL, NULL, NULL, NULL, NULL, '2026-07-26', '18:00', '20:00', NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, '待审批', NULL, NULL, NULL, NULL),
(9, '员工三', 2, 'reimburse', '交通费报销', '出差交通费', NULL, NULL, 1500.00, NULL, NULL, NULL, NULL, NULL, '交通费', '2026-07-15', NULL, NULL, NULL, NULL, NULL, NULL, '已通过', 1, '系统管理员', '2026-07-22 10:24:45', '同意报销'),
(10, '员工四', 3, 'purchase', '办公用品采购', '采购打印纸', NULL, NULL, 500.00, NULL, NULL, NULL, NULL, NULL, NULL, NULL, '打印纸', 10, 50.00, NULL, NULL, NULL, '待审批', NULL, NULL, NULL, NULL),
(7, '员工一', 1, 'card', '迟到补卡', '因交通拥堵迟到', NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, '2026-07-19', '09:15', 'late', '已通过', 2, '张三', '2026-07-22 09:46:58', '情况属实');

-- 4. 公告数据
INSERT INTO `t_announcement` (`title`, `content`, `publisher_id`, `publisher_name`, `category`, `priority`, `is_top`, `status`) VALUES
('年度体检通知', '公司将于8月组织年度体检，请各位同事安排时间。', 1, '系统管理员', '通知', '重要', 1, '已发布'),
('新版OA系统上线', '新版OA系统已正式上线，请大家积极使用。', 1, '系统管理员', '通知', '紧急', 1, '已发布'),
('信息安全通知', '请各位同事注意信息安全，定期修改密码。', 4, '王五', '制度', '重要', 0, '已发布');

-- 5. 考勤数据
INSERT INTO `t_attendance` (`employee_id`, `employee_name`, `date`, `check_in_time`, `check_out_time`, `status`, `remark`) VALUES
(7, '员工一', '2026-07-15', '2026-07-15 08:55:00', '2026-07-15 18:10:00', '正常', NULL),
(7, '员工一', '2026-07-16', '2026-07-16 09:15:00', '2026-07-16 18:00:00', '迟到', '交通拥堵'),
(8, '员工二', '2026-07-15', '2026-07-15 08:50:00', '2026-07-15 18:20:00', '正常', NULL),
(8, '员工二', '2026-07-16', '2026-07-16 09:00:00', '2026-07-16 18:05:00', '正常', NULL);

-- 6. 会议室数据
INSERT INTO `t_meeting_room` (`name`, `capacity`, `equipment`, `location`, `status`) VALUES
('第一会议室', 10, '投影仪、白板', '3楼301', '可用'),
('第二会议室', 20, '投影仪、视频会议', '3楼302', '可用'),
('培训室', 50, '投影仪、音响', '5楼501', '可用'),
('VIP会议室', 15, '投影仪、视频会议、茶水', '6楼601', '可用');

-- 7. 会议预约数据
INSERT INTO `t_meeting` (`title`, `room_id`, `room_name`, `organizer_id`, `organizer_name`, `participants`, `participant_ids`, `start_time`, `end_time`, `status`, `remark`) VALUES
('项目启动会', 1, '第一会议室', 7, '员工一', '员工一,员工二,员工三', '7,8,9', '2026-07-21 09:00:00', '2026-07-21 11:00:00', '已预约', '新项目启动'),
('部门周例会', 2, '第二会议室', 2, '张三', '技术部全体', '1,2,7,8', '2026-07-22 14:00:00', '2026-07-22 16:00:00', '已预约', '周例会');

-- 8. 消息数据（包含event_type、biz_type、biz_id、is_todo、jump_url字段）
INSERT INTO `t_message` (`sender_id`, `sender_name`, `receiver_id`, `receiver_name`, `title`, `content`, `msg_type`, `event_type`, `biz_type`, `biz_id`, `related_id`, `is_read`, `is_todo`, `jump_url`) VALUES
(1, '系统管理员', 7, '员工一', '申请已通过', '您的出差申请已通过', 'APPROVAL', 'APPROVED_FINAL', 'approval', 2, 2, 0, 0, '/approval/detail/2'),
(2, '张三', 7, '员工一', '补卡已通过', '您的迟到补卡已通过', 'APPROVAL', 'APPROVED_FINAL', 'approval', 6, 6, 1, 0, '/approval/detail/6'),
(1, '系统管理员', 7, '员工一', '申请已通过', '您的请假申请已由张三审批通过。', 'APPROVAL', 'APPROVED_FINAL', 'approval', 1, 1, 0, 0, '/approval/detail/1'),
(1, '系统管理员', 8, '员工二', '申请已通过', '您的加班申请已由系统管理员审批通过。', 'APPROVAL', 'APPROVED_FINAL', 'approval', 3, 3, 0, 0, '/approval/detail/3'),
(2, '张三', 1, '系统管理员', '会议信息变更通知', '您参加的会议【部门周例会】信息已更新：开始时间变更为「2026-07-22 10:25」；结束时间变更为「2026-07-22 10:30」；请关注最新安排。', 'MEETING', NULL, 'approval', NULL, NULL, 0, 0, NULL);

-- 9. 字典数据（补卡类型使用英文值）
INSERT INTO `t_dict` (`key`, `name`, `options`) VALUES
('leave_type', '请假类型', '[{"label":"事假","value":"事假"},{"label":"病假","value":"病假"},{"label":"年假","value":"年假"},{"label":"调休","value":"调休"},{"label":"婚假","value":"婚假"},{"label":"产假","value":"产假"},{"label":"丧假","value":"丧假"}]'),
('expense_type', '报销类型', '[{"label":"交通费","value":"交通费"},{"label":"住宿费","value":"住宿费"},{"label":"餐饮费","value":"餐饮费"},{"label":"办公费","value":"办公费"},{"label":"差旅费","value":"差旅费"},{"label":"其他","value":"其他"}]'),
('card_type', '补卡类型', '[{"label":"迟到补卡","value":"late"},{"label":"早退补卡","value":"early"},{"label":"漏签补卡","value":"miss"}]'),
('approval_status', '审批状态', '[{"label":"待审批","value":"待审批"},{"label":"已通过","value":"已通过"},{"label":"已拒绝","value":"已拒绝"},{"label":"已撤回","value":"已撤回"}]');

-- 10. 角色数据
INSERT INTO `t_role` (`name`, `code`, `desc`, `icon`, `color`, `permissions`) VALUES
('系统管理员', 'SYSTEM_ADMIN', '系统管理员，拥有所有权限', 'admin', '#ff6b6b', '["user:manage","department:manage","approval:all","announcement:manage","meeting:manage","attendance:manage"]'),
('部门主管', 'DEPARTMENT_MANAGER', '部门主管，管理本部门事务', 'manager', '#4ecdc4', '["user:department","approval:department","announcement:view","meeting:reserve","attendance:view"]'),
('普通员工', 'EMPLOYEE', '普通员工，基本操作权限', 'user', '#45b7d1', '["approval:apply","announcement:view","meeting:reserve","attendance:check"]');

SET FOREIGN_KEY_CHECKS = 1;

SELECT 'OA管理系统数据库初始化完成！' AS '消息';
SELECT '管理员账号：admin / 123456' AS '登录信息';
SELECT '部门主管：zhangsan / 123456（技术部）' AS '登录信息';
SELECT '部门主管：lisi / 123456（产品部）' AS '登录信息';
SELECT '普通员工：emp001 / 123456' AS '登录信息';

-- ==========================================
-- 消息事件类型字典参考
-- ==========================================
/*
事件类型表：

| 事件类型        | 触发时机                             | 通知接收人                 | 是否计入待办 | 文案要点                                    |
|-----------------|--------------------------------------|----------------------------|------------|-------------------------------------------|
| SUBMITTED       | 发起人提交单据                       | 第一级审批人               | 是         | "你有一条新的待审批单据"                   |
| APPROVED_NODE   | 非末级节点同意，流程流转到下一级     | 下一级审批人               | 是         | "有新的待审批单据"                         |
| APPROVED_FINAL  | 末级节点同意，流程结束               | 发起人（+抄送人）          | 否，仅提醒 | "你的XX单据已审批通过"                     |
| REJECTED        | 任一节点审批人驳回                   | 发起人                     | 否，仅提醒 | "你的单据被驳回，原因：xx"                 |
| WITHDRAWN       | 发起人撤回                           | 当前节点审批人             | 否，失效   | "该单据已被撤回"                           |
| CC_ADDED        | 抄送人列表变更（未来支持）           | 新增的抄送人               | 否，仅提醒 | "你被添加为该单据的抄送人"                 |
*/
