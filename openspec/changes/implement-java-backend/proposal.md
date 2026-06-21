## Why

将 Python FastAPI 后端替换为 Java Spring Boot 3.3.x 后端（基于 ContiNew Admin 模板）。设计阶段已于 `2026-06-20-migrate-backend-to-java-continew` 完成并归档，本 change 是设计到实现的衔接——把设计文档转化为可执行的编码任务。

## What Changes

- **Python 后端删除**：删除 `backend/app/`、`backend/tests/`、`backend/.venv/`、`backend/htmlcov/`、`backend/pyproject.toml`、`backend/uv.lock`、`backend/alembic.ini`、`backend/Dockerfile`（Python 版）、`backend/.dockerignore`、`backend/LICENSE`、`backend/README.md`
- **目录重组**：`backend/newbackend/continew-admin/` → `backend/lifeassistant/`（删除 `backend/newbackend/` 中间层）
- **ContiNew 模板改造**：包名 `top.continew.admin` → `top.lifeassistant`，启动类重命名，删除 plugin/extension 模块
- **5 张新表**：`user` / `todo` / `activity` / `shared_record` / `push_subscription`，Flyway V1 迁移
- **8 个业务模块**：config / core / auth / user / todo / activity / sharedrecord / notification / monitor
- **~44 个 REST API 端点**：覆盖认证、用户、伴侣、共享记录、待办、活动、推送、管理后台
- **伴侣共享**：`shared_record` 为第一优先功能（双方可写、完全互见，`WHERE created_by IN (a,b)` 查询）
- **原有 event / life_log / timeline_entry 统一为 activity**（时间块单表）
- **Docker 构建**：Maven 多阶段构建，bellsoft-liberica JDK 17 + ZGC，Java Dockerfile 放在 `backend/lifeassistant/Dockerfile`
- **Docker Compose 更新**：`compose.yml` 和 `compose.override.yml` 中 back-end 服务的 context/command/healthcheck 从 Python 切换为 Java
- **backend/.env 更新**：删除 Python 专用环境变量（如 `PROJECT_NAME`），保留跨语言通用变量（DB/Redis/SMTP/Domain）
- **前端 API 层**：端点路径和响应格式有变化，需对齐（不在本 change 范围）

## Capabilities

### New Capabilities

- `java-backend-config`: 多环境配置（application-{dev,prod}.yml）、全局异常处理、CORS、WebMvc、MyBatis-Plus 配置
- `java-backend-core`: Maven 多模块结构、启动类、Flyway 迁移、API 文档（SpringDoc）、Logback
- `java-backend-auth`: JWT 认证（Sa-Token）— 登录/刷新/登出、密码重置（Thymeleaf 邮件）
- `java-backend-user`: 用户注册、个人信息 CRUD、伴侣绑定（邀请码 JWT + 双向写入）、超管用户管理
- `java-backend-shared-record`: ★第一优先 — 伴侣共享记录 CRUD，`WHERE created_by IN (user_id, partner_id)` 查询，创建者权限控制
- `java-backend-todo`: 待办 CRUD、标记完成（自动 completed_at）、伴侣分配、伴侣待办查询
- `java-backend-activity`: 时间块活动 CRUD、时间范围查询、伴侣活动全量可见
- `java-backend-notification`: Web Push 订阅管理、VAPID 推送、静默时段判断、@Scheduled 定时提醒+摘要
- `java-backend-monitor`: 超管统计端点（健康检查、概览、推送统计、用户列表）

### Modified Capabilities

无。`openspec/specs/` 当前为空，所有 capability 均为新建。

## Impact

- **删除**：`backend/app/`（Python FastAPI 全部业务代码）、`backend/tests/`（pytest）、`backend/.venv/`、`backend/htmlcov/`、`backend/pyproject.toml`、`backend/uv.lock`、`backend/alembic.ini`、`backend/Dockerfile`、`backend/.dockerignore`、`backend/LICENSE`、`backend/README.md`、`backend/newbackend/`（移走 continew-admin 后删除空目录）
- **重命名**：`backend/newbackend/continew-admin/` → `backend/lifeassistant/`
- **新增/修改文件**：`backend/lifeassistant/` 内约 50+ 文件（包名替换、Entity 重写、新业务模块），详见 decisions.md 重写文件清单
- **新增**：`backend/lifeassistant/Dockerfile`（Maven 多阶段构建，bellsoft-liberica JDK 17 + ZGC）
- **修改**：`compose.yml` — backend 服务 context、prestart/healthcheck 命令
- **修改**：`compose.override.yml` — 去掉 fastapi 命令 + develop watch，改为 Java 启动命令
- **修改**：`backend/.env` — 删除 `PROJECT_NAME`/`STACK_NAME` 等 Python 专用变量
- **Nginx**：`backend/nginx/` 配置保持不动（容器名 `backend:8000` 不变）
- **依赖**：移除 Liquibase，新增 Flyway + web-push；移除 continew-plugin 和 continew-extension
- **前端不在此变更范围**：API 对齐为后续变更
