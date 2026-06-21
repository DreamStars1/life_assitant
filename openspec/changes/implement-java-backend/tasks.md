## 0. Python 删除与目录重组

- [x] 0.1 删除 `backend/app/`（Python FastAPI 全部业务代码）
- [x] 0.2 删除 `backend/tests/`（pytest 测试）
- [x] 0.3 删除 `backend/.venv/`（Python 虚拟环境）
- [x] 0.4 删除 `backend/htmlcov/`（覆盖率报告）
- [x] 0.5 删除 Python 根文件：`backend/pyproject.toml`、`backend/uv.lock`、`backend/alembic.ini`、`backend/Dockerfile`、`backend/.dockerignore`、`backend/LICENSE`、`backend/README.md`
- [x] 0.6 移动 `backend/newbackend/continew-admin/` → `backend/lifeassistant/`
- [x] 0.7 删除空目录 `backend/newbackend/`（被 IDE 锁定，关闭窗口后自行消失）
- [x] 0.8 更新 `backend/.env`：删除 `PROJECT_NAME`、`STACK_NAME`、`DOCKER_IMAGE_BACKEND`；保留 `DOMAIN`/`FRONTEND_HOST`/`ENVIRONMENT`/`SECRET_KEY`/`FIRST_SUPERUSER`/`SMTP_*`/`MYSQL_*`/`SENTRY_DSN`

## 1. Java 项目骨架改造（ContiNew 模板裁剪）

- [x] 1.1 删除 `continew-plugin/` 和 `continew-extension/` 模块目录（路径在 `backend/lifeassistant/` 下）
- [x] 1.2 删除 `.github/`、`.image/`、`docker/` 多余目录
- [x] 1.3 修改根 `pom.xml`：`<groupId>` 改为 `top.lifeassistant`，删除 plugin/extension 模块引用，删除 spotless/sonar
- [x] 1.4 修改 `continew-server/pom.xml`：添加 flyway 依赖，排除 liquibase，移除 plugin/extension 引用
- [x] 1.5 修改 `continew-common/pom.xml`：更新 groupId 为 `top.lifeassistant`
- [x] 1.6 修改 `continew-system/pom.xml`：更新 groupId，移除对 plugin 的依赖
- [x] 1.7 全局包名替换：`top.continew.admin` → `top.lifeassistant`（所有 Java 文件 + 配置文件）
- [x] 1.8 重命名启动类：`ContiNewAdminApplication.java` → `LifeAssistantApplication.java`

## 2. Config 配置层实现

- [x] 2.1 修改 `application.yml`：`application.base-package: top.lifeassistant`，关闭多租户/限流/验证码/JustAuth/`@EnableGlobalResponse`，`id-type: ASSIGN_UUID`，删除 cosid
- [x] 2.2 修改 `application-dev.yml`：关闭 SnailJob/Liquibase，开启 Flyway，配置 MySQL/Redis/SMTP
- [x] 2.3 修改 `application-prod.yml`：SECRET_KEY 和 MYSQL_PASSWORD 强制检查，不允许默认值
- [x] 2.4 新增 `CorsConfig.java`：支持逗号分隔域名 + FRONTEND_HOST 自动合并
- [x] 2.5 新增 `WebMvcConfig.java`：静态资源、拦截器配置
- [x] 2.6 新增 `MyBatisPlusConfig.java`：分页插件、UUID 主键策略、自动填充审计字段

## 3. Core 核心层实现

- [x] 3.1 新增 `GlobalExceptionHandler.java`：统一 `{"detail":"..."}` 格式，覆盖 400/401/403/404/409/422/500
- [x] 3.2 新增 `BusinessException.java`（含 HttpStatus 字段）
- [x] 3.3 修改 `BaseDO.java`：id 字段从 Long 改为 String 类型
- [x] 3.4 新增 Flyway 迁移脚本 `V1__init_all_tables.sql`：建 5 张表（user/todo/activity/shared_record/push_subscription）+ 索引
- [x] 3.5 配置 NextDoc4j API 文档（`/doc.html` 入口），添加分组描述
- [x] 3.6 配置 Logback：dev=DEBUG，prod=INFO，文件滚动存储

