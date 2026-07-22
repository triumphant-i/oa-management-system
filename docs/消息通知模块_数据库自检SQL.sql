-- ========================================
-- 消息通知模块 - 数据库自检SQL
-- ========================================

-- 【检查清单1】消息表结构验证
-- 用途：确认所有新增字段已正确添加到t_message表

SELECT 
  COLUMN_NAME,
  COLUMN_TYPE,
  IS_NULLABLE,
  COLUMN_DEFAULT,
  COLUMN_COMMENT
FROM INFORMATION_SCHEMA.COLUMNS
WHERE TABLE_SCHEMA = 'oa_system' 
  AND TABLE_NAME = 't_message'
  AND COLUMN_NAME IN ('event_type', 'biz_type', 'biz_id', 'is_todo', 'jump_url')
ORDER BY COLUMN_NAME;

-- 预期结果：5行（event_type, biz_type, biz_id, is_todo, jump_url）都应该存在

---

-- 【检查清单2】消息表索引验证
-- 用途：确认所有性能索引已正确创建

SHOW INDEXES FROM t_message 
WHERE Key_name IN ('idx_receiver_is_todo', 'idx_receiver_is_read', 'idx_receiver_biz_type', 'idx_event_create_time', 'idx_biz_id');

-- 预期结果：5行索引都应该存在

---

-- 【检查清单3】查询某个审批的所有相关消息
-- 用途：验证从提交→通过/拒绝→撤回的完整流程中是否生成了正确的消息
-- 使用示例：将 {approval_id} 替换为实际的审批ID

SELECT 
  m.id,
  m.sender_name,
  m.receiver_name,
  m.event_type,
  m.is_todo,
  m.is_read,
  m.biz_id,
  m.jump_url,
  m.create_time,
  SUBSTRING(m.content, 1, 60) as content_summary
FROM t_message m
WHERE m.biz_id = {approval_id}
  AND m.event_type IN ('SUBMITTED', 'APPROVED_FINAL', 'REJECTED', 'WITHDRAWN', 'APPROVED_NODE')
ORDER BY m.create_time;

-- 预期结果：
-- - SUBMITTED: 提交时1条，is_todo=1
-- - APPROVED_FINAL: 通过时1条，is_todo=0
-- - REJECTED: 拒绝时1条，is_todo=0（如果是拒绝流程）
-- - WITHDRAWN: 撤回时1条，is_todo=0（如果是撤回流程）

---

-- 【检查清单4】统计用户的待办消息数
-- 用途：验证/message/todoCount接口应返回的数值
-- 使用示例：将 {user_id} 替换为实际的用户ID（如审批人ID）

SELECT 
  COUNT(*) as todo_count,
  COUNT(CASE WHEN is_read = 0 THEN 1 END) as unread_count,
  COUNT(CASE WHEN is_read = 1 THEN 1 END) as read_count
FROM t_message 
WHERE receiver_id = {user_id}
  AND is_todo = 1;

-- 预期结果：
-- - todo_count：待办总数
-- - unread_count：未读待办数（通常应该等于/message/todoCount的返回值）
-- - read_count：已读待办数

---

-- 【检查清单5】验证消息分类统计
-- 用途：确认各分类消息数量，用于验证/message/list的category参数效果

SELECT 
  '全部' as category, COUNT(*) as count FROM t_message WHERE receiver_id = {user_id}
UNION ALL
SELECT 
  '待办' as category, COUNT(*) FROM t_message 
  WHERE receiver_id = {user_id} AND is_todo = 1 AND is_read = 0
UNION ALL
SELECT 
  '抄送' as category, COUNT(*) FROM t_message 
  WHERE receiver_id = {user_id} AND biz_type != 'approval'
UNION ALL
SELECT 
  '系统通知' as category, COUNT(*) FROM t_message 
  WHERE receiver_id = {user_id} AND msg_type != 'APPROVAL';

-- 预期结果：
-- - 全部：所有消息总数
-- - 待办：is_todo=1且is_read=0的消息数
-- - 抄送：非审批类型的消息数
-- - 系统通知：非APPROVAL类型的消息数

---

-- 【检查清单6】验证越权防护
-- 用途：确认用户只能查看自己的消息

-- 场景：用户A(ID=2)试图查看用户B(ID=3)的消息时，应该失败
-- SQL检查：验证是否存在receiver_id不等于当前用户的消息被访问

SELECT 
  id,
  receiver_id,
  sender_name,
  SUBSTRING(content, 1, 40) as content_summary,
  create_time
FROM t_message 
WHERE receiver_id = {other_user_id}
LIMIT 5;

-- 预期结果：这些消息应该无法被其他用户查看或修改

---

-- 【检查清单7】验证消息跳转链接
-- 用途：确认jump_url字段已正确填充，用于前端路由跳转

SELECT 
  id,
  event_type,
  biz_id,
  jump_url,
  CASE 
    WHEN jump_url IS NULL THEN '❌ 缺失'
    WHEN jump_url LIKE '/approval/detail/%' THEN '✅ 正确'
    ELSE '⚠️ 格式异常'
  END as jump_url_status
FROM t_message 
WHERE event_type IS NOT NULL
LIMIT 20;

-- 预期结果：所有event_type非空的消息都应该有合法的jump_url

---

-- 【检查清单8】验证事件类型分布
-- 用途：统计各事件类型的消息数量，用于验证事件发布是否正常工作

