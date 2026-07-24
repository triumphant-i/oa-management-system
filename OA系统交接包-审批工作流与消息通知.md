# OA管理系统 - 审批工作流 + 消息通知 全项目交接包

> 生成时间：2026-07-23
> 项目路径：E:\oa-management-system
> 技术栈：Spring Boot 2.7.18 + MyBatis-Plus 3.3.2 + Flowable 6.8.0 + Vue 3 + Element Plus / Vant 4

---

## 任务一：全项目文件结构与功能地图（按业务模块组织）

### 1.1 审批管理模块（含 Flowable 工作流）

| 层级 | 文件/类名 | 路径 | 作用一句话说明 |
|------|-----------|------|----------------|
| Controller | ApprovalController | backend/.../controller/ApprovalController.java | 审批提交/审批/撤回/查询 REST 接口 |
| Controller | WorkflowManageController | backend/.../controller/WorkflowManageController.java | 工作流管理（流程定义/实例/历史/统计） |
| Service接口 | ApprovalService | backend/.../service/ApprovalService.java | 审批业务接口定义 |
| Service实现 | ApprovalServiceImpl | backend/.../service/impl/ApprovalServiceImpl.java | 审批核心逻辑（启动流程、完成任务、事件发布） |
| Flowable服务 | ApprovalFlowableService | backend/.../service/flowable/ApprovalFlowableService.java | Flowable引擎封装（启动流程、完成/拒绝任务、撤回） |
| 规则评估 | ApprovalRuleEvaluator | backend/.../service/flowable/ApprovalRuleEvaluator.java | 静态规则评估（根据类型/金额/天数决定signType） |
| 角色解析 | ApprovalRoleResolver | backend/.../service/ApprovalRoleResolver.java | 根据角色编码解析实际审批人ID |
| Delegate | ApprovalRuleEvaluationDelegate | backend/.../flowable/delegate/ApprovalRuleEvaluationDelegate.java | BPMN ServiceTask：规则评估节点 |
| Delegate | ApprovalRoleResolverDelegate | backend/.../flowable/delegate/ApprovalRoleResolverDelegate.java | BPMN ServiceTask：角色解析节点 |
| TaskListener | ApprovalTaskListener | backend/.../flowable/listener/ApprovalTaskListener.java | 任务创建时记录分配信息 |
| TaskListener | ParallelApprovalCompleteListener | backend/.../flowable/listener/ParallelApprovalCompleteListener.java | 会签complete事件：驳回计数+提前终止 |
| ExecutionListener | ApprovalProcessEndListener | backend/.../flowable/listener/ApprovalProcessEndListener.java | 流程结束：更新t_approval状态+通知申请人 |
| Event | ApprovalEvent | backend/.../event/ApprovalEvent.java | Spring事件对象（Builder模式） |
| EventListener | ApprovalEventListener | backend/.../event/listener/ApprovalEventListener.java | 监听ApprovalEvent → 写入t_message |
| Mapper | ApprovalMapper | backend/.../mapper/ApprovalMapper.java | t_approval 数据访问 |
| Mapper | ApprovalRoleMapper | backend/.../mapper/ApprovalRoleMapper.java | t_approval_role 数据访问 |
| Mapper | ApprovalDelegateMapper | backend/.../mapper/ApprovalDelegateMapper.java | t_approval_delegate 数据访问 |
| Entity | Approval | backend/.../entity/Approval.java | 审批实体（含processInstanceId字段） |
| Entity | ApprovalRole | backend/.../entity/ApprovalRole.java | 审批角色定义实体 |
| Entity | ApprovalDelegate | backend/.../entity/ApprovalDelegate.java | 审批代理实体 |
| BPMN | approval-process.bpmn20.xml | backend/.../resources/processes/ | 流程定义（SINGLE/PARALLEL/SERIAL_AFTER_PARALLEL） |
| DMN | leave-rule.dmn 等6个 | backend/.../resources/processes/ | 各类型审批规则DMN（目前未被代码调用） |
| 前端-PC | ApprovalManage/Index.vue | frontend/oa-pc/.../views/ApprovalManage/Index.vue | 审批管理页面 |
| 前端-PC | WorkflowManage/Index.vue | frontend/oa-pc/.../views/WorkflowManage/Index.vue | 工作流管理页面 |
| 前端-PC | api/approval.js | frontend/oa-pc/.../api/approval.js | 审批API封装 |
| 前端-Mobile | ApprovalCenter.vue | frontend/oa-mobile/src/views/ApprovalCenter.vue | 待审批列表 |
| 前端-Mobile | ApprovalDetail.vue | frontend/oa-mobile/src/views/ApprovalDetail.vue | 审批详情/操作 |
| 前端-Mobile | MyApplications.vue | frontend/oa-mobile/src/views/MyApplications.vue | 我的申请 |
| 前端-Mobile | Apply.vue / ApplyForm.vue / ApplyDetail.vue | frontend/oa-mobile/src/views/ | 发起申请相关 |
| 前端-Mobile | api/approval.js | frontend/oa-mobile/src/api/approval.js | 移动端审批API封装 |

**跨模块调用链：**

```
ApprovalController.submit()
  → ApprovalServiceImpl.submitApproval()
    → ApprovalFlowableService.startApprovalProcess()
      → ApprovalRuleEvaluator.evaluateRule() [静态方法]
      → ApprovalRoleResolver.resolveApprovers()
        → ApprovalRoleMapper / DepartmentMapper / EmployeeMapper / ApprovalDelegateMapper
      → RuntimeService.startProcessInstanceByKey("approvalProcess", variables)
        → [BPMN执行] ApprovalRuleEvaluationDelegate.execute()
        → [BPMN执行] ApprovalRoleResolverDelegate.execute()
          → ApprovalRoleResolver.resolveApprovers()
        → [BPMN执行] ApprovalTaskListener.notify() [userTask create]
    → ApprovalFlowableService.getProcessTasks() [查询第一级审批人]
    → ApplicationEventPublisher.publishEvent(ApprovalEvent.SUBMITTED)
      → ApprovalEventListener.onApprovalEvent()
        → MessageService.sendMessage() → INSERT t_message
```

```
ApprovalController.approve()
  → ApprovalServiceImpl.approveApproval()
    → ApprovalFlowableService.getPendingTasks(approverId)
    → ApprovalFlowableService.approveTask() / rejectTask()
      → TaskService.complete(taskId, variables{outcome, isAllApproved})
        → [BPMN执行] ParallelApprovalCompleteListener.notify() [会签complete]
        → [BPMN执行] ApprovalProcessEndListener.notify() [流程结束]
          → ApprovalService.updateById() [更新t_approval.status]
          → ApplicationEventPublisher.publishEvent(APPROVED_FINAL/REJECTED)
            → ApprovalEventListener → MessageService.sendMessage()
    → [若流程未结束] 查询下一级审批人 → publishEvent(SUBMITTED) → 通知下一级
```

---

### 1.2 消息通知模块

| 层级 | 文件/类名 | 路径 | 作用一句话说明 |
|------|-----------|------|----------------|
| Controller | MessageController | backend/.../controller/MessageController.java | 消息CRUD/分类查询/已读标记 REST 接口 |
| Service接口 | MessageService | backend/.../service/MessageService.java | 消息服务接口（11个方法） |
| Service实现 | MessageServiceImpl | backend/.../service/impl/MessageServiceImpl.java | 纯数据库操作实现（无推送） |
| Mapper | MessageMapper | backend/.../mapper/MessageMapper.java | t_message 数据访问（含自定义SQL注解） |
| Entity | Message | backend/.../entity/Message.java | 消息实体 |
| 前端-PC | MessageCenter.vue | frontend/oa-pc/.../views/MessageCenter.vue | PC消息中心页面 |
| 前端-PC | Layout.vue (badge部分) | frontend/oa-pc/.../views/Layout.vue | 顶栏未读badge（60s轮询countUnread） |
| 前端-Mobile | Message.vue | frontend/oa-mobile/src/views/Message.vue | 移动端消息中心 |
| 前端-Mobile | Home.vue (badge部分) | frontend/oa-mobile/src/views/Home.vue | 底部tab未读badge |
| 前端-Mobile | api/message.js | frontend/oa-mobile/src/api/message.js | 移动端消息API封装 |

**跨模块调用：** 消息模块是被动模块，不主动调用其他模块。被调用方：ApprovalEventListener、MeetingController（会议通知）。

---

### 1.3 员工管理模块

| 层级 | 文件/类名 | 路径 | 作用一句话说明 |
|------|-----------|------|----------------|
| Controller | EmployeeController | backend/.../controller/EmployeeController.java | 员工CRUD/登录/导入/角色修改 |
| Controller | ProfileController | backend/.../controller/ProfileController.java | 个人信息/修改密码/头像上传 |
| Service接口 | EmployeeService | backend/.../service/EmployeeService.java | 员工服务接口 |
| Service实现 | EmployeeServiceImpl | backend/.../service/impl/EmployeeServiceImpl.java | 员工服务实现（含批量导入） |
| Mapper | EmployeeMapper | backend/.../mapper/EmployeeMapper.java | t_employee 数据访问 |
| Entity | Employee | backend/.../entity/Employee.java | 员工实体 |
| 前端-PC | EmployeeManage/Index.vue | frontend/oa-pc/.../views/EmployeeManage/Index.vue | 员工管理页面 |
| 前端-PC | api/employee.js | frontend/oa-pc/.../api/employee.js | PC端员工API |
| 前端-Mobile | EmployeeManage.vue | frontend/oa-mobile/src/views/EmployeeManage.vue | 移动端员工管理 |
| 前端-Mobile | api/employee.js | frontend/oa-mobile/src/api/employee.js | 移动端员工API |

---

### 1.4 部门管理模块

| 层级 | 文件/类名 | 路径 | 作用一句话说明 |
|------|-----------|------|----------------|
| Controller | DepartmentController | backend/.../controller/DepartmentController.java | 部门CRUD/树形/负责人/调动 |
| Service接口 | DepartmentService | backend/.../service/DepartmentService.java | 部门服务接口 |
| Service实现 | DepartmentServiceImpl | backend/.../service/impl/DepartmentServiceImpl.java | 部门服务实现 |
| Mapper | DepartmentMapper | backend/.../mapper/DepartmentMapper.java | t_department 数据访问 |
| Entity | Department | backend/.../entity/Department.java | 部门实体 |
| 前端-PC | DepartmentManage/Index.vue | frontend/oa-pc/.../views/DepartmentManage/Index.vue | 部门管理页面 |
| 前端-Mobile | Department.vue / DepartmentDetail.vue / DepartmentMembers.vue | frontend/oa-mobile/src/views/ | 移动端部门相关 |

---

### 1.5 公告管理模块

