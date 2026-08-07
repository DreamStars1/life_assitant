## 1. 数据库

- [ ] 1.1 Flyway 建 `partner_info`（含唯一约束与审计字段）
- [ ] 1.2 同迁移回填：双向 `partner_id` 对 → `partner_since` / `points_balance`
- [ ] 1.3 实体 + Mapper/`PartnerInfoService`（按双方 id 排序查找/创建/更新/删除）

## 2. 后端 — 生命周期

- [ ] 2.1 `bind-partner`：创建 `partner_info`（since=今天，balance=0）
- [ ] 2.2 `unbindPartner`：删 `partner_info` + 删双方积分流水 + 清 `partner_id`
- [ ] 2.3 删号路径：有伴侣时删 `partner_info`（及流水）并清对方 `partner_id`
- [ ] 2.4 `PUT /identity/partner-since`：校验并更新 `partner_since`
- [ ] 2.5 `UserPublicResp` 暴露 `partnerSince`（从 `partner_info` 组装）

## 3. 后端 — 积分缓存

- [ ] 3.1 `getBalance` 改读 `points_balance`
- [ ] 3.2 `addPoints` 同事务更新 `points_balance += delta`

## 4. 前端

- [ ] 4.1 `UserState` / `setInfo` 映射 `partnerSince`
- [ ] 4.2 API：`PUT /identity/partner-since`
- [ ] 4.3 看板天数改为 `(today - since) + 1`；无值显示 0
- [ ] 4.4 看板纪念日卡片可编辑日期
- [ ] 4.5 伴侣详情页可编辑日期
- [ ] 4.6 文案保持「在一起」；按需补保存成功等提示
