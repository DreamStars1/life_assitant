# 伴侣积分流水：可选/可改记录日期

## 背景

积分流水 `partner_points.created_at` 在创建时固定为 `LocalDateTime.now()`，无法补记过去某天的加减分，也无法改正记错的日期。列表仅展示流水，无单条更新 API。用户需要：新建时可选业务日，事后可改已有流水的日期（精度到天）。

## 目标

- 新建加减分时可指定记录日期（日精度）；缺省为今天
- 双方任一方可修改任意属于该对的已有流水日期
- 继续复用 `created_at`，不新增业务日期列
- 改日期不影响 `points_balance` 与分值/原因

## 非目标

- 不新增 `record_date` 列或 DB 迁移
- 不支持修改 `pointsChange` / `reason`、不支持删除单条流水
- 仪表盘快捷加减分不加日期 UI（默认今天；补记走积分页）
- 本版不改 MCP（`points.change` / 新 action 另开）
- 不做批量改日期、不做对方确认流程

## 数据与时间规则

继续使用 `partner_points.created_at`（`LocalDateTime`）。

给定 `recordDate`（`LocalDate`）与「今天」：

| 情况 | `createdAt` 写入值 |
|------|-------------------|
| 未传 / 等于今天 | `LocalDateTime.now()` |
| 早于今天 | `recordDate.atStartOfDay()`（该日 `00:00:00`） |
| 晚于今天 | 拒绝（400） |

列表排序仍按 `created_at` 倒序。余额逻辑不变：新建仍插流水 + 更新缓存；改日期只更新该行 `created_at`。

权限：记录的 `createdBy` 须为当前用户或其伴侣（与历史列表可见范围一致）。任一方可改任一条该对流水的日期。

## API

### 扩展：记录积分变动

`POST /partner/points`

```json
{
  "pointsChange": 3,
  "reason": "洗碗",
  "recordDate": "2026-08-05"
}
```

- `recordDate` 可选；校验与时间规则同上
- 其余行为与现网一致（须绑定、写流水、更新余额）

### 新增：修改流水日期

`PATCH /partner/points/{id}`

```json
{
  "recordDate": "2026-08-05"
}
```

- `recordDate` 必填
- 须已绑定伴侣
- 记录不存在或不属于双方 → `BadRequestException`（400），与现有伴侣积分接口一致
- 未来日期 → 400
- 不修改 `pointsChange`、`reason`、余额

## 前端

主要改动：`pages/partner/dashboard/points.vue` + `api/modules/partner-points.ts` + i18n。

### 新建

- 加减分表单增加「记录日期」行，默认今天
- `van-calendar`，`max-date` = 今天（与看板在一起日期等现有用法一致）
- 提交调用 `addPoints` 时带上 `recordDate`

### 改已有流水

- 点击流水项（或日期行）→ 日历 → 确认后 `PATCH`
- 成功后刷新列表（或本地更新该条 `createdAt`）；不必重拉余额

### 展示

- 流水日期按「天」展示（如本地化日期，不含时分秒），与日精度一致

### 仪表盘

- `dashboard/index.vue` 快捷加减分保持默认今天，不加日期控件

## 错误与边界

- 未绑定：「请先绑定伴侣」（现有）
- 未来日期：400
- 非法/越权 id：400（`BadRequestException`）
- 改日期与余额解耦：任何成功的改日期都不得改变 `points_balance`

## 校验自检

对「今天 → now / 过去 → startOfDay / 未来 → 拒绝」的日期解析规则留一处可运行检查（小单测或 assert 自检即可）。

## 发版

用户可见行为变更：bump 前端 `package.json` version、后端 `application.version`，根目录 `CHANGELOG.md` 记一笔。

## 测试要点

- 不传 `recordDate`：行为与现网一致（`createdAt` 接近 now）
- 传今天：`createdAt` 为当前时刻量级，非强制零点
- 传过去日：`createdAt` 为该日 00:00:00；余额正确加减
- 传未来日：新建与改日期均 400
- 任一方可改对方创建的流水日期；未绑定或越权失败
- 改日期后余额不变；列表按新 `createdAt` 排序
- 前端：新建可选日、点流水可改日、展示不含时分秒