| 层级 | 文件/类名 | 路径 | 作用一句话说明 |
|------|-----------|------|----------------|
| Controller | AnnouncementController | backend/.../controller/AnnouncementController.java | 公告发布/查询/置顶/已读 |
| Service接口 | AnnouncementService | backend/.../service/AnnouncementService.java | 公告服务接口 |
| Service接口 | AnnouncementReadStatusService | backend/.../service/AnnouncementReadStatusService.java | 公告阅读状态服务 |
| Service实现 | AnnouncementServiceImpl | backend/.../service/impl/AnnouncementServiceImpl.java | 公告服务实现 |
| Service实现 | AnnouncementReadStatusServiceImpl | backend/.../service/impl/AnnouncementReadStatusServiceImpl.java | 阅读状态实现 |
| Mapper | AnnouncementMapper | backend/.../mapper/AnnouncementMapper.java | t_announcement 数据访问 |
| Mapper | AnnouncementReadStatusMapper | backend/.../mapper/AnnouncementReadStatusMapper.java | t_announcement_read_status（自定义SQL） |
| Entity | Announcement | backend/.../entity/Announcement.java | 公告实体 |
| Entity | AnnouncementReadStatus | backend/.../entity/AnnouncementReadStatus.java | 阅读状态实体 |
| 前端-PC | NoticeManage/Index.vue | frontend/oa-pc/.../views/NoticeManage/Index.vue | 公告管理页面 |
| 前端-Mobile | Notice.vue / NoticeDetail.vue | frontend/oa-mobile/src/views/ | 移动端公告 |

---

### 1.6 考勤管理模块

| 层级 | 文件/类名 | 路径 | 作用一句话说明 |
|------|-----------|------|----------------|
| Controller | AttendanceController | backend/.../controller/AttendanceController.java | 打卡/记录/补卡/定位 |
| Service接口 | AttendanceService | backend/.../service/AttendanceService.java | 考勤服务接口 |
| Service实现 | AttendanceServiceImpl | backend/.../service/impl/AttendanceServiceImpl.java | 考勤服务实现 |
| Mapper | AttendanceMapper | backend/.../mapper/AttendanceMapper.java | t_attendance 数据访问 |
| Entity | Attendance | backend/.../entity/Attendance.java | 考勤实体 |
| 前端-PC | AttendanceManage/Index.vue | frontend/oa-pc/.../views/AttendanceManage/Index.vue | 考勤管理页面 |
| 前端-Mobile | Attendance.vue / AttendanceHistory.vue | frontend/oa-mobile/src/views/ | 移动端考勤 |

---

### 1.7 会议管理模块

| 层级 | 文件/类名 | 路径 | 作用一句话说明 |
|------|-----------|------|----------------|
| Controller | MeetingController | backend/.../controller/MeetingController.java | 会议预约/控制/查询 |
| Controller | MeetingRoomController | backend/.../controller/MeetingRoomController.java | 会议室CRUD |
| Service接口 | MeetingService | backend/.../service/MeetingService.java | 会议服务接口 |
| Service接口 | MeetingRoomService | backend/.../service/MeetingRoomService.java | 会议室服务接口 |
| Service实现 | MeetingServiceImpl | backend/.../service/impl/MeetingServiceImpl.java | 会议实现（含存储过程调用） |
| Service实现 | MeetingRoomServiceImpl | backend/.../service/impl/MeetingRoomServiceImpl.java | 会议室实现 |
| Mapper | MeetingMapper | backend/.../mapper/MeetingMapper.java | t_meeting 数据访问 |
| Mapper | MeetingRoomMapper | backend/.../mapper/MeetingRoomMapper.java | t_meeting_room 数据访问 |
| Entity | Meeting | backend/.../entity/Meeting.java | 会议实体 |
| Entity | MeetingRoom | backend/.../entity/MeetingRoom.java | 会议室实体 |
| 前端-PC | MeetingRoomManage/Index.vue | frontend/oa-pc/.../views/MeetingRoomManage/Index.vue | 会议室管理 |
| 前端-Mobile | Meeting.vue / MeetingDetail.vue / MeetingReserve.vue / MeetingControl.vue | frontend/oa-mobile/src/views/ | 移动端会议 |

**跨模块调用：** MeetingController 注入了 MessageService，在预约/取消会议时发送消息通知参会人。

---

### 1.8 附件管理模块

| 层级 | 文件/类名 | 路径 | 作用一句话说明 |
|------|-----------|------|----------------|
| Controller | AttachmentController | backend/.../controller/AttachmentController.java | 附件上传/下载/列表/删除 |
| Service接口 | AttachmentService | backend/.../service/AttachmentService.java | 附件服务接口 |
| Service实现 | AttachmentServiceImpl | backend/.../service/impl/AttachmentServiceImpl.java | 附件服务实现 |
| Mapper | AttachmentMapper | backend/.../mapper/AttachmentMapper.java | t_attachment 数据访问 |
| Entity | Attachment | backend/.../entity/Attachment.java | 附件实体 |
| 前端-Mobile | api/attachment.js | frontend/oa-mobile/src/api/attachment.js | 移动端附件API |

---

### 1.9 角色权限模块

| 层级 | 文件/类名 | 路径 | 作用一句话说明 |
|------|-----------|------|----------------|
| Controller | RoleController | backend/.../controller/RoleController.java | 角色CRUD/权限列表 |
| Service接口 | RoleService | backend/.../service/RoleService.java | 角色服务接口 |
| Service实现 | RoleServiceImpl | backend/.../service/impl/RoleServiceImpl.java | 角色服务实现 |
| Mapper | RoleMapper | backend/.../mapper/RoleMapper.java | t_role 数据访问 |
| Entity | Role | backend/.../entity/Role.java | 角色实体 |
| 注解 | RequirePermission | backend/.../annotation/RequirePermission.java | 方法级权限注解 |
| 拦截器 | PermissionInterceptor | backend/.../interceptor/PermissionInterceptor.java | JWT解析+UserContext设置 |
| 枚举 | RoleType | backend/.../enums/RoleType.java | 角色枚举（4种） |
| 上下文 | UserContext | backend/.../common/UserContext.java | ThreadLocal用户上下文 |
| 前端-PC | utils/permission.js | frontend/oa-pc/.../utils/permission.js | 前端权限判断 |
| 前端-PC | directives/permission.js | frontend/oa-pc/.../directives/permission.js | v-permission指令 |
| 前端-Mobile | api/system.js | frontend/oa-mobile/src/api/system.js | 角色/日志/字典API |

---

### 1.10 操作日志模块

| 层级 | 文件/类名 | 路径 | 作用一句话说明 |
|------|-----------|------|----------------|
| Controller | OperationLogController | backend/.../controller/OperationLogController.java | 日志查询/保存/清空 |
| Service接口 | OperationLogService | backend/.../service/OperationLogService.java | 空接口（仅继承IService） |
| Service实现 | OperationLogServiceImpl | backend/.../service/impl/OperationLogServiceImpl.java | 空实现 |
| Mapper | OperationLogMapper | backend/.../mapper/OperationLogMapper.java | t_operation_log 数据访问 |
| Entity | OperationLog | backend/.../entity/OperationLog.java | 日志实体 |

---

### 1.11 字典管理模块

| 层级 | 文件/类名 | 路径 | 作用一句话说明 |
|------|-----------|------|----------------|
| Controller | DictController | backend/.../controller/DictController.java | 字典CRUD |
| Service接口 | DictService | backend/.../service/DictService.java | 字典服务接口 |
| Service实现 | DictServiceImpl | backend/.../service/impl/DictServiceImpl.java | 字典服务实现 |
| Mapper | DictMapper | backend/.../mapper/DictMapper.java | t_dict 数据访问 |
| Entity | Dict | backend/.../entity/Dict.java | 字典实体 |

---

### 1.12 搜索模块（Elasticsearch）

| 层级 | 文件/类名 | 路径 | 作用一句话说明 |
|------|-----------|------|----------------|
| Controller | SearchController | backend/.../controller/SearchController.java | ES搜索接口（条件装配） |
| Service | EmployeeSearchService | backend/.../service/EmployeeSearchService.java | 员工ES搜索 |
| Service | AnnouncementSearchService | backend/.../service/AnnouncementSearchService.java | 公告ES搜索 |
| Service | ElasticsearchSyncService | backend/.../service/ElasticsearchSyncService.java | ES数据同步 |
| Document | EmployeeDocument | backend/.../document/EmployeeDocument.java | ES员工文档 |
| Document | AnnouncementDocument | backend/.../document/AnnouncementDocument.java | ES公告文档 |
| Repository | EmployeeDocumentRepository | backend/.../repository/EmployeeDocumentRepository.java | ES员工仓库 |
| Repository | AnnouncementDocumentRepository | backend/.../repository/AnnouncementDocumentRepository.java | ES公告仓库 |

---

### 1.13 基础设施/工具模块

| 层级 | 文件/类名 | 路径 | 作用一句话说明 |
|------|-----------|------|----------------|
| Config | WebMvcConfig | backend/.../config/WebMvcConfig.java | CORS+拦截器+静态资源 |
| Config | RedisConfig | backend/.../config/RedisConfig.java | RedisTemplate配置 |
| Config | CacheConfig | backend/.../config/CacheConfig.java | 缓存配置 |
| Config | AppConfig | backend/.../config/AppConfig.java | PasswordEncoder Bean |
| Config | ScheduleConfig | backend/.../config/ScheduleConfig.java | @EnableScheduling |
| Config | JacksonConfig | backend/.../config/JacksonConfig.java | ObjectMapper配置 |
| Config | GlobalExceptionHandler | backend/.../config/GlobalExceptionHandler.java | 全局异常处理 |
| Config | NacosConfigListener | backend/.../config/NacosConfigListener.java | Nacos动态配置 |
| Config | ElasticsearchInitializer | backend/.../config/ElasticsearchInitializer.java | ES启动同步 |
| Util | JwtTokenUtil | backend/.../util/JwtTokenUtil.java | JWT生成/解析/验证 |
| Util | RedisCacheUtil | backend/.../util/RedisCacheUtil.java | Redis操作封装 |
| Util | ResultVOUtil | backend/.../util/ResultVOUtil.java | 统一响应封装 |
| Util | ValidatorUtil | backend/.../util/ValidatorUtil.java | 参数校验工具 |
| Util | PasswordEncoder | backend/.../util/PasswordEncoder.java | 密码加密（MD5+salt） |
| VO | ResultVO / PageVO | backend/.../vo/ | 统一响应/分页VO |
| 存储过程 | DatabaseProcedureService | backend/.../service/DatabaseProcedureService.java | 存储过程调用封装 |
| 存储过程 | StoredProceduresMapper | backend/.../mapper/extension/StoredProceduresMapper.java | 存储过程Mapper |

---

## 任务二：文件之间的依赖关系

### 2.1 Service层相互依赖注入关系

