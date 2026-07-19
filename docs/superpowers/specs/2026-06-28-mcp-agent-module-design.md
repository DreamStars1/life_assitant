# Python MCP Agent 模块设计

> **工具面更新（2026-07-19）：** 细粒度工具已收束为 6 个域路由工具。
> 见 `docs/superpowers/specs/2026-07-19-mcp-domain-router-design.md`。
> 工具：`todo` / `record` / `schedule` / `media` / `points` / `checkin`（各带 `action`）。

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
│       ├── server.py             # MCP Streamable HTTP Server（6 个域路由 @mcp.tool）
│       ├── client.py             # Java REST API 客户端
│       ├── dispatch_util.py      # action 校验错误辅助
│       └── tools/
│           ├── __init__.py
│           ├── todo.py           # HTTP + dispatch
│           ├── record.py
│           ├── schedule.py
│           ├── media.py
│           ├── points.py
│           └── checkin.py
```

**依赖：** `mcp`（Python MCP SDK），`httpx`（HTTP 客户端）

**端口：** `8089`

**MCP 协议：** Streamable HTTP（用户选定）

**MCP 工具（6 域路由）：** 各域一个 `@mcp.tool`，必填 `action` 分发到 `tools/{domain}.py` 的 `dispatch()`。REST 映射与 action 清单见 `2026-07-19-mcp-domain-router-design.md`。

| 域工具 | 说明 |
|--------|------|
| `todo` | 待办 |
| `record` | 一起做过的事 |
| `schedule` | 日程 |
| `media` | 一起看过的内容 |
| `points` | 伴侣积分 |
| `checkin` | 作息打卡 |

~~**历史：11 个细粒度工具（已废弃）**~~ — 旧名如 `todo_create`、`todo_list` 等已删除，不做别名兼容。

## 开发期启动管理

统一由 `restart-all.ps1` 管理，新增 `restart-agent.ps1` 到 `.cursor/skills/dev-restart/scripts/`。

## 后续规划

- Docker 容器化：`python/agent/Dockerfile` + `docker-compose.yml` agent service
- Nginx 反代：生产环境通过 Nginx 统一入口暴露
