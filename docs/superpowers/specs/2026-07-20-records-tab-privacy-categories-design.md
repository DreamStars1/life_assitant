# 记录 Tab 拆分、待办导航合并、私密与自定义分类

**日期：** 2026-07-20
**状态：** 已确认；实现计划见 `docs/superpowers/plans/2026-07-20-records-tab-privacy-categories.md`

## 背景

当前问题与目标：

1. 底部「今日」与「待办」功能重叠；日程已有日视图，今日摘要页可删。
2. 「一起做过的事」与「一起看过的」挤在同一页 Tab；后者应独立为「记录」并强化个人向能力。
3. 「一起看过的」列表应按观看时间排序；无私密、无自定义分类、进度无历史时间轴。

## 目标

- 去掉今日视图；待办用截止日期颜色表达紧迫度。
- 「记录」独立底部 Tab；伴侣页只留一起做过的事。
- 记录支持私密（默认共享）、用户自定义分类、进度变更时间轴（可进入独立页）。
- 列表排序：`last_watched_at` 优先，否则 `update_time` 日期。

## 非目标

- 待办「今日」筛选、记录可选排序切换。
- 自定义分类共享给对方；进度以外的审计日志。
- 未绑定伴侣也可用记录模块。
- 私密的服务端强隔离（本次仅藏 UI；仍依赖现有伴侣绑定限制）。
- **不要求** MCP 新增私密 / 自定义分类 / 进度时间轴 action；只保证现有能力不坏。

## MCP 兼容（硬约束）

现有 Agent 域工具 `media`（`list` / `get` / `create` / `update`）必须在本次改动后**无需改调用方**仍可用。

| 约束 | 说明 |
|------|------|
| REST 前缀 | 保持 `/shared-media`（及现有 `{id}` 详情 / multipart create·update） |
| 入参 | 继续接受现有字段：`title`、`media_type`（`movie`/`book`/`tv`）、`description`、`last_watched_at`、`is_finished`、list 的 `media_type`/`status`/分页 |
| 默认值 | 新字段对旧客户端透明：未传 `is_private` → `false`；未传自定义分类 → 仍用默认 `media_type` |
| 响应 | 可**增量**加字段（如 `isPrivate`、`mediaTypeLabel`）；不得删除或改义现有字段 |
| 行为 | `list`/`get`/`create`/`update` 语义与现网一致；排序变更可接受；不强制 Agent 改代码 |
| 进度 | MCP 本就不覆盖 progress；时间轴表不影响现有 media 工具 |

验收：跑现有 `python/agent` 中 media 相关单测（如 `test_schedule_media_http`）及一次真实 `media list/create` 冒烟通过。

---

## 1. 导航与路由

### TabBar（5 项）

1. 待办 `/todos`
2. 伴侣 `/share` — 仅「一起做过的事」
3. 记录 `/records`（路径可微调）— 原「一起看过的」
4. 看板 `/partner/dashboard`
5. 我的 `/profile`

去掉「今日」Tab 项。

### `/` 与登录

- `pages/index.vue` 改为重定向：`router.replace('/todos')`（模式对齐现有 `events` → 日程）。
- 登录成功默认落地、`router` 中「已登录访问登录页」跳转：由 `Today` 改为 `Todos`。
- `rootRouteList` 去掉 `Today`（重定向页不显示 TabBar）。路由名 `Today` 可保留以减少类型抖动，或改为 `HomeRedirect`——实现取改动更少者。

### 页面拆分

- `/share`：删除内部 records/media 的 `van-tabs`，只保留一起做过的事。
- 新建记录页：迁入现有媒体列表、筛选、添加/编辑弹窗、进详情跳转。
- `/events` → `/todos?view=schedule` 保持不变。
- 待办页内「待办 / 日程」主 Tab 与日/周视图不变。

---

## 2. 待办截止日期颜色