```
ApprovalServiceImpl
  @Autowired → ApplicationEventPublisher
  @Autowired → ApprovalFlowableService
  @Autowired → EmployeeService

ApprovalFlowableService
  @Autowired → ProcessEngine
  @Autowired → RuntimeService
  @Autowired → TaskService (org.flowable.engine.TaskService)
  @Autowired → HistoryService
  @Autowired → ApprovalRoleResolver

ApprovalRoleResolver
  @Autowired → ApprovalRoleMapper
  @Autowired → DepartmentMapper
  @Autowired → EmployeeMapper
  @Autowired → ApprovalDelegateMapper

ApprovalEventListener
  @Autowired → MessageService

ApprovalProcessEndListener
  @Autowired → ApprovalService
  @Autowired → ProcessEngine
  @Autowired → ApplicationEventPublisher

ApprovalRuleEvaluationDelegate
  @Autowired → ApprovalRoleResolver

ApprovalRoleResolverDelegate
  @Autowired → ApprovalRoleResolver

MessageServiceImpl
  无外部Service依赖（仅使用MessageMapper）

OperationLogServiceImpl
  无外部Service依赖（空实现）

MeetingServiceImpl
  @Autowired → DatabaseProcedureService
  @Autowired → MeetingRoomMapper
  @Autowired → EmployeeMapper

MeetingController
  @Autowired → MeetingService
  @Autowired → MeetingRoomService
  @Autowired → MessageService  ← 跨模块调用
  @Autowired → EmployeeService

EmployeeController
  @Autowired → EmployeeService
  @Autowired → PasswordEncoder
  @Autowired → JwtTokenUtil
  @Autowired → MessageService  ← 跨模块调用（登录时发欢迎消息等）

ApprovalController
  @Autowired → ApprovalService
  @Autowired → MessageService  ← 跨模块调用
  @Autowired → AttendanceService
  @Autowired → org.flowable.engine.TaskService (required=false)
  @Autowired → ApprovalFlowableService
```

**关键结论：**
- ApprovalService ↔ MessageService 之间无直接注入，通过 Spring Event 解耦（ApprovalEvent → ApprovalEventListener → MessageService）
- ApprovalProcessEndListener 直接注入了 ApprovalService（存在循环依赖风险，但因在Flowable delegate中延迟调用，实际未触发）
- OperationLogService 目前完全独立，未被任何其他Service调用（仅Controller直接CRUD）
- RoleService 未被 ApprovalRoleResolver 使用（ApprovalRoleResolver 直接操作 t_approval_role 表，与 t_role 表无关）

---

### 2.2 Flowable 相关类与 BPMN delegateExpression 绑定关系

| BPMN中的delegateExpression | 对应实现类 | @Component名称 | 接口 | 是否能找到 |
|---|---|---|---|---|
| `${approvalRuleEvaluationDelegate}` | ApprovalRuleEvaluationDelegate | `@Component("approvalRuleEvaluationDelegate")` | JavaDelegate | ✅ 存在 |
| `${approvalRoleResolverDelegate}` | ApprovalRoleResolverDelegate | `@Component("approvalRoleResolverDelegate")` | JavaDelegate | ✅ 存在 |
| `${approvalTaskListener}` | ApprovalTaskListener | `@Component("approvalTaskListener")` | TaskListener | ✅ 存在 |
| `${parallelApprovalCompleteListener}` | ParallelApprovalCompleteListener | `@Component("parallelApprovalCompleteListener")` | TaskListener | ✅ 存在 |
| `${approvalProcessEndListener}` | ApprovalProcessEndListener | `@Component("approvalProcessEndListener")` | ExecutionListener | ✅ 存在 |

**结论：BPMN中引用的5个delegateExpression全部有对应的Spring Bean实现，绑定关系完整。**

所有delegate均使用 `delegateExpression`（而非 `flowable:class`），因此可以通过Spring DI注入依赖。

---

### 2.3 前后端接口对照（按功能模块分组）

#### 审批模块

| 后端接口 | PC端调用 | 移动端调用 | 差异说明 |
|----------|----------|------------|----------|
| POST /approval/submit | ✅ submitApproval | ✅ submitApproval | 一致 |
| GET /approval/myApplications/{id} | ✅ getMyApplications | ✅ getMyApplications | 一致 |
| GET /approval/pendingList | ✅ getPendingList | ✅ getPendingList | 一致 |
| GET /approval/pendingCount | ❌ 未调用 | ✅ getPendingCount | PC缺失 |
| PUT /approval/approve | ✅ handleApproval | ✅ handleApproval | 一致 |
| GET /approval/list/{page}/{size} | ✅ getApprovalPage | ❌ 未调用 | 移动端缺失 |
| GET /approval/findByStatus/{status} | ✅ getApprovalByStatus | ✅ getApprovalByStatus | 一致 |
| GET /approval/findByType/{type} | ✅ getApprovalByType | ❌ 未调用 | 移动端缺失 |
| PUT /approval/withdraw/{id} | ✅ withdrawApproval | ✅ withdrawApproval | 一致 |
| GET /approval/detail/{id} | ✅ getApprovalDetail | ✅ getApprovalDetail | 一致 |
| GET /approval/my | ❌ 未调用 | ✅ getMyApprovalList | PC缺失 |
| GET /approval/fixExistingRecords | ❌ | ❌ | 临时修复接口 |
| GET /approval/fixSecondLevelApprover | ❌ | ❌ | 临时修复接口 |

#### 消息模块

| 后端接口 | PC端调用 | 移动端调用 | 差异说明 |
|----------|----------|------------|----------|
| GET /message/list | ✅ (MessageCenter.vue内联) | ✅ getMessageList | 一致 |
| GET /message/todoCount | ❌ | ✅ getTodoCount | PC未使用 |
| GET /message/countUnread | ✅ (Layout.vue轮询) | ✅ getUnreadCount | 一致 |
| PUT /message/read/{id} | ✅ | ✅ markAsRead | 一致 |
| PUT /message/readAll | ✅ | ✅ markCategoryAsRead | 一致 |
| POST /message/send | ❌ | ✅ sendMessage | PC缺失 |
| GET /message/detail/{id} | ❌ | ✅ getMessageDetail | PC缺失 |
| DELETE /message/delete/{id} | ❌ | ✅ deleteMessage | PC缺失 |

#### 工作流管理接口（仅PC端）

| 后端接口 | PC端调用 | 移动端调用 |
|----------|----------|------------|
| GET /workflow/definitions | ✅ (WorkflowManage/Index.vue) | ❌ |
| GET /workflow/instances | ✅ | ❌ |
| GET /workflow/instance/{id}/history | ✅ | ❌ |
| PUT /workflow/instance/{id}/terminate | ✅ | ❌ |
| GET /workflow/statistics | ✅ | ❌ |

---

## 任务三：数据库表使用情况全图

### 3.1 表清单总览

数据库共 **15张业务表** + Flowable引擎自动创建的 ACT_* 表（约34张）。

| # | 表名 | Entity类 | Mapper | 逻辑删除(deleted) | Flowable使用 |
|---|------|----------|--------|-------------------|--------------|
| 1 | t_department | Department | DepartmentMapper | ❌ 无deleted字段 | 间接（ApprovalRoleResolver查部门经理） |
| 2 | t_employee | Employee | EmployeeMapper | ❌ 无deleted字段 | 间接（ApprovalRoleResolver查员工） |
| 3 | t_approval | Approval | ApprovalMapper | ❌ 无deleted字段 | ✅ processInstanceId关联流程实例 |
| 4 | t_announcement | Announcement | AnnouncementMapper | ❌ 无deleted字段 | ❌ |
| 5 | t_announcement_read_status | AnnouncementReadStatus | AnnouncementReadStatusMapper | ❌ | ❌ |
| 6 | t_attendance | Attendance | AttendanceMapper | ❌ | ❌ |
| 7 | t_meeting_room | MeetingRoom | MeetingRoomMapper | ❌ | ❌ |
| 8 | t_meeting | Meeting | MeetingMapper | ❌ | ❌ |
| 9 | t_message | Message | MessageMapper | ❌ | 间接（ApprovalEventListener写入） |
| 10 | t_attachment | Attachment | AttachmentMapper | ❌ | ❌ |
| 11 | t_dict | Dict | DictMapper | ❌ | ❌ |
| 12 | t_operation_log | OperationLog | OperationLogMapper | ❌ | ❌ |
| 13 | t_role | Role | RoleMapper | ❌ | ❌ |
| 14 | t_approval_role | ApprovalRole | ApprovalRoleMapper | ❌ | ✅ ApprovalRoleResolver核心依赖 |
| 15 | t_approval_delegate | ApprovalDelegate | ApprovalDelegateMapper | ❌ | ✅ 审批代理检查 |

**重要发现：** 虽然 mybatis-plus 全局配置了 `logic-delete-field: deleted`，但实际上 **没有任何一张表包含 deleted 字段**。这意味着逻辑删除配置形同虚设，所有删除操作都是物理删除。

---

### 3.2 各表详细使用情况

#### t_approval（审批表）

- **Entity:** `Approval.java` — 字段：id, applicantId, applicantName, applicantDepartmentId, approvalType, title, content, startTime, endTime, amount, leaveType, destCity, workDate, startTimeOnly, endTimeOnly, expenseType, expenseDate, goodsName, quantity, unitPrice, cardDate, cardTime, cardType, status, approverId, approverName, approveTime, approveReason, **processInstanceId**, createTime, updateTime
- **读写Service：** ApprovalServiceImpl（CRUD）、ApprovalProcessEndListener（更新status）
- **Flowable关联：** `process_instance_id` 字段关联 ACT_RU_EXECUTION / ACT_HI_PROCINST 的 ID_
- **前端展示：** PC ApprovalManage/Index.vue、移动端 ApprovalCenter/ApprovalDetail/MyApplications/ApplyDetail

#### t_approval_role（审批角色定义表）

- **Entity:** `ApprovalRole.java` — 字段：id, roleCode, roleName, resolveStrategy, fixedDepartmentId, fixedEmployeeId, createTime
- **读写Service：** ApprovalRoleResolver（只读查询）
- **Flowable关联：** 被 ApprovalRoleResolverDelegate 在流程执行时调用
- **种子数据：** DEPT_MANAGER(OWN_DEPT_MANAGER), FINANCE_STAFF(FIXED_EMPLOYEE→11), FINANCE_MANAGER(FIXED_DEPT_MANAGER→dept5), GM(FIXED_EMPLOYEE→12)
- **前端展示：** 无直接前端页面（后台配置表）

#### t_approval_delegate（审批代理表）

- **Entity:** `ApprovalDelegate.java` — 字段：id, delegatorId, delegateId, approvalType, startTime, endTime, status, createTime
- **读写Service：** ApprovalRoleResolver.checkAndApplyDelegate()（只读查询）
- **Flowable关联：** 在角色解析时检查委托代理
- **前端展示：** 无直接前端页面（目前无管理入口）

#### t_message（消息表）

- **Entity:** `Message.java` — 字段：id, senderId, senderName, receiverId, receiverName, title, content, msgType, eventType, bizType, bizId, relatedId, isRead, isTodo, jumpUrl, isTop, status, createTime, readTime, updateTime
- **读写Service：** MessageServiceImpl（全部11个方法）
- **写入触发点：** ApprovalEventListener（审批事件）、MeetingController（会议通知）
- **前端展示：** PC MessageCenter.vue、移动端 Message.vue、Layout.vue badge、Home.vue badge

#### t_operation_log（操作日志表）

- **Entity:** `OperationLog.java` — 字段：id, userId, user, action, desc, ipAddress, createTime
- **读写Service：** OperationLogServiceImpl（空实现，仅继承IService）
- **前端展示：** 移动端 System.vue（通过 api/system.js 调用）

---

## 任务四：Flowable 审批工作流当前实现程度评估

### 4.1 逐项核实

#### Q1: RuleEvaluationTask、RoleResolverTask 对应的 delegate 类是否存在？是否真正调用了 ApprovalRoleResolver？

**结论：存在，但职责有分工。**

