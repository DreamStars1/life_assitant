# 积分流水改日期：对方确认（cc 发起跳过）

## 背景

`PATCH /partner/points/{id}` 改流水日期目前即时生效（见 `2026-08-08-partner-points-record-date-design.md`）。需要在改已有流水日期时增加对方确认；发起人昵称为 `cc` 时跳过确认、仍即时生效。曾考虑 Redis 存 pending，最终选用 MySQL 挂字段，避免列表双源拼装，并与待办 ack 心智一致。

## 目标

- 非 `cc` 用户修改已有积分流水日期时，写入待确认申请，**不改**生效日期，直至对方同意
- 对方可单条同意 / 拒绝，或一键同意全部待其确认的申请
- 发起人 `fullName === 'cc'`（精确匹配）时跳过确认，行为与现网 PATCH 一致
- 积分页展示待确认状态，并用小红点提示「待我确认」数量
- 待确认超过 24 小时视为过期作废

## 非目标

- 新建加减分（含选非今天）不走确认
- 不做推送、不做独立待确认页
- 不做一键拒绝、不做发起人撤回
- 不改 MCP；不改 `pointsChange` / `reason` / 余额
- 不用 Redis 存 pending

## 数据模型

Flyway 迁移扩展 `partner_points`：

| 列 | 类型 | 说明 |
|----|------|------|
| `pending_record_date` | `DATE` NULL | 申请改为的业务日；无申请为 NULL |
| `pending_requested_by` | `CHAR(36)` NULL | 发起人用户 ID |
| `pending_requested_at` | `DATETIME` NULL | 发起时间；用于 24h 过期 |

同一流水最多一条 pending；再次申请则**覆盖**这三列。无 pending 时三列均为 NULL。

生效日期仍只看 `created_at`（日精度规则不变：今天→now，过去→startOfDay，未来→400）。

### 过期

常量 **TTL = 24 小时**（自 `pending_requested_at` 起）。在列表组装、单条同意/拒绝、一键全部确认时：若已过期，清空三列（懒清理）；对已过期申请执行同意 → `400`「申请已过期」。

## cc 判定

后端读取发起人 `UserDO.fullName`，与字符串 `'cc'` **精确匹配**（与柴犬宠白名单一致）。

- 仅影响「发起改日期」：匹配则直接写 `created_at` 并清空任何既有 pending
- `cc` 作为确认方时，仍正常使用同意 / 拒绝 / 一键确认

前端可用 `fullName === 'cc'` 优化文案，**以服务端响应为准**。

## 权限

- 改日期 / 建 pending：须已绑定；记录 `createdBy` ∈ {自己, 伴侣}（与现网一致）
- 同意 / 拒绝 / 一键确认：操作者必须是该 pending 的**发起人的伴侣**（不能自己批自己）
- 列表可见范围不变

## API

### 修改流水日期（行为变更）

`PATCH /partner/points/{id}` body：`{ "recordDate": "YYYY-MM-DD" }`（必填）

| 发起人 | 行为 |
|--------|------|
| `fullName === 'cc'` | 直接更新 `created_at`（现有 `resolveCreatedAt`）；清空 pending 三列；响应标明已生效 |
| 其他 | 写入/覆盖 pending 三列；**不改** `created_at`；响应标明待确认 |

未来日 → 400。不碰余额。

### 单条同意

`POST /partner/points/{id}/date-change/approve`

- 校验：有未过期 pending；当前用户 = 发起人的伴侣
- 将 `pending_record_date` 经 `resolveCreatedAt` 写入 `created_at`，清空 pending
- 无 pending / 过期 / 非确认方 → 400

### 单条拒绝

`POST /partner/points/{id}/date-change/reject`

- 校验同同意（确认方 + 未过期 pending）
- 仅清空 pending 三列

### 一键全部确认

`POST /partner/points/date-change/approve-all`

- 找出当前用户作为确认方、未过期的全部 pending，逐条套用与单条同意相同规则
- 返回同意条数（0 条亦 200）
- 本版**不提供**一键拒绝

### 历史列表（扩展）

现有分页 history 每条增加 pending 展示字段（若未过期），例如：

- `pendingRecordDate` / `pendingRequestedBy` / `pendingRequestedAt`（或等价命名）
- 响应级或同包字段：`pendingConfirmCount` = 待**当前用户**确认的未过期条数（供小红点）

## 前端（`points.vue` + API + i18n）

- 改日期交互不变（点流水 → 日历 → PATCH）
  - 已生效 → toast「已更新日期」
  - 待确认 → toast「已提交，等待对方确认」
- 列表项有未过期 pending：展示「申请改为 … · 待确认」
  - 当前用户为确认方：同意 / 拒绝按钮
  - 当前用户为发起人：仅文案（不可撤回）
- 有待我确认时：顶部「全部确认」；入口/标题旁小红点（`pendingConfirmCount`）
- 本版不做推送；进页刷新 count 即可

## 错误与边界

| 情况 | 行为 |
|------|------|
| 未来日 | 400 |
| 无 pending 时同意/拒绝 | 400 |
| 过期后同意 | 清空 + 400「申请已过期」 |
| 自己批自己 | 400 |
| 未绑定 / 越权 | 与现网伴侣积分一致 |
| 同意时跨日导致「未来」 | `resolveCreatedAt` 拒绝 → 400，**保留** pending，便于次日或改申请后再试 |
| 任意成功路径 | 不得改变 `points_balance` |

## 发版

用户可见行为变更：bump 前端 `package.json` version、后端 `application.version`，根目录 `CHANGELOG.md` 记一笔。

## 测试要点

- 非 cc PATCH → pending 有值、`created_at` 不变；cc PATCH → `created_at` 变、无 pending
- 同意 → 日期变、pending 清空；拒绝 → 日期不变、pending 清空
- 再申请覆盖旧 pending
- 超过 24h：列表不展示 pending；同意 400
- approve-all 批量同意；余额始终不变
- 前端：红点、「全部确认」、确认方按钮 / 发起人只读文案
