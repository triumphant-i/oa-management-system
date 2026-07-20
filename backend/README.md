# OA办公管理系统 - 后端部分

## 项目说明
本项目是基于宿舍管理系统的后端改造，将其转换为OA办公自动化管理系统。

## 技术栈
- **框架**: Spring Boot 2.x
- **ORM**: MyBatis-Plus 3.x
- **数据库**: MySQL 8.0
- **构建工具**: Maven 3.6+

## 已完成的功能模块

### 1. 员工管理模块 (EmployeeController)
- 员工登录验证
- 员工信息的增删改查
- 分页查询和条件搜索
- 修改密码功能
- 按部门查询员工

### 2. 部门管理模块 (DepartmentController)
- 部门的增删改查
- 查询部门下的员工数量
- 删除部门前的员工检查

### 3. 审批管理模块 (ApprovalController)
- 提交申请（请假、出差、加班等）
- 查询我的申请
- 待审批列表管理
- 审批处理（通过/拒绝）
- 申请撤回功能

### 4. 公告管理模块 (AnnouncementController)
- 发布公告
- 公告的增删改查
- 公告置顶功能
- 按分类查询公告
- 撤回公告

### 5. 考勤管理模块 (AttendanceController)
- 签到/签退功能
- 今日考勤状态查询
- 考勤记录查询
- 迟到/早退判断
- 补卡申请

### 6. 会议室管理模块 (MeetingRoomController)
- 会议室的增删改查
- 查询可用会议室
- 更新会议室状态
- 按容量查询会议室

### 7. 会议预约模块 (MeetingController)
- 预约会议
- 时间冲突检测
- 查询我的会议
- 取消会议
- 查询会议室预约情况

### 8. 管理员模块 (AdminController)
- 管理员登录验证
- 管理员的增删改查

## 数据库设计

### 核心表结构
1. `t_employee` - 员工表
2. `t_department` - 部门表
3. `t_approval` - 审批表
4. `t_announcement` - 公告表
5. `t_attendance` - 考勤表
6. `t_meeting_room` - 会议室表
7. `t_meeting` - 会议预约表
8. `t_admin` - 管理员表

### 默认账号
- **管理员账号**: admin / 123456
- **员工账号**: zhangsan / 123456
- **员工账号**: lisi / 123456
- **员工账号**: wangwu / 123456

## API接口说明

### 公共返回格式
```json
{
  "code": 0,        // 0表示成功，-1表示失败
  "message": "成功", // 返回信息
  "data": {}        // 返回数据
}
```

### 主要接口

#### 1. 登录接口
```
POST /admin/login      # 管理员登录
POST /employee/login   # 员工登录

请求参数：
{
  "username": "admin",
  "password": "123456"
}
```

#### 2. 员工管理接口
```
GET  /employee/list/{page}/{size}      # 分页查询员工
POST /employee/save                     # 添加员工
PUT  /employee/update                   # 更新员工
DELETE /employee/deleteById/{id}        # 删除员工
GET  /employee/findById/{id}            # 根据ID查询
```

#### 3. 审批管理接口
```
POST /approval/submit                   # 提交申请
GET  /approval/myApplications/{id}      # 查询我的申请
GET  /approval/pendingList              # 待审批列表
PUT  /approval/approve                  # 审批处理
```

#### 4. 考勤管理接口
```
POST /attendance/checkIn                # 签到
POST /attendance/checkOut               # 签退
GET  /attendance/todayStatus/{id}       # 今日状态
GET  /attendance/myRecords/{id}         # 我的考勤记录
```

#### 5. 会议室接口
```
GET  /meetingRoom/list                  # 查询所有会议室
GET  /meetingRoom/available             # 查询可用会议室
POST /meeting/book                      # 预约会议
GET  /meeting/myMeetings/{id}           # 我的会议
```

## 启动步骤

### 1. 配置数据库
修改 `src/main/resources/application.yml`：
```yaml
spring:
  datasource:
    driver-class-name: com.mysql.cj.jdbc.Driver
    url: jdbc:mysql://localhost:3306/oa_system?useUnicode=true&characterEncoding=utf-8&useSSL=false&serverTimezone=Asia/Shanghai
    username: root
    password: your_password
```

### 2. 初始化数据库
执行数据库脚本：
```bash
mysql -u root -p < database/oa_system.sql
```

### 3. 启动项目
```bash
# 进入项目目录
cd backend/oa-springboot

# 编译项目
mvn clean install

# 启动项目
mvn spring-boot:run
```

### 4. 验证启动
访问：http://localhost:8080/employee/list/1/10

## 项目结构
```
oa-springboot/
├── src/main/java/com/southwind/
│   ├── controller/          # 控制器层
│   │   ├── AdminController.java
│   │   ├── EmployeeController.java
│   │   ├── DepartmentController.java
│   │   ├── ApprovalController.java
│   │   ├── AnnouncementController.java
│   │   ├── AttendanceController.java
│   │   ├── MeetingRoomController.java
│   │   └── MeetingController.java
│   ├── service/             # 服务层
│   ├── mapper/              # 数据访问层
│   ├── entity/              # 实体类
│   ├── form/                # 表单类
│   ├── vo/                  # 视图对象
│   └── util/                # 工具类
└── src/main/resources/
    ├── application.yml      # 配置文件
    └── mapper/              # MyBatis映射文件
```

## 注意事项

1. **跨域配置**: 已在Spring Boot中配置CORS，允许前端访问
2. **事务管理**: Service层方法已添加@Transactional注解
3. **异常处理**: 统一使用ResultVO返回结果
4. **日志记录**: 建议在关键操作处添加日志记录

## 后续优化建议

1. 添加JWT token认证
2. 添加权限管理（RBAC）
3. 添加日志审计功能
4. 优化数据库索引
5. 添加缓存机制（Redis）
6. 添加文件上传功能
7. 添加消息推送功能