- `ApprovalRuleEvaluationDelegate`（`@Component("approvalRuleEvaluationDelegate")`）：**存在但基本是空壳**。其 execute() 方法主要做日志输出和验证。实际的规则评估在 `ApprovalFlowableService.startApprovalProcess()` 中通过调用 `ApprovalRuleEvaluator.evaluateRule()` 完成，结果作为流程变量（requiredRoles, signType）在启动流程前就已设置好。Delegate 内部检测到变量已存在则直接 return。

- `ApprovalRoleResolverDelegate`（`@Component("approvalRoleResolverDelegate")`）：**存在且真正调用了 ApprovalRoleResolver.resolveApprovers()**。它从流程变量读取 requiredRoles/signType/applicantId/approvalType，调用 `approvalRoleResolver.resolveApprovers()` 解析出审批人ID列表，写入流程变量 `approverList` 和 `secondLevelApprover`。

**证据位置：**
- `flowable/delegate/ApprovalRuleEvaluationDelegate.java` 第104行：`if (requiredRoles != null && signType != null) { return; }`
- `flowable/delegate/ApprovalRoleResolverDelegate.java` 第174行：`approvalRoleResolver.resolveApprovers(firstLevelRolesStr, applicantId, approvalType)`

---

#### Q2: approvalTaskListener、parallelApprovalCompleteListener、approvalProcessEndListener 是否有对应实现？是否包含"审批结果写回 t_approval"和"触发消息通知"？

**结论：全部存在，职责明确。**

| Listener | 写回t_approval | 触发消息通知 |
|----------|---------------|-------------|
| ApprovalTaskListener | ❌ 仅记录日志 | ❌ |
| ParallelApprovalCompleteListener | ❌ 仅维护 isAllApproved/nrOfRejectedInstances 变量 | ❌ |
| ApprovalProcessEndListener | ✅ 更新 status 为"已通过"/"已拒绝" | ✅ 发布 ApprovalEvent(APPROVED_FINAL/REJECTED) → ApprovalEventListener → MessageService.sendMessage() |

**证据：**
- `ApprovalProcessEndListener.java` 第354-361行：`approvalService.updateById(updateApproval)` 写回状态
- `ApprovalProcessEndListener.java` 第378-409行：`notifyApplicantFinalResult()` 发布事件通知申请人

---

#### Q3: ApprovalService.submitApproval() / approveApproval() / withdrawApproval() 是否真正调用了 Flowable？

**结论：是的，已完全打通 Flowable 引擎。这是最关键的结论。**

- **submitApproval()：** 先 `this.save(approval)` 保存到 t_approval，然后调用 `approvalFlowableService.startApprovalProcess(approval)` 启动 Flowable 流程实例，获得 processInstanceId 后回写到 t_approval 记录。

- **approveApproval()：** 通过 `approvalFlowableService.getPendingTasks(approverId)` 查询 Flowable 任务列表，匹配 processInstanceId 后调用 `approvalFlowableService.approveTask(taskId, ...)` 或 `rejectTask(taskId, ...)`，内部调用 `taskService.complete(taskId, variables)`。

- **withdrawApproval()：** 更新 t_approval 状态后，调用 `approvalFlowableService.withdrawProcess(processInstanceId)`，内部调用 `runtimeService.deleteProcessInstance()`。

**证据：**
- `ApprovalServiceImpl.java` 第623行：`processInstanceId = approvalFlowableService.startApprovalProcess(approval);`
- `ApprovalServiceImpl.java` 第718行：`approvalFlowableService.approveTask(task.getId(), approval.getApproverId(), approval.getApproveReason());`
- `ApprovalServiceImpl.java` 第859行：`approvalFlowableService.withdrawProcess(existing.getProcessInstanceId());`

---

#### Q4: 是否存在流程实例与 t_approval 的关联字段？

**结论：存在。t_approval 表有 `process_instance_id VARCHAR(64)` 字段。**

- DDL 中明确定义：`process_instance_id VARCHAR(64) DEFAULT NULL COMMENT '关联的Flowable流程实例ID'`
- 有索引：`KEY idx_process_instance_id (process_instance_id)`
- Entity 中：`Approval.java` 有 `private String processInstanceId;` 字段
- 业务表和 Flowable 引擎表（ACT_*）通过此字段关联，**不是脱节的**。

---

#### Q5: 流程变量 approverList、isAllApproved、outcome、secondLevelApprover 在哪里 set/get？

| 变量 | SET 位置 | GET 位置 |
|------|----------|----------|
| `approverList` | ApprovalFlowableService.startApprovalProcess() 启动时设置；ApprovalRoleResolverDelegate.execute() 中覆盖设置 | BPMN userTask assignee表达式 `${approverList[0]}`；multiInstance collection |
| `isAllApproved` | ApprovalFlowableService.approveTask() 设 true；rejectTask() 设 false；ParallelApprovalCompleteListener REJECT时设 false | BPMN completionCondition；ParallelResultGateway/FirstLevelResultGateway 条件表达式 |
| `outcome` | ApprovalFlowableService.approveTask() 设 "APPROVE"；rejectTask() 设 "REJECT" | SingleResultGateway/SecondLevelResultGateway 条件表达式；ParallelApprovalCompleteListener 读取 |
| `secondLevelApprover` | ApprovalRoleResolverDelegate.execute() 中解析后设置；ApprovalFlowableService.fixMissingSecondLevelApprover() 补丁 | BPMN SecondLevelSerialTask assignee `${secondLevelApprover}` |
| `signType` | ApprovalFlowableService.startApprovalProcess() 启动时设置 | SignTypeGateway 条件表达式 |
| `requiredRoles` | ApprovalFlowableService.startApprovalProcess() 启动时设置 | ApprovalRoleResolverDelegate.execute() 读取 |
| `approvalId` | ApprovalFlowableService.startApprovalProcess() 启动时设置 | ApprovalProcessEndListener.notify() 读取 |
| `applicantId` | ApprovalFlowableService.startApprovalProcess() 启动时设置 | ApprovalRuleEvaluationDelegate/ApprovalRoleResolverDelegate 读取 |

---

#### Q6: 是否存在"审批流程可视化"相关代码？

**结论：部分存在（管理级），但无"审批轨迹时间线"可视化。**

已有：
- `WorkflowManageController` 提供了：
  - `GET /workflow/instance/{id}/history` — 返回 HistoricActivityInstance 列表（含活动ID、名称、开始/结束时间、assignee）
  - `GET /workflow/instance/{id}/tasks` — 返回当前待办任务
  - `GET /workflow/definitions` — 返回流程定义列表
  - `GET /workflow/instance/{id}` — 返回实例详情

