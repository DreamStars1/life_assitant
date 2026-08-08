# Life Assistant 更新日志

---

## v1.8.0 (2026-08-08)

### ✨ 新特性

- **积分流水记录日期**：新建加减分可选业务日；双方可改已有流水日期（日精度，复用 `created_at`）

### 🖥 后端

- `POST /partner/points` 可选 `recordDate`；新增 `PATCH /partner/points/{id}`
- 版本号: `1.4.0-SNAPSHOT` → `1.5.0-SNAPSHOT`

### 📱 前端

- 积分页选日 / 点流水改日；列表按天展示
- 版本号: `1.7.0` → `1.8.0`

---

## v1.7.0 (2026-08-08)

### ✨ 新特性

- **悬浮柴犬彩蛋**：`fullName` 为 `cc` / `小星露` 时全局右下角可拖拽 Q 版小狗；点 3 次鼻青脸肿，约 5 秒恢复

### 📱 前端

- `FloatingShibaPet` + `ShibaFace`；位置 `localStorage`；无后端依赖
- 版本号: `1.6.0` → `1.7.0`

---

## v1.6.0 (2026-08-07)

### ✨ 新特性

- **伴侣关系表 `partner_info`**：一对情侣一行，存「在一起」起点日与积分余额缓存
- **可编辑在一起日期**：看板纪念日卡片与伴侣详情页可改；任一方改完立即双方生效
- **在一起天数**：按 `partner_since` 计算（含起始日，当天为第 1 天）；不再用账号注册日
- **积分余额缓存**：读 `points_balance`；加减分同事务写流水并更新缓存；流水表保留

### 🖥 后端

- Flyway `V18__create_partner_info.sql`（建表 + 已绑定情侣回填）
- `PUT /identity/partner-since`；绑定建行、解绑/删号清关系行与双方积分流水
- `UserPublicResp.partnerSince`；版本号: `1.3.0-SNAPSHOT` → `1.4.0-SNAPSHOT`

### 📱 前端

- store / API 映射 `partnerSince`；看板挂载刷新用户信息
- 版本号: `1.5.0` → `1.6.0`

---

## v1.5.0 (2026-08-03)

### ✨ 新特性

- **健康管家**：从「我的」右下角入口进入；今日情况 / 身体档案 / 趋势三页
- **健康管家**：后端持久化（档案、三餐含蛋白、体重、每日胃状态/周期、规律/耐受/触发）
- **健康管家**：MCP `health` 域工具 + Cursor Skill 工作流手册（外部 AI 可录入/查询）

### 🖥 后端

- 新增 `/health/*` REST 与 Flyway `V16` 七表
- 版本号: `1.2.0-SNAPSHOT` → `1.3.0-SNAPSHOT`

### 📱 前端

- 新增 `/health` 页面与 Profile 浮动入口（Q 版医生头像）
- 版本号: `1.4.0` → `1.5.0`

---

## v1.2.0 (2026-07-10)

### ✨ 新特性

- **共享媒体**：新增进度管理 — 支持标记/取消"已看完"、自动记录看完时间
- **共享媒体**：媒体列表支持滑动删除，带确认弹窗
- **共享媒体**：新增 `finished_at` 字段记录看完日期，列表页展示

### 🖥 后端

- `SharedMediaDO/Resp` 增加 `finishedAt` 字段
- `PATCH /shared-media/{id}` 支持 `isFinished` 参数，自动更新 `finished_at`
- Flyway: 新增 `V7__add_finished_at.sql` 迁移
- 版本号: `1.0.0-SNAPSHOT` → `1.1.0-SNAPSHOT`

### 📱 前端

- 共享媒体列表页：`van-swipe-cell` 左滑删除 + `showConfirmDialog` 确认弹窗
- 媒体详情页：进度弹窗增加「标记为已看完」开关，保存时同步更新 `isFinished`
- 列表页：已看完项目旁显示完成日期（如 `07-10`）
- 版本号: `1.1.0` → `1.2.0`