仅改待办列表截止日期文案样式（`todos/index.vue`），不改优先级色点、不改列表排序、不按颜色筛选。

相对本地「今天」的 day diff：

| diff | 文案 | 颜色 |
|------|------|------|
| `< 0` | 已逾期 N 天 | 红 |
| `0` | 今天 | 橙 |
| `1` | 明天 | 黄 |
| `≥ 2` | 后天 / M/D | 灰 |
| 无 dueDate | 不显示日期色标 | — |

今日页整页废弃后，其旧色标逻辑不再维护。

---

## 3. 记录列表排序

后端列表（含分页）默认：

```text
ORDER BY COALESCE(last_watched_at, DATE(update_time)) DESC, update_time DESC
```

- 有「上次一起看」用该日期；没有则用 `update_time` 的日期。
- 前端不二次排序。
- 不做用户可选排序。

---

## 4. 私密记录

### 字段与默认

- `shared_media.is_private`：`BOOLEAN`，默认 `false`（共享）。
- 创建/编辑表单：「仅自己可见」开关，默认关（选项 A）。

### 可见性（本次约定）

- **前端**：对对方隐藏自己的私密记录入口与列表项；自己始终可见自己的私密 + 共享，以及对方的非私密记录（若列表仍拉双方数据，则前端过滤 `is_private && createdBy !== me`）。
- **服务端**：本次不强做 404 隔离；保留现有 `requirePartner`。接受对方直接调 API 仍可能读到的风险。

### 列表时间文案（私密 vs 共享）

**共享（`is_private=false`）**

- 有 `lastWatchedAt`：显示「上次一起看：yyyy-MM-dd」。
- 无：可不显示该行，或与现网一致。

**私密（`is_private=true`）**

- 有 `lastWatchedAt`：显示该日期，文案中性（如「上次观看」/ 纯日期），**不出现**「一起看」。
- 无：显示 `update_time` 日期，如「更新于 yyyy-MM-dd」。

排序与文案解耦，仍按 §3。

### 进度 / 评论

保持现有交互（共同 / 个人进度 UI 不因私密隐藏共同进度入口）。私密记录对对方藏 UI 后，对方本就看不到该条。

可随时在编辑中切换共享 ↔ 私密。

---

## 5. 自定义分类

### 默认类型（不变）

`movie` / `book` / `tv` → 电影 / 书籍 / 漫剧。

另保留系统值 `uncategorized` → 未分类（筛选项保留）。

### 新表 `media_category`（名可微调）

| 列 | 说明 |
|----|------|
| id | 主键 |
| user_id | 所有者；按用户隔离 |
| name | 显示名 |
| created_at / update_time | 常规时间戳 |

用户只能 CRUD 自己的分类。

### `media_type` 约定（选项 A）

- 默认：存 `movie` / `book` / `tv`。
- 自定义：存分类 `id` 字符串；`id` 使用 UUID（或其它不会与保留字碰撞的值），保留字含 `movie` / `book` / `tv` / `uncategorized`。
- 删分类后回落：`uncategorized`。

### API / UI

- 类型选项 = 默认三种 + 当前用户自定义（+ 筛选时「未分类」）。
- 记录页提供分类管理（增 / 改名 / 删）。
- 删除允许：将该用户下 `media_type = 该 id` 的记录批量改为 `uncategorized`。
- 响应带 `mediaTypeLabel`（服务端解析：默认映射 / 查分类名 / 「未分类」），对方看共享记录只读标签，不把你的自定义项并进对方选择器。

校验与 label 解析集中一处，避免 `media_type` 混用字符串与 id 散落多处。

---

## 6. 进度变更时间轴

### 数据

- 保留 `media_progress`：继续 upsert，表示**当前**共同 / 个人进度。
- 新表（如 `media_progress_event`）：每次进度更新**追加**一行。

建议字段：`id`, `media_id`, `user_id`（操作者）, `scope`（`shared`|`personal`）, `progress_text`, `created_at`。

