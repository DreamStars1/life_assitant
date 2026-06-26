# MCP 服务集成：将待办和共享记录功能封装为 MCP 工具

## 背景

Life Assistant 已完成待办（个人待办 + 指派伴侣 + 确认回复）和共享记录（记录一起做过的事）功能。当前所有功能仅通过 Vue 前端 + REST API 对外提供，第三方无法集成。

需要将核心业务功能封装为标准 MCP（Model Context Protocol）服务，同时支持 AI 客户端（如 Claude Desktop）和其他后端服务通过统一协议调用。

## 方案

### 整体架构

在现有 `lifeassistant-server` 模块中嵌入 MCP Server，通过 **Streamable HTTP（MCP 2026 推荐远程协议）** 对外暴露。服务端部署，客户端远程连接。

新增模块均位于 `lifeassistant-server` 模块内，无需新增 Maven 子模块。

```
lifeassistant-server/src/main/java/top/lifeassistant/
├── mcp/
│   ├── config/
│   │   └── McpConfig.java               # MCP Server 配置（HTTP 端点）
│   ├── auth/
│   │   └── ApiTokenAuthHelper.java       # API Token → UserDO 认证逻辑
│   ├── tool/
│   │   ├── TodoMcpTools.java             # 待办相关 MCP 工具（7 个）
│   │   └── SharedRecordMcpTools.java     # 共享记录 MCP 工具（4 个）
│   └── interceptor/
│       └── ApiTokenFilter.java           # HTTP Header Token 认证过滤器
```

### API Token 管理

#### 数据库 — 新增 `api_token` 表

```sql
CREATE TABLE api_token (
    id           CHAR(36)     NOT NULL COMMENT 'UUID 主键',
    user_id      CHAR(36)     NOT NULL COMMENT '所属用户',
    name         VARCHAR(50)  NOT NULL COMMENT '令牌别名（用户自定义）',
    token_hash   VARCHAR(64)  NOT NULL COMMENT '令牌 SHA-256 哈希',
    token_prefix CHAR(8)      NOT NULL COMMENT '令牌前 8 位（前端展示用）',
    last_used_at DATETIME     DEFAULT NULL COMMENT '最后使用时间',
    expires_at   DATETIME     DEFAULT NULL COMMENT '过期时间，NULL 表示永不过期',
    is_active    TINYINT(1)   NOT NULL DEFAULT 1 COMMENT '是否有效',
    created_at   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    KEY idx_user_id (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='API 访问令牌';
```

Token 格式：`la_` + 32 位随机十六进制字符串，如 `la_a1b2c3d4e5f6a7b8c9d0e1f2a3b4c5d`。

**安全策略：**
- 服务端存 SHA-256 哈希（token 是高熵随机字符串，不需要 BCrypt 的慢哈希和 72 字节限制）
- `token_prefix` 存前 8 位，前端列表只展示 `la_a1b2c3d4...`
- 撤销采用软删除（`is_active=0`），保留审计痕迹
- token 原文创建时仅返回一次

