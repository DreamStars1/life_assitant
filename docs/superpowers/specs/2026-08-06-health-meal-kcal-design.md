# 健康管家：餐次结构化热量 + 日况消耗

**日期：** 2026-08-06
**状态：** 已确认
**前置：** `docs/superpowers/specs/2026-08-03-health-butler-design.md`

## 目标

为健康管家补齐热量结构化数据：

1. 每餐可记录结构化 `kcal`（手动与 MCP/AI 共用同一字段）
2. 当日「已摄入热量总量」= 当日各餐 `kcal` 之和
3. 今日概览展示「已摄入」与「今日消耗」；消耗以档案静息代谢为默认，允许按日覆盖
4. 趋势「平均摄入」接入近 30 日均摄入 kcal

## 决策摘要

| 项 | 结论 |
|----|------|
| 餐次热量 | `health_meal.kcal` 可空；手动 + MCP 均可写，后写覆盖 |
| 日总摄入 | 读时 `SUM(当日有值的餐 kcal)`；不落库缓存 |
| 部分餐未填 | 只加已填；不提示「部分未填」；全无则显示 `—` |
| 日消耗抽象 | **扩展现有 `health_daily` 为日况聚合**（胃 / 周期 / 能量覆盖同级）；**不**新建日消耗表 |
| 今日消耗 | `health_daily.burn_kcal ?? health_profile.resting_kcal`；皆空 → `—` |
| 近 30 日均摄入 | 对齐日均蛋白：按日加总后，对「至少一餐有 kcal」的天数求平均 |
| 历史反馈文案 | 不解析、不迁移 `feedback` 中的「约 xxx kcal」 |
| 范围外 | 体重明细筛选行为；「最舒适」占位；食物识别自动估热 |

## 数据模型

### `health_meal`

新增：

| 列 | 类型 | 说明 |
|----|------|------|
| `kcal` | INT NULL | 该餐热量；可空 |

真相源：每餐热量只存在餐次行。

### `health_daily`（日况）

语义上明确为「按日聚合」：胃状态、周期、能量覆盖同级字段。

新增：

| 列 | 类型 | 说明 |
|----|------|------|
| `burn_kcal` | INT NULL | 当日消耗覆盖；`NULL` = 未覆盖，回退静息 |

### `health_profile`

`resting_kcal` 不变：全天默认消耗基准。改档案静息后，所有未覆盖日期的「今日消耗」展示随之变化。

### 派生规则（读时计算，不落库）

| 指标 | 规则 |
|------|------|
| 已摄入热量总量 | `SUM(当日各餐 kcal)`，仅加非空；全空 → `null` / UI `—` |
| 今日消耗 | `burn_kcal ?? resting_kcal`；皆空 → `null` / UI `—` |
| `avg30IntakeKcal` | 近 30 天按日加总 → 对「至少一餐有 kcal」的天数求平均；无此类天 → `null` |

## API / MCP

路径不变，只扩字段。

### 餐次

- `PUT /health/meals`、`HealthMealResp`：可选 `kcal`（Integer，可空）
- MCP `meal_upsert`：可选 `kcal`
- 写入语义对齐现有 `proteinG`：整餐 upsert 时一并提交；空表单项 → `null`（清除该餐热量）

### 日况

- `GET/PUT /health/daily`：可选 `burnKcal`
- MCP `daily_update`：可选 `burn_kcal`
- **清空语义**（对齐档案 `targetKg`）：请求 JSON **包含** `burnKcal` 键时才写入；值为数字则设覆盖，值为 `null` 则清除覆盖并回退静息；**省略**该键则不改动。实现用 `burnKcalPresent`（或等价自定义反序列化），不新增接口。
- 响应只返回**存储的覆盖值**，不返回派生的已摄入/有效消耗（避免与 meal 双源）

### 今日概览读模型

无新接口。今日页已加载 `profile` + `daily` + `meals`，前端组装：

- 已摄入 = `Σ meals.kcal`
- 今日消耗 = `daily.burnKcal ?? profile.restingKcal`

### 趋势

- `GET /health/summary` 增加 `avg30IntakeKcal`
- 前端「平均摄入」绑定该字段，单位 `kcal`

### 校验

- `kcal` / `burnKcal` / `restingKcal`：非负整数；`> 20000` 拒写并返回可读 `message`
- 餐次 `food` 仍必填；`kcal` 可空

### Skill

更新 `.cursor/skills/health-butler-mcp/SKILL.md`：记饮食可带 `kcal`；改日况可带 `burn_kcal`；说明消耗默认 = 静息。

## UI

### 今日概览卡

- 在「晨重 / 胃状态 / 周期」下增加能量行：
  - **已摄入**：只读；无数据 `—`
  - **今日消耗**：可点；弹层填 kcal，可清空；保存写 `burnKcal`
- 可选一行副文案：有覆盖显示「已覆盖」，否则有静息时「默认静息」；实现时可按噪点取舍，数字优先

### 三餐卡与记饮食

- 卡片：与蛋白同级展示 `热量 X kcal` / `热量：待填`
- 表单字段：食物、蛋白 g、热量 kcal（可空）、反馈感受
- `feedback` 仅表示感受，不再承载热量

### 趋势

- 「平均摄入」接 `avg30IntakeKcal`；「日均蛋白」「最舒适」不变（最舒适仍占位）

### 档案

- 静息代谢编辑不变

### 视觉

- 沿用现有绿卡 / metric 样式，不另起视觉体系

## 边界与错误

- 只记 1 餐热量 → 已摄入为该值
- 有餐次但全无 `kcal` → 已摄入 `—`
- 无日况行且无静息 → 今日消耗 `—`
- 历史 feedback 热量文案不迁移
- 体重明细筛选不在本变更范围
- HTTP 错误沿用现有 400/404；App 与 MCP 共用 REST

## 自检清单

1. 早餐 `kcal=150`、午餐无 → 已摄入 150
2. 无 `burnKcal`、`restingKcal=1400` → 今日消耗 1400；覆盖 1800 → 1800；清空 → 1400
3. 两天有摄入、一天无 → `avg30IntakeKcal` = 有摄入两天的日总和平均
4. MCP `meal_upsert(kcal=…)`、`daily_update(burn_kcal=…)` 可读写

## 非目标

- 新建日消耗专用表
- 日总摄入物化/缓存表
- 从 feedback 抽热量
- 活动消耗拆分（步数、运动条目等）
- 自动食物识别估热
