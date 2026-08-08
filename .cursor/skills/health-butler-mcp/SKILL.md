---
name: health-butler-mcp
description: 通过 Life Assistant MCP health 域记录/查询健康管家数据（饮食含热量 kcal、日消耗覆盖、体重、规律等）。在用户要求用 MCP/AI 记饮食、记热量、改今日消耗、记体重、加健康规律或查健康数据时使用。
---

# 健康管家 MCP

工具名：`health`。每次调用传 `action` + 所需字段。鉴权：`Authorization: Bearer la_...`。

热量相关约定（全局）：

- `kcal` / `burn_kcal` / `resting_kcal`：非负整数，上限 **20000**
- 当日**已摄入** = 当日各餐已填 `kcal` 之和（未填的餐不计）
- 当日**消耗** = 日况 `burn_kcal` 覆盖 ?? 档案 `resting_kcal`（皆空则无值）

### 后端校验枚举（必须用下列精确值，否则 400）

| 参数 | action | 允许值 |
|------|--------|--------|
| `meal_type` | `meal_upsert` | `早餐` \| `午餐` \| `晚餐` |
| `weight_type` | `weight_add` | `晨重` \| `晚重` |
| `type_`（筛选） | `weight_list` | `晨重` \| `晚重` \| `all` |
| `level` | `tolerance_add` | `舒适` \| `低风险` \| `谨慎` \| `高风险` |
| `stars` | `trigger_add` | 整数 `1`–`5` |
| `kcal` / `burn_kcal` / `resting_kcal` | 写入类 | 整数 `0`–`20000`（可空字段省略即可） |

### App 约定可选值（后端不强制枚举，建议与 App 一致）

| 参数 | action | 建议值 |
|------|--------|--------|
| `stomach_status` | `daily_update` | `正常` \| `不适` \| `反流` |
| `cycle_phase` | `daily_update` | `月经期` \| `卵泡期` \| `排卵期` \| `黄体期` |
| `cycle_day` | `daily_update` | 正整数（第几天） |

---

## 1. 记饮食（含热量）

1. 确定 `date`（默认今天 `YYYY-MM-DD`）与 `meal_type`：`早餐` | `午餐` | `晚餐`
2. `action=meal_upsert`：

| 参数 | 必填 | 说明 |
|------|------|------|
| `date` | 是 | `YYYY-MM-DD` |
| `meal_type` | 是 | `早餐` / `午餐` / `晚餐` |
| `food` | 是 | 吃了什么 |
| `protein_g` | 否 | 蛋白 g（数字） |
| `kcal` | 否 | **该餐热量**（整数）。省略则不改/不写该字段；MCP **不能**单独清空已有 kcal（需 App 表单清空） |
| `feedback` | 否 | 饭后感受（勿把热量写进 feedback） |

3. 同日同餐次再次 upsert 即整餐覆盖（传入的 `kcal`/`protein_g`/`feedback` 会写入；未传的可选字段按工具实现：未传则不带上 body）。

示例：

```text
action=meal_upsert  date=2026-08-06  meal_type=早餐  food=水煮蛋2个  protein_g=13  kcal=150
```

## 2. 查询饮食

- `action=meal_list`，参数 `date`（必填）
- 返回该日各餐：`food` / `protein_g` / `kcal` / `feedback`

## 3. 记体重

- `action=weight_add`
- 参数：`date`，`weight_type`=`晨重`|`晚重`，`kg`，可选 `standard`（晨重建议 true）

## 4. 增加健康规律

- 可选先 `memory_list`（`page`,`page_size`）
- `action=memory_add`：`title` 必填，`detail` 可选
- 上限 100 条；满则提示删旧再加

## 5. 档案（目标体重 / 静息代谢）

- `action=profile_update`，按需传：
  - `target_kg` — 目标体重
  - `resting_kcal` — **静息代谢**（整数 kcal，作「今日消耗」默认值）
  - 以及 `display_name` / `motto` / `height_cm`
- 省略的字段不改；清空目标请在 App 操作（MCP 省略不会清空）

## 6. 日况（胃 / 周期 / 消耗覆盖）

读写：

- `action=daily_get`，参数 `date`
- `action=daily_update`，参数 `date` + 要改的字段

| 参数 | 说明 |
|------|------|
| `stomach_status` / `stomach_note` | 胃状态 |
| `cycle_phase` / `cycle_day` | 周期 |
| `burn_kcal` | 设置**当日消耗覆盖**（整数）。有值则今日消耗显示为该值 |
| `clear_burn_kcal` | 设为 `true` 时**清除**当日覆盖，消耗回退档案 `resting_kcal` |

消耗三态（**勿混用**）：

1. 设置覆盖：只传 `burn_kcal=<int>`
2. 清除覆盖：只传 `clear_burn_kcal=true`（不要同时传 `burn_kcal`）
3. 两者都省略：当日消耗覆盖不变

同时传 `burn_kcal` 与 `clear_burn_kcal=true` → 返回 `invalid_combination`，不会写入。

示例：

```text
# 今天实际消耗 1800（覆盖静息）
action=daily_update  date=2026-08-06  burn_kcal=1800

# 取消覆盖，回退静息
action=daily_update  date=2026-08-06  clear_burn_kcal=true
```

## 7. 耐受 / 触发（翻页）

- `tolerance_list`（可选 `page`、`page_size`）/ `tolerance_add`（`name`、`level`：舒适/低风险/谨慎/高风险）
- `trigger_list`（可选 `page`、`page_size`）/ `trigger_add`（`name`、`stars` 1–5，`note` 可选）

## 8. 趋势摘要

- `action=summary`（无额外参数）
- 主要字段含：
  - 最近晨重、近 7 日均晨重、目标体重、距目标
  - `avg30ProteinG` — 近 30 日日均蛋白（仅统计有蛋白的天）
  - `avg30IntakeKcal` — 近 30 日日均**摄入热量**（按日加总各餐 `kcal`，再对「至少一餐有 kcal」的天数求平均）

## 常用 action 一览

`profile_get` | `profile_update` | `meal_list` | `meal_upsert` | `meal_delete` | `weight_list` | `weight_add` | `daily_get` | `daily_update` | `memory_list` | `memory_add` | `tolerance_list` | `tolerance_add` | `trigger_list` | `trigger_add` | `summary`
