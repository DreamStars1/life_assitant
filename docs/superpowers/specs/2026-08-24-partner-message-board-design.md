# 伴侣留言板 +「一起做过的事」迁入看板二级页

## 背景

当前 IA：

| Tab / 入口 | 实际内容 |
|---|---|
| Tab「伴侣」`/share` | 绑定引导 +「一起做过的事」列表 |
| 看板「共享记录」统计卡 | 跳转到同一 `/share` |
| Tab「记录」`/records` | 共享影视；详情内才有类聊天留言 |
| 积分 / 待办 | 独立能力，与留言无关 |

产品目标：Tab「伴侣」改为双方气泡留言板；「一起做过的事」改为从看板「共享记录」进入的二级页；文字留言可选用开关同步记事、建待办、加减积分。

## 目标

1. 「一起做过的事」仅作为看板「共享记录」二级页（路由如 `/partner/dashboard/shared-records`）
2. Tab「伴侣」改为气泡留言板：纯文字 **或** 纯图片（互斥）；图片最多 9 张/条；预览可跨留言左右滑（复用影音评论图库逻辑）
3. 文字留言可选开关：同步到一起做过的事、建待办（发帖时选指派：自己 / 对方 / 不指定）、加减积分
4. 未绑定仍在 Tab「伴侣」显示绑定引导；绑定后才显示留言板
5. 发帖带开关时由**后端事务编排**，全部成功或全部回滚

## 非目标

- 图文同条
- 留言编辑；删除留言时级联删除已创建的记事 / 待办 / 积分流水
- 留言已读 / 推送 / 实时 WebSocket
- MCP 发伴侣留言（可后续加）
- 拆开看板「共享记录」与「已看完」为两张独立可点卡片（本轮点击仍只进一起做过的事二级页）
- OSS；继续本地上传 + URL 落库

## 方案概览

采用**新域 `partner_message`**，图片上传对齐影音评论两步提交；发文字且勾选扩展动作时，单一 `POST` 在事务内写留言并调用现有 shared_record / todo / points 服务逻辑。

「一起做过的事」后端 API（`/shared-records`）不变，仅前端路由与入口迁移。

---

## 导航与页面

| 入口 | 变更后 |
|---|---|
| Tab「伴侣」`/share` | 气泡留言板；未绑定 → 绑定引导 |
| 看板「共享记录」卡 | → `/partner/dashboard/shared-records`（一起做过的事） |
| Tab「记录」`/records` | 不变 |
| 「我的 → 伴侣」 | 不变（详情 / 解绑） |

- 看板「共享记录」数字仍统计 `shared_record` 条数
- 绑定成功后留在留言板，不自动跳看板
- 现有 `/share` 列表 UI 迁到二级页；`/share` 页面改为留言板（或等价重写同路由）

---

## 留言板 UI

### 列表（已绑定）

- 气泡时间线，对齐影音详情留言：自己一侧、对方一侧
- 每条：纯文字 **或** 纯图九宫格；点击图片用跨留言展平预览（复用 / 抽离 `commentImageGallery` 模式）
- 分页加载历史；新消息在底部
- 首版：无编辑；删除仅作者

### 发帖区

- 默认文字输入 + 发送
- 「图片」模式：`van-uploader`，`max-count=9`，扩展名与 5MB 校验对齐影音评论；与文字互斥（有图清空文字并隐藏开关；切回文字清空已选图）
- **仅文字模式**可选开关（可多选）：
  1. **记到一起做过的事**：标题 = 留言全文（过长截断至现有 title 限制）；内容可空；`occurred_at` 默认今天
  2. **建待办**：勾选后选指派 `self` / `partner` / `none`（不指定）；待办标题 = 留言全文；未显式选指派时按 `none` 处理，不拦发送
  3. **改积分**：勾选后必填非 0 的 `pointsChange`（正负均可）；`pointsReason` 默认留言全文，可改

### 未绑定

- 现有绑定引导；不展示列表与发帖区

---

## 数据模型

### 新表 `partner_message`

| 字段 | 类型 | 说明 |
|---|---|---|
| `id` | PK | |
| `created_by` | 用户 ID | 作者 |
| `content` | TEXT 可空 | 纯图时可空 |
| `image_urls` | JSON 可空 | 相对路径数组；纯文字为 null |
| `shared_record_id` | 可空 | 同步记事成功回写 |
| `todo_id` | 可空 | 同步待办成功回写 |
| `points_id` | 可空 | 同步积分流水成功回写 |
| 审计字段 | | 与项目惯例一致（创建时间等） |

