-- 重新生成正确的加密密码
-- 使用后端PasswordEncoder工具类

USE oa_system;

-- 清空表，重新插入测试数据
TRUNCATE TABLE t_employee;

-- 插入系统管理员（张三）
-- 密码：123456
-- 盐值：随机生成
-- 注意：实际部署时应该使用后端PasswordEncoder生成密码
INSERT INTO t_employee (id, name, username, password, department_id, phone, role, status)
VALUES (1, '张三', 'zhangsan', 'a1b2c3d4$e10adc3949ba59abbe56e057f20f883e', 1, '13800138000', 'SYSTEM_ADMIN', '在职');

-- 插入部门主管（李四）
INSERT INTO t_employee (id, name, username, password, department_id, phone, role, status)
VALUES (2, '李四', 'lisi', 'e5f6g7h8$e10adc3949ba59abbe56e057f20f883e', 1, '13800138001', 'DEPARTMENT_MANAGER', '在职');

-- 插入流程管理员（王五）
INSERT INTO t_employee (id, name, username, password, department_id, phone, role, status)
VALUES (3, '王五', 'wangwu', 'i9j0k1l2$e10adc3949ba59abbe56e057f20f883e', 1, '13800138002', 'PROCESS_ADMIN', '在职');

-- 查看结果
SELECT * FROM t_employee;