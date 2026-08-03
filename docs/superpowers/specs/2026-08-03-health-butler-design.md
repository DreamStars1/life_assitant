# 健康管家（小满）接入 App + MCP 设计

**日期：** 2026-08-03
**状态：** 已确认
**原型真源：** `docs/原型图/index.html`

## 目标

将健康管家原型落地到 Life Assistant 移动端，并从第一天起用 Java 后端持久化；对外通过现有 Python MCP Agent 新增 `health` 域，供其他 AI 录入/查询。App 内不做对话式交互。

## 决策摘要

| 项 | 结论 |
|----|------|
| 范围 | 完整三页：今日情况 / 身体档案 / 趋势 |
| 数据 | 后端持久化；App 与 MCP 共用同一 REST |
| MCP | 仅 Server 侧：新增域工具 `health`（域路由风格） |
| 可见性 | 仅本人（`user_id` 隔离）；不与伴侣共享 |
| App 交互 | 结构化页面 + 表单/弹层；无聊天窗 |
| 入口 | 「我的」右下角浮动按钮：Q 版医生头像 +「健康管家」 |
| Skill | 工作流手册（参数与步骤）；不写隐私说明 |

## 架构

```
Profile 浮动入口 ──► /health（前端三 Tab）
                         │
                         ▼
                Java REST /api/health/*
                （JWT，仅当前用户）
                         ▲
外部 AI ──MCP──► Python Agent health(action=...)
                         │
                         └── Bearer la_xxx ──► 同一套 REST
```

## 前端

### 入口

- 页面：`/profile`（「我的」）
- 右下角浮动按钮：Q 版医生头像 + 文案「健康管家」（非用户头像）
- 点击进入 `/health`；需登录；不加入 `rootRouteList`（保留返回箭头，不占主 TabBar）

### `/health` 三页（对齐原型）

**今日情况**

- 顶部概览：晨重；目标缺口可点设置（未设显示「待设置」）；胃状态、周期可点修改（按日）
- 日期切换器居中；无「今日记录」标题；右侧「记饮食」
- **进入默认今天**（不恢复上次选中日期）
- 三餐卡片：食物、`protein_g`、反馈；点卡片或「记饮食」录入（餐次为参数：早餐/午餐/晚餐）

**身体档案**

- 基础档案（身高、目标体重、静息代谢）+ 鼓励文案（motto）
- 个人健康规律 / 常见耐受 / 胃部触发因素：左右翻页 +「添加」
- 规律上限 100 条

**趋势**

- 「记体重」；晨重/晚重/全部筛选；折线 + 明细
- 近 30 天饮食统计；日均蛋白由有 `protein_g` 的日期聚合

### 实现约定

- Vue 3 + Vant；`prompt` 仅存于原型，App 用弹层/表单
- i18n：`navbar` 等按现有惯例补文案
- 视觉：沿用原型色板（绿 `#315d43`、lime `#cfe681` 等），与现有 App 壳层兼容即可

## 数据模型

均含 `user_id`；查询必须带当前用户过滤。

| 表 | 说明 |
|----|------|
| `health_profile` | 一人一行：展示名、motto、身高、目标体重（可空）、静息代谢等 |
| `health_meal` | `user_id + date + meal_type` 唯一；`food`、`protein_g`（可空）、`feedback` |
| `health_weight` | 日期、晨/晚、kg、是否纳入趋势 |
| `health_daily` | `user_id + date` 唯一；胃状态 status/note；周期 phase/day |
| `health_memory` | title、detail；最多 100；分页 |
| `health_tolerance` | 名称、level；分页 |
| `health_trigger` | 名称、note、stars(1–5)；分页 |

分页约定（memory / tolerance / trigger）：`page` 从 1、`pageSize` 默认与原型一屏一致（规律 5、耐受 4、触发 3）；响应含 `total` + `items`。

不做：通用 `health_log` 表（饮食以 meal 为准）；导出/恢复初始档案。

## REST

前缀：`/api/health`（具体路径实现时可微调，保持资源化、无动词路径）。

- `GET/PUT /profile`
- `GET /meals?date=`；`POST/PATCH/DELETE` 单餐
- `GET/POST /weights`（及必要筛选）
- `GET/PUT /daily?date=`
- `GET/POST/PATCH/DELETE` memory | tolerance | trigger（list 带 page/pageSize）
- `GET /summary`：趋势聚合、近 30 日均蛋白等

鉴权：现有 JWT。MCP 走现有 API Token → 用户身份，再调同一 Service。

错误：资源不存在或非本人 → 404；规律满 100 → 400；校验失败可读 `message`。

## MCP

- 在 Python Agent 增加第 7 个域工具 `health`，`action` 分发，风格对齐 `todo` / `checkin`
- 覆盖：profile、meal、weight、daily、memory、tolerance、trigger、summary 的读写
- 记饮食：`meal_type` 为参数（早餐|午餐|晚餐），不是单独的「只记早餐」工具

### Skill

路径：`.cursor/skills/health-butler-mcp/SKILL.md`

内容：按工作流写清操作步骤、`action`、参数含义与示例。至少包括：

1. 记饮食（餐次参数 + date/food/protein_g/feedback）
2. 记体重
3. 增加健康规律
4. 查询饮食记录
5. 设目标体重、改胃状态/周期、翻页查耐受/触发

不写隐私边界说明（鉴权已限制本人数据）。

## 测试与验收

- 后端：CRUD + 分页；满 100；跨用户隔离；summary 聚合正确
- 前端：入口进入三页；默认今天；蛋白与目标/胃/周期可编辑；档案翻页添加；趋势记重
- MCP：Token 记一餐 ↔ App 可见；App 改目标 ↔ MCP 读一致；Skill 工作流可照做

## 非目标（本轮）

- App 内 AI 对话
- 伴侣共享健康数据
- 导出 / 恢复初始档案
- 食物识别自动估热量或蛋白

## 参考

- 交互原型：`docs/原型图/index.html`
- MCP 域路由：`docs/superpowers/specs/2026-07-19-mcp-domain-router-design.md`
