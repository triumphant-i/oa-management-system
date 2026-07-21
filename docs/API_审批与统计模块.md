# API 文档（审批与统计模块）

## 2026-07-21 修复：审批模块鉴权与状态流转

### 1. 提交申请
- **方法/路径**：`POST /approval/submit`
- **权限**：已登录用户
- **请求参数**：无
- **请求体示例**：
```json
{
  "approvalType": "leave",
  "title": "年假申请",
  "content": "家中有事",
  "startTime": "2026-07-22T09:00:00",
  "endTime": "2026-07-22T18:00:00",
  "applicantId": 999,
  "applicantName": "前端传的会被忽略"
}
```
- **成功返回示例**：
```json
{
  "code": 0,
  "message": "成功",
  "data": 123
}
```
- **失败返回示例**：
```json
{
  "code": -1,
  "message": "审批类型不合法",
  "data": null
}
```

### 2. 我的申请（支持分页/筛选）
- **方法/路径**：`GET /approval/myApplications/{applicantId}`
- **权限**：仅本人可查（`applicantId` 必须等于当前登录人）
- **Query 参数**：
  - `page`（可选，分页页码）
  - `size`（可选，分页大小）
  - `approvalType`（可选，按类型筛选）
  - `status`（可选，按状态筛选，支持“待审批/已通过/已拒绝/已撤回”，也兼容 `pending/approved/rejected/withdrawn`）
- **请求示例**：
```http
GET /approval/myApplications/7?page=1&size=10&approvalType=leave&status=待审批
```
- **成功返回示例（分页）**：
```json
{
  "code": 0,
  "message": "成功",
  "data": {
    "total": 1,
    "records": [
      {
        "id": 1,
        "approvalType": "leave",
        "status": "待审批"
      }
    ],
    "current": 1,
    "size": 10
  }
}
```
- **失败返回示例**：
```json
{
  "code": -1,
  "message": "无权查看他人申请",
  "data": null
}
```

### 3. 待审批列表
- **方法/路径**：`GET /approval/pendingList`
- **权限**：
  - `SYSTEM_ADMIN/PROCESS_ADMIN`：可看全部待审批
  - `DEPARTMENT_MANAGER`：仅看本部门待审批
  - `EMPLOYEE`：返回空列表（不报错）
- **请求参数**：兼容保留 `approverId`、`role`，后端不再信任其值
- **成功返回示例（员工角色）**：
```json
{
  "code": 0,
  "message": "成功",
  "data": []
}
```

### 4. 待审批数量
- **方法/路径**：`GET /approval/pendingCount`
- **权限**：同“待审批列表”
- **成功返回示例（员工角色）**：
```json
{
  "code": 0,
  "message": "成功",
  "data": 0
}
```

### 5. 审批处理（同意/拒绝）
- **方法/路径**：`PUT /approval/approve`
- **权限**：
  - `SYSTEM_ADMIN/PROCESS_ADMIN`：可审批全部
  - `DEPARTMENT_MANAGER`：仅可审批本部门申请
  - `EMPLOYEE`：无权限
- **请求体示例**：
```json
{
  "id": 1,
  "status": "已通过",
  "approveReason": "同意"
}
```
- **说明**：
  - 仅“待审批”可处理
  - 审批人信息从当前登录人获取，不信任前端 `approverId/approverName`
  - 使用条件更新：`WHERE id = ? AND status = '待审批'`，防止重复提交覆盖状态
- **成功返回示例**：
```json
{
  "code": 0,
  "message": "成功",
  "data": null
}
```
- **失败返回示例**：
```json
{
  "code": -1,
  "message": "申请状态已变更，请刷新后重试",
  "data": null
}
```

### 6. 撤回申请
- **方法/路径**：`PUT /approval/withdraw/{id}`
- **权限**：仅申请人本人，且仅“待审批”可撤回
- **说明**：使用条件更新，避免并发下误撤回
- **成功返回示例**：
```json
{
  "code": 0,
  "message": "成功",
  "data": null
}
```
- **失败返回示例**：
```json
{
  "code": -1,
  "message": "只能撤回自己的申请",
  "data": null
}
```
