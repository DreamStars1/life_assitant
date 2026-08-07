## Context

看板「在一起」天数误用 `createdAt`；库中无共享起点日。积分靠 `partner_points` 全量 SUM。产品确认：保留流水，新增关系表缓存余额，并支持可编辑在一起日期。详细产品规则见 `docs/superpowers/specs/2026-08-07-partner-info-meet-date-design.md`。

## Goals / Non-Goals

**Goals:**
- `partner_info`：`partner_since` + `points_balance`
- 绑定/解绑/迁移/改日期/余额双写
- 看板天数含起始日；双入口编辑

**Non-Goals:**
- 纪念日提醒、改日期需确认、移除 `partner_id`、废弃流水、改打卡表

## Decisions

### 1. 新建 `partner_info`，保留 `user.partner_id`

关系属性放独立表；「对方是谁」仍用 `partner_id`，避免全项目改读路径。

**备选**: 只在 user 上加 `partner_since` — 无法干净挂余额缓存；独立表一次到位。

### 2. 积分：流水 + `points_balance` 缓存

`getBalance` 读缓存；`addPoints` 同事务插流水并 `+= delta`。

**备选**: 继续 SUM — 当前规模够用，但产品要求提前加缓存。

### 3. id 排序存 `user_a_id` / `user_b_id`

字典序较小为 a，唯一约束防双行。

### 4. API `PUT /identity/partner-since`

与 bind/unbind 同属 Identity；校验已绑定与日期 ≤ 今天。

### 5. 迁移回填

`partner_since = DATE(LEAST(created_at))`；`points_balance = SUM(流水)`。

### 6. 解绑删流水

余额随 `partner_info` 删除；历史流水一并删，避免再绑定后污染历史列表。

## Risks / Trade-offs

- [缓存与流水不一致] → 同事务双写；迁移后可用 SUM 校验脚本抽查
- [保留 partner_id 仍是双写关系] → 接受；本次不拆绑定模型
- [单向 partner_id 脏数据] → 迁移跳过，不自动修复

## Migration Plan

1. Flyway 建表 + 回填
2. 部署后端（读缓存、写关系）
3. 部署前端
4. Rollback：回滚应用后表可留；需回滚 schema 再 drop（会丢关系属性）

## Open Questions

- 无