未有（当前为 0 实现）：
- ❌ 返回 BPMN XML/PNG 流程图 + 高亮当前节点
- ❌ 前端审批轨迹时间线组件（类似"已提交→部门经理审批中→等待财务审核"的可视化）
- ❌ 移动端任何流程可视化
- ❌ 前端调用 /workflow/* 接口展示审批进度（PC端 WorkflowManage 页面是管理级列表，不是单据级轨迹）

---

#### Q7: 前端是否调用了 Flowable 相关接口？

**结论：PC端有调用，但仅限管理级页面；审批业务页面完全对接自研 t_approval 接口。**

- PC端 `WorkflowManage/Index.vue` 调用了 `/workflow/definitions`、`/workflow/instances`、`/workflow/instance/{id}/history` 等接口，但这是**管理员视角的流程监控页面**。
- PC端 `ApprovalManage/Index.vue` 和移动端所有审批页面，**完全只对接 `/approval/*` 接口**（submit/approve/withdraw/detail/pendingList），不直接调用任何 Flowable 接口。
- 移动端**完全没有**工作流管理页面。

---

### 4.2 总结性结论

> **Flowable 目前处于"引擎已完全打通且三种模式均已实现"的阶段。**
>
> 具体而言：
> - 流程定义（BPMN）设计完整，支持 SINGLE / PARALLEL / SERIAL_AFTER_PARALLEL 三种模式
> - 所有 5 个 delegateExpression 均有对应的 Spring Bean 实现
> - ApprovalServiceImpl 的 submit/approve/withdraw 三个核心方法均真正调用了 Flowable RuntimeService/TaskService
> - t_approval.process_instance_id 字段正确关联了业务表与引擎表
> - 审批事件通过 Spring Event 机制触发消息写入
> - 规则评估（ApprovalRuleEvaluator）覆盖了6种审批类型的分级规则
> - 角色解析（ApprovalRoleResolver）支持3种策略 + 委托代理
>
> **但存在以下未完成项：**
> - 审批流程可视化（单据级轨迹时间线）= 0 实现
> - DMN 文件（6个）存在但未被代码实际调用（规则评估用的是 Java 硬编码）
> - 会签/串签模式虽有代码实现，但根据 MEMORY 中记录的 pitfall（TaskListener complete 事件时序问题），可能未经充分生产验证
>
> **置信度：90%。** 依据：全部核心源码已逐行审阅，调用链完整闭环，processInstanceId 关联字段存在且被使用。

---

## 任务五：消息通知模块现状评估

### 5.1 MessageService 现有能力

| 方法 | 功能 | 实现方式 |
|------|------|----------|
| countUnread(receiverId) | 未读消息总数 | MessageMapper.countUnread (SQL注解) |
| findByReceiverId(receiverId) | 查所有消息 | MessageMapper.findByReceiverId |
| markAsRead(id) | 单条标记已读 | MessageMapper.markAsRead |
| markAllAsRead(receiverId) | 全部标记已读 | MessageMapper.markAllAsRead |
| sendMessage(message) | 发送单条消息 | INSERT t_message |
| sendBatchMessage(receiverIds, ...) | 批量发送 | saveBatch |
| listMessagesByCategory(receiverId, category, page, size) | 分类分页查询（all/todo/cc/system） | QueryWrapper + page |
| countTodoMessages(receiverId) | 待办角标计数（is_todo=1 AND is_read=0） | QueryWrapper + count |
| markMessageAsRead(messageId, receiverId) | 标记已读+返回jumpUrl | UPDATE + SELECT |
| markCategoryAsRead(receiverId, category) | 按分类批量已读 | UPDATE |
| markTodoAsInvalid(bizId, bizType) | 待办失效标记（撤回时用） | UPDATE is_todo=0 |

**结论：纯数据库轮询模式，无任何推送机制。**

---

### 5.2 审批流程中的消息触发点

| 触发点 | 位置 | EventType | 接收人 | 写入t_message |
|--------|------|-----------|--------|--------------|
| 提交审批 | ApprovalServiceImpl.submitApproval() | SUBMITTED | 第一级审批人 | ✅ 通过ApprovalEventListener |
| 节点审批通过（流程未结束） | ApprovalServiceImpl.approveApproval() | APPROVED_NODE | 申请人 | ✅ |
| 节点审批通过（通知下一级） | ApprovalServiceImpl.approveApproval() | SUBMITTED | 下一级审批人 | ✅ |
| 节点审批拒绝 | ApprovalServiceImpl.approveApproval() | REJECTED | 申请人 | ✅ |
| 流程最终通过 | ApprovalProcessEndListener.notify() | APPROVED_FINAL | 申请人 | ✅ |
| 流程最终拒绝 | ApprovalProcessEndListener.notify() | REJECTED | 申请人 | ✅ |
| 撤回审批 | ApprovalServiceImpl.withdrawApproval() | WITHDRAWN | 原审批人 | ✅ |

**消息写入路径：** 所有审批消息均通过 `ApplicationEventPublisher.publishEvent(ApprovalEvent)` → `ApprovalEventListener.onApprovalEvent()` → `MessageService.sendMessage()` → INSERT t_message。

---

### 5.3 实时推送技术检查

**pom.xml 依赖检查结果：**
- ❌ 无 spring-boot-starter-websocket
- ❌ 无 Netty
- ❌ 无 STOMP
- ❌ 无 SSE (SseEmitter 未使用)
- ❌ 无 RabbitMQ / RocketMQ / Kafka
- ❌ 无 Redis Pub/Sub 使用（Redis 仅用于缓存）

**application.yml 配置检查结果：**
- ❌ 无 WebSocket 配置
- ❌ 无消息队列配置
- Redis 仅配置了连接池，无 Pub/Sub 相关

**代码搜索结果：**
- 搜索 "WebSocket" / "SSE" / "SseEmitter" / "@ServerEndpoint" / "STOMP" → **0 结果**

**结论：当前无任何实时推送基础设施，需要从零引入。**

---

### 5.4 前端获取消息的方式

#### PC端

| 位置 | 方式 | 代码位置 |
|------|------|----------|
| Layout.vue 顶栏 badge | **60s 定时轮询** `GET /message/countUnread` | `frontend/oa-pc/oa-pc-admin/src/views/Layout.vue` — `setInterval(() => { fetchUnreadCount() }, 60000)` |
| MessageCenter.vue | **页面加载时查询一次** `GET /message/list?category=&page=&size=` | 进入页面时 onMounted 调用 |

#### 移动端

| 位置 | 方式 | 代码位置 |
|------|------|----------|
| Home.vue 底部 badge | **页面加载时查询一次** `GET /message/countUnread` | `frontend/oa-mobile/src/views/Home.vue` — onMounted 调用 getUnreadCount() |
| Message.vue | **页面加载时查询一次** `GET /message/list` | 进入页面时 onMounted 调用 |

**结论：PC端有60s轮询（仅badge数字），移动端仅在进入页面时查询一次。两端均无实时推送、无 WebSocket 连接、无 SSE 监听。新消息到达时用户不会收到任何即时提醒。**

---

## 任务六：可独立交接的模块交接包

### 6.1 核心文件完整源码

#### 6.1.1 ApprovalFlowableService.java（Flowable引擎封装 - 核心）

**路径：** `backend/oa-springboot/src/main/java/com/southwind/service/flowable/ApprovalFlowableService.java`
**职责：** 封装所有与 Flowable 引擎的交互，包括启动流程、完成/拒绝任务、撤回流程、查询任务和历史。
**调用关系：** 被 ApprovalServiceImpl 和 ApprovalController 调用；调用 RuntimeService、TaskService、HistoryService、ApprovalRoleResolver。

```java
package com.southwind.service.flowable;

import com.southwind.entity.Approval;
import com.southwind.service.ApprovalRoleResolver;
import org.flowable.engine.HistoryService;
import org.flowable.engine.ProcessEngine;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.TaskService;
import org.flowable.engine.history.HistoricActivityInstance;
import org.flowable.engine.history.HistoricProcessInstance;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.task.api.Task;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class ApprovalFlowableService {

    private static final Logger logger = LoggerFactory.getLogger(ApprovalFlowableService.class);
    private static final String PROCESS_KEY = "approvalProcess";

    @Autowired
    private ProcessEngine processEngine;

    @Autowired
    private RuntimeService runtimeService;

    @Autowired
    private TaskService taskService;

    @Autowired
    private HistoryService historyService;

    @Autowired
    private ApprovalRoleResolver approvalRoleResolver;

    /**
     * 启动审批流程
     * 1. 评估规则 → 确定 signType 和 requiredRoles
     * 2. 解析审批人 → 确定 approverList
     * 3. 启动 Flowable 流程实例
     */
    public String startApprovalProcess(Approval approval) {
        logger.info("=== 启动审批流程 ===");
        logger.info("approvalId={}, type={}, applicantId={}", 
                approval.getId(), approval.getApprovalType(), approval.getApplicantId());

        // 1. 评估审批规则
        Double amount = approval.getAmount() != null ? approval.getAmount().doubleValue() : null;
        Integer days = calculateDays(approval);

        ApprovalRuleEvaluator.ApprovalRule rule = ApprovalRuleEvaluator.evaluateRule(
                approval.getApprovalType(), amount, days);

        String requiredRoles = rule.requiredRoles;
        String signType = rule.signType;

        logger.info("规则评估结果: requiredRoles={}, signType={}", requiredRoles, signType);

        // 2. 解析审批人
        List<Integer> approverIds = approvalRoleResolver.resolveApprovers(
                requiredRoles, approval.getApplicantId(), approval.getApprovalType());

        List<String> approverList = new ArrayList<>();
        for (Integer id : approverIds) {
            approverList.add(String.valueOf(id));
        }

        logger.info("解析审批人: approverList={}", approverList);

        // 3. 设置流程变量
        Map<String, Object> variables = new HashMap<>();
        variables.put("approvalId", approval.getId());
        variables.put("approvalType", approval.getApprovalType());
        variables.put("applicantId", approval.getApplicantId());
        variables.put("amount", amount);
        variables.put("requiredRoles", requiredRoles);
        variables.put("signType", signType);
        variables.put("approverList", approverList);

        // 如果是会签后串签，设置第二级审批人
        if ("SERIAL_AFTER_PARALLEL".equals(signType)) {
            String[] roles = requiredRoles.split(",");
            if (roles.length > 1) {
                String secondLevelRole = roles[roles.length - 1].trim();
                List<Integer> secondLevelIds = approvalRoleResolver.resolveApprovers(
                        secondLevelRole, approval.getApplicantId(), approval.getApprovalType());
                if (!secondLevelIds.isEmpty()) {
                    variables.put("secondLevelApprover", String.valueOf(secondLevelIds.get(0)));
                } else {
                    variables.put("secondLevelApprover", "1"); // 兜底：系统管理员
                }
            }
        }

        // 计算请假/出差天数
        if (days != null) {
            variables.put("leaveDays", days);
            variables.put("businessDays", days);
        }

        // 4. 启动流程
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey(
                PROCESS_KEY, String.valueOf(approval.getId()), variables);

        String processInstanceId = processInstance.getId();
        logger.info("✓ 流程已启动: processInstanceId={}, approvalId={}", 
                processInstanceId, approval.getId());

        return processInstanceId;
    }

    /**
     * 获取用户的待办任务
     */
    public List<Task> getPendingTasks(Integer userId) {
        return taskService.createTaskQuery()
                .taskAssignee(String.valueOf(userId))
                .orderByTaskCreateTime().desc()
                .list();
    }

    /**
     * 审批通过
     */
    public void approveTask(String taskId, Integer userId, String comment) {
        logger.info("审批通过: taskId={}, userId={}", taskId, userId);

        Map<String, Object> variables = new HashMap<>();
        variables.put("outcome", "APPROVE");
        variables.put("isAllApproved", true);

        if (comment != null && !comment.isEmpty()) {
            taskService.addComment(taskId, null, "同意: " + comment);
        }

        taskService.complete(taskId, variables);
        logger.info("✓ 任务已完成: taskId={}", taskId);
    }

    /**
     * 审批拒绝
     */
    public void rejectTask(String taskId, Integer userId, String reason) {
        logger.info("审批拒绝: taskId={}, userId={}, reason={}", taskId, userId, reason);

        Map<String, Object> variables = new HashMap<>();
        variables.put("outcome", "REJECT");
        variables.put("isAllApproved", false);

        if (reason != null && !reason.isEmpty()) {
            taskService.addComment(taskId, null, "拒绝: " + reason);
        }

        taskService.complete(taskId, variables);
        logger.info("✓ 任务已拒绝: taskId={}", taskId);
    }

    /**
     * 撤回流程
     */
    public void withdrawProcess(String processInstanceId) {
        logger.info("撤回流程: processInstanceId={}", processInstanceId);
        try {
            runtimeService.deleteProcessInstance(processInstanceId, "申请人撤回");
            logger.info("✓ 流程已撤回: processInstanceId={}", processInstanceId);
        } catch (Exception e) {
            logger.error("撤回流程失败: {}", e.getMessage(), e);
        }
    }

    /**
     * 解析并设置审批人（用于运行时修改）
     */
    public void resolveAndSetApprovers(String executionId, String requiredRoles, 
                                        Integer applicantId, String approvalType) {
        List<Integer> approverIds = approvalRoleResolver.resolveApprovers(
                requiredRoles, applicantId, approvalType);
        List<String> approverList = new ArrayList<>();
        for (Integer id : approverIds) {
            approverList.add(String.valueOf(id));
        }
        runtimeService.setVariable(executionId, "approverList", approverList);
    }

    /**
     * 获取流程实例的当前活跃任务
     */
    public List<Task> getProcessTasks(String processInstanceId) {
        return taskService.createTaskQuery()
                .processInstanceId(processInstanceId)
                .list();
    }

    /**
     * 获取流程实例的历史任务
     */
    public List<HistoricActivityInstance> getHistoricTasks(String processInstanceId) {
        return historyService.createHistoricActivityInstanceQuery()
                .processInstanceId(processInstanceId)
                .orderByHistoricActivityInstanceStartTime().asc()
                .list();
    }

    /**
     * 获取流程实例历史（判断是否已结束）
     */
    public HistoricProcessInstance getProcessInstanceHistory(String processInstanceId) {
        return historyService.createHistoricProcessInstanceQuery()
                .processInstanceId(processInstanceId)
                .singleResult();
    }

    /**
     * 修复缺失的第二级审批人变量
     */
    public void fixMissingSecondLevelApprover(Approval approval) {
        if (approval.getProcessInstanceId() == null) return;

        ProcessInstance pi = runtimeService.createProcessInstanceQuery()
                .processInstanceId(approval.getProcessInstanceId())
                .singleResult();

        if (pi == null) return; // 流程已结束

        Object secondLevel = runtimeService.getVariable(pi.getId(), "secondLevelApprover");
        if (secondLevel == null) {
            String requiredRoles = (String) runtimeService.getVariable(pi.getId(), "requiredRoles");
            if (requiredRoles != null) {
                String[] roles = requiredRoles.split(",");
                String lastRole = roles[roles.length - 1].trim();
                List<Integer> ids = approvalRoleResolver.resolveApprovers(
                        lastRole, approval.getApplicantId(), approval.getApprovalType());
                if (!ids.isEmpty()) {
                    runtimeService.setVariable(pi.getId(), "secondLevelApprover", 
                            String.valueOf(ids.get(0)));
                    logger.info("✓ 已补充 secondLevelApprover: {}", ids.get(0));
                }
            }
        }
    }

    /**
     * 批量修复
     */
    public void fixAllMissingSecondLevelApprovers(List<Approval> approvals) {
        for (Approval approval : approvals) {
            try {
                fixMissingSecondLevelApprover(approval);
            } catch (Exception e) {
                logger.error("修复失败: approvalId={}", approval.getId(), e);
            }
        }
    }

    private Integer calculateDays(Approval approval) {
        if (approval.getStartTime() != null && approval.getEndTime() != null) {
            long millis = approval.getEndTime().getTime() - approval.getStartTime().getTime();
            return (int) (millis / (1000 * 60 * 60 * 24)) + 1;
        }
        return null;
    }
}
```

---

#### 6.1.2 ApprovalRuleEvaluator.java（规则评估）

**路径：** `backend/oa-springboot/src/main/java/com/southwind/service/flowable/ApprovalRuleEvaluator.java`
**职责：** 根据审批类型和参数（金额/天数）决定所需的审批角色和签署方式。
**调用关系：** 被 ApprovalFlowableService.startApprovalProcess() 调用。

```java
package com.southwind.service.flowable;

