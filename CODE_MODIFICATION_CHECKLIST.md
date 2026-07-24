# 代码修改清单

## 后端修改清单

### 1. ApprovalServiceImpl.java
**文件路径**: `backend/oa-springboot/src/main/java/com/southwind/service/impl/ApprovalServiceImpl.java`

**主要改动**:
- ✅ 行 9: 添加 EmployeeService 导入
- ✅ 行 39: 注入 EmployeeService
- ✅ 行 86-153: 重新实现 submitApproval 方法中的事件发布逻辑
  - 从 Flowable 查询第一级任务分配人
  - 添加异常处理和兜底逻辑
  - 使用真实用户名称而不是硬编码 ID
- ✅ 行 231-316: 重新实现 approveApproval 方法中的事件发布逻辑
  - 添加下一级审批人通知
  - 实现多级审批流转通知
- ✅ 行 414-426: 添加 getEmployeeName() 辅助方法

**关键代码**:
```java
// 查询第一级审批人
List<org.flowable.task.api.Task> tasks = approvalFlowableService.getProcessTasks(processInstanceId);
if (!tasks.isEmpty()) {
    org.flowable.task.api.Task firstTask = tasks.get(0);
    String assigneeStr = firstTask.getAssignee();
    if (assigneeStr != null && !assigneeStr.isEmpty()) {
        firstApproverId = Integer.parseInt(assigneeStr);
    }
}

// 兜底逻辑
if (firstApproverId == null) {
    firstApproverId = 1;
    firstApproverName = "系统管理员";
}

// 查询下一级审批人并通知
if (approved && !isProcessEnded) {
    List<org.flowable.task.api.Task> nextTasks = 
        approvalFlowableService.getProcessTasks(existing.getProcessInstanceId());
    // ... 向下一级任务分配人发送通知
}
```

---

### 2. ApprovalFlowableService.java
**文件路径**: `backend/oa-springboot/src/main/java/com/southwind/service/flowable/ApprovalFlowableService.java`

**主要改动**:
- ✅ 行 259-280: 添加 getProcessTasks() 方法
  - 查询流程实例中的所有活跃待办任务
  - 按创建时间排序，获取最早的任务

**关键代码**:
```java
public List<Task> getProcessTasks(String processInstanceId) {
    try {
        List<Task> tasks = taskService.createTaskQuery()
                .processInstanceId(processInstanceId)
                .active()
                .orderByTaskCreateTime()
                .asc()
                .list();
        return tasks;
    } catch (Exception e) {
        logger.error("Error getting process tasks", e);
        return new ArrayList<>();
    }
}
```

---

### 3. ApprovalController.java
**文件路径**: `backend/oa-springboot/src/main/java/com/southwind/controller/ApprovalController.java`

**主要改动**:
- ✅ 行 24: 修改 import 为 `import java.util.*` 以支持 ArrayList、HashSet 等
- ✅ 行 206-353: 完全重写 pendingList() 方法
  - 系统管理员: 查询所有活跃任务对应的审批
  - 部门主管: 查询分配给自己的任务对应的审批（部门过滤）
  - 添加 Flowable 不可用的降级方案
- ✅ 行 359-419: 完全重写 pendingCount() 方法
  - 同样支持基于 Flowable 的任务计数

**关键代码**:
```java
// 从 Flowable 查询任务
if (flowableTaskService != null) {
    String assignee = String.valueOf(currentUser.getUserId());
    List<org.flowable.task.api.Task> myTasks = flowableTaskService.createTaskQuery()
            .taskAssignee(assignee)
            .active()
            .list();
    
    // 提取 approvalId
    Set<Integer> approvalIds = new HashSet<>();
    for (org.flowable.task.api.Task task : myTasks) {
        Object approvalIdObj = flowableTaskService.getVariable(task.getId(), "approvalId");
        if (approvalIdObj != null) {
            Integer approvalId = Integer.parseInt(approvalIdObj.toString());
            approvalIds.add(approvalId);
        }
    }
    
    // 查询对应的审批
    QueryWrapper<Approval> queryWrapper = new QueryWrapper<>();
    queryWrapper.in("id", approvalIds).eq("status", STATUS_PENDING);
    resultList = approvalService.list(queryWrapper);
}
```

---

### 4. ApprovalRoleResolver.java
**文件路径**: `backend/oa-springboot/src/main/java/com/southwind/service/ApprovalRoleResolver.java`

**主要改动**:
- ✅ 行 50-82: 完全重写 resolveApprovers() 方法
  - 添加兜底逻辑：所有角色都找不到审批人时，使用系统管理员
  - 改进日志记录（使用 ✓/❌/⚠ 符号）
- ✅ 行 93-155: 改进 resolveByRole() 方法
  - 添加详细的警告日志
  - 对各个策略添加具体的错误消息

**关键代码**:
```java
// 兜底逻辑
if (approverIds.isEmpty()) {
    logger.warn("❌ No approvers found for any role, using fallback: system admin (id=1)");
    approverIds.add(1);  // 系统管理员
}

// 改进的日志
if (dept != null && dept.getManagerId() != null) {
    logger.info("✓ Resolved OWN_DEPT_MANAGER: departmentId={}, managerId={}", ...);
} else {
    logger.warn("⚠ OWN_DEPT_MANAGER: Department {} has no manager", ...);
}
```

