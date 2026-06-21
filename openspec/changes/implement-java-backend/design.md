## Context

当前 `backend/newbackend/continew-admin/` 是 ContiNew Admin v4.2.0-SNAPSHOT 原始模板，包名 `top.continew.admin`，启动类 `ContiNewAdminApplication`。含 continew-plugin/continew-extension 两个多余模块，使用 Liquibase 做数据库迁移，Entity 为 RBAC 体系（用户/角色/菜单/部门）。

Python FastAPI 后端运行于 `backend/app/`，含 9 个路由模块。本 change 将：
1. 把 `backend/newbackend/continew-admin/` 移动到 `backend/lifeassistant/` 并改造为 LifeAssistant
2. 删除 `backend/app/`、`backend/tests/`、`backend/.venv/` 等全部 Python 资产
3. 更新 `compose.yml`、`compose.override.yml`、`backend/.env` 以适配 Java 后端

设计阶段产出归档于 `openspec/changes/archive/2026-06-20-migrate-backend-to-java-continew/`，包含完整的 5 表设计、~44 个 API 端点规范、包结构、ADR、部署方案。

## Goals / Non-Goals

**Goals:**
- 将 ContiNew 模板改造为 LifeAssistant Java 后端骨架（包名/模块/配置）
- 目录重组：`backend/newbackend/continew-admin/` → `backend/lifeassistant/`
- **BREAKING**：删除 Python 后端全部代码（`backend/app/`、`backend/tests/`、`backend/.venv/` 等）
- 更新 `compose.yml`、`compose.override.yml`、`backend/.env` 适配 Java 后端
- 实现 5 张核心表的 Flyway V1 迁移
- 实现 ~44 个 REST API 端点，响应格式为裸 JSON（不用 R 包装）
- 伴侣共享功能（shared_record）作为第一优先交付
- Maven 多阶段 Docker 构建，与现有 Nginx/docker-compose 兼容

**Non-Goals:**
- 不修改前端代码（API 对齐为后续变更）
- 不实现 AI 分析（agent/analysis 模块）
- 不实现微信集成
- 不留 ContiNew RBAC（角色/菜单/部门/字典/日志/租户）
- 不留 continew-plugin / continew-extension 模块

## Decisions

继承归档中的所有 ADR（D0-D10），无需重新决策。实现层面补充：

| 决策 | 选择 | 理由 |
|------|------|------|
| 实现顺序 | config → core → auth → user → shared-record → todo → activity → notification → monitor | 用户要求：基建优先，shared-record 第一优先 |
| Flyway 迁移 | 单文件 V1__init_all_tables.sql，一次性建 5 张表 | 5 张表同步创建，避免多次迁移 |
| Entity 策略 | 每个业务包自含 entity（`xx/model/entity/XxDO.java`），不集中到 system | ADR D7，每模块独立自包含 |
| BaseDO 改造 | id 字段从 Long 改为 String，其余审计字段保留 | CHAR(36) UUID 主键 |
| 异常处理 | 统一 `GlobalExceptionHandler` + `{"detail":"..."}` 格式 | 兼容 Python 后端错误格式 |
| Sa-Token 配置 | 关闭多租户、限流、验证码、JustAuth、`@EnableGlobalResponse` | decisions.md 禁用清单 |
| 用户查询 @CurrentUser | 自定义注解 + Sa-Token 解析 token → 查 user 表 | 不用 ContiNew 的 Log 体系 |

## Risks / Trade-offs

| Risk | Mitigation |
|------|------------|
| Flyway 单文件建 5 张表，若有一张失败则整体回滚 | 5 张表相互独立（仅 FK 引用 user.id），可拆为 5 个迁移文件减少回滚范围 |
| Sa-Token 配置项多（多租户/限流/验证码），漏关一个可能引入意外行为 | 逐条对照 decisions.md 禁用清单，写 checklist 验证 |
| partner_id 自引用绑定需双向写入，并发下可能数据不一致 | 用 `@Transactional` + 数据库行锁，单次绑定是低频操作 |
| 前端 API 路径与 Python 后端不同（如 `/partner/activities` vs 旧 `/timeline/*`） | Java 后端保留 Python 兼容路径作为过渡，Nginx 分流 |