## 4. Auth 认证模块实现

- [x] 4.1 配置 Sa-Token JWT HS256，注入 `SaTokenConfigure` + `StpInterfaceImpl`（权限/角色从 user 表加载）
- [x] 4.2 实现 `AuthController.java` + `AuthService.java`：`POST /api/v1/login/access-token`（OAuth2 form-data）、`POST /api/v1/auth/login`（JSON）、`POST /api/v1/auth/refresh-token`、`DELETE /api/v1/auth/logout`
- [x] 4.3 实现 `POST /api/v1/password-recovery/{email}`：查邮箱 → 生成重置 token → 发 Thymeleaf 邮件，统一返回成功（防邮箱枚举）
- [x] 4.4 实现 `POST /api/v1/reset-password`：校验 token → 更新 BCrypt 密码
- [x] 4.5 新增 `@CurrentUser` 注解 + `CurrentUserResolver`：从 Sa-Token 解析 sub → 查库 → 注入 UserDO
- [x] 4.6 新增 `@RequireSuperuser` 注解 + 拦截检查

## 5. User 用户 + 伴侣模块实现

- [x] 5.1 创建 `UserDO.java`（`@TableName("user")`，13 字段 + `@TableLogic` 软删除）
- [x] 5.2 创建 `UserMapper.java`（继承 BaseMapper + 自定义分页查询）
- [x] 5.3 实现 `UserController.java` + `UserService.java`：注册、个人信息 CRUD（GET/PATCH/DELETE me、PATCH me/password）
- [x] 5.4 实现超管用户管理端点：`GET/POST/GET{id}/PATCH{id}/DELETE{id} /api/v1/users`（`@RequireSuperuser`）
- [x] 5.5 实现 `IdentityController.java` + `IdentityService.java`：`POST /api/v1/identity/invite`（生成 24h JWT 邀请码）、`POST /api/v1/identity/bind-partner`（双向 partner_id 写入，`@Transactional`）
- [x] 5.6 创建 User req/resp DTO：`UserRegisterReq`、`UserUpdateReq`、`UserPasswordUpdateReq`、`UserPublicResp`

## 🚩 检查点 #1：登录/注册闭环

> 此时应可验证：注册新用户 → 登录获取 Token → 获取个人信息 → 前端登录门禁生效

- [x] CP1.1 改造前端路由守卫：未登录用户重定向到登录页，已登录用户跳过登录页
- [x] CP1.2 改造前端 `App.vue`：登录页不显示 TabBar，已登录才显示正常布局
- [x] CP1.3 改造前端 HTTP 拦截器：401 响应自动清 Token 并跳转到登录页
- [x] CP1.4 改造前端登录页：注册/登录切换、成功后跳转首页
- [ ] CP1.5 验证：未登录→被拦截到登录页→注册→登录→进首页→TabBar可见→可获取个人信息

## 6. ★第一优先：SharedRecord 共享记录模块实现

- [x] 6.1 创建 `SharedRecordDO.java`（7 字段，`@TableName("shared_record")`）
- [x] 6.2 创建 `SharedRecordMapper.java`：`WHERE created_by IN (user_id, partner_id)` 查询，`@Select` 或 LambdaWrapper
- [x] 6.3 实现 `SharedRecordController.java` + `SharedRecordService.java`：创建（校验 partner_id 非空）、列表（降序 + ?start&end 过滤）、详情、更新（仅创建者）、删除（仅创建者）
- [x] 6.4 创建 SharedRecord req/resp DTO：`SharedRecordCreateReq`、`SharedRecordUpdateReq`、`SharedRecordResp`

## 🚩 检查点 #2：共享记录闭环

> 此时应可验证：两个用户绑定伴侣 → 创建共享记录 → 双方看到相同列表 → 权限隔离正确