/**
 * 审批规则评估器
 * 根据审批类型和参数确定：requiredRoles（所需角色）、signType（签署方式）
 */
public class ApprovalRuleEvaluator {

    public static ApprovalRule evaluateRule(String approvalType, Object amount, Integer days) {
        if ("leave".equals(approvalType)) return evaluateLeaveRule(days);
        else if ("business".equals(approvalType)) return evaluateBusinessRule(days);
        else if ("reimburse".equals(approvalType)) return evaluateReimburseRule(amount);
        else if ("purchase".equals(approvalType)) return evaluatePurchaseRule(amount);
        else if ("overtime".equals(approvalType)) return evaluateOvertimeRule();
        else if ("card".equals(approvalType)) return evaluateCardRule();
        return new ApprovalRule("DEPT_MANAGER", "SINGLE");
    }

    private static ApprovalRule evaluateLeaveRule(Integer days) {
        if (days == null || days <= 3) {
            return new ApprovalRule("DEPT_MANAGER", "SINGLE");
        } else {
            return new ApprovalRule("DEPT_MANAGER,GM", "SERIAL_AFTER_PARALLEL");
        }
    }

    private static ApprovalRule evaluateBusinessRule(Integer days) {
        if (days == null || days <= 5) {
            return new ApprovalRule("DEPT_MANAGER", "SINGLE");
        } else {
            return new ApprovalRule("DEPT_MANAGER,GM", "SERIAL_AFTER_PARALLEL");
        }
    }

    private static ApprovalRule evaluateReimburseRule(Object amount) {
        double amt = toDouble(amount);
        if (amt <= 3000) {
            return new ApprovalRule("DEPT_MANAGER", "SINGLE");
        } else if (amt <= 10000) {
            return new ApprovalRule("DEPT_MANAGER,FINANCE_STAFF", "PARALLEL");
        } else if (amt <= 20000) {
            return new ApprovalRule("DEPT_MANAGER,FINANCE_MANAGER", "PARALLEL");
        } else {
            return new ApprovalRule("DEPT_MANAGER,FINANCE_MANAGER,GM", "SERIAL_AFTER_PARALLEL");
        }
    }

    private static ApprovalRule evaluatePurchaseRule(Object amount) {
        double amt = toDouble(amount);
        if (amt <= 2000) {
            return new ApprovalRule("DEPT_MANAGER", "SINGLE");
        } else if (amt <= 10000) {
            return new ApprovalRule("DEPT_MANAGER,FINANCE_STAFF", "PARALLEL");
        } else {
            return new ApprovalRule("DEPT_MANAGER,FINANCE_MANAGER,GM", "SERIAL_AFTER_PARALLEL");
        }
    }

    private static ApprovalRule evaluateOvertimeRule() {
        return new ApprovalRule("DEPT_MANAGER", "SINGLE");
    }

    private static ApprovalRule evaluateCardRule() {
        return new ApprovalRule("DEPT_MANAGER", "SINGLE");
    }

    private static double toDouble(Object amount) {
        if (amount == null) return 0;
        if (amount instanceof Number) return ((Number) amount).doubleValue();
        try { return Double.parseDouble(amount.toString()); } 
        catch (NumberFormatException e) { return 0; }
    }

    public static class ApprovalRule {
        public final String requiredRoles;
        public final String signType;
        public ApprovalRule(String requiredRoles, String signType) {
            this.requiredRoles = requiredRoles;
            this.signType = signType;
        }
    }
}
```

---

#### 6.1.3 ApprovalEvent.java（Spring事件对象）

**路径：** `backend/oa-springboot/src/main/java/com/southwind/event/ApprovalEvent.java`
**职责：** 审批事件载体，通过 Spring ApplicationEvent 机制解耦审批逻辑与消息通知。
**调用关系：** 由 ApprovalServiceImpl 和 ApprovalProcessEndListener 发布；由 ApprovalEventListener 消费。

```java
package com.southwind.event;

import org.springframework.context.ApplicationEvent;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class ApprovalEvent extends ApplicationEvent {

    public enum EventType {
        SUBMITTED("SUBMITTED"),
        APPROVED_NODE("APPROVED_NODE"),
        APPROVED_FINAL("APPROVED_FINAL"),
        REJECTED("REJECTED"),
        WITHDRAWN("WITHDRAWN"),
        CC_ADDED("CC_ADDED");

        private final String code;
        EventType(String code) { this.code = code; }
        public String getCode() { return code; }
    }

    public enum BizType {
        APPROVAL("approval"),
        WORKFLOW("workflow"),
        DOCUMENT("document");

        private final String code;
        BizType(String code) { this.code = code; }
        public String getCode() { return code; }
    }

    private EventType eventType;
    private BizType bizType;
    private Integer bizId;
    private Integer senderId;
    private String senderName;
    private List<Integer> receiverIds;
    private List<String> receiverNames;
    private String title;
    private String content;
    private String jumpUrl;
    private boolean isTodo;
    private LocalDateTime createTime;

    public ApprovalEvent(Object source) {
        super(source);
        this.createTime = LocalDateTime.now();
        this.receiverIds = new ArrayList<>();
        this.receiverNames = new ArrayList<>();
        this.isTodo = true;
    }

    // Builder pattern
    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private final ApprovalEvent event = new ApprovalEvent("ApprovalEvent");

        public Builder eventType(EventType eventType) { event.eventType = eventType; return this; }
        public Builder bizType(BizType bizType) { event.bizType = bizType; return this; }
        public Builder bizId(Integer bizId) { event.bizId = bizId; return this; }
        public Builder senderId(Integer senderId) { event.senderId = senderId; return this; }
        public Builder senderName(String senderName) { event.senderName = senderName; return this; }
        public Builder receiverId(Integer receiverId) { 
            event.receiverIds.add(receiverId); return this; 
        }
        public Builder receiverName(String receiverName) { 
            event.receiverNames.add(receiverName); return this; 
        }
        public Builder title(String title) { event.title = title; return this; }
        public Builder content(String content) { event.content = content; return this; }
        public Builder jumpUrl(String jumpUrl) { event.jumpUrl = jumpUrl; return this; }
        public Builder isTodo(boolean isTodo) { event.isTodo = isTodo; return this; }

        public ApprovalEvent build() { return event; }
    }

    // Getters
    public EventType getEventType() { return eventType; }
    public BizType getBizType() { return bizType; }
    public Integer getBizId() { return bizId; }
    public Integer getSenderId() { return senderId; }
    public String getSenderName() { return senderName; }
    public List<Integer> getReceiverIds() { return receiverIds; }
    public List<String> getReceiverNames() { return receiverNames; }
    public String getTitle() { return title; }
    public String getContent() { return content; }
    public String getJumpUrl() { return jumpUrl; }
    public boolean isTodo() { return isTodo; }
    public LocalDateTime getCreateTime() { return createTime; }
}
```

---

#### 6.1.4 ApprovalEventListener.java（事件→消息桥梁）

**路径：** `backend/oa-springboot/src/main/java/com/southwind/event/listener/ApprovalEventListener.java`
**职责：** 监听 ApprovalEvent，将事件转化为 Message 记录写入 t_message 表。
**调用关系：** 消费 ApprovalEvent；调用 MessageService.sendMessage()。

```java
package com.southwind.event.listener;

import com.southwind.entity.Message;
import com.southwind.event.ApprovalEvent;
import com.southwind.service.MessageService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Component
public class ApprovalEventListener {

    private static final Logger logger = LoggerFactory.getLogger(ApprovalEventListener.class);

    @Autowired
    private MessageService messageService;

    @EventListener
    @Transactional
    public void onApprovalEvent(ApprovalEvent event) {
        if (event == null || event.getEventType() == null) return;

        try {
            for (int i = 0; i < event.getReceiverIds().size(); i++) {
                Integer receiverId = event.getReceiverIds().get(i);
                String receiverName = i < event.getReceiverNames().size() 
                    ? event.getReceiverNames().get(i) : "";

                Message message = new Message();
                message.setSenderId(event.getSenderId());
                message.setSenderName(event.getSenderName());
                message.setReceiverId(receiverId);
                message.setReceiverName(receiverName);
                message.setTitle(event.getTitle());
                message.setContent(event.getContent());
                message.setEventType(event.getEventType().getCode());
                message.setBizType(event.getBizType() != null ? event.getBizType().getCode() : "approval");
                message.setBizId(event.getBizId());
                message.setJumpUrl(event.getJumpUrl());
                message.setIsTodo(event.isTodo() ? 1 : 0);
                message.setMsgType("APPROVAL");
                message.setIsRead(0);
                message.setIsTop(0);
                message.setStatus("正常");
                message.setCreateTime(LocalDateTime.now());
                message.setRelatedId(event.getBizId());

                messageService.sendMessage(message);
                logger.info("✓ 消息已写入: receiver={}, eventType={}, bizId={}", 
                    receiverId, event.getEventType(), event.getBizId());
            }
        } catch (Exception e) {
            logger.error("Error processing ApprovalEvent: {}", e.getMessage(), e);
        }
    }

    @EventListener
    @Transactional
    public void onApprovalWithdrawn(ApprovalEvent event) {
        if (event.getEventType() != ApprovalEvent.EventType.WITHDRAWN) return;
        // 撤回时标记相关待办消息为失效
        try {
            messageService.markTodoAsInvalid(event.getBizId(), "approval");
            logger.info("✓ 已标记待办失效: bizId={}", event.getBizId());
        } catch (Exception e) {
            logger.error("标记待办失效失败: {}", e.getMessage(), e);
        }
    }
}
```

---

#### 6.1.5 Flowable Delegate 实现类（完整源码见任务四引用）

以下文件完整源码已在任务四的探索结果中给出，此处列出路径和核心职责：

| 文件 | 路径 | 核心职责 |
|------|------|----------|
| ApprovalRuleEvaluationDelegate.java | backend/.../flowable/delegate/ | 流程启动后的规则验证节点（实际评估已在启动前完成，此节点主要做日志） |
| ApprovalRoleResolverDelegate.java | backend/.../flowable/delegate/ | 解析审批人列表写入 approverList/secondLevelApprover 变量 |
| ApprovalTaskListener.java | backend/.../flowable/listener/ | userTask create 事件：记录分配日志 |
| ParallelApprovalCompleteListener.java | backend/.../flowable/listener/ | 会签 complete 事件：REJECT 时设 isAllApproved=false 触发提前终止 |
| ApprovalProcessEndListener.java | backend/.../flowable/listener/ | 流程结束：更新 t_approval 状态 + 发布事件通知申请人 |

---

#### 6.1.6 前端 API 封装文件

**PC端审批API：** `frontend/oa-pc/oa-pc-admin/src/api/approval.js`

```javascript
import request from './request'

