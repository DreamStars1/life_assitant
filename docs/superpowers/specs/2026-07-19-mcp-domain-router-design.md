# MCP 域路由工具面设计

## 背景

Life Assistant 的 MCP 接入在 Python Agent（FastMCP + Streamable HTTP）上，透传 `Authorization: Bearer la_xxx` 调用 Java REST。当前约 17 个细粒度工具（`todo_*` / `record_*` / `points_*` / `checkin_*`）。

问题：

1. **日程**与**一起看过的内容（shared-media）**已有完整 REST，但未暴露为 MCP。
2. 工具数量随资源增长继续线性增加，会加重模型选错工具，并拉长客户端工具列表。

目标：在可 breaking change 的前提下，用域路由把工具面压到少量稳定接口，并覆盖日程与一起看过的内容。

## 约束（已确认）

| 项 | 决定 |
|----|------|
| 主要痛点 | 优先改善模型 tool-selection；次要改善人扫读负担 |
| 兼容性 | 允许 breaking；旧工具名直接替换，不做双轨 |
| 覆盖深度 | Agent 精简面：查/建/改为主；次要流程不做 |
| 重构范围 | 全量重整：todo / record / schedule / media / points / checkin |
| 方案 | **域路由**：每域 1 个工具 + `action` 分发 |

## 架构

```
MCP Client  --Streamable HTTP-->  Python Agent (6 domain tools)
                                      |
                                      | Bearer la_xxx
                                      v
                                 Java REST (:8000)
```

**不变：** 认证、部署、子域、`JavaClient` 透传模式、返回 JSON 字符串。

**变：** `server.py` 从 17 个 `@mcp.tool` 改为 6 个；每个域工具内部按 `action` 分发到 `tools/{domain}.py`。

### 工具清单（6）

| 工具名 | 域 | 说明 |
|--------|----|------|
| `todo` | 待办 | 含 toggle / acknowledge |
| `record` | 一起做过的事 | `/shared-records` |
| `schedule` | 日程 | 新；含伴侣日程查询 |
| `media` | 一起看过的 | 新；`/shared-media` |
| `points` | 伴侣积分 | |
| `checkin` | 起床/睡觉打卡 | |

统一形态：

```text
domain_tool(action: str, **fields) -> JSON string
```

工具 description 必须写清域边界，尤其：

- `record` = 一起**做过**的事
- `media` = 一起**看过**的内容（电影/书/剧）

## 各域 action

时间字段：ISO 字符串；日期-only 时沿用现有 Agent 惯例补 `T00:00:00`（与 Java `LocalDateTime` 对齐）。
非法 `action` 或缺必填字段：返回结构化错误 JSON（见「错误处理」），不静默忽略。

### `todo`

| action | 必填 | 可选 | REST |
|--------|------|------|------|
| `list` | — | `is_completed`, `priority`, `start_due_date`, `end_due_date` | `GET /todos` |
| `upcoming` | — | — | 首页未完成待办接口（保留，避免模型滥用大列表） |
| `get` | `id` | — | `GET /todos/{id}` |
| `create` | `title` | `description`, `priority`, `due_date`, `assign_to_partner` | `POST /todos` |
| `update` | `id` | 同 create 可改字段 | `PATCH /todos/{id}` |
| `toggle` | `id` | — | 切换完成 |
| `acknowledge` | `id` | — | 被指派者确认 |

### `record`

| action | 必填 | 可选 | REST |
|--------|------|------|------|
| `list` | — | `start`, `end` | `GET /shared-records` |
| `get` | `id` | — | `GET /shared-records/{id}` |
| `create` | `title` | `content`, `occurred_at` | `POST /shared-records` |
| `update` | `id` | `title`, `content`, `occurred_at` | `PATCH /shared-records/{id}` |

### `schedule`（新）

无按 id 的 get REST，故不设 `get`。

| action | 必填 | 可选 | REST |
|--------|------|------|------|
| `list` | `from`, `to` | `scope`=`me`\|`partner`（默认 `me`） | `GET /schedule/events` 或 `.../partner` |
| `create` | `title`, `start_at`, `end_at` | `note`, `recurrence`, `recurrence_end_date` | `POST /schedule/events` |
| `update` | `id` | 同 create 可改字段 | `PATCH /schedule/events/{id}` |

