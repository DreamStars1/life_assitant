## Context

当前后端基于 FastAPI + SQLModel，前端基于 Vue3 + Vant 4。已有模型 Todo、Event、EventRecord、LifeLog 之间存在不合理的外键耦合和字段设计。用户的核心心智模型是"按时间查看一天"，而非按数据类型切换页面。

## Goals / Non-Goals

**Goals:**

- 数据层解耦：Event（计划）、LifeLog（实际）、Todo（待办）三个独立实体，无外键关联
- 前端 4 Tab 重构：今日（时间轴聚合）/ 日程（日历）/ 待办（清单）/ 我的（分析）
- 今日时间轴：计划（蓝色块）和实际（绿色标记）叠在同一根轴上
- 日程页支持"计划/实际"切换模式
- Todo 简化：四态 → is_completed 布尔值

**Non-Goals:**

- 不改变用户认证流程
- 不引入新的外部依赖
- 不做原生 APP/多租户/国际化改造
- 对比分析依赖 AI 阶段（已有 Agent 基础），本次只做数据展示

## Decisions

### D-1: Event 去掉 actual 字段

**选择**：Event 表移除 `actual_start_time`、`actual_end_time`，只保留计划时间。

**理由**：实际时间应由 LifeLog 独立记录。Event 和 LifeLog 通过时间范围匹配做对比，不靠外键。这解决了"计划跑步但实际散步"无法表达的问题。

**替代方案**：保留 actual 字段同时关联 LifeLog — 不可行，一对多（一个计划可能对应多条实际记录）和多对一（多个计划对应一条实际记录）关系无法简单建模。

### D-2: LifeLog 独立扩展

**选择**：LifeLog 增加 `quantity`（float, 数量）、`unit`（str, 单位）、`mood`（int, 1-10）、`tags`（JSON array）字段。

**理由**：用户记录"跑了5公里"需要数量+单位，"心情不错"需要 mood 分。独立实体让录入更自由。

### D-3: Todo 简化

**选择**：`status` 四态 → `is_completed` 布尔值 + `completed_at` 时间戳。

**理由**：情侣场景不需要复杂的 in_progress 工作流。pending/done/cancelled 用 `is_completed=false` + `cancelled_at` 即可表达。减少 UI 复杂度。

### D-4: EventRecord 独立化

**选择**：EventRecord 去掉 `event_id` 外键，改名 `TimelineEntry`，加 `source_type` 字段标记来源（event/lifelog/manual）。

**理由**：今日时间轴上需要展示来自多个来源的条目。统一的时间线表比分散查询更高效。

### D-5: 前端 Tab 结构

**选择**：底部 Tab 改为 今日 / 日程 / 待办 / 我的。

- **今日**：时间轴聚合，同一根轴上 Event（蓝色块）和 LifeLog（绿色点）按时间排列
- **日程**：月/周/日三视图 + "计划/实际"切换开关
- **待办**：按"今天/即将/已完成"分组
- **我的**：个人信息 + 本周统计卡片 + 设置入口

### D-6: 伴侣切换

**选择**：今日和日程页顶部增加"我的/我们的"切换开关，切换后时间轴/日历展示双方数据。

**理由**：伴侣需要快速切换看自己的计划 vs 双方共同的安排，而不是跳到另一个页面。

## 数据模型变更

```
Todo（待办）
├─ id: UUID
├─ user_id: FK→user
├─ title: str
├─ description: str?
├─ due_date: datetime?
├─ priority: str (low/medium/high/urgent)
├─ is_completed: bool          ← 从 status 简化为布尔值
├─ completed_at: datetime?     ← 新增
├─ cancelled_at: datetime?     ← 新增
├─ category: str?
├─ assigned_to: FK→user?
├─ assigned_by: FK→user?
├─ created_at: datetime

Event（日程/计划）
├─ id: UUID
├─ user_id: FK→user
├─ title: str
├─ start_time: datetime        ← 原名 planned_start_time
├─ end_time: datetime?         ← 原名 planned_end_time
├─ status: str                 ← 新增（planned/completed/missed/cancelled）
│  (planned/completed/missed/cancelled)
├─ category: str?
├─ location: str?              ← 新增
├─ remind_before: int?
├─ repeat_rule: JSON?
├─ shared_with_partner: bool   ← 原名 is_shared
├─ created_at: datetime

TimelineEntry（时间线条目，原名 EventRecord）
├─ id: UUID
├─ user_id: FK→user
├─ source_type: str            ← 新增（event/lifelog/manual）
├─ source_id: UUID?            ← 可选关联源
├─ content: str
├─ image_url: str?
├─ logged_at: datetime?        ← 实际发生时间
├─ created_at: datetime

LifeLog（生活记录/实际）
├─ id: UUID
├─ user_id: FK→user
├─ log_type: str
├─ content: str
├─ quantity: float?            ← 新增
├─ unit: str?                  ← 新增
├─ mood: int?                  ← 新增（1-10）
├─ tags: JSON?                 ← 新增
├─ metadata_json: JSON?
├─ logged_at: datetime?
├─ created_at: datetime
```

## 前端展示变更

```
Tab 1: 今日（每日总览）
├─ 日期标题 + 伴侣切换
├─ 时间轴视图
│  ├─ 蓝色块 = Event（计划）
│  ├─ 绿色图标 = LifeLog（实际）
│  ├─ 灰色虚线 = 被取消的计划
│  └─ 时间轴上可点击展开详情
├─ 今日待办（到期提醒列表）
└─ [+ 记一笔] 快速添加

Tab 2: 日程（日历）
├─ 月/周/日 三视图切换
├─ "计划"模式：显示 Events，按状态着色
├─ "实际"模式：显示 LifeLogs，按类型图标着色
├─ 伴侣切换：我的/我们的
└─ 点击日期 → 新建日程 / 查看当天记录

Tab 3: 待办
├─ 按到期分组：今天/即将/已完成
├─ 勾选完成
└─ 新建/编辑/删除

Tab 4: 我的
├─ 个人信息（头像/名称/伴侣）
├─ 本周统计卡片（计划完成率/运动 vs 实际）
├─ 设置入口
└─ 退出登录
```

## 迁移计划

1. 生成 Alembic 迁移：Event 去字段 + 加字段，LifeLog 加字段，Todo 改字段，创建 TimelineEntry 表
2. 存量数据迁移脚本：EventRecord → TimelineEntry（source_type=event），旧 status 映射到 is_completed
3. 后端 Service/API 适配新模型
4. 前端 4 Tab 重构
5. 测试适配

## Risks / Trade-offs

| 风险 | 缓解 |
|------|------|
| 存量数据迁移复杂 | 编写明确的一对一迁移脚本，先备份再执行 |
| 前端改造成本高（4 Tab + 时间轴） | 分步实施：先改后端模型，再依次改造今日→日程→待办→我的 |
| 时间轴性能（大量条目渲染） | 首版只加载当天数据 + 虚拟滚动 |
| "我们的"视图查询复杂度 | 按需加载，先读本地缓存再请求 API |