SELECT 
  event_type,
  COUNT(*) as message_count,
  COUNT(DISTINCT biz_id) as approval_count,
  COUNT(CASE WHEN is_todo = 1 THEN 1 END) as todo_count,
  COUNT(CASE WHEN is_read = 0 THEN 1 END) as unread_count
FROM t_message 
WHERE event_type IS NOT NULL
GROUP BY event_type
ORDER BY message_count DESC;

-- 预期结果：
-- - SUBMITTED: 提交事件，is_todo应该都为1
-- - APPROVED_FINAL: 通过事件，is_todo应该都为0
-- - REJECTED: 拒绝事件，is_todo应该都为0
-- - WITHDRAWN: 撤回事件，is_todo应该都为0

---

-- 【检查清单9】验证消息时间序列
-- 用途：确认消息创建时间的正确性，用于调试事件发布顺序

SELECT 
  biz_id,
  GROUP_CONCAT(
    CONCAT_WS('|', event_type, DATE_FORMAT(create_time, '%H:%i:%S'))
    ORDER BY create_time
    SEPARATOR ' → '
  ) as event_sequence
FROM t_message 
WHERE event_type IS NOT NULL
GROUP BY biz_id
HAVING biz_id IN (
  SELECT DISTINCT biz_id FROM t_message 
  WHERE event_type IN ('SUBMITTED', 'APPROVED_FINAL', 'REJECTED')
)
LIMIT 10;

-- 预期结果：
-- 正常流程: SUBMITTED → APPROVED_FINAL
-- 拒绝流程: SUBMITTED → REJECTED
-- 撤回流程: SUBMITTED → WITHDRAWN

---

-- 【检查清单10】查询特定用户的消息分页数据
-- 用途：验证/message/list接口应返回的实际数据
-- 使用示例：将 {user_id} 替换为实际的用户ID

SELECT 
  id,
  sender_name,
  receiver_name,
  title,
  event_type,
  is_todo,
  is_read,
  create_time,
  jump_url
FROM t_message 
WHERE receiver_id = {user_id}
ORDER BY create_time DESC
LIMIT 10;

-- 预期结果：
-- - 返回最新的10条消息
-- - 所有receiver_id都等于查询的user_id
-- - 字段齐全，可直接用于前端展示

---

-- 【检查清单11】索引查询性能验证
-- 用途：确认索引是否有效提升查询性能

-- 查询1：待办消息查询（应使用idx_receiver_is_todo索引）
EXPLAIN 
SELECT * FROM t_message 
WHERE receiver_id = {user_id} 
  AND is_todo = 1 
  AND is_read = 0 
ORDER BY create_time DESC;

-- 查询2：消息分类查询（应使用idx_receiver_biz_type索引）
EXPLAIN 
SELECT * FROM t_message 
WHERE receiver_id = {user_id} 
  AND biz_type = 'approval' 
ORDER BY create_time DESC;

-- 预期结果：
-- - type = 'ref' 或 'range'（表示使用了索引）
-- - rows 远小于表的总行数
-- - Extra 不包含 'Using filesort'（如果有ORDER BY）

---

-- 【检查清单12】批量操作验证
-- 用途：验证/message/readAll接口的批量更新效果

-- 在执行批量标记已读前的状态
SELECT 
  COUNT(*) as total_unread,
  COUNT(CASE WHEN is_todo = 1 THEN 1 END) as unread_todo
FROM t_message 
WHERE receiver_id = {user_id} 
  AND is_read = 0;

-- 执行批量标记已读（模拟API调用）
UPDATE t_message 
SET is_read = 1, read_time = NOW()
WHERE receiver_id = {user_id} 
  AND is_read = 0
  AND is_todo = 1;

-- 查看执行后的状态（应该是0）
SELECT COUNT(*) as unread_after FROM t_message 
WHERE receiver_id = {user_id} 
  AND is_read = 0 
  AND is_todo = 1;

-- 预期结果：
-- - 更新前：unread_todo = N
-- - 更新后：unread_after = 0

---

-- 【检查清单13】消息撤回场景验证
-- 用途：检查WITHDRAWN事件后的消息状态变化

-- 查询某个审批的撤回消息
SELECT 
  id,
  event_type,
  receiver_id,
  is_todo,
  is_read,
  content
FROM t_message 
WHERE biz_id = {withdrawn_approval_id}
  AND event_type = 'WITHDRAWN';

-- 该消息应该通知被撤回前的审批人
-- receiver_id 应该是前一级的审批人ID
-- is_todo 应该是 0（不计入待办）

-- 预期结果：
-- - 存在1条WITHDRAWN消息
-- - is_todo = 0, is_read = 0（初始未读）
-- - content 包含"已被撤回"

---

-- 【检查清单14】消息表容量监控
-- 用途：监控消息表增长情况，用于容量规划

SELECT 
  table_name,
  ROUND(((data_length + index_length) / 1024 / 1024), 2) AS size_mb,
  table_rows,
  ROUND((data_length / 1024 / 1024), 2) AS data_mb,
  ROUND((index_length / 1024 / 1024), 2) AS index_mb
FROM information_schema.tables 
WHERE table_schema = 'oa_system' 
  AND table_name = 't_message';

-- 预期结果：监控表大小和行数，确保在预期范围内

---

-- 【检查清单15】事件监听器异常恢复
-- 用途：验证事件处理异常不会影响业务主流程

-- 检查是否有failed消息（事件监听器应该自动处理异常）
SELECT 
  id,
  status,
  COUNT(*) as failure_count
FROM t_message 
WHERE status IN ('error', 'failed')
GROUP BY status;

-- 预期结果：应该为空（事件监听器捕获了异常）

