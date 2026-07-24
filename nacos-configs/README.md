
# Nacos 配置指南

## 1. Nacos 安装与启动

### 下载 Nacos
访问 Nacos 官网下载页面：https://nacos.io/zh-cn/docs/quick-start.html

下载最新版本的 Nacos（推荐使用 Nacos 2.x 版本）

### 启动 Nacos（单机模式）

Windows 系统：
```bash
cd nacos/bin
startup.cmd -m standalone
```

Linux/Mac 系统：
```bash
cd nacos/bin
sh startup.sh -m standalone
```

### 访问 Nacos 控制台
启动成功后，在浏览器中访问：
http://localhost:8848/nacos

默认账号密码：
- 用户名：nacos
- 密码：nacos

---

## 2. 导入配置文件

### 方式一：手动在控制台创建配置

#### 第一步：创建 `common-config.yml`

1. 登录 Nacos 控制台
2. 点击左侧菜单「配置管理」→「配置列表」
3. 点击右上角「+」号创建配置
4. 填写以下信息：
   - **Data ID**: `common-config.yml`
   - **Group**: `DEFAULT_GROUP`
   - **配置格式**: `YAML`
   - **配置内容**: 复制 `common-config.yml` 文件的内容
5. 点击「发布」

#### 第二步：创建 `oa-management-service.yml`

1. 在配置列表页面，再次点击右上角「+」号
2. 填写以下信息：
   - **Data ID**: `oa-management-service.yml`
   - **Group**: `DEFAULT_GROUP`
   - **配置格式**: `YAML`
   - **配置内容**: 复制 `oa-management-service.yml` 文件的内容
3. 点击「发布」

---

## 3. 配置说明

### 配置文件结构

本项目使用两个配置文件：

1. **common-config.yml** - 公共配置（多个服务可共享）
   - 日志配置
   - 文件上传配置

2. **oa-management-service.yml** - 应用专属配置
   - 服务端口
   - 数据库配置
   - MyBatis-Plus 配置
   - Flowable 配置

### Bootstrap 配置说明

项目中的 `bootstrap.yml` 已经配置好：
- Nacos 地址：localhost:8848
- 命名空间：public
- 分组：DEFAULT_GROUP
- 配置格式：YAML
- 支持动态刷新

---

## 4. 验证配置

### 启动后端项目

确保 Nacos 已启动并配置导入成功后，启动 Spring Boot 项目。

项目启动时会从 Nacos 拉取配置，你会在控制台看到类似日志：
```
Located property source: [BootstrapPropertySource {name='bootstrapProperties-oa-management-service.yml,DEFAULT_GROUP'}, BootstrapPropertySource {name='bootstrapProperties-common-config.yml,DEFAULT_GROUP'}]
```

### 查看服务注册

在 Nacos 控制台左侧菜单点击「服务管理」→「服务列表」，你应该能看到 `oa-management-service` 服务已注册成功。

---

## 5. 动态刷新配置

本项目已启用配置动态刷新功能。

如需测试动态刷新：
1. 在 Nacos 控制台修改配置并发布
2. 使用 `@RefreshScope` 注解的 Bean 会自动获取最新配置

---

## 常见问题

### Q: 项目启动时连接 Nacos 失败？
A: 请检查：
   1. Nacos 是否已启动
   2. Nacos 地址是否正确（默认 localhost:8848）
   3. 防火墙是否阻止了连接

### Q: 配置没有生效？
A: 请检查：
   1. Data ID 和 Group 是否与 bootstrap.yml 中配置一致
   2. 配置格式是否选择了 YAML
   3. 配置内容是否有语法错误

### Q: 如何修改数据库密码？
A: 在 Nacos 控制台中编辑 `oa-management-service.yml`，修改 `spring.datasource.password`，然后发布即可。