`recurrence`：`none` / `daily` / `weekly`（与后端一致）。

**本轮不做：** `delete`、邀约、邀约回应。

### `media`（新）

| action | 必填 | 可选 | REST |
|--------|------|------|------|
| `list` | — | `media_type`, `status`, 分页字段（与 `SharedMediaPageQuery` 对齐） | `GET /shared-media` |
| `get` | `id` | — | `GET /shared-media/{id}` |
| `create` | `title`, `media_type` | `description`, `last_watched_at` | `POST /shared-media` |
| `update` | `id` | `title`, `media_type`, `description`, `last_watched_at`, `is_finished` | `PATCH /shared-media/{id}` |

`media_type`：`movie` / `book` / `tv`。
`status`（list）：`finished` / `unfinished`。

**本轮不做：** 封面上传、评论、进度（progress）、删除。

**客户端注意：** 现网 create/update 为 `multipart/form-data`（`@RequestParam`）。Agent 侧需在 `JavaClient` 增加无文件的 form 提交（例如 `post_form` / `patch_form`），字段-only、不传 `cover`。本轮不改 Java API 形状。

### `points`

| action | 必填 | 可选 | REST |
|--------|------|------|------|
| `get` | — | — | `GET /partner/points` |
| `history` | — | `page`, `size` | `GET /partner/points/history` |
| `change` | `points_change`, `reason` | — | `POST /partner/points` |

### `checkin`

| action | 必填 | 可选 | REST |
|--------|------|------|------|
| `do` | `checkin_type` | — | `POST /partner/checkin` |
| `today` | — | — | `GET /partner/checkin/today` |
| `weekly` | — | — | `GET /partner/checkin/weekly` |

`checkin_type` 与现网枚举一致（起床/睡觉）。

## 错误处理

| 情况 | 行为 |
|------|------|
| 缺 Authorization | 中间件失败（与现网一致） |
| 非法 `action` | 返回 `{"error":"invalid_action","allowed":[...]}` |
| 缺必填字段 | 返回 `{"error":"missing_field","field":"..."}` |
| Java 4xx/5xx | 延续现有：`raise_for_status` / 透传可读错误，不吞错 |

错误以 JSON 字符串返回给 MCP client，便于模型修正参数后重试。

## 迁移（breaking）

旧工具 → 新调用示例：

| 旧 | 新 |
|----|----|
| `todo_list(...)` | `todo(action="list", ...)` |
| `todo_create(...)` | `todo(action="create", ...)` |
| `record_get(id)` | `record(action="get", id=id)` |
| `points_change(...)` | `points(action="change", ...)` |
| `checkin_do(...)` | `checkin(action="do", ...)` |

对外文档 / README / 部署说明中的工具列表同步改为一版 6 工具；不做别名兼容期。

## 代码结构

```
python/agent/src/life_assistant_agent/
├── server.py              # 注册 6 个域工具 + action 分发
├── client.py              # 现有 JSON 方法 + media 所需 form 方法
└── tools/
    ├── todo.py
    ├── record.py
    ├── schedule.py        # 新增
    ├── media.py           # 新增
    ├── points.py
    └── checkin.py
```

`tools/*.py` 继续只做 HTTP 封装；分发与参数校验放在 `server.py`（或极薄的 per-domain dispatch 函数，避免再引入框架）。

## 测试

不引入新测试框架；最小可运行检查：

1. **分发**：合法 action → 打到对应 client 封装（mock HTTP）。
2. **校验**：非法 action、缺必填 → 稳定错误 JSON。
3. **烟雾**：`schedule` / `media` 各至少一条 list/create 的 URL/body（或 form fields）拼装正确。

不做：未暴露能力（邀约、评论、progress、封面）的测试；全量 Java 集成非必须。

## 非目标

- 回到 Java 内嵌 MCP
- 动词路由 / 双工具超载方案
- 日程邀约流、media 评论/进度/封面
- 删除类 MCP 操作
- 旧工具名兼容层

## 成功标准

1. MCP 对外可见工具数 = **6**。
2. 日程可 list（含 partner）/ create / update；一起看过的可 list / get / create / update。
3. 原 todo / record / points / checkin 能力在域工具下可用（含 toggle、acknowledge、打卡、加减分）。
4. 旧细粒度工具名不再注册。