更新进度接口：同一事务内先 upsert 当前进度，再 insert event。删媒体时级联删 event。

### 读接口

- `GET .../shared-media/{id}/progress-events`（路径可微调），按 `created_at` 降序。

### UI

- 记录详情页有明确入口（如「进度时间轴」）。
- 点进去进入**独立页**展示完整时间轴（非仅详情内嵌一小段）。
- 每条：时间 + 操作者 + scope 标签 + 进度文案。
- 当前进度条逻辑不变；最新 event 文案应与当前进度一致。

### 范围

- **只记进度更新**；不记标题 / 封面 / 评论 / 私密切换等。

---

## 7. 架构要点

| 单元 | 职责 | 依赖 |
|------|------|------|
| TabBar + 路由重定向 | 导航合并与落地 | 路由名 / `rootRouteList` |
| 待办页色标 | 截止日期视觉 | 仅前端 `dueDate` |
| `/share` | 一起做过的事 | 现有 shared-record API |
| `/records` + 详情 / 时间轴页 | 记录 CRUD、分类、私密文案、进时间轴 | shared-media + category + progress-events |
| SharedMediaService | 排序、is_private 字段、mediaTypeLabel | Mapper / category |
| MediaCategoryService | 用户隔离 CRUD、删除回落 | 新表 |
| MediaProgressService | upsert + 写 event | progress + event 表 |

后端 API 前缀仍用 `shared-media`；表名可继续 `shared_media`。

---

## 8. 错误处理

- 分类名空 / 重名：400 + 明确文案。
- 进度文案空：沿用现有前端校验；后端保持与现网一致。
- 私密字段非法：忽略或 400（实现时二选一并写死）。
- 删除分类：事务内改写引用记录再删分类行。

---

## 9. 验收清单

1. TabBar 无「今日」；`/` 与登录进待办；截止日期四色正确。
2. 「伴侣」仅一起做过的事；「记录」独立 Tab；列表排序符合 §3。
3. 可设私密；对方 UI 看不到；列表时间文案符合 §4（不要求 API 级隔离）。
4. 可增删自定义分类；删后变「未分类」；与 movie/book/tv 并列且用户隔离；响应有 `mediaTypeLabel`。
5. 更新进度后，从详情进入时间轴独立页能看到追加记录；当前进度同步更新。
6. 现有 MCP `media` 工具（list/get/create/update）无需改 Agent 代码仍可用；相关单测 / 冒烟通过。

## 10. 实现时拆分建议

本变更跨导航、待办展示、记录域（排序 / 私密 / 分类 / 进度历史）。实现计划可按可交付增量拆成多段，例如：① 导航合并 + 色标 → ② 记录 Tab 拆出 + 排序 → ③ 私密文案 → ④ 自定义分类 → ⑤ 进度时间轴页。每段可独立手测。

---

## 决策记录

| 项 | 选择 |
|----|------|
| 今日页 | 删除（重定向），不做今日筛选 |
| 首页路由 | `/` → `/todos` |
| 截止日期色 | 逾期红 / 今天橙 / 明天黄 / ≥2 灰 |
| 伴侣 vs 记录 | 伴侣=一起做过的事；记录=原一起看过的 |
| 私密默认 | 默认共享，可勾选仅自己 |
| 私密隔离 | 藏 UI 即可，不强拦 API |
| 私密列表时间 | 有 lastWatched 显示中性文案；无则「更新于 update_time」 |
| 进度/评论在私密下 | UI 保持现状 |
| 自定义分类存储 | 仍用 `media_type`；自定义存 id |
| 删分类 | 允许；回落 `uncategorized` |
| 时间轴 | 只记进度；追加历史；独立页可进入 |
| 排序 | COALESCE(last_watched_at, DATE(update_time)) DESC |
| MCP | 现有 `media` list/get/create/update 保持可用；不强制扩展新能力 |