#### REST API — Token 管理端点（需登录）

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/api-tokens` | 创建 token，body: `{ name, expiresAt? }` → 返回完整 token（仅此一次） |
| GET | `/api-tokens` | 列出当前用户所有 token（只返回 prefix，不返回原文） |
| DELETE | `/api-tokens/{id}` | 撤销 token（软删除） |

Token 响应用于前端展示：

```json
{
  "id": "uuid",
  "name": "Claude 桌面端",
  "tokenPrefix": "la_a1b2c3d4",
  "lastUsedAt": null,
  "expiresAt": null,
  "isActive": true,
  "createdAt": "2026-06-26T12:00:00"
}
```

创建时额外返回 `fullToken` 字段（一次性）。

#### MCP 认证流程

API Token **不作为 MCP 工具参数**，通过 HTTP Header `Authorization: Bearer la_xxx` 传递，避免 token 泄露到 AI 对话上下文中。

MCP 工具类中不声明 apiToken 参数。`ApiTokenFilter` 作为前置过滤器，在每个 HTTP 请求到达时校验 Token：

1. 从 `Authorization` Header 提取 token
2. 查 `api_token` 表校验：`token_hash` 匹配 && `is_active=1` && (`expires_at IS NULL` OR `expires_at > NOW()`)
3. 更新 `last_used_at`
4. 通过 user_id 获取 UserDO → 存入当前线程上下文供工具方法使用

`ApiTokenFilter` 先检查 HTTP Header 中是否有 API Token，有则走 API Token 认证，无则走 Sa-Token 会话认证。两者共存，不影响现有 REST API。

### MCP 工具定义

所有工具方法均通过 Spring AI 的 `@McpTool` 注解声明。Token 从 HTTP Header 提取，工具参数中不出现。

#### TodoMcpTools — 7 个工具

| 工具名 | `@McpToolParam` 参数 | 对应 REST 端点 | 说明 |
|--------|----------------------|----------------|------|
| `todo_create` | title(required), description?, priority?, dueDate?, assignToPartner?(boolean) | POST /todos | 创建待办。`assignToPartner=true` 时自动指派给当前用户的伴侣 |
| `todo_list` | isCompleted?, priority?, startDueDate?, endDueDate? | GET /todos | 查询待办列表 |
| `todo_upcoming` | 无 | GET /todos/upcoming | 首页最近待办 |
| `todo_get` | id(required) | GET /todos/{id} | 待办详情 |
| `todo_update` | id(required), title?, description?, priority?, dueDate? | PATCH /todos/{id} | 更新待办 |
| `todo_toggle` | id(required) | POST /todos/{id}/toggle | 切换完成状态 |
| `todo_acknowledge` | id(required), message? | POST /todos/{id}/acknowledge | 确认收到 |

#### SharedRecordMcpTools — 4 个工具

| 工具名 | `@McpToolParam` 参数 | 对应 REST 端点 | 说明 |
|--------|----------------------|----------------|------|
| `record_create` | title(required), content?, occurredAt? | POST /shared-records | 记录一起做过的事 |
| `record_list` | start?, end? | GET /shared-records | 查询记录列表 |
| `record_get` | id(required) | GET /shared-records/{id} | 记录详情 |
| `record_update` | id(required), title?, content?, occurredAt? | PATCH /shared-records/{id} | 更新记录 |

### 前端

在设置页新增「API 令牌」管理入口，操作流程与现有「确认回复模板」风格一致。

#### 设置页新增入口

```
设置页
├── 确认回复模板        (已有)
├── API 令牌            (新增 → 弹出层)
│   ├── 令牌列表：名称 + la_abc... + 创建时间 + 最后使用时间
│   ├── 添加按钮 → 弹窗输入名称（+ 可选过期时间）
│   ├── 创建后全屏展示 token 原文（等宽字体 + 一键复制）
│   │   └── 明确提示"关闭后将无法再次查看完整令牌"
│   ├── 每条可撤销（确认弹窗）
│   └── 空态：暂无 API 令牌
└── 退出登录            (已有)
```

#### 新建/新增文件

- `front/vue3-vant-mobile/src/api/modules/api-tokens.ts` — API Token CRUD 接口
- 修改 `front/vue3-vant-mobile/src/pages/settings/index.vue` — 新增入口和弹出层

### Maven 依赖变更

在 `lifeassistant-server` 的 `pom.xml` 中新增：

```xml
<!-- Spring AI MCP Server (Streamable HTTP) -->
<dependency>
    <groupId>org.springframework.ai</groupId>
    <artifactId>spring-ai-mcp-server-webmvc</artifactId>
    <!-- ponytail: 实际版本号需根据项目 Spring Boot 版本（ContiNew Starter 2.15.0 → Spring Boot 3.x）确认兼容性 -->
    <version>1.1.0</version>
</dependency>
```

另需在父 POM 的 `<dependencyManagement>` 中引入 Spring AI BOM 以统一管理传递依赖版本。

已知约束：Spring AI MCP 的 `@McpTool` 注解目前要求方法参数使用 `@McpToolParam` 而非标准的 `@RequestParam`/`@RequestBody`，工具类不继承 Spring Web 的注解语义。工具类内部直接注入 Spring Service bean。Streamable HTTP 的具体配置方式需在实施时根据 Spring AI 版本确认。

### 客户端配置示例

不同客户端连接方式不同：

#### Claude Desktop（通过 mcp-remote 桥接）

Claude Desktop 的 `claude_desktop_config.json` 只支持 stdio 本地进程，远程服务需通过 `mcp-remote` 桥接：

```json
{
  "mcpServers": {
    "life-assistant": {
      "command": "npx",
      "args": [
        "mcp-remote@latest",
        "https://api.life-assitant.top/mcp",
        "--header",
        "Authorization: Bearer la_a1b2c3d4e5f6a7b8c9d0e1f2a3b4c5d"
      ]
    }
  }
}
```

#### Cursor

Cursor 原生支持 Streamable HTTP，直接在 `mcp.json` 中配置：

```json
{
  "mcpServers": {
    "life-assistant": {
      "url": "https://api.life-assitant.top/mcp",
      "headers": {
        "Authorization": "Bearer la_a1b2c3d4e5f6a7b8c9d0e1f2a3b4c5d"
      }
    }
  }
}
```

#### Claude Code

Claude Code 原生支持，通过命令行添加：

```bash
claude mcp add life-assistant \
  --transport http \
  https://api.life-assitant.top/mcp \
  --header "Authorization: Bearer la_a1b2c3d4e5f6a7b8c9d0e1f2a3b4c5d"
```

#### Workbuddy（腾讯云 CodeBuddy）

Workbuddy 原生支持 HTTP 类型 MCP 服务器，配置方式：

```json
{
  "mcpServers": {
    "life-assistant": {
      "type": "http",
      "url": "https://api.life-assitant.top/mcp",
      "headers": {
        "Authorization": "Bearer la_a1b2c3d4e5f6a7b8c9d0e1f2a3b4c5d"
      }
    }
  }
}
```

或通过 CLI 添加：

```bash
codebuddy mcp add --scope user life-assistant \
  --transport http \
  https://api.life-assitant.top/mcp
```

### 不变的部分

- 现有 REST Controller 不动
- 现有 Service 层不动
- 现有 Flyway 迁移文件不动
- 现有 Sa-Token 认证逻辑不动
- 现有前端页面结构不动

### 未涉及/未来考虑

- 推送通知集成：当前 todo 尚未集成 Web Push，MCP 工具也不触发推送
- MCP 工具限流：当前不做调用频率限制，可根据后续使用情况补充
- 英文翻译：API Token 功能的前端文本先写中文
- Streamable HTTP 传输的安全加固（如 CORS、请求大小限制）
