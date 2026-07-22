# OA管理系统 - 审批工作流与消息通知模块 API 文档

## 概述

本文档描述消息通知模块和审批流程事件相关的 API 接口。

### 架构设计

采用事件驱动的解耦设计：
- **审批模块**：只负责审批业务逻辑，在状态变化时发布事件
- **消息模块**：通过 `@EventListener` 订阅审批事件，自动生成消息通知
- **事件总线**：使用 Spring `ApplicationEventPublisher` + `@EventListener` 实现进程内事件通知

---

## 消息中心接口

### 1. 分页查询消息列表

**请求方法**: `GET`  
**请求路径**: `/message/list`  
**权限要求**: 登录用户

#### 请求参数

| 参数名 | 类型 | 必需 | 说明 | 默认值 |
|--------|------|------|------|--------|
| category | String | 否 | 消息分类：`all`=全部, `todo`=待办(is_todo=1且is_read=0), `cc`=抄送, `system`=系统通知 | all |
| page | Integer | 否 | 页码（从1开始） | 1 |
| size | Integer | 否 | 每页条数 | 10 |

#### 响应示例

**成功场景** (HTTP 200)
```json
{
  "code": 0,
  "data": {
    "records": [
      {
        "id": 1,
        "senderId": 1,
        "senderName": "张三",
        "receiverId": 2,
        "receiverName": "李四",
        "title": "新的待审批单据",
        "content": "您有一条新的请假申请待审批。标题：2026年7月请假",
        "eventType": "SUBMITTED",
        "bizType": "approval",
        "bizId": 101,
        "isTodo": 1,
        "isRead": 0,
        "jumpUrl": "/approval/detail/101",
        "createTime": "2026-07-21 14:30:00"
      }
    ],
    "total": 15,
    "current": 1,
    "size": 10
  },
  "message": "success"
}
```

**失败场景** (未登录)
```json
{
  "code": -1,
  "data": null,
  "message": "未登录"
}
```

---

### 2. 获取待办数量

**请求方法**: `GET`  
**请求路径**: `/message/todoCount`  
**权限要求**: 登录用户  
**用途**: 首页/底部导航角标显示

#### 响应示例

**成功场景** (HTTP 200)
```json
{
  "code": 0,
  "data": {
    "count": 5
  },
  "message": "success"
}
```

---

### 3. 标记单条消息已读

**请求方法**: `PUT`  
**请求路径**: `/message/read/{messageId}`  
**权限要求**: 登录用户（只能标记自己的消息）

#### 请求参数

| 参数名 | 位置 | 说明 |
|--------|------|------|
| messageId | Path | 消息ID |

#### 响应示例

**成功场景** (HTTP 200) - 返回jump_url供前端跳转
```json
{
  "code": 0,
  "data": {
    "jumpUrl": "/approval/detail/101"
  },
  "message": "success"
}
```

**越权场景** (HTTP 200)
```json
{
  "code": -1,
  "data": null,
  "message": "消息不存在或无权访问"
}
```

---

### 4. 批量标记消息已读

**请求方法**: `PUT`  
**请求路径**: `/message/readAll`  
**权限要求**: 登录用户

#### 请求参数

| 参数名 | 类型 | 必需 | 说明 | 默认值 |
|--------|------|------|------|--------|
| category | String | 否 | 消息分类：`all`/`todo`/`cc`/`system` | all |

#### 响应示例

**成功场景** (HTTP 200)
```json
{
  "code": 0,
  "data": {
    "affected": 3
  },
  "message": "success"
}
```

---

## 审批相关接口（涉及事件发布）

### 审批事件类型与消息流向

