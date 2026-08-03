---
name: health-butler-mcp
description: 通过 Life Assistant MCP health 域记录/查询健康管家数据（饮食、体重、规律等）。在用户要求用 MCP/AI 记饮食、记体重、加健康规律或查健康数据时使用。
---

# 健康管家 MCP

工具名：`health`。每次调用传 `action` + 所需字段。鉴权使用用户 API Token（`Authorization: Bearer la_...`）。

## 1. 记饮食

1. 确定 `date`（默认今天 `YYYY-MM-DD`）与 `meal_type`：`早餐` | `午餐` | `晚餐`
2. 调用 `action=meal_upsert`，参数：
   - `date`（必填）
   - `meal_type`（必填）
   - `food`（必填，吃了什么）
   - `protein_g`（可选，数字）
   - `feedback`（可选，饭后感受）
3. 同日同餐次再次 upsert 即覆盖。

## 2. 查询饮食

- `action=meal_list`，参数 `date`
- 返回该日各餐 food / protein_g / feedback

## 3. 记体重

- `action=weight_add`
- 参数：`date`，`weight_type`=`晨重`|`晚重`，`kg`，可选 `standard`（晨重建议 true）

## 4. 增加健康规律

- 先可选 `memory_list`（`page`,`page_size`）查看
- `action=memory_add`：`title` 必填，`detail` 可选
- 上限 100 条；失败时提示用户删旧再加

## 5. 设目标体重

- `action=profile_update`，参数 `target_kg`（数字）
- 清空目标请在 App 操作（MCP 省略字段不会清空已有目标）

## 6. 胃状态 / 周期

- `action=daily_update`
- 参数：`date`，以及 `stomach_status`/`stomach_note`/`cycle_phase`/`cycle_day` 中需要的字段

## 7. 耐受 / 触发（翻页）

- `action=tolerance_list`（可选 `page`、`page_size`）/ `action=tolerance_add`（`name`、`level`）
- `action=trigger_list`（可选 `page`、`page_size`）/ `action=trigger_add`（`name`、`stars` 1–5，`note` 可选）

## 8. 趋势摘要

- `action=summary` → 7 日均晨重、目标缺口、近 30 日均蛋白
