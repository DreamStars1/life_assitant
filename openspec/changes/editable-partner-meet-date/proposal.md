## Why

伴侣看板的「在一起」天数目前用账号 `createdAt` 估算，既不是绑定日，也不是真实起点；双方还可能看到不同天数。需要可编辑的共享起点日。同时积分余额每次全表 SUM 流水，随记录变多会变慢，需要在关系层缓存余额（流水仍保留做历史）。

## What Changes

- 新建 `partner_info` 表（一对情侣一行）：`partner_since`、`points_balance`
- 保留 `user.partner_id` 与 `partner_points` 流水
- 绑定创建关系行；解绑/删号删除关系行并清理双方积分流水
- `PUT /identity/partner-since` 任一方可改起点日（≤ 今天）
- 看板天数按 `partner_since` 计算（含起始日）；看板卡片 + 伴侣详情可编辑
- 积分余额读缓存；加减分同事务更新缓存
- 已绑定情侣迁移：起点 = 双方较早 `createdAt`；余额 = 流水 SUM

## Capabilities

### New Capabilities

- `partner-info`: 伴侣关系行的创建/查询/删除，含在一起日期与积分余额缓存
- `partner-meet-date`: 在一起日期的默认赋值、修改与看板展示

### Modified Capabilities

<!-- 无 openspec/specs 下已有能力需改需求级描述；积分行为变更含在 partner-info 中 -->

## Impact

- **数据库**: 新表 `partner_info` + 数据回填迁移
- **后端**: 绑定/解绑/删号；`PartnerPointsService.getBalance`/`addPoints`；新 PUT API；用户响应增加 `partnerSince`
- **前端**: store、看板天数与编辑入口、伴侣详情编辑、积分接口无契约变更（实现改读缓存）