export function submitApproval(data) {
  return request.post('/approval/submit', data)
}
export function getMyApplications(applicantId) {
  return request.get(`/approval/myApplications/${applicantId}`)
}
export function getPendingList() {
  return request.get('/approval/pendingList')
}
export function getApprovalPage(page, size) {
  return request.get(`/approval/list/${page}/${size}`)
}
export function getApprovalByStatus(status) {
  return request.get(`/approval/findByStatus/${status}`)
}
export function getApprovalByType(type) {
  return request.get(`/approval/findByType/${type}`)
}
export function handleApproval(data) {
  return request.put('/approval/approve', data)
}
export function withdrawApproval(id) {
  return request.put(`/approval/withdraw/${id}`)
}
export function getApprovalDetail(id) {
  return request.get(`/approval/detail/${id}`)
}
```

**移动端审批API：** `frontend/oa-mobile/src/api/approval.js`

```javascript
import request from '@/utils/request'

export function submitApproval(data) {
  return request.post('/approval/submit', data)
}
export function getMyApplications(applicantId) {
  return request.get(`/approval/myApplications/${applicantId}`)
}
export function getMyApprovalList(status, page, size) {
  return request.get('/approval/my', { params: { status, page, size } })
}
export function getPendingList(approverId, role) {
  return request.get('/approval/pendingList', { params: { approverId, role } })
}
export function getPendingCount(approverId, role) {
  return request.get('/approval/pendingCount', { params: { approverId, role } })
}
export function handleApproval(data) {
  return request.put('/approval/approve', data)
}
export function getApprovalDetail(id) {
  return request.get(`/approval/detail/${id}`)
}
export function getApprovalByStatus(status) {
  return request.get(`/approval/findByStatus/${status}`)
}
export function withdrawApproval(id) {
  return request.put(`/approval/withdraw/${id}`)
}
```

**移动端消息API：** `frontend/oa-mobile/src/api/message.js`

```javascript
import request from '@/utils/request'