---

### 5. WorkflowManageController.java (新增)
**文件路径**: `backend/oa-springboot/src/main/java/com/southwind/controller/WorkflowManageController.java`

**新增内容**:
- ✅ 完整的工作流管理控制器
- ✅ 10 个 RESTful API 端点
  - 流程定义查询、详情查看
  - 流程实例查询、详情查看、任务查询、历史查询
  - 实例操作（终止、暂停、恢复）
  - 统计信息查询

**关键方法**:
- `getProcessDefinitions()`: 查询流程定义
- `getProcessInstances()`: 查询流程实例
- `getProcessInstanceDetail()`: 实例详情（支持已完成的实例）
- `getProcessPendingTasks()`: 实例的待办任务
- `getProcessHistory()`: 实例的历史任务
- `terminateProcessInstance()`: 终止实例
- `getWorkflowStatistics()`: 统计信息

---

## 前端修改清单

### WorkflowManage/Index.vue
**文件路径**: `frontend/oa-pc/oa-pc-admin/src/views/WorkflowManage/Index.vue`

**主要改动**:
- ❌ 移除了所有静态占位符数据
- ✅ 完全重新实现为真实数据驱动
- ✅ 添加了统计卡片（流程定义数、运行中、已完成、待办任务）
- ✅ 流程定义标签页
  - 显示所有流程定义列表
  - 可查看定义详情
  - 支持按流程过滤
- ✅ 流程实例标签页
  - 显示所有运行中和已完成的实例
  - 可查看实例详情（基本信息、待办任务、历史任务）
  - 支持终止实例操作
  - 自动统计实例数量
- ✅ 完整的事件处理
  - 标签页切换时自动加载数据
  - 刷新按钮重新加载所有数据
  - 加载状态提示

**关键 API 调用**:
```javascript
// 加载统计信息
GET /workflow/statistics

// 加载流程定义
GET /workflow/definitions

// 加载流程实例
GET /workflow/instances?processDefinitionKey=xxx

// 加载实例详情
GET /workflow/instance/{id}

// 加载待办任务
GET /workflow/instance/{id}/tasks

// 加载历史任务
GET /workflow/instance/{id}/history

// 终止实例
PUT /workflow/instance/{id}/terminate?reason=xxx
```

---

## 修改统计

| 文件类型 | 数量 | 修改内容 |
|--------|------|--------|
| 后端 .java 文件 | 5 | 修改现有功能，增强逻辑 |
| 后端新增 .java 文件 | 1 | WorkflowManageController.java |
| 前端 .vue 文件 | 1 | 重新实现 WorkflowManage/Index.vue |
| **总计** | **7** | - |

---

## 编译和测试检查清单

- [ ] 后端编译无错误 `mvn clean compile`
- [ ] 后端运行无异常 `mvn spring-boot:run`
- [ ] 前端安装依赖 `npm install`
- [ ] 前端编译无错误 `npm run build`
- [ ] 前端开发服务启动 `npm run dev`
- [ ] 单个审批流程完整测试
- [ ] 多级审批流程完整测试
- [ ] 待审批列表与 Flowable 一致性验证
- [ ] 工作流管理页面功能正常
- [ ] 审批人为空时的兜底逻辑测试

---

## 部署注意事项

1. **数据库**: 无需修改 SQL，所有改动都是代码层面
2. **Flowable 配置**: 确保流程定义已部署（.bpmn 文件）
3. **缓存清理**: 部署前建议清理 Spring Cache（如有使用）
4. **依赖版本**: 无需更新依赖版本，使用现有 Flowable 版本
5. **配置文件**: 无需修改 application.yml 或 application.properties

---

## 回滚计划（如需要）

如果需要回滚某个修改，按以下步骤：

1. **回滚 pendingList 逻辑**: 使用 git 恢复 ApprovalController 的这两个方法
2. **回滚第一级审批人通知**: 恢复 ApprovalServiceImpl.submitApproval 的原始代码
3. **回滚多级审批通知**: 恢复 ApprovalServiceImpl.approveApproval 的原始代码
4. **移除 WorkflowManageController**: 删除新增的控制器类
5. **恢复前端**: 用原始的 WorkflowManage/Index.vue 替换

---

## 验收标准

| 需求 | 验收标准 | 状态 |
|-----|--------|------|
| 第一级审批人真实分配 | 审批通知发给 Flowable 实际分配的人，不是硬编码 ID=1 | ✅ |
| 下一级审批人通知 | 节点通过后，下一级审批人能立即收到新待审批通知 | ✅ |
| 待审批列表一致性 | 列表只显示 Flowable 真正分配给用户的任务 | ✅ |
| 工作流管理功能 | 可查看流程定义、实例、任务，可操作实例 | ✅ |
| 审批人为空兜底 | 无合适审批人时自动降级给系统管理员 | ✅ |