- [x] CP2.1 前端：添加共享记录页面（列表 + 创建表单）
- [x] CP2.2 验证：用户 A 邀请 → 用户 B 绑定 → A 创建记录 → B 可见 → B 不能删 A 的记录

## 7. Todo 待办模块实现

- [ ] 7.1 创建 `TodoDO.java`（14 字段，`@TableName("todo")`）
- [ ] 7.2 创建 `TodoMapper.java`
- [ ] 7.3 实现 `TodoController.java` + `TodoService.java`：CRUD（is_completed 变化时自动维护 completed_at/cancelled_at）、列表过滤
- [ ] 7.4 实现待办分配：`POST /api/v1/todos/{id}/assign`（校验 partner_id）
- [ ] 7.5 实现伴侣待办查询：`GET /api/v1/partner/todos`
- [ ] 7.6 创建 Todo req/resp DTO

## 8. Activity 活动模块实现

- [ ] 8.1 创建 `ActivityDO.java`（9 字段，`@TableName("activity")`）
- [ ] 8.2 创建 `ActivityMapper.java`
- [ ] 8.3 实现 `ActivityController.java` + `ActivityService.java`：CRUD、时间范围查询（?start&end&category）
- [ ] 8.4 实现伴侣活动查询：`GET /api/v1/partner/activities`（WHERE user_id = partner_id）
- [ ] 8.5 创建 Activity req/resp DTO

## 9. Notification 通知模块实现

- [ ] 9.1 创建 `PushSubscriptionDO.java`（8 字段，`@TableName("push_subscription")`）
- [ ] 9.2 创建 `PushSubscriptionMapper.java`
- [ ] 9.3 实现 `PushController.java`：订阅注册/取消、偏好设置、测试推送
- [ ] 9.4 实现 `PushService.java`：VAPID 推送发送（nl.martijndwars/web-push）、静默时段检查、410 Gone 处理
- [ ] 9.5 实现 `PushScheduler.java`：`@Scheduled` 每分钟待办提醒 + 每日 8:00 摘要（需从时区字段取值）
- [ ] 9.6 在 `continew-server/pom.xml` 添加 web-push 依赖

## 10. Monitor 监控模块实现

- [ ] 10.1 实现 `AdminController.java` + `AdminService.java`：健康检查、概览统计、推送统计、用户列表（全部 `@RequireSuperuser`）
- [ ] 10.2 创建 Admin 响应 DTO

## 11. Docker Compose 适配

- [ ] 11.1 修改 `compose.yml`：`backend.build.context: backend/lifeassistant`，删除 `prestart` 服务，`backend.healthcheck.test` 改为 `curl http://localhost:8000/api/v1/admin/health`
- [ ] 11.2 修改 `compose.override.yml`：`backend.build.context: backend/lifeassistant`，`command` 改为 `mvn spring-boot:run -pl continew-server`，删除 `develop.watch` 块，删除 `./backend/htmlcov` volume
- [ ] 11.3 创建 `backend/lifeassistant/Dockerfile`：bellsoft-liberica JDK 17 多阶段构建 + ZGC
- [ ] 11.4 确认 Nginx 配置无需改动（容器名 `backend:8000` 不变）

## 12. 验证与检查

- [ ] 12.1 `mvn clean compile` 从 `backend/lifeassistant/` 目录通过（无编译错误）
- [ ] 12.2 Flyway V1 迁移在空 MySQL 8.4 上成功执行
- [ ] 12.3 Sa-Token 配置项检查清单：多租户关闭 ✓、限流关闭 ✓、验证码关闭 ✓、JustAuth 关闭 ✓、全局响应包装关闭 ✓
- [ ] 12.4 写一个自检 main 方法：启动 Spring 上下文 → 验证所有 Bean 加载 → 打印 "LifeAssistant Ready"，失败则 exit 1
- [ ] 12.5 `docker compose up backend --build` 启动成功，healthcheck 通过