## Directory Restructure

```
BEFORE                                    AFTER
═══════════════════════════════════       ═══════════════════════════════
backend/                                 backend/
├── app/                   ❌ DELETE      ├── lifeassistant/    ← 原 newbackend/continew-admin/
├── newbackend/                           │   └── Dockerfile    ← 新增
│   └── continew-admin/    ❌ MOVE → root ├── nginx/            (不动)
├── tests/                 ❌ DELETE      ├── scripts/          (更新)
├── .venv/                 ❌ DELETE      └── .env              (更新)
├── htmlcov/               ❌ DELETE
├── Dockerfile             ❌ DELETE
├── .dockerignore          ❌ DELETE
├── LICENSE                ❌ DELETE
├── README.md              ❌ DELETE
├── pyproject.toml         ❌ DELETE
├── uv.lock                ❌ DELETE
├── alembic.ini            ❌ DELETE
├── nginx/                 (保留)
├── scripts/               (更新)
└── .env                   (更新)
```

### compose.yml 关键变动

| 项 | 旧值 (Python) | 新值 (Java) |
|----|-------------|------------|
| `backend.build.context` | `backend` | `backend/lifeassistant` |
| `backend.build.dockerfile` | `Dockerfile` | `Dockerfile`（不变，在 context 目录下） |
| `backend.healthcheck.test` | `curl http://localhost:8000/api/v1/utils/health-check/` | `curl http://localhost:8000/api/v1/admin/health` |
| `prestart.command` | `bash scripts/prestart.sh` | 删除 prestart 服务（Flyway 在容器启动时自动迁移） |

### compose.override.yml 关键变动

| 项 | 旧值 (Python) | 新值 (Java) |
|----|-------------|------------|
| `backend.build.context` | `backend` | `backend/lifeassistant` |
| `backend.command` | `fastapi run --reload app/main.py` | `mvn spring-boot:run -pl continew-server`（开发模式） |
| `backend.develop.watch` | Python 文件同步 | 删除（Java 不支持 Docker Compose Watch 热重载） |
| `backend.volumes` | `./backend/htmlcov:/app/htmlcov` | 删除 |
| `SMTP_*` environment | mailcatcher mock | 保留 |

### backend/.env 关键变动

| 操作 | 变量 |
|------|------|
| 删除 | `PROJECT_NAME`、`STACK_NAME`、`DOCKER_IMAGE_BACKEND` |
| 保留 | `DOMAIN`、`FRONTEND_HOST`、`ENVIRONMENT`、`BACKEND_CORS_ORIGINS`、`SECRET_KEY`、`FIRST_SUPERUSER`、`FIRST_SUPERUSER_PASSWORD`、`SMTP_*`、`MYSQL_*`、`SENTRY_DSN` |

## Migration Plan

```
Phase 1: 目录重组 + 代码改造
  1. 移动 continew-admin/ → lifeassistant/，删除 newbackend/ 空目录
  2. 删除全部 Python 资产
  3. 改造 Java 代码（包名/模块/Entity/API）
  4. 实现 5 张表 + ~44 个 API

Phase 2: Docker 适配 + 验证
  5. 新增 Java Dockerfile
  6. 更新 compose.yml / compose.override.yml / backend/.env
  7. 本地 docker-compose up 验证

Phase 3: 后续（不在本 change）
  8. 前端 API 对齐
```

## Open Questions

1. **BaseDO 审计字段**: 保留 `@TableField(fill = FieldFill.INSERT)` 自动填充还是显式在 Service 层设置？建议保留 MyBatis-Plus 自动填充，减少 Service 层样板代码。

2. **密码重置邮件**: Thymeleaf 模板放在 `resources/templates/` 还是独立模块？建议放 `continew-server/src/main/resources/templates/`，沿用 Spring Boot 标准惯例。

3. **API 路径兼容**: 是否需要保留 Python 后端的 `/api/v1/login/access-token`（OAuth2 form-data）路径？建议保留，前端已有代码依赖此端点。