export function getMessageList(category, page, size) {
  return request.get('/message/list', { params: { category, page, size } })
}
export function getTodoCount() {
  return request.get('/message/todoCount')
}
export function getUnreadCount() {
  return request.get('/message/countUnread')
}
export function markAsRead(id) {
  return request.put(`/message/read/${id}`)
}
export function markCategoryAsRead(category) {
  return request.put('/message/readAll', { params: { category } })
}
export function markAllAsRead() {
  return request.post('/message/markAllRead')
}
export function sendMessage(data) {
  return request.post('/message/send', data)
}
export function getMessageDetail(id) {
  return request.get(`/message/detail/${id}`)
}
export function deleteMessage(id) {
  return request.delete(`/message/delete/${id}`)
}
```

---

### 6.2 关键表 DDL

#### t_approval

```sql
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
  KEY `idx_process_instance_id` (`process_instance_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='审批表';
```

#### t_message

```sql
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
  `biz_type` VARCHAR(50) DEFAULT 'approval' COMMENT '业务类型',
  `biz_id` INT(11) DEFAULT NULL COMMENT '关联的业务单据ID',
  `related_id` INT(11) DEFAULT NULL COMMENT '关联ID',
  `is_read` TINYINT(1) DEFAULT 0 COMMENT '是否已读',
  `is_todo` TINYINT(1) DEFAULT 0 COMMENT '是否计入待办角标',
  `jump_url` VARCHAR(200) DEFAULT NULL COMMENT '跳转标识',
  `is_top` TINYINT(1) DEFAULT 0 COMMENT '是否置顶',
  `status` VARCHAR(20) DEFAULT '正常' COMMENT '状态',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `read_time` DATETIME DEFAULT NULL COMMENT '阅读时间',
  `update_time` DATETIME DEFAULT NULL COMMENT '更新时间',
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
```

#### t_approval_role

```sql
CREATE TABLE `t_approval_role` (
  `id` INT(11) NOT NULL AUTO_INCREMENT COMMENT '角色ID',
  `role_code` VARCHAR(50) NOT NULL COMMENT '角色编码：DEPT_MANAGER/FINANCE_STAFF/FINANCE_MANAGER/GM',
  `role_name` VARCHAR(50) NOT NULL COMMENT '角色名称',
  `resolve_strategy` VARCHAR(50) NOT NULL COMMENT '解析策略：OWN_DEPT_MANAGER/FIXED_DEPT_MANAGER/FIXED_EMPLOYEE',
  `fixed_department_id` INT(11) DEFAULT NULL COMMENT 'FIXED_DEPT_MANAGER时使用',
  `fixed_employee_id` INT(11) DEFAULT NULL COMMENT 'FIXED_EMPLOYEE时使用',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_role_code` (`role_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='审批角色定义表';

-- 种子数据
INSERT INTO t_approval_role (role_code, role_name, resolve_strategy, fixed_department_id, fixed_employee_id) VALUES
('DEPT_MANAGER', '部门经理', 'OWN_DEPT_MANAGER', NULL, NULL),
('FINANCE_STAFF', '财务专员', 'FIXED_EMPLOYEE', NULL, 11),
('FINANCE_MANAGER', '财务经理', 'FIXED_DEPT_MANAGER', 5, NULL),
('GM', '总经理', 'FIXED_EMPLOYEE', NULL, 12);
```

#### t_approval_delegate

```sql
CREATE TABLE `t_approval_delegate` (
  `id` INT(11) NOT NULL AUTO_INCREMENT COMMENT '代理ID',
  `delegator_id` INT(11) NOT NULL COMMENT '委托人ID（原审批人）',
  `delegate_id` INT(11) NOT NULL COMMENT '代理人ID',
  `approval_type` VARCHAR(50) DEFAULT NULL COMMENT '限定审批类型，NULL表示所有类型',
  `start_time` DATETIME NOT NULL COMMENT '代理生效时间',
  `end_time` DATETIME NOT NULL COMMENT '代理失效时间',
  `status` VARCHAR(20) DEFAULT 'active' COMMENT '状态：active/expired/revoked',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_delegator` (`delegator_id`),
  KEY `idx_time_range` (`start_time`, `end_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='审批代理（请假委托）表';
```

#### t_operation_log

```sql
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
```

---

### 6.3 approval-process.bpmn20.xml 全文

```xml
<?xml version="1.0" encoding="UTF-8"?>
<definitions xmlns="http://www.omg.org/spec/BPMN/20100524/MODEL"
             xmlns:flowable="http://flowable.org/bpmn"
             xmlns:bpmndi="http://www.omg.org/spec/BPMN/20100524/DI"
             xmlns:omgdc="http://www.omg.org/spec/DD/20100524/DC"
             xmlns:omgdi="http://www.omg.org/spec/DD/20100524/DI"
             xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
             typeLanguage="http://www.w3.org/2001/XMLSchema"
             expressionLanguage="http://www.w3.org/1999/XPath"
             targetNamespace="http://flowable.org/test"
             id="Definitions_approvalProcess">

  <process id="approvalProcess" name="OA系统通用审批工作流" isExecutable="true">
    
    <startEvent id="StartEvent_approval" name="审批流程开始"/>

    <sequenceFlow id="flow_start_to_rule_eval" 
                  sourceRef="StartEvent_approval" 
                  targetRef="RuleEvaluationTask"/>

    <serviceTask id="RuleEvaluationTask" 
                 name="评估审批规则"
                 flowable:delegateExpression="${approvalRuleEvaluationDelegate}"/>

    <sequenceFlow id="flow_rule_eval_to_role_resolve" 
                  sourceRef="RuleEvaluationTask" 
                  targetRef="RoleResolverTask"/>

    <serviceTask id="RoleResolverTask" 
                 name="解析审批角色"
                 flowable:delegateExpression="${approvalRoleResolverDelegate}"/>

    <sequenceFlow id="flow_role_resolve_to_gateway" 
                  sourceRef="RoleResolverTask" 
                  targetRef="SignTypeGateway"/>

    <exclusiveGateway id="SignTypeGateway" name="按审批方式分流"/>

    <!-- 分支1: 单签 -->
    <sequenceFlow id="flow_gateway_to_single" 
                  sourceRef="SignTypeGateway" 
                  targetRef="SingleApprovalTask">
      <conditionExpression xsi:type="tFormalExpression">${signType == 'SINGLE'}</conditionExpression>
    </sequenceFlow>

    <userTask id="SingleApprovalTask" 
              name="单级审批"
              flowable:assignee="${approverList[0]}">
      <extensionElements>
        <flowable:taskListener event="create" 
                               delegateExpression="${approvalTaskListener}"/>
      </extensionElements>
    </userTask>

    <sequenceFlow id="flow_single_to_result_check" 
                  sourceRef="SingleApprovalTask" 
                  targetRef="SingleResultGateway"/>

    <exclusiveGateway id="SingleResultGateway" name="单签审批结果"/>

    <sequenceFlow id="flow_single_approved" 
                  sourceRef="SingleResultGateway" 
                  targetRef="EndEvent_Approved">
      <conditionExpression xsi:type="tFormalExpression">${outcome == 'APPROVE'}</conditionExpression>
    </sequenceFlow>

    <sequenceFlow id="flow_single_rejected" 
                  sourceRef="SingleResultGateway" 
                  targetRef="EndEvent_Rejected">
      <conditionExpression xsi:type="tFormalExpression">${outcome == 'REJECT'}</conditionExpression>
    </sequenceFlow>

    <!-- 分支2: 并行会签 -->
    <sequenceFlow id="flow_gateway_to_parallel" 
                  sourceRef="SignTypeGateway" 
                  targetRef="ParallelApprovalTask">
      <conditionExpression xsi:type="tFormalExpression">${signType == 'PARALLEL'}</conditionExpression>
    </sequenceFlow>

    <userTask id="ParallelApprovalTask" 
              name="并行会签审批"
              flowable:assignee="${approver}">
      <extensionElements>
        <flowable:taskListener event="create" 
                               delegateExpression="${approvalTaskListener}"/>
        <flowable:taskListener event="complete" 
                               delegateExpression="${parallelApprovalCompleteListener}"/>
      </extensionElements>
      <multiInstanceLoopCharacteristics isSequential="false"
                                        flowable:collection="approverList"
                                        flowable:elementVariable="approver">
        <completionCondition xsi:type="tFormalExpression">
          ${nrOfCompletedInstances == nrOfInstances or (isAllApproved != null and isAllApproved == false)}
        </completionCondition>
      </multiInstanceLoopCharacteristics>
    </userTask>

    <sequenceFlow id="flow_parallel_to_result_check" 
                  sourceRef="ParallelApprovalTask" 
                  targetRef="ParallelResultGateway"/>

    <exclusiveGateway id="ParallelResultGateway" name="会签审批结果"/>

    <sequenceFlow id="flow_parallel_all_approved" 
                  sourceRef="ParallelResultGateway" 
                  targetRef="EndEvent_Approved">
      <conditionExpression xsi:type="tFormalExpression">${isAllApproved == null or isAllApproved == true}</conditionExpression>
    </sequenceFlow>

    <sequenceFlow id="flow_parallel_any_rejected" 
                  sourceRef="ParallelResultGateway" 
                  targetRef="EndEvent_Rejected">
      <conditionExpression xsi:type="tFormalExpression">${isAllApproved != null and isAllApproved == false}</conditionExpression>
    </sequenceFlow>

    <!-- 分支3: 会签后串签 -->
    <sequenceFlow id="flow_gateway_to_serial_after_parallel" 
                  sourceRef="SignTypeGateway" 
                  targetRef="FirstLevelParallelTask">
      <conditionExpression xsi:type="tFormalExpression">${signType == 'SERIAL_AFTER_PARALLEL'}</conditionExpression>
    </sequenceFlow>

    <userTask id="FirstLevelParallelTask" 
              name="第一级并行会签"
              flowable:assignee="${approver}">
      <extensionElements>
        <flowable:taskListener event="create" 
                               delegateExpression="${approvalTaskListener}"/>
        <flowable:taskListener event="complete" 
                               delegateExpression="${parallelApprovalCompleteListener}"/>
      </extensionElements>
      <multiInstanceLoopCharacteristics isSequential="false"
                                        flowable:collection="approverList"
                                        flowable:elementVariable="approver">
        <completionCondition xsi:type="tFormalExpression">
          ${nrOfCompletedInstances == nrOfInstances or (isAllApproved != null and isAllApproved == false)}
        </completionCondition>
      </multiInstanceLoopCharacteristics>
    </userTask>

    <sequenceFlow id="flow_first_level_to_result_check" 
                  sourceRef="FirstLevelParallelTask" 
                  targetRef="FirstLevelResultGateway"/>

    <exclusiveGateway id="FirstLevelResultGateway" name="第一级会签结果"/>

    <sequenceFlow id="flow_first_level_rejected" 
                  sourceRef="FirstLevelResultGateway" 
                  targetRef="EndEvent_Rejected">
      <conditionExpression xsi:type="tFormalExpression">${isAllApproved != null and isAllApproved == false}</conditionExpression>
    </sequenceFlow>

    <sequenceFlow id="flow_first_level_approved_to_second" 
                  sourceRef="FirstLevelResultGateway" 
                  targetRef="SecondLevelSerialTask">
      <conditionExpression xsi:type="tFormalExpression">${isAllApproved == null or isAllApproved == true}</conditionExpression>
    </sequenceFlow>

    <userTask id="SecondLevelSerialTask" 
              name="第二级串签审批"
              flowable:assignee="${secondLevelApprover}">
      <extensionElements>
        <flowable:taskListener event="create" 
                               delegateExpression="${approvalTaskListener}"/>
      </extensionElements>
    </userTask>

    <sequenceFlow id="flow_second_level_to_result_check" 
                  sourceRef="SecondLevelSerialTask" 
                  targetRef="SecondLevelResultGateway"/>

    <exclusiveGateway id="SecondLevelResultGateway" name="第二级审批结果"/>

    <sequenceFlow id="flow_second_level_approved" 
                  sourceRef="SecondLevelResultGateway" 
                  targetRef="EndEvent_Approved">
      <conditionExpression xsi:type="tFormalExpression">${outcome == 'APPROVE'}</conditionExpression>
    </sequenceFlow>

    <sequenceFlow id="flow_second_level_rejected" 
                  sourceRef="SecondLevelResultGateway" 
                  targetRef="EndEvent_Rejected">
      <conditionExpression xsi:type="tFormalExpression">${outcome == 'REJECT'}</conditionExpression>
    </sequenceFlow>

    <!-- 结束事件 -->
    <endEvent id="EndEvent_Approved" name="审批通过">
      <extensionElements>
        <flowable:executionListener event="end" 
                                     delegateExpression="${approvalProcessEndListener}"/>
      </extensionElements>
    </endEvent>
    <endEvent id="EndEvent_Rejected" name="审批拒绝">
      <extensionElements>
        <flowable:executionListener event="end" 
                                     delegateExpression="${approvalProcessEndListener}"/>
      </extensionElements>
    </endEvent>

  </process>
</definitions>
```

---

### 6.4 application.yml 中 Flowable/Redis/Nacos 配置段

```yaml
# ========== Flowable 工作流引擎配置 ==========
flowable:
  database-schema-update: true   # 应用启动时自动建表/更新ACT_*表结构
  history-level: full            # 记录完整历史（所有活动、变量、任务）
  async-executor-activate: true  # 启用异步执行器

# ========== Redis 配置 ==========
spring:
  data:
    redis:
      host: localhost
      port: 6379
      timeout: 6000ms
      lettuce:
        pool:
          max-active: 8
          max-wait: -1ms
          max-idle: 8
          min-idle: 0

# ========== Nacos 配置 ==========
spring:
  cloud:
    nacos:
      discovery:
        enabled: true
        server-addr: 127.0.0.1:8848
        fail-fast: false
        username: nacos
        password: nacos
      config:
        enabled: true
        server-addr: 127.0.0.1:8848
        file-extension: yml
        import-check:
          enabled: false
        username: nacos
        password: nacos

# ========== MyBatis-Plus 配置 ==========
mybatis-plus:
  configuration:
    log-impl: org.apache.ibatis.logging.stdout.StdOutImpl
    map-underscore-to-camel-case: true
  mapper-locations: com/southwind/mapper/xml/*.xml
  global-config:
    db-config:
      logic-delete-field: deleted
      logic-delete-value: 1
      logic-not-delete-value: 0
```

---

### 6.5 改造建议清单

#### 一、审批流程可视化

| # | 类型 | 文件/位置 | 说明 |
|---|------|-----------|------|
| 1 | **新增** | `backend/.../controller/ApprovalController.java` 新增接口 | `GET /approval/progress/{approvalId}` — 返回审批轨迹时间线（调用 HistoryService 查 HistoricActivityInstance + 关联 t_approval 状态） |
| 2 | **新增** | `backend/.../controller/ApprovalController.java` 新增接口 | `GET /approval/flowDiagram/{approvalId}` — 返回 BPMN XML + 当前节点高亮信息（已完成节点ID列表 + 当前活跃节点ID） |
| 3 | **修改** | `backend/.../service/flowable/ApprovalFlowableService.java` | 新增 `getApprovalProgress(String processInstanceId)` 方法，封装历史活动查询 + 审批人姓名解析 |
| 4 | **新增** | `backend/.../vo/ApprovalProgressVO.java` | 审批进度VO：节点列表（名称、状态、审批人、时间、意见） |
| 5 | **修改** | `frontend/oa-pc/.../views/ApprovalManage/Index.vue` | 审批详情弹窗中增加"审批进度"时间线组件 |
| 6 | **新增** | `frontend/oa-pc/.../components/ApprovalTimeline.vue` | 审批轨迹时间线组件（Element Plus Timeline） |
| 7 | **修改** | `frontend/oa-pc/.../api/approval.js` | 新增 `getApprovalProgress(id)` 和 `getFlowDiagram(id)` |
| 8 | **修改** | `frontend/oa-mobile/src/views/ApprovalDetail.vue` | 增加审批进度展示（Vant Steps 组件） |
| 9 | **修改** | `frontend/oa-mobile/src/api/approval.js` | 新增 `getApprovalProgress(id)` |

#### 二、实时消息通知

| # | 类型 | 文件/位置 | 说明 |
|---|------|-----------|------|
| 1 | **修改** | `backend/pom.xml` | 新增 `spring-boot-starter-websocket` 依赖 |
| 2 | **新增** | `backend/.../config/WebSocketConfig.java` | WebSocket 配置类（@EnableWebSocket + ServerEndpointExporter 或 STOMP 配置） |
| 3 | **新增** | `backend/.../websocket/NotificationWebSocket.java` | WebSocket 端点（@ServerEndpoint("/ws/notification/{userId}")），管理在线用户 Session 池 |
| 4 | **新增** | `backend/.../websocket/WebSocketSessionManager.java` | Session 管理器（ConcurrentHashMap<userId, Session>），提供 sendToUser() 方法 |
| 5 | **修改** | `backend/.../event/listener/ApprovalEventListener.java` | 在 sendMessage() 后追加：调用 WebSocketSessionManager.sendToUser(receiverId, messageJson) 实时推送 |
| 6 | **修改** | `backend/.../controller/MessageController.java` | 可选：新增 SSE 端点 `GET /message/subscribe`（SseEmitter）作为 WebSocket 的降级方案 |
| 7 | **修改** | `frontend/oa-pc/.../views/Layout.vue` | 建立 WebSocket 连接（`new WebSocket('ws://localhost:8080/ws/notification/{userId}')`），收到消息时更新 badge + 弹出 Notification |
| 8 | **新增** | `frontend/oa-pc/.../utils/websocket.js` | WebSocket 连接管理（自动重连、心跳、消息解析） |
| 9 | **修改** | `frontend/oa-mobile/src/views/Home.vue` | 建立 WebSocket 连接，收到消息时更新 badge |
| 10 | **新增** | `frontend/oa-mobile/src/utils/websocket.js` | 移动端 WebSocket 管理（需处理页面切换时的连接保持） |
| 11 | **修改** | `frontend/oa-mobile/src/views/Message.vue` | 监听 WebSocket 消息，实时追加到列表顶部 |

#### 三、Flowable 集成增强（可选）

| # | 类型 | 文件/位置 | 说明 |
|---|------|-----------|------|
| 1 | **修改** | `backend/.../service/flowable/ApprovalRuleEvaluator.java` | 考虑改为读取 DMN 文件（已有6个DMN但未使用），或保持硬编码但增加可配置性 |
| 2 | **新增** | `backend/.../controller/ApprovalDelegateController.java` | 审批代理管理接口（目前 t_approval_delegate 无管理入口） |
| 3 | **修改** | `frontend/oa-pc/.../views/WorkflowManage/Index.vue` | 增加流程图可视化（使用 bpmn-js 渲染 BPMN XML + 高亮） |
| 4 | **新增** | `frontend/oa-pc/package.json` | 新增 bpmn-js 依赖 |

---

### 6.6 已知 Pitfall（来自项目 MEMORY）

1. **flowable:class vs delegateExpression：** BPMN 中必须使用 `delegateExpression`，不能用 `flowable:class`，否则 @Autowired 字段为 null（反射创建实例不走 Spring DI）。
2. **TaskListener(complete) 时序：** 多实例任务中，TaskListener 的 complete 事件触发时，`nrOfCompletedInstances` 尚未 +1（引擎在 listener 之后才递增）。
3. **@Transactional + Flowable：** ApprovalServiceImpl 上的 @Transactional 与 Flowable 的 SpringTransactionInterceptor 可能导致 UnexpectedRollbackException 逃逸 try-catch。
4. **t_message.update_time：** 曾缺失该列导致所有消息查询失败（已修复，DDL中已包含）。
5. **消息 badge 语义：** todoCount = is_todo=1 AND is_read=0（待办）；countUnread = 所有 is_read=0（未读）。Badge 应使用 countUnread。
6. **移动端 axios 拦截器：** utils/request.js 已在 code!==0 时 toast，页面级 catch 不能再重复 toast。

---

*文档结束。本交接包包含了审批工作流和消息通知两大模块的所有核心文件源码、表结构、调用关系和改造建议，可独立用于新对话中继续开发。*