| 事件 | 触发时机 | 通知接收人 | 是否待办 | 消息文案示例 |
|------|----------|-----------|---------|------------|
| SUBMITTED | 申请人提交单据 | 第一级审批人（部门主管） | ✅ 是 | "您有一条新的请假申请待审批" |
| APPROVED_FINAL | 末级节点同意，流程结束 | 申请人 | ❌ 否 | "您的请假申请已审批通过" |
| REJECTED | 任一节点拒绝 | 申请人 | ❌ 否 | "您的请假申请已被拒绝，原因：理由说明" |
| WITHDRAWN | 申请人撤回 | 当前审批人 | ❌ 失效 | "申请人已撤回了请假申请" |

---

## 完整的审批流程示例

### 场景：提交 → 通过 → 撤回 流程

#### 步骤1：申请人提交申请

**请求**
```bash
curl -X POST http://localhost:8080/approval/submit \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer {token}" \
  -d '{
    "approvalType": "leave",
    "title": "2026年7月请假",
    "content": "7月21-25日请假",
    "startTime": "2026-07-21T09:00:00",
    "endTime": "2026-07-25T17:00:00",
    "leaveType": "annual"
  }'
```

**响应** (申请ID: 101)
```json
{
  "code": 0,
  "data": 101,
  "message": "success"
}
```

**此时数据库变化**:
- `t_approval` 新增记录：status='待审批', applicantId=当前用户
- `t_message` 新增消息给第一级审批人：eventType='SUBMITTED', isTodo=1, isRead=0

#### 步骤2：审批人查看待办

**请求**
```bash
curl -X GET "http://localhost:8080/message/list?category=todo&page=1&size=10" \
  -H "Authorization: Bearer {approver_token}"
```

**响应** (审批人看到刚才提交的消息)
```json
{
  "code": 0,
  "data": {
    "records": [
      {
        "id": 1,
        "eventType": "SUBMITTED",
        "bizId": 101,
        "title": "新的待审批单据",
        "content": "您有一条新的请假申请待审批。标题：2026年7月请假",
        "isTodo": 1,
        "isRead": 0,
        "jumpUrl": "/approval/detail/101"
      }
    ],
    "total": 1,
    "current": 1,
    "size": 10
  }
}
```

#### 步骤3：审批人点击待办消息跳转

**请求**  (先标记消息已读)
```bash
curl -X PUT http://localhost:8080/message/read/1 \
  -H "Authorization: Bearer {approver_token}"
```

**响应** (获得跳转链接)
```json
{
  "code": 0,
  "data": {
    "jumpUrl": "/approval/detail/101"
  }
}
```

#### 步骤4：审批人在详情页面通过申请

**请求**
```bash
curl -X PUT http://localhost:8080/approval/approve \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer {approver_token}" \
  -d '{
    "id": 101,
    "status": "已通过",
    "approveReason": ""
  }'
```

**此时数据库变化**:
- `t_approval` 更新：status='已通过', approverId=审批人ID, approveTime=当前时间
- `t_message` 新增消息给申请人：eventType='APPROVED_FINAL', isTodo=0, isRead=0

#### 步骤5：申请人收到通过消息

**请求**
```bash
curl -X GET "http://localhost:8080/message/list?category=all&page=1&size=10" \
  -H "Authorization: Bearer {applicant_token}"
```

**响应** (申请人看到审批通过的消息)
```json
{
  "code": 0,
  "data": {
    "records": [
      {
        "id": 2,
        "eventType": "APPROVED_FINAL",
        "bizId": 101,
        "title": "申请已通过",
        "content": "您的请假申请已由王五审批通过。",
        "isTodo": 0,
        "isRead": 0,
        "jumpUrl": "/approval/detail/101"
      }
    ],
    "total": 1,
    "current": 1,
    "size": 10
  }
}
```

#### 步骤6：拒绝场景示例

**请求**
```bash
curl -X PUT http://localhost:8080/approval/approve \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer {approver_token}" \
  -d '{
    "id": 102,
    "status": "已拒绝",
    "approveReason": "请假时间过长，不符合公司政策"
  }'
```

