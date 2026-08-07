# 伴侣关系表 + 可编辑「在一起」日期

## 背景

看板「在一起」天数目前用当前用户 `createdAt`（账号注册日）计算，不是认识/在一起起点，且双方看到的天数可能不一致。用户往往早已认识才绑定 App，需要可设定、可修改的共享起点日。

积分余额目前每次对 `partner_points` 全量 `SUM`。流水需保留做历史；同时在关系层缓存余额，避免随流水变长而变慢。

## 目标

- 新建 `partner_info`：一对情侣一行，存 `partner_since` 与 `points_balance`
- 保留 `user.partner_id` 与 `partner_points` 流水
- 可编辑在一起起点；看板天数按该日期计算（含起始日）
- 加减分：插流水 + 更新余额缓存（同事务）
- 已绑定情侣迁移：起点用双方较早 `createdAt`；余额用现有流水 SUM 回填

## 非目标

- 纪念日提醒 / 推送
- 改日期需对方确认
- 去掉 `user.partner_id`
- 把流水迁入 `partner_info` 或废弃流水表
- 本次不改打卡表结构

## 数据模型

### `partner_info`

| 字段 | 类型 | 说明 |
|------|------|------|
| `id` | CHAR(36) | 主键 |
| `user_a_id` | CHAR(36) | 一方（两 id 字典序较小者） |
| `user_b_id` | CHAR(36) | 另一方 |
| `partner_since` | DATE NOT NULL | 在一起起点（绑定与迁移均写入） |
| `points_balance` | INT NOT NULL DEFAULT 0 | 积分余额缓存 |
| `created_at` | DATETIME | 创建时间 |
| `update_time` | DATETIME | 更新时间 |

- 唯一约束 `(user_a_id, user_b_id)`
- 写入前对两 id 排序，避免 A-B / B-A 双行
- `user.partner_id` 保留，继续表示「对方是谁」

### 与现有表关系

- `partner_points`：流水不变；历史列表仍查流水；余额改读 `points_balance`
- 解绑：删 `partner_info`；按双方 `created_by` 删除积分流水（`PartnerPointsService.deleteByUsers`）；清双方 `partner_id`；共享记录删除逻辑保持现状

## 行为

### 绑定

双向写 `partner_id`；插入 `partner_info`：`partner_since = 当天`，`points_balance = 0`。

### 解绑 / 删号

- 解绑：清 `partner_id`、删 `partner_info`、删双方积分流水、删共享记录（现有）
- `DELETE /users/me`：若有伴侣，清对方 `partner_id`，并删该对 `partner_info`（及流水，与解绑一致）

### 更新在一起日期

`PUT /identity/partner-since`，body：`{ "partnerSince": "yyyy-MM-dd" }`

- 须已绑定
- 日期 ≤ 今天（服务端日），否则 400
- 更新该对 `partner_info.partner_since`
- 任一方可改，立即对双方生效
- 返回带 `partnerSince` 的用户公开信息

### 积分

- `getBalance`：读 `partner_info.points_balance`
- `addPoints`：同事务插入流水 + `points_balance += pointsChange`

### 天数展示

- 公式：`今天 - partnerSince + 1`（当天为第 1 天）
- 无日期时显示 0
- UI 文案保持「在一起」

### 编辑入口

- 看板「在一起」卡片可点选日期并保存
- 伴侣详情页同样可看/改

## 迁移

Flyway（如 `V18__create_partner_info.sql`）：

1. 建表
2. 数据回填：对每对双向 `partner_id`
   - `partner_since` = `DATE(LEAST(a.created_at, b.created_at))`
   - `points_balance` = `COALESCE(SUM(points_change) WHERE created_by IN (a,b), 0)`
   - `user_a_id` / `user_b_id` = 排序后的两 id

单向脏数据：跳过或仅日志；不自动修 `partner_id`。

## API / 前端数据

- `UserPublicResp` / 前端 `UserState` 增加 `partnerSince`
- 用户信息接口组装时从 `partner_info` 读取（有伴侣才查）

## 测试要点

- 绑定后双方有相同 `partner_since`（当天）与余额 0
- 改日期双边一致；未来日期拒绝
- 加减分后余额与流水 SUM 一致
- 解绑后无 `partner_info`、无双方流水
- 迁移后已绑定对：起点为较早注册日、余额等于历史 SUM
- 看板天数 = 差额 + 1