**服务端硬约束**

- 必须：非空文字 **或** 1～9 张图；二者都空 → 400
- 文字与图片互斥：同时有非空 `content` 与非空 `image_urls` → 400
- `image_urls` 长度 ≤ 9；URL 须为本系统伴侣留言图前缀（如 `/uploads/partner-messages/`）
- 扩展开关仅允许纯文字帖；纯图带开关 → 400
- 可见范围：当前用户与其 `partnerId` 双方创建的留言（同 `shared_record`）

Flyway：新建表 migration。

---

## API

### 上传图片

| 方法 | 路径 | 说明 |
|---|---|---|
| POST | `/partner/messages/images` | multipart `files`，1～9 张 |

- 须已绑定伴侣
- 格式 jpg/png/webp/gif；单张 ≤ 5MB
- 落盘：`uploads/partner-messages/`（`UploadStorage` 增目录方法）
- 响应：`{ "urls": string[] }`

### 列表

| 方法 | 路径 | 说明 |
|---|---|---|
| GET | `/partner/messages` | `page` / `size`；双方可见；含 `imageUrls`、作者信息足够前端分左右气泡 |

### 发帖（编排）

| 方法 | 路径 | 说明 |
|---|---|---|
| POST | `/partner/messages` | 事务内写留言 + 可选扩展 |

请求体示意：

```json
{
  "content": "今晚洗碗",
  "imageUrls": null,
  "publishSharedRecord": true,
  "publishTodo": true,
  "todoAssignedTo": "partner",
  "publishPoints": true,
  "pointsChange": -5,
  "pointsReason": "今晚洗碗"
}
```

- `todoAssignedTo`：`self` | `partner` | `none`；`publishTodo=true` 且缺省时按 `none`
- `pointsReason` 可省略：默认用 `content`
- `publishPoints=true` 时 `pointsChange` 必填且 ≠ 0
- 编排顺序建议：校验 → 写 `partner_message` → 按开关调用现有 SharedRecord / Todo / Points 服务 → 回写关联 id
- 任一步失败整单回滚
- 积分规则（含是否允许余额为负）完全沿用现有 `PartnerPointsService`

### 删除

| 方法 | 路径 | 说明 |
|---|---|---|
| DELETE | `/partner/messages/{id}` | 仅作者；**不**级联删记事 / 待办 / 积分 |

---

## 前端改动要点

- `pages/share/index.vue`：改为留言板（绑定逻辑保留）
- 新建 `pages/partner/dashboard/shared-records.vue`（或等价）：迁入原一起做过的事列表
- 看板统计卡 `router.push` 目标改为二级页路由
- API 模块：`partner-messages.ts`；图库工具可抽公共或复制影音 `commentImageGallery` 模式
- TabBar / `rootRouteList` / i18n：按需微调标题文案（Tab 仍可叫「伴侣」）

---

## 错误与边界

| 情况 | 行为 |
|---|---|
| 未绑定 | 留言 API 4xx；前端绑定引导 |
| 纯图 + 扩展开关 | 400 |
| 积分变更 = 0 | 400 |
| 删除留言 | 已创建的记事 / 待办 / 积分保留 |
| 多开关部分失败 | 整单回滚，用户可重试 |

---

## 测试 / 验收

1. 看板「共享记录」→ 二级页可查/增/改一起做过的事；Tab「伴侣」不再是该列表
2. 绑定后留言板：可发纯文字、可发最多 9 张图；图可跨帖预览滑动
3. 文字 + 记事开关 → 二级页出现对应记录
4. 文字 + 待办开关 + 三种指派 → 待办列表与指派正确
5. 文字 + 积分开关 → 余额与流水正确
6. 多开关同时勾选 → 全成或全败（无半成品）
7. 未绑定只见绑定引导
8. 删除留言不删除已同步的记事 / 待办 / 积分

## 发版

用户可感知功能：按 `app-version-bump` 做 **minor**（`package.json`、`application.version`、根 `CHANGELOG.md`、`docs/USER_CHANGELOG.md`）。
