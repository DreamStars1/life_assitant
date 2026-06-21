## Why

用户绑定伴侣后没有任何解除绑定的途径。当前唯一能"换人"的方式是删除自己账号再重新注册，且 `DELETE /users/me` 并未清理对方 `partner_id`，会导致 dangling reference。这是一个基本功能缺失。

## What Changes

- 新增 `POST /identity/unbind-partner` 端点，双向清除双方的 `partner_id`
- 前端 `share/index.vue`（伴侣页）增加解除绑定入口，带二次确认
- 前端 `profile/index.vue`（个人中心）在已绑定伴侣的 cell 上增加解绑操作
- 解除后用户自己创建的 `shared_record` 保留但不再对前伴侣可见
- 修复 `DELETE /users/me` 中未清理对方 `partner_id` 的旁路问题

## Capabilities

### New Capabilities

- `partner-unbind`: 允许用户解除与当前伴侣的绑定关系，双向清理 `partner_id`

### Modified Capabilities

<!-- 无现有 spec 需要修改 -->

## Impact

- **后端**: `IdentityController` 新增 `/identity/unbind-partner`；`UserController.deleteMe` 需增加伴侣清理逻辑
- **前端**: `src/pages/share/index.vue` 新增解绑按钮；`src/pages/profile/index.vue` 新增解绑入口
- **数据库**: 无 schema 变更，仅 `user.partner_id` 字段值变更
- **依赖**: `SharedRecordService` 的 `requirePartner()` 逻辑不受影响（解除后 `partner_id` 为 null 自然拦截）
