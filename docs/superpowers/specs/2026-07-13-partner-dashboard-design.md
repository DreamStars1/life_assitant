# 伴侣看板 — 积分 + 作息打卡 + 数据展示

## 背景

目前伴侣模块已有：共享记录（"一起做过的事"）、共享媒体（"一起看过的"含进度+评论）、待办分配、日程共享。这些功能以"事后记录"为主，缺少实时互动和数据可视化模块。

目标是在伴侣功能之上增加一个**看板页面**，整合以下内容：

- **积分**：一个共享积分，双方可增减，记录原因，支持负值
- **数据展示**：纪念日天数、共享记录数、已看完媒体数等统计
- **作息打卡**：起床/睡觉打卡记录，7 天折线图展示趋势

## 方案

### 导航

底部 TabBar 在"伴侣"和"我的"之间新增一个 Tab：**看板**。

| Today | 待办 | 伴侣 | **看板** | 我的 |
|-------|------|------|---------|------|

- 路由名：`PartnerDashboard`，路径：`/partner/dashboard`
- 图标：`chart-trending-o`（Vant 图标）
- 加入 `rootRouteList` 以显示底部 TabBar
- 未绑定伴侣时显示空状态提示"请先绑定伴侣"，点击跳转到 `/share`

### 页面布局（卡片网格，2 列等高等宽）

```
┌──────────────┬──────────────┐
│   💕 纪念日    │   ⭐ 积分     │
│   128 天      │   +42        │
│   小可爱      │   [+][-]     │
├──────────────┴──────────────┤
│   📋 积分记录                │
│   做了好吃的饭         +5    │
│   忘记倒垃圾           -3    │
│   「更多 →」                 │
├──────────────┬──────────────┤
│   📊 数据统计   │   😴 作息打卡 │
│   37 条记录     │   🌅 07:32   │
│   12 部已看完   │   🌙 未打卡   │
├──────────────┴──────────────┤
│   📈 近7天作息折线图           │
│   (起床/睡觉时间趋势)          │
└──────────────────────────────┘
```

- 每个卡片等高等宽
- 全宽卡片（积分记录、折线图）占据整行
- 每个卡片可点击进入详情页

### 积分功能

**数据库新增 `partner_points` 表**

| 字段 | 类型 | 说明 |
|------|------|------|
| `id` | CHAR(36) PK | UUID |
| `created_by` | CHAR(36) FK → `user.id` | 操作人 |
| `points_change` | INT | 变动值（正为加，负为减） |
| `reason` | VARCHAR(100) | 变动原因 |
| `created_at` | DATETIME | 变动时间 |

**积分计算**：`SELECT SUM(points_change) FROM partner_points WHERE created_by IN (me.id, partner.id)`，当前积分始终为双方变动的代数和。

**API 端点**

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/partner/points` | 获取当前积分余额 |
| GET | `/partner/points/history` | 积分变动历史（分页） |
| POST | `/partner/points` | 记录积分变动 `{ points_change: int, reason: string }` |

**前端交互**：
- 看板卡片上 `[+]` `[-]` 按钮打开弹出层，选择分数（stepper）+ 填写原因（必填），确认后提交
- 点击积分卡片/积分记录卡片 → 进入积分详情页 `/partner/dashboard/points`
- 详情页同样支持选择分数 + 填写原因 + 完整历史列表（分页）
- 每次变动必填原因，不允许无原因变动

### 作息打卡功能

**数据库新增 `partner_checkin` 表**

| 字段 | 类型 | 说明 |
|------|------|------|
| `id` | CHAR(36) PK | UUID |
| `user_id` | CHAR(36) FK → `user.id` | 打卡人 |
| `checkin_type` | VARCHAR(10) | `wake` / `sleep` |
| `checkin_time` | DATETIME | 打卡时间 |
| `checkin_date` | DATE | 打卡日期 |

**约束**：每人每个作息日每种类型只能打卡一次（`UNIQUE(user_id, checkin_date, checkin_type)`）。

**作息日分界**：以凌晨 **4:00** 为界。`0:00–3:59` 归入前一天，`4:00` 起算当天。例如 7/14 02:00 打睡觉卡，记为 `checkin_date = 7/13`。

**API 端点**

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/partner/checkin` | 打卡 `{ checkin_type: 'wake' | 'sleep' }` |
| GET | `/partner/checkin/today` | 获取双方今日打卡状态 |
| GET | `/partner/checkin/weekly` | 获取近 7 天双方打卡数据（用于折线图） |

**前端交互**：
- 看板卡片显示今日打卡状态（已打卡显示时间，未打卡显示"未打卡"）
- 点击作息卡片/折线图卡片 → 进入作息详情页 `/partner/dashboard/sleep`
- 详情页包含打卡按钮 + 7 天折线图（起床/睡觉两条线）
- 折线图使用项目中已有的 `echarts` 库

### 技术实现

#### 后端

- 新建 `lifeassistant-system` 模块下的控制器：`PartnerPointsController`、`PartnerCheckinController`
- 新建服务：`PartnerPointsService`、`PartnerCheckinService`
- 新建实体：`PartnerPointsDO`、`PartnerCheckinDO`
- 新建 Mapper 和 Flyway 迁移文件（V8__create_partner_points.sql、V9__create_partner_checkin.sql）
- 积分余额接口直接用 SUM 聚合查询，无需缓存

#### 前端

- 新建页面前提：`/partner/dashboard/index.vue` — 看板主页（路由名 `PartnerDashboard`）
- `/partner/dashboard/points.vue` — 积分详情页
- `/partner/dashboard/sleep.vue` — 作息详情页
- 更新 `TabBar.vue` 添加看板 Tab
- 更新 `config/routes.ts` 添加 `PartnerDashboard`
- 更新 `api/modules/` 新增 `partner-points.ts`、`partner-checkin.ts`
- 更新 `locales/zh-CN.json` 和 `en-US.json`
- 未绑定伴侣时展示空状态

### 安全

- 所有端点需登录鉴权（已有 Sa-Token 拦截）
- 积分和打卡数据仅限伴侣双方可见（查询时用 `IN (currentUser.id, partnerId)`）

### 未纳入

- 情绪记录、今日便签、共同小目标等——作为后续扩展，当前不做
