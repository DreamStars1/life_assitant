## 1. 后端 — 解除绑定 API

- [x] 1.1 在 `IdentityController` 新增 `POST /identity/unbind-partner` 方法，加 `@Transactional`
- [x] 1.2 在 unbind 事务中加入 `shared_record` 删除逻辑：删除双方所有 `created_by IN (me.id, partner.id)` 的记录

## 2. 后端 — 修复用户删除时的伴侣清理

- [x] 2.1 在 `UserController.deleteMe` 中，删除用户前检查 `partnerId`，若存在则获取对方并置 null

## 3. 前端 — 伴侣详情页（新页面）

- [x] 3.1 新建 `src/pages/partner/detail.vue`：未绑定态显示提示和去绑定按钮；已绑定态展示伴侣名称 + "解除伴侣绑定"按钮（`showConfirmDialog` 确认，提示记录将被删除），成功后跳转个人中心
- [x] 3.2 修改 `src/pages/profile/index.vue`：伴侣 cell 改为始终 `is-link`，跳转到 `/partner/detail`

## 4. 前端 — 国际化

- [x] 4.1 在 `src/locales/zh-CN.json` 添加解除绑定相关文案（`unbind`, `unbindConfirm`, `unbindWarning` 等 key）
