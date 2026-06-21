## Why

当前数据模型存在两个核心设计错误：

1. **Event（计划）和 LifeLog（实际）用外键硬关联** — 计划≠实际，用户可能"计划跑步却实际散步了"，硬关联反而限制死。对比应该是 AI 层面的时间范围匹配，不是数据层面的外键。
2. **待办（Todo）和日程（Event）语义混淆** — "买牛奶"是截止日期的待办，"3点开会"是占据时间块的日程，不是同一回事。

同时前端页面展示没有围绕"时间"这个用户心智模型来设计。用户打开 APP 最关心的是"今天有什么"、"今天完成得怎么样"，而不是按数据类型切换页面。

## What Changes

- **BREAKING**: Event 模型去掉 `actual_*` 字段（actual_start_time / actual_end_time），Event 只负责"计划"
- **BREAKING**: 去掉 Event 和 EventRecord 之间的外键关联，EventRecord 变为独立的时间线记录
- **BREAKING**: LifeLog 从简单记录扩展为独立实体，增加 `quantity`（数量）、`unit`（单位）、`mood`（心情）、`tags`（标签）字段
- **BREAKING**: Todo 的状态从四态（pending/in_progress/done/cancelled）简化为 `is_completed` 布尔值
- **新增**：前端 4 个 Tab 重新设计：今日（时间轴聚合）、日程（日历计划/实际切换）、待办（任务清单）、我的（分析+设置）
- **修改**：今日首页改为时间轴视图，计划（蓝色块）和实际（绿色标记）叠在同一根时间轴上
- **修改**：日程页增加"计划/实际"切换模式
- **移除**：前端冗余的"数据分析"独立页面，对比分析融入"今日"时间轴和"我的"周报

## Capabilities

### New Capabilities

- `daily-timeline`: 今日首页时间轴视图，在同一根轴上叠加展示计划（Event）与实际（LifeLog），支持"我的/我们的"伴侣切换
- `calendar-actual-mode`: 日程日历的"实际"模式，日历上以类型图标显示 LifeLog 记录

### Modified Capabilities

- `event-management`: **BREAKING** Event 去掉 actual 字段，只保留计划属性；去掉 EventRecord 外键关联；增加 status 四态（planned/completed/missed/cancelled）
- `life-logging`: **BREAKING** LifeLog 增加 quantity/unit/mood/tags 字段，作为独立实体不再关联 Event
- `todo-management`: **BREAKING** Todo 状态简化为 is_completed 布尔值
- `partner-collaboration`: 伴侣共享的"我们的"视角渗透到每个页面（今日/日程/待办）
- `pwa-app`: 4 Tab 重构，今日页时间轴重构，日程页增加实际模式

## Impact

- **后端模型**：Event、LifeLog、Todo 三张表字段变更，需生成新 Alembic 迁移
- **后端 Service**：Event service 去掉 actual 相关逻辑；Todo service 简化状态逻辑；LifeLog service 增加新字段支持
- **后端 API**：Event/LifeLog/Todo 的 Pydantic schema 变更
- **前端页面**：index.vue（今日时间轴）、events/（日历+实际模式）、todos/（简化状态）、profile/（增加周报卡片）
- **测试**：test_smoke.py 适配新模型
- **数据库**：存量数据需迁移脚本处理