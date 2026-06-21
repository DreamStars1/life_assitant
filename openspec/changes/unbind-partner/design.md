## Context

当前 `IdentityController` 已实现伴侣绑定（`POST /identity/bind-partner`），user 表通过 `partner_id` 自引用字段实现 1:1 双向绑定。用户一旦绑定后无任何解除途径，且 `DELETE /users/me` 未清理对方 `partner_id`（产生 dangling reference）。

## Goals / Non-Goals

**Goals:**
- 提供 `POST /identity/unbind-partner` 接口，双向清除双方 `partner_id`
- 前端在"我的" → 伴侣详情页提供解除入口，带二次确认
- 修复 `DELETE /users/me` 未清理前伴侣 `partner_id` 的旁路问题

**Non-Goals:**
- 不涉及 shared_record 所有权转移（直接删除）
- 不改变现有的 1:1 绑定模型（ponytail: 升级到多伴侣需多对多中间表）

## Decisions

### 1. unbind 端点放在 IdentityController
放在 `IdentityController` 而非新建 controller，与 `bindPartner` 在同一处管理伴侣生命周期。方法签名:
```java
@Operation(summary = "解除伴侣绑定")
@PostMapping("/identity/unbind-partner")
@Transactional
public Map<String, String> unbindPartner(@CurrentUser UserDO me)
```
逻辑：校验 `partnerId != null` → 获取对方 → 双向置 null → 保存 → 返回 `{"message": "已解除伴侣绑定"}`。

**备选**: 放在 UserController — 但伴侣生命周期与身份（identity）更相关，且 `bindPartner` 已在此处。

### 2. 前端解除入口：伴侣详情页
**"我的" → 点击"伴侣" → 伴侣详情页**。新建 `src/pages/partner/detail.vue`，自动注册 `/partner/detail` 路由。

个人中心 `profile/index.vue` 的伴侣 cell 改为始终 `is-link`，点击跳转到 `/partner/detail`。

伴侣详情页包含：
- **未绑定状态**：显示"尚未绑定伴侣"，提供跳转到 `/share` 绑定的按钮
- **已绑定状态**：展示伴侣头像（占位）、名称，底部"解除伴侣绑定"按钮，点击弹出 `showConfirmDialog`（提示记录将被永久删除、对方不会收到通知），确认后调 `POST /identity/unbind-partner`，成功后跳转回个人中心并刷新

**备选**: 在 share 页也加解除按钮 → 用户明确要求只放在伴侣详情页，避免入口分散。

### 3. DELETE /users/me 伴侣清理
在 `UserController.deleteMe` 中，删除用户前检查 `partnerId`，若存在则获取对方并置 null。

### 4. 解除时删除 shared_record
`unbind-partner` 端点在同一 `@Transactional` 事务中，使用 `LambdaQueryWrapper` 或 Mapper 方法删除双方所有的 shared_record：
```java
mapper.delete(new LambdaQueryWrapper<SharedRecordDO>()
    .in(SharedRecordDO::getCreatedBy, List.of(me.getId(), partner.getId())));
```
MyBatis-Plus 的 `BaseMapper` 自带 `delete(Wrapper)`，无需新增 Mapper 方法。

## Risks / Trade-offs

- [对方可能不知情] 解除是单方面操作，不通知对方。→ 前端在确认对话框提示"对方不会收到通知"。
- [数据不可恢复] 解除后 shared_record 被删除。→ 确认对话框需明确提示记录将被永久删除。
