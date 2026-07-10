# Python MCP Agent 模块设计

## 背景

Java 后端的 Spring AI MCP Server 协议兼容性问题（SDK 0.18.3 与 Cursor 客户端 JSON-RPC 格式不兼容），决定将 MCP 接入层迁移到 Python。Java 后端保留 API Token 鉴权、REST API 和 `api_token` 管理能力。

## 架构

```
┌──────────────┐  Streamable HTTP  ┌────────────────────┐  HTTP  ┌──────────────────┐
│  MCP Client   │  localhost:8089   │  Python Agent      │  8000  │  Java Backend    │
│  (Cursor 等)  │ ───────────────→  │  life-assistant    │ ─────→ │  :8000           │
│  Auth: la_xxx │  ←────────────── │  Bearer la_xxx 透传 │ ←───── │  ApiTokenFilter  │
└──────────────┘                   └────────────────────┘        └──────────────────┘
```

## 认证流程

- MCP Client 发送 `Authorization: Bearer la_xxx` 到 Python Agent
- Python Agent 透传该 header 到 Java REST API
- Java `ApiTokenFilter` 拦截请求：`Bearer la_` 开头 → `ApiTokenAuthHelper.authenticate()` → `StpUtil.login(id)`
- 验证失败 → 401

## Java 后端改动

| 文件 | 操作 |
|------|------|
| `mcp/auth/ApiTokenAuthHelper.java` | 保留 ✅ |
| `mcp/interceptor/ApiTokenFilter.java` | **新增** — OncePerRequestFilter，校验 Bearer la_xxx token 后通过 StpUtil.login 建立会话 |
| `apitoken/` (controller/service/mapper/entity) | 保留 ✅ |
| Flyway V3/V5 | 保留 ✅ |

## Python Agent 模块

**目录结构：**
```
python/agent/
├── pyproject.toml
├── scripts/
│   └── restart-agent.ps1        # 开发启动脚本（移到 dev-restart skill）
├── src/
│   └── life_assistant_agent/
│       ├── __init__.py
│       ├── __main__.py           # python -m 入口
│       ├── server.py             # MCP Streamable HTTP Server
│       ├── client.py             # Java REST API 客户端
│       └── tools/
│           ├── __init__.py
│           ├── todo.py           # 7 个工具
│           └── record.py         # 4 个工具
```

**依赖：** `mcp`（Python MCP SDK），`httpx`（HTTP 客户端）

**端口：** `8089`

**MCP 协议：** Streamable HTTP（用户选定）

**11 个工具映射到 Java REST API：**

| 工具 | Java REST API |
|------|--------------|
| `todo_create` | `POST /todos` |
| `todo_list` | `GET /todos` |
| `todo_upcoming` | `GET /todos/upcoming` |
| `todo_get` | `GET /todos/{id}` |
| `todo_update` | `PUT /todos/{id}` |
| `todo_toggle` | `PUT /todos/{id}/toggle` |
| `todo_acknowledge` | `PUT /todos/{id}/acknowledge` |
| `record_create` | `POST /shared-records` |
| `record_list` | `GET /shared-records` |
| `record_get` | `GET /shared-records/{id}` |
| `record_update` | `PUT /shared-records/{id}` |

## 开发期启动管理

统一由 `restart-all.ps1` 管理，新增 `restart-agent.ps1` 到 `.cursor/skills/dev-restart/scripts/`。

## 后续规划

- Docker 容器化：`python/agent/Dockerfile` + `docker-compose.yml` agent service
- Nginx 反代：生产环境通过 Nginx 统一入口暴露
