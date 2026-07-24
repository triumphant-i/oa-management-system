-- ==========================================
-- OA管理系统完整数据库脚本（基于本地数据库生成）
-- 创建时间: 2026-07-24
-- 适配版本: SpringBoot 2.7.18 + Vue 3 + Flowable 6.8.0
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
  `version` INT(11) DEFAULT 0 COMMENT '乐观锁版本',
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
  `shift_id` INT(11) DEFAULT NULL COMMENT '班次ID',
  `position` VARCHAR(50) DEFAULT NULL COMMENT '职位',
  `status` VARCHAR(20) DEFAULT '在职' COMMENT '状态（在职/离职）',
  `role` VARCHAR(50) DEFAULT 'EMPLOYEE' COMMENT '用户角色（SYSTEM_ADMIN/DEPARTMENT_MANAGER/EMPLOYEE）',
  `avatar` VARCHAR(500) DEFAULT NULL COMMENT '头像URL',
  `join_date` VARCHAR(20) DEFAULT NULL COMMENT '入职日期',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `version` INT(11) DEFAULT 0 COMMENT '乐观锁版本',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_username` (`username`),
  KEY `idx_department_id` (`department_id`),
  KEY `idx_role` (`role`),
  KEY `idx_shift_id` (`shift_id`),
  CONSTRAINT `fk_employee_department` FOREIGN KEY (`department_id`) REFERENCES `t_department` (`id`) ON DELETE SET NULL ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='员工表';

-- ==========================================
-- 3. 班次表 (t_shift)
-- ==========================================
DROP TABLE IF EXISTS `t_shift`;
CREATE TABLE `t_shift` (
  `id` INT(11) NOT NULL AUTO_INCREMENT COMMENT '班次ID',
  `name` VARCHAR(50) NOT NULL COMMENT '班次名称',
  `work_start` TIME NOT NULL COMMENT '上班时间',
  `work_end` TIME NOT NULL COMMENT '下班时间',
  `lunch_start` TIME COMMENT '午休开始时间',
  `lunch_end` TIME COMMENT '午休结束时间',
  `lunch_deduct_minutes` INT(11) DEFAULT 60 COMMENT '午休扣除分钟数',
  `late_grace_minutes` INT(11) DEFAULT 5 COMMENT '迟到宽容分钟数',
  `late_threshold_minutes` INT(11) DEFAULT 30 COMMENT '迟到阈值（超过此时间算缺勤）',
  `overtime_threshold_minutes` INT(11) DEFAULT 30 COMMENT '加班阈值（超过此时间算加班）',
  `is_default` TINYINT(1) DEFAULT 0 COMMENT '是否默认班次',
  `is_custom` TINYINT(1) DEFAULT 0 COMMENT '是否自定义班次（专属）',
  `employee_id` INT(11) COMMENT '关联员工ID（专属班次）',
  `description` VARCHAR(200) COMMENT '班次描述',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_shift_name` (`name`),
  KEY `idx_shift_employee` (`employee_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='班次表';

-- ==========================================
-- 4. 审批表 (t_approval)
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
  `status` VARCHAR(20) DEFAULT 'pending' COMMENT '状态（pending/approved/rejected/withdrawn）',
  `approver_id` INT(11) DEFAULT NULL COMMENT '审批人ID',
  `approver_name` VARCHAR(50) DEFAULT NULL COMMENT '审批人姓名',
  `approve_time` DATETIME DEFAULT NULL COMMENT '审批时间',
  `approve_reason` VARCHAR(200) DEFAULT NULL COMMENT '审批意见',
  `process_instance_id` VARCHAR(64) DEFAULT NULL COMMENT '关联的Flowable流程实例ID',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_applicant_id` (`applicant_id`),
  KEY `idx_applicant_department_id` (`applicant_department_id`),
  KEY `idx_approver_id` (`approver_id`),
  KEY `idx_status` (`status`),
  KEY `idx_approval_type` (`approval_type`),
  KEY `idx_create_time` (`create_time`),
  KEY `idx_process_instance_id` (`process_instance_id`),
  CONSTRAINT `fk_approval_applicant` FOREIGN KEY (`applicant_id`) REFERENCES `t_employee` (`id`) ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT `fk_approval_approver` FOREIGN KEY (`approver_id`) REFERENCES `t_employee` (`id`) ON DELETE SET NULL ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='审批表';

-- ==========================================
-- 5. 公告表 (t_announcement)
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
  `version` INT(11) DEFAULT 0 COMMENT '乐观锁版本',
  PRIMARY KEY (`id`),
  KEY `idx_publisher_id` (`publisher_id`),
  KEY `idx_status` (`status`),
  KEY `idx_is_top` (`is_top`),
  CONSTRAINT `fk_announcement_publisher` FOREIGN KEY (`publisher_id`) REFERENCES `t_employee` (`id`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='公告表';

-- ==========================================
-- 6. 公告阅读状态表 (t_announcement_read_status)
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
-- 7. 考勤表 (t_attendance)
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
  `late_minutes` INT(11) DEFAULT 0 COMMENT '迟到分钟数',
  `early_minutes` INT(11) DEFAULT 0 COMMENT '早退分钟数',
  `overtime_before` DECIMAL(5,2) DEFAULT 0 COMMENT '班前加班时长(小时)',
  `overtime_after` DECIMAL(5,2) DEFAULT 0 COMMENT '下班后加班时长(小时)',
  `work_hours` DECIMAL(5,2) DEFAULT 0 COMMENT '有效工时(小时)',
  `attendance_status` VARCHAR(20) DEFAULT '未处理' COMMENT '考勤状态(NORMAL/LATE/EARLY/ABSENT/OVERTIME/LATE_EARLY)',
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
-- 8. 考勤补卡表 (t_attendance_correction)
-- ==========================================
DROP TABLE IF EXISTS `t_attendance_correction`;
CREATE TABLE `t_attendance_correction` (
  `id` INT(11) NOT NULL AUTO_INCREMENT COMMENT '补卡ID',
  `employee_id` INT(11) NOT NULL COMMENT '员工ID',
  `employee_name` VARCHAR(50) NOT NULL COMMENT '员工姓名',
  `department_id` INT(11) DEFAULT NULL COMMENT '部门ID',
  `date` DATE NOT NULL COMMENT '补卡日期',
  `time` TIME NOT NULL COMMENT '补卡时间',
  `type` VARCHAR(20) NOT NULL COMMENT '补卡类型（late/early/miss）',
  `reason` VARCHAR(500) NOT NULL COMMENT '补卡原因',
  `status` VARCHAR(20) DEFAULT '待审批' COMMENT '状态',
  `approver_id` INT(11) DEFAULT NULL COMMENT '审批人ID',
  `approver_name` VARCHAR(50) DEFAULT NULL COMMENT '审批人姓名',
  `approve_time` DATETIME DEFAULT NULL COMMENT '审批时间',
  `approve_reason` VARCHAR(500) DEFAULT NULL COMMENT '审批意见',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_employee_id` (`employee_id`),
  KEY `idx_date` (`date`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='考勤补卡表';

-- ==========================================
-- 9. 考勤加班表 (t_attendance_overtime)
-- ==========================================
DROP TABLE IF EXISTS `t_attendance_overtime`;
CREATE TABLE `t_attendance_overtime` (
  `id` INT(11) NOT NULL AUTO_INCREMENT COMMENT '加班记录ID',
  `employee_id` INT(11) NOT NULL COMMENT '员工ID',
  `employee_name` VARCHAR(50) DEFAULT NULL COMMENT '员工姓名',
  `date` VARCHAR(20) NOT NULL COMMENT '加班日期',
  `sequence` INT(11) DEFAULT 1 COMMENT '同日期加班序号',
  `start_time` DATETIME DEFAULT NULL COMMENT '加班开始时间',
  `end_time` DATETIME DEFAULT NULL COMMENT '加班结束时间',
  `overtime_type` VARCHAR(20) DEFAULT NULL COMMENT '加班类型',
  `check_in_latitude` DECIMAL(10,7) DEFAULT NULL COMMENT '加班签到纬度',
  `check_in_longitude` DECIMAL(10,7) DEFAULT NULL COMMENT '加班签到经度',
  `check_out_latitude` DECIMAL(10,7) DEFAULT NULL COMMENT '加班签退纬度',
  `check_out_longitude` DECIMAL(10,7) DEFAULT NULL COMMENT '加班签退经度',
  `duration` DECIMAL(5,2) DEFAULT NULL COMMENT '加班时长(小时)',
  `status` VARCHAR(20) DEFAULT '待审核' COMMENT '状态',
  `remark` VARCHAR(500) DEFAULT NULL COMMENT '备注',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_employee_id` (`employee_id`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='考勤加班表';

-- ==========================================
-- 10. 会议室表 (t_meeting_room)
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
-- 11. 会议预约表 (t_meeting)
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
  `is_reminded` TINYINT(1) DEFAULT 0 COMMENT '是否已发送结束提醒（0-未发送，1-已发送）',
  `version` INT(11) DEFAULT 0 COMMENT '版本号（乐观锁）',
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
-- 12. 消息表 (t_message)
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
  `event_type` VARCHAR(50) DEFAULT NULL COMMENT '事件类型',
  `biz_type` VARCHAR(50) DEFAULT 'approval' COMMENT '业务类型（approval/workflow/document等）',
  `biz_id` INT(11) DEFAULT NULL COMMENT '关联的业务单据ID',
  `related_id` INT(11) DEFAULT NULL COMMENT '关联ID',
  `is_read` TINYINT(1) DEFAULT 0 COMMENT '是否已读',
  `is_todo` TINYINT(1) DEFAULT 0 COMMENT '是否计入待办角标',
  `jump_url` VARCHAR(200) DEFAULT NULL COMMENT '跳转标识',
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
-- 13. 附件表 (t_attachment)
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
-- 14. 字典表 (t_dict)
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
-- 15. 操作日志表 (t_operation_log)
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
-- 16. 角色表 (t_role)
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
-- 17. 缓存记录表 (t_cache_record)
-- ==========================================
DROP TABLE IF EXISTS `t_cache_record`;
CREATE TABLE `t_cache_record` (
  `id` INT(11) NOT NULL AUTO_INCREMENT COMMENT '缓存记录ID',
  `cache_key` VARCHAR(200) NOT NULL COMMENT '缓存键',
  `cache_type` VARCHAR(50) NOT NULL COMMENT '缓存类型',
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
-- 18. ES同步记录表 (t_es_sync_record)
-- ==========================================
DROP TABLE IF EXISTS `t_es_sync_record`;
CREATE TABLE `t_es_sync_record` (
  `id` INT(11) NOT NULL AUTO_INCREMENT COMMENT '同步记录ID',
  `index_name` VARCHAR(100) NOT NULL COMMENT '索引名称',
  `document_id` VARCHAR(100) NOT NULL COMMENT '文档ID',
  `operation_type` VARCHAR(20) NOT NULL COMMENT '操作类型',
  `sync_status` VARCHAR(20) DEFAULT 'SUCCESS' COMMENT '同步状态',
  `error_message` VARCHAR(500) DEFAULT NULL COMMENT '错误信息',
  `sync_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '同步时间',
  PRIMARY KEY (`id`),
  KEY `idx_index_name` (`index_name`),
  KEY `idx_document_id` (`document_id`),
  KEY `idx_sync_status` (`sync_status`),
  KEY `idx_sync_time` (`sync_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='ES同步记录表';

-- ==========================================
-- 19. 权限日志表 (t_permission_log)
-- ==========================================
DROP TABLE IF EXISTS `t_permission_log`;
CREATE TABLE `t_permission_log` (
  `id` INT(11) NOT NULL AUTO_INCREMENT COMMENT '权限日志ID',
  `user_id` INT(11) NOT NULL COMMENT '用户ID',
  `user_name` VARCHAR(50) NOT NULL COMMENT '用户名',
  `target_type` VARCHAR(50) NOT NULL COMMENT '目标类型',
  `target_id` INT(11) NOT NULL COMMENT '目标ID',
  `action` VARCHAR(50) NOT NULL COMMENT '操作类型',
  `old_value` VARCHAR(200) DEFAULT NULL COMMENT '旧值',
  `new_value` VARCHAR(200) DEFAULT NULL COMMENT '新值',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_target_id` (`target_id`),
  KEY `idx_action` (`action`),
  KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='权限日志表';

-- ==========================================
-- 20. 系统配置表 (t_system_config)
-- ==========================================
DROP TABLE IF EXISTS `t_system_config`;
CREATE TABLE `t_system_config` (
  `id` INT(11) NOT NULL AUTO_INCREMENT COMMENT '配置ID',
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
-- 21. 审批角色表 (t_approval_role) —— Flowable工作流改造新增
-- ==========================================
DROP TABLE IF EXISTS `t_approval_role`;
CREATE TABLE `t_approval_role` (
  `id` INT(11) NOT NULL AUTO_INCREMENT COMMENT '角色ID',
  `role_code` VARCHAR(50) NOT NULL COMMENT '角色编码：DEPT_MANAGER/FINANCE_STAFF/FINANCE_MANAGER/GM',
  `role_name` VARCHAR(50) NOT NULL COMMENT '角色名称',
  `resolve_strategy` VARCHAR(50) NOT NULL COMMENT '解析策略：OWN_DEPT_MANAGER=发起人所在部门负责人；FIXED_DEPT_MANAGER=固定某部门负责人；FIXED_EMPLOYEE=固定某个人',
  `fixed_department_id` INT(11) DEFAULT NULL COMMENT 'resolve_strategy=FIXED_DEPT_MANAGER 时使用',
  `fixed_employee_id` INT(11) DEFAULT NULL COMMENT 'resolve_strategy=FIXED_EMPLOYEE 时使用',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_role_code` (`role_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='审批角色定义表';

-- ==========================================
-- 22. 审批代理表 (t_approval_delegate) —— Flowable工作流改造新增（审批代理/请假委托）
-- ==========================================
DROP TABLE IF EXISTS `t_approval_delegate`;
CREATE TABLE `t_approval_delegate` (
  `id` INT(11) NOT NULL AUTO_INCREMENT COMMENT '代理ID',
  `delegator_id` INT(11) NOT NULL COMMENT '委托人ID（原审批人）',
  `delegate_id` INT(11) NOT NULL COMMENT '代理人ID',
  `approval_type` VARCHAR(50) DEFAULT NULL COMMENT '限定审批类型，NULL表示所有类型都代理',
  `start_time` DATETIME NOT NULL COMMENT '代理生效时间',
  `end_time` DATETIME NOT NULL COMMENT '代理失效时间',
  `status` VARCHAR(20) DEFAULT 'active' COMMENT '状态：active/expired/revoked',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_delegator` (`delegator_id`),
  KEY `idx_time_range` (`start_time`, `end_time`),
  CONSTRAINT `fk_delegate_delegator` FOREIGN KEY (`delegator_id`) REFERENCES `t_employee` (`id`) ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT `fk_delegate_delegate` FOREIGN KEY (`delegate_id`) REFERENCES `t_employee` (`id`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='审批代理（请假委托）表';

-- ==========================================
-- 23. 审批规则表 (t_approval_rule) —— 审批流程规则配置
-- ==========================================
DROP TABLE IF EXISTS `t_approval_rule`;
CREATE TABLE `t_approval_rule` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `approval_type` VARCHAR(50) NOT NULL COMMENT '审批类型',
  `condition_field` VARCHAR(32) NOT NULL DEFAULT 'NONE' COMMENT '条件字段（NONE/DAYS/AMOUNT）',
  `min_value` DECIMAL(18,2) DEFAULT NULL COMMENT '最小值',
  `max_value` DECIMAL(18,2) DEFAULT NULL COMMENT '最大值',
  `required_roles` VARCHAR(255) NOT NULL COMMENT '所需审批角色（逗号分隔）',
  `sign_type` VARCHAR(32) NOT NULL COMMENT '签名类型（SINGLE/PARALLEL/SERIAL_AFTER_PARALLEL）',
  `notify_superior_on_reject` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否通知上级',
  `notify_admin_on_reject` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否通知管理员',
  `priority` INT NOT NULL DEFAULT 0 COMMENT '优先级',
  `status` TINYINT(1) NOT NULL DEFAULT 1 COMMENT '状态：1启用，0禁用',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_approval_rule_match` (`approval_type`, `status`, `condition_field`, `priority`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='审批流规则配置';

-- ==========================================
-- 添加外键约束
-- ==========================================
ALTER TABLE `t_department` 
ADD CONSTRAINT `fk_department_manager` FOREIGN KEY (`manager_id`) REFERENCES `t_employee` (`id`) ON DELETE SET NULL ON UPDATE CASCADE;

-- ==========================================
-- 触发器
-- ==========================================
DELIMITER $$

-- ==========================================
-- 存储过程：检查会议室可用性
-- ==========================================
CREATE PROCEDURE `sp_check_room_availability`(
    IN `roomId` INT,
    IN `startTime` DATETIME,
    IN `endTime` DATETIME,
    OUT `isAvailable` BOOLEAN,
    OUT `conflictCount` INT
)
BEGIN
    SELECT COUNT(*) INTO `conflictCount`
    FROM `t_meeting`
    WHERE `room_id` = `roomId`
      AND `status` <> '已取消'
      AND (
          (`startTime` BETWEEN `start_time` AND `end_time`)
          OR (`endTime` BETWEEN `start_time` AND `end_time`)
          OR (`start_time` BETWEEN `startTime` AND `endTime`)
      );
    
    SET `isAvailable` = `conflictCount` = 0;
END$$

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
-- 初始化数据（基于本地数据库实际数据）
-- ==========================================

-- 1. 部门数据
INSERT INTO `t_department` (`name`, `manager_id`, `manager_name`, `manager_phone`, `parent_id`, `level`, `path`, `description`, `version`) VALUES
('技术部', 2, '张三', '13800138001', 0, 1, '1', '负责技术研发', 0),
('产品部', 3, '李四', '13800138002', 0, 1, '2', '负责产品设计', 0),
('运营部', 4, '王五', '13800138003', 0, 1, '3', '负责运营推广', 0),
('人事部', 5, '赵六', '13800138004', 0, 1, '4', '负责人力资源', 0),
('财务部', 6, '钱七', '13800138005', 0, 1, '5', '负责财务管理', 0);

-- 2. 员工数据（密码均为 123456，格式：salt$MD5(salt+password)）
INSERT INTO `t_employee` (`username`, `password`, `name`, `gender`, `phone`, `email`, `department_id`, `shift_id`, `position`, `status`, `role`, `avatar`, `join_date`, `version`) VALUES
('admin', '117b95a6$51ca19c4df60f4d61ba99ef493ac1097', '系统管理员', '男', '13800000000', 'admin@oa.com', 1, 1, '系统管理员', '在职', 'SYSTEM_ADMIN', '', '2026-01-01', 2),
('zhangsan', 'adb046e2$2dc92ba0cfdaf0a1ca7c4c0a5643bb0d', '张三', '男', '13800138001', 'zhangsan@oa.com', 1, 2, '技术总监', '在职', 'DEPARTMENT_MANAGER', NULL, '2026-01-01', 0),
('lisi', '2a20fba1$c85ccc96e3613bfbc1a26553927ac308', '李四', '女', '13800138002', 'lisi@oa.com', 2, 2, '产品总监', '在职', 'DEPARTMENT_MANAGER', NULL, '2026-01-01', 0),
('wangwu', 'f4c5a0f5$7311b2b39797f532ed34d0a677ba1ca0', '王五', '男', '13800138003', 'wangwu@oa.com', 3, 2, '运营总监', '在职', 'DEPARTMENT_MANAGER', NULL, '2026-01-01', 0),
('zhaoliu', 'f6f8a323$ec58ad05e08b696a900d85e4e3993a10', '赵六', '女', '13800138004', 'zhaoliu@oa.com', 4, 2, '人事主管', '在职', 'DEPARTMENT_MANAGER', NULL, '2026-01-01', 0),
('qianqi', '8f6af415$3a867c792bff55aa043720ec714f5aba', '钱七', '男', '13800138005', 'qianqi@oa.com', 5, 2, '财务主管', '在职', 'DEPARTMENT_MANAGER', NULL, '2026-01-01', 0),
('emp001', 'f4ab09ca$587356ebf25fe7eaf41affe8b97e8a61', '员工一', '男', '13800138011', 'emp001@oa.com', 1, 3, 'Java开发工程师', '在职', 'EMPLOYEE', NULL, '2026-03-01', 0),
('emp002', 'dcb68990$1a892bd3ca9e07b490b0fbc521cce252', '员工二', '女', '13800138012', 'emp002@oa.com', 1, 3, '前端开发工程师', '在职', 'EMPLOYEE', NULL, '2026-03-01', 0),
('emp003', '530e80b1$74581ab20d887519370591843b3f874b', '员工三', '男', '13800138013', 'emp003@oa.com', 2, 2, '产品经理', '在职', 'EMPLOYEE', NULL, '2026-03-01', 0),
('emp004', 'bcd88888$e772397f7bf3db6d9cc7ff5720cb39a2', '员工四', '女', '13800138014', 'emp004@oa.com', 3, 2, '运营专员', '在职', 'EMPLOYEE', NULL, '2026-03-01', 0),
('emp005', 'fb3ba473$c7071a6d59ce6ea61079e6f615ed4df5', '员工五', '男', '13800138015', 'emp005@oa.com', 5, 2, '会计', '在职', 'EMPLOYEE', '', '2026-07-21', 0),
('sunba', '6649261e$10b340aef7e12724c26bb910dbaf6547', '孙八', '男', '13800138006', 'sunba@oa.com', 5, 2, '财务专员', '在职', 'EMPLOYEE', NULL, '2026-07-21', 0),
('zhouzongjingli', '9508591f$3f60a234b3baafabd5ec16cedf58210e', '周总', '男', '13800138007', 'zhouzongjingli@oa.com', NULL, 2, '总经理', '在职', 'EMPLOYEE', NULL, '2026-07-21', 0);

-- 3. 班次数据
INSERT INTO `t_shift` (`name`, `work_start`, `work_end`, `lunch_start`, `lunch_end`, `lunch_deduct_minutes`, `late_grace_minutes`, `late_threshold_minutes`, `overtime_threshold_minutes`, `is_default`, `is_custom`, `employee_id`, `description`) VALUES
('早班', '08:00:00', '16:00:00', '12:00:00', '12:30:00', 30, 5, 30, 30, 0, 0, NULL, '早班：08:00-16:00，午休30分钟'),
('标准班', '09:00:00', '18:00:00', '12:00:00', '13:00:00', 60, 5, 30, 30, 1, 0, NULL, '标准班：09:00-18:00，午休60分钟'),
('弹性班', '09:30:00', '18:30:00', '12:00:00', '13:00:00', 60, 15, 60, 30, 0, 0, NULL, '弹性班：09:30-18:30，宽容15分钟'),
('晚班', '13:00:00', '21:00:00', '17:00:00', '18:00:00', 60, 5, 30, 30, 0, 0, NULL, '晚班：13:00-21:00，午休60分钟');

-- 4. 审批数据（当前数据库为空）
-- 暂无审批数据

-- 5. 公告数据
INSERT INTO `t_announcement` (`title`, `content`, `publisher_id`, `publisher_name`, `category`, `priority`, `is_top`, `status`, `version`) VALUES
('年度体检通知', '公司将于8月组织年度体检，请各位同事安排时间。', 1, '系统管理员', '通知', '重要', 0, '已发布', 1),
('新版OA系统上线', '新版OA系统已正式上线，请大家积极使用。', 1, '系统管理员', '通知', '紧急', 0, '已发布', 0),
('信息安全通知', '请各位同事注意信息安全，定期修改密码。', 4, '王五', '制度', '重要', 0, '已发布', 0);

-- 6. 考勤数据
INSERT INTO `t_attendance` (`employee_id`, `employee_name`, `date`, `check_in_time`, `check_out_time`, `status`, `late_minutes`, `early_minutes`, `overtime_before`, `overtime_after`, `work_hours`, `attendance_status`, `remark`, `check_in_latitude`, `check_in_longitude`) VALUES
(1, '系统管理员', '2026-07-24', '2026-07-24 08:19:40', NULL, '迟到', 19, 0, 0.00, 0.00, 0.00, '未处理', NULL, 26.0244593, 119.4080477),
(2, '张三', '2026-07-24', '2026-07-24 08:19:53', NULL, '正常', 0, 0, 0.00, 0.00, 0.00, '未处理', NULL, 26.0244765, 119.4080551);

-- 7. 会议室数据
INSERT INTO `t_meeting_room` (`name`, `capacity`, `equipment`, `location`, `status`) VALUES
('第一会议室', 10, '投影仪、白板', '3楼301', '可用'),
('第二会议室', 20, '投影仪、视频会议', '3楼302', '可用'),
('培训室', 50, '投影仪、音响', '5楼501', '可用'),
('VIP会议室', 15, '投影仪、视频会议、茶水', '6楼601', '可用');

-- 8. 会议预约数据（当前数据库为空）
-- 暂无会议数据

-- 9. 消息数据（当前数据库为空）
-- 暂无消息数据

-- 10. 字典数据
INSERT INTO `t_dict` (`key`, `name`, `options`) VALUES
('leave_type', '请假类型', '[{"label":"事假","value":"事假"},{"label":"病假","value":"病假"},{"label":"年假","value":"年假"},{"label":"调休","value":"调休"},{"label":"婚假","value":"婚假"},{"label":"产假","value":"产假"},{"label":"丧假","value":"丧假"}]'),
('expense_type', '报销类型', '[{"label":"交通费","value":"交通费"},{"label":"住宿费","value":"住宿费"},{"label":"餐饮费","value":"餐饮费"},{"label":"办公费","value":"办公费"},{"label":"差旅费","value":"差旅费"},{"label":"其他","value":"其他"}]'),
('card_type', '补卡类型', '[{"label":"迟到补卡","value":"late"},{"label":"早退补卡","value":"early"},{"label":"漏签补卡","value":"miss"}]'),
('approval_status', '审批状态', '[{"label":"待审批","value":"pending"},{"label":"已通过","value":"approved"},{"label":"已拒绝","value":"rejected"},{"label":"已撤回","value":"withdrawn"}]');

-- 11. 角色数据
INSERT INTO `t_role` (`name`, `code`, `desc`, `icon`, `color`, `permissions`, `user_count`) VALUES
('系统管理员', 'SYSTEM_ADMIN', '系统管理员，拥有所有权限', 'admin', '#ff6b6b', '["user:manage","department:manage","approval:all","announcement:manage","meeting:manage","attendance:manage"]', 0),
('部门主管', 'DEPARTMENT_MANAGER', '部门主管，管理本部门事务', 'manager', '#4ecdc4', 'true', 0),
('普通员工', 'EMPLOYEE', '普通员工，基本操作权限', 'user', '#45b7d1', 'false', 0);

-- 12. 系统配置数据
INSERT INTO `t_system_config` (`config_key`, `config_value`, `config_type`, `description`, `is_public`) VALUES
('REDIS_ENABLED', 'true', 'BOOLEAN', 'Redis缓存开关', 1),
('ES_ENABLED', 'true', 'BOOLEAN', 'Elasticsearch开关', 1),
('CACHE_EXPIRE_TIME', '3600', 'NUMBER', '缓存过期时间（秒）', 0),
('ES_SEARCH_PAGE_SIZE', '10', 'NUMBER', 'ES搜索默认分页大小', 1),
('TOKEN_EXPIRE_TIME', '86400', 'NUMBER', 'Token过期时间（秒）', 0),
('MAX_PAGE_SIZE', '100', 'NUMBER', '分页最大条数', 1),
('DEFAULT_PAGE_SIZE', '10', 'NUMBER', '分页默认条数', 1);

-- 13. 审批角色数据 —— Flowable工作流改造新增（依赖上面已插入的孙八id=12、周总id=13）
INSERT INTO `t_approval_role` (`role_code`, `role_name`, `resolve_strategy`, `fixed_department_id`, `fixed_employee_id`) VALUES
('DEPT_MANAGER', '本部门经理', 'OWN_DEPT_MANAGER', NULL, NULL),
('FINANCE_STAFF', '财务专员', 'FIXED_EMPLOYEE', NULL, 12),      -- 12 = 财务专员"孙八"
('FINANCE_MANAGER', '财务经理', 'FIXED_DEPT_MANAGER', 5, NULL), -- 5 = 财务部id，动态解析到部门负责人
('GM', '总经理', 'FIXED_EMPLOYEE', NULL, 13);                   -- 13 = 总经理"周总"

-- 14. 审批规则数据 —— 审批流程规则配置，区间为左开右闭，NULL表示无边界
INSERT INTO `t_approval_rule`
(`approval_type`,`condition_field`,`min_value`,`max_value`,`required_roles`,`sign_type`,`notify_superior_on_reject`,`notify_admin_on_reject`,`priority`,`status`)
VALUES
('leave','DAYS',NULL,3,'DEPT_MANAGER','SINGLE',0,0,10,1),
('leave','DAYS',3,NULL,'DEPT_MANAGER,GM','SERIAL_AFTER_PARALLEL',0,0,20,1),
('business','DAYS',NULL,5,'DEPT_MANAGER','SINGLE',0,0,10,1),
('business','DAYS',5,NULL,'DEPT_MANAGER,GM','SERIAL_AFTER_PARALLEL',0,0,20,1),
('reimburse','AMOUNT',NULL,3000,'DEPT_MANAGER','SINGLE',0,0,10,1),
('reimburse','AMOUNT',3000,10000,'DEPT_MANAGER,FINANCE_STAFF','PARALLEL',0,0,20,1),
('reimburse','AMOUNT',10000,20000,'DEPT_MANAGER,FINANCE_MANAGER','PARALLEL',0,0,30,1),
('reimburse','AMOUNT',20000,NULL,'DEPT_MANAGER,FINANCE_MANAGER,GM','SERIAL_AFTER_PARALLEL',0,0,40,1),
('purchase','AMOUNT',NULL,2000,'DEPT_MANAGER','SINGLE',0,0,10,1),
('purchase','AMOUNT',2000,10000,'DEPT_MANAGER,FINANCE_STAFF','PARALLEL',0,0,20,1),
('purchase','AMOUNT',10000,NULL,'DEPT_MANAGER,FINANCE_MANAGER,GM','SERIAL_AFTER_PARALLEL',0,0,30,1),
('overtime','NONE',NULL,NULL,'DEPT_MANAGER','SINGLE',0,0,10,1),
('card','NONE',NULL,NULL,'DEPT_MANAGER','SINGLE',0,0,10,1);

-- ==========================================
-- 创建视图
-- ==========================================
DROP VIEW IF EXISTS `v_employee_detail`;
CREATE VIEW `v_employee_detail` AS 
SELECT 
  `e`.`id` AS `id`,
  `e`.`username` AS `username`,
  `e`.`name` AS `name`,
  `e`.`gender` AS `gender`,
  `e`.`phone` AS `phone`,
  `e`.`email` AS `email`,
  `e`.`department_id` AS `department_id`,
  `d`.`name` AS `department_name`,
  `e`.`position` AS `position`,
  `e`.`status` AS `status`,
  `e`.`role` AS `role`,
  `e`.`avatar` AS `avatar`,
  `e`.`create_time` AS `create_time`,
  `e`.`update_time` AS `update_time` 
FROM (`t_employee` `e` LEFT JOIN `t_department` `d` ON ((`e`.`department_id` = `d`.`id`)));

DROP VIEW IF EXISTS `v_department_detail`;
CREATE VIEW `v_department_detail` AS 
SELECT 
  `d`.`id` AS `id`,
  `d`.`name` AS `name`,
  `d`.`manager_id` AS `manager_id`,
  `e`.`name` AS `manager_name`,
  `d`.`manager_phone` AS `manager_phone`,
  `d`.`parent_id` AS `parent_id`,
  `d`.`level` AS `level`,
  `d`.`path` AS `path`,
  `d`.`description` AS `description`,
  `d`.`create_time` AS `create_time`,
  `d`.`update_time` AS `update_time`
FROM (`t_department` `d` LEFT JOIN `t_employee` `e` ON ((`d`.`manager_id` = `e`.`id`)));

DROP VIEW IF EXISTS `v_meeting_detail`;
CREATE VIEW `v_meeting_detail` AS 
SELECT 
  `m`.`id` AS `id`,
  `m`.`title` AS `title`,
  `m`.`room_id` AS `room_id`,
  `m`.`room_name` AS `room_name`,
  `m`.`organizer_id` AS `organizer_id`,
  `m`.`organizer_name` AS `organizer_name`,
  `m`.`participants` AS `participants`,
  `m`.`participant_ids` AS `participant_ids`,
  `m`.`start_time` AS `start_time`,
  `m`.`end_time` AS `end_time`,
  `m`.`status` AS `status`,
  `m`.`remark` AS `remark`,
  `m`.`create_time` AS `create_time`,
  `m`.`update_time` AS `update_time`,
  `r`.`location` AS `room_location`
FROM (`t_meeting` `m` LEFT JOIN `t_meeting_room` `r` ON ((`m`.`room_id` = `r`.`id`)));

DROP VIEW IF EXISTS `v_approval_statistics`;
CREATE VIEW `v_approval_statistics` AS 
SELECT 
  `a`.`approval_type` AS `approval_type`,
  `a`.`status` AS `status`,
  COUNT(`a`.`id`) AS `count`,
  `d`.`name` AS `department_name`
FROM (`t_approval` `a` LEFT JOIN `t_department` `d` ON ((`a`.`applicant_department_id` = `d`.`id`)))
GROUP BY `a`.`approval_type`, `a`.`status`, `d`.`name`;

DROP VIEW IF EXISTS `v_system_monitor`;
CREATE VIEW `v_system_monitor` AS 
SELECT 
  (SELECT COUNT(*) FROM `t_employee`) AS `employee_count`,
  (SELECT COUNT(*) FROM `t_department`) AS `department_count`,
  (SELECT COUNT(*) FROM `t_approval`) AS `approval_count`,
  (SELECT COUNT(*) FROM `t_announcement`) AS `announcement_count`,
  (SELECT COUNT(*) FROM `t_meeting`) AS `meeting_count`,
  (SELECT COUNT(*) FROM `t_attendance`) AS `attendance_count`,
  (SELECT COUNT(*) FROM `t_message` WHERE `is_read` = 0) AS `unread_message_count`,
  (SELECT COUNT(*) FROM `t_approval` WHERE `status` = 'pending') AS `pending_approval_count`;

SET FOREIGN_KEY_CHECKS = 1;

SELECT 'OA管理系统数据库初始化完成！' AS '消息';
SELECT '管理员账号：admin / 123456' AS '登录信息';
SELECT '部门主管：zhangsan / 123456（技术部）' AS '登录信息';
SELECT '部门主管：lisi / 123456（产品部）' AS '登录信息';
SELECT '普通员工：emp001 / 123456' AS '登录信息';
SELECT '财务专员：sunba / 123456（孙八，Flowable审批角色新增）' AS '登录信息';
SELECT '总经理：zhouzongjingli / 123456（周总，Flowable审批角色新增）' AS '登录信息';
