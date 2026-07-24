CREATE TABLE IF NOT EXISTS t_role (
    id INT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(50) NOT NULL,
    code VARCHAR(50) NOT NULL UNIQUE,
    `desc` VARCHAR(255),
    icon VARCHAR(100),
    color VARCHAR(50),
    permissions TEXT,
    user_count INT DEFAULT 0,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS t_dict (
    id INT PRIMARY KEY AUTO_INCREMENT,
    `key` VARCHAR(50) NOT NULL UNIQUE,
    name VARCHAR(100) NOT NULL,
    options TEXT,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS t_operation_log (
    id INT PRIMARY KEY AUTO_INCREMENT,
    user_id INT,
    user VARCHAR(50),
    action VARCHAR(100),
    `desc` VARCHAR(255),
    ip_address VARCHAR(50),
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP
);

INSERT INTO t_role (name, code, `desc`, icon, color) VALUES ('系统管理员', 'SYSTEM_ADMIN', '系统管理员', 'icon-setting', '#FF5722');
INSERT INTO t_role (name, code, `desc`, icon, color) VALUES ('部门主管', 'DEPARTMENT_HEAD', '部门主管', 'icon-user', '#1890FF');
INSERT INTO t_role (name, code, `desc`, icon, color) VALUES ('普通员工', 'EMPLOYEE', '普通员工', 'icon-user-o', '#52C41A');

INSERT INTO t_dict (`key`, name, options) VALUES ('leave_type', '请假类型', '[{\"label\":\"事假\",\"value\":\"personal\"},{\"label\":\"病假\",\"value\":\"sick\"},{\"label\":\"年假\",\"value\":\"annual\"},{\"label\":\"加班调休\",\"value\":\"compensatory\"}]');
INSERT INTO t_dict (`key`, name, options) VALUES ('approval_status', '审批状态', '[{\"label\":\"待审批\",\"value\":\"pending\"},{\"label\":\"已通过\",\"value\":\"approved\"},{\"label\":\"已拒绝\",\"value\":\"rejected\"}]');

CREATE TABLE IF NOT EXISTS t_message (
    id INT PRIMARY KEY AUTO_INCREMENT,
    sender_id INT COMMENT '发送人ID',
    sender_name VARCHAR(50) COMMENT '发送人姓名',
    receiver_id INT NOT NULL COMMENT '接收人ID',
    receiver_name VARCHAR(50) COMMENT '接收人姓名',
    title VARCHAR(100) NOT NULL COMMENT '消息标题',
    content TEXT COMMENT '消息内容',
    msg_type VARCHAR(20) DEFAULT 'SYSTEM' COMMENT '消息类型(SYSTEM/APPROVAL/ATTENDANCE/MEETING/ANNOUNCEMENT/OTHER)',
    is_read INT DEFAULT 0 COMMENT '是否已读(0未读/1已读)',
    is_top INT DEFAULT 0 COMMENT '是否置顶(0否/1是)',
    status VARCHAR(20) DEFAULT '正常' COMMENT '状态(正常/已删除)',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    read_time DATETIME COMMENT '阅读时间',
    INDEX idx_receiver_id (receiver_id),
    INDEX idx_is_read (is_read),
    INDEX idx_create_time (create_time)
);