**此时生成的消息**
```json
{
  "eventType": "REJECTED",
  "title": "申请已拒绝",
  "content": "您的请假申请已由王五拒绝，理由：请假时间过长，不符合公司政策。",
  "isTodo": 0
}
```

---

## 数据库自检 SQL

### 查询消息表中的所有审批相关事件
```sql
SELECT 
  id, 
  receiver_id, 
  event_type, 
  title, 
  content, 
  is_todo, 
  is_read, 
  create_time
FROM t_message 
WHERE event_type IN ('SUBMITTED', 'APPROVED_FINAL', 'REJECTED', 'WITHDRAWN')
ORDER BY create_time DESC;
```

### 查询某用户的待办消息数
```sql
SELECT COUNT(*) as todo_count 
FROM t_message 
WHERE receiver_id = {userId} 
  AND is_todo = 1 
  AND is_read = 0;
```

### 查询某个审批单据的所有相关消息
```sql
SELECT 
  id, 
  sender_name, 
  receiver_name, 
  event_type, 
  title, 
  is_todo, 
  create_time
FROM t_message 
WHERE biz_id = {approvalId} 
  AND event_type IN ('SUBMITTED', 'APPROVED_FINAL', 'REJECTED', 'WITHDRAWN')
ORDER BY create_time;
```

### 验证消息表的索引
```sql
SHOW INDEXES FROM t_message 
WHERE Key_name IN ('idx_receiver_is_todo', 'idx_receiver_is_read', 'idx_receiver_biz_type', 'idx_event_create_time', 'idx_biz_id');
```

---

## 关键实现说明

### 1. 事件驱动架构
- 位置：`com.southwind.event.ApprovalEvent`
- 事件类型枚举：`ApprovalEvent.EventType` 定义了所有支持的事件类型
- Builder 模式支持灵活构造事件

### 2. 事件监听器
- 位置：`com.southwind.event.listener.ApprovalEventListener`
- 使用 `@EventListener` 注解自动监听 `ApprovalEvent`
- 根据事件类型生成对应的消息通知
- 异常处理：事件监听器异常不影响主业务流程

### 3. 消息字段说明
- `event_type`：事件类型，用于消息分类和统计
- `biz_type`：业务类型（暂支持'approval'，为将来接入其他模块预留）
- `biz_id`：业务单据ID（如审批ID），用于关联查询
- `is_todo`：是否计入待办，1=是/0=否
- `is_read`：消息是否已读，1=已读/0=未读
- `jump_url`：消息点击后的跳转链接，由前端拼接路由

### 4. 越权防护
- 所有消息接口都验证 `receiver_id == currentUser.userId`
- 确保用户只能查看/修改自己的消息
- SQL 查询中包含 `receiver_id` 条件

---

## 注意事项

1. **未来工作流改进**：
   - 当前 `SUBMITTED` 事件固定发给系统管理员（ID=1）
   - 待工作流引擎完成后，应改为发送给实际的第一级审批人
   - `APPROVED_NODE` 和 `APPROVED_FINAL` 的区分需要工作流引擎判断

2. **消息撤回逻辑**：
   - `WITHDRAWN` 事件发送后，应将对应审批人的待办消息的 `is_todo` 改为 0
   - 当前版本仅发送提示消息，消息失效逻辑待优化

3. **扩展性**：
   - 消息模块可轻松接入其他业务模块（工作流、公文等）
   - 只需发布对应事件，监听器会自动生成消息

---

## 测试清单

- [ ] 提交申请 → SUBMITTED 事件 → 消息生成
- [ ] 审批人查看待办列表
- [ ] 标记消息已读 → 获取 jump_url
- [ ] 通过申请 → APPROVED_FINAL 事件 → 消息生成
- [ ] 拒绝申请 → REJECTED 事件 → 消息生成
- [ ] 撤回申请 → WITHDRAWN 事件 → 消息生成
- [ ] 批量标记已读功能
- [ ] 越权访问防护验证
- [ ] 数据库索引查询性能验证

