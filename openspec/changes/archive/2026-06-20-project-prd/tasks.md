# 阶段一：手动 CRUD 核心（先跑通，无需 AI）

> **已有基座**：
> - 后端：`backend/`（FastAPI 全栈——认证/DB/CI 就绪）
> - 用户端：`front/vue3-vant-mobile/`（PWA/Vant/登录页就绪）
> - 管理后台：`admin_front/vue3-element-admin/`（Element Plus/ECharts/权限就绪）
>
> 以下任务仅列出需要**新增和修改**的部分。

---

## 1. 基础设施扩展

- [x] 1.1 `backend/compose.yml`：新增 Redis 7 + 用户端前端（`front`）+ 管理后台（`admin`）服务
- [x] 1.2 Traefik 路由：`app.xxx.cn` → frontend，`admin.xxx.cn` → admin，`api.xxx.cn` → backend
- [x] 1.3 `backend/pyproject.toml`：新增 apscheduler, redis, pywebpush, openai, python-dateutil
- [x] 1.4 `backend/app/core/config.py`：新增 REDIS_URL, VAPID_*, ILINK_TOKEN, DEEPSEEK_* 配置

## 2. 数据库模型扩展

- [x] 2.1 `backend/app/models.py`：User 扩展——partner_id, wechat_id, timezone, push_enabled, quiet_hours_start/end
- [x] 2.2 `backend/app/identity/models.py`：PushSubscription（SQLModel table=True，UUID）
- [x] 2.3 `backend/app/planning/models.py`：Todo + Event + EventRecord（3 张新表）
- [x] 2.4 `backend/app/lifelog/models.py`：LifeLog（log_type Enum, metadata_json JSON）
- [x] 2.5 `backend/app/agent/models.py`：Conversation + `app/analysis/models.py`：AnalysisResult
- [x] 2.6 生成 Alembic 迁移 → 验证 `alembic upgrade head`

## 3. BC1: identity（扩展现有 User）

- [x] 3.1 `backend/app/identity/service.py`：generate_invite (JWT 24h), bind_partner, bind_wechat
- [x] 3.2 `backend/app/identity/router.py`：POST /invite, POST /bind-partner, POST /bind-wechat
- [x] 3.3 挂载 identity router 到 `api/main.py`

## 4. BC2: planning（待办 + 日程 + 共享）

- [x] 4.1 `backend/app/planning/service.py`：Todo CRUD + assign + 伴侣查询；Event CRUD + 预计/实际 + recurrence + EventRecord
- [x] 4.2 `backend/app/planning/router.py`：/todos/*, /events/*, /events/{id}/records, /partner/*
- [x] 4.3 `backend/app/utils/date_parser.py`：自然语言→datetime

## 5. BC3: lifelog（生活记录）

- [x] 5.1 `backend/app/lifelog/service.py`：create, list, stats（周/月聚合）
- [x] 5.2 `backend/app/lifelog/router.py`：/life-logs/*

## 6. BC4: monitor（管理后台专用 API，需 superuser）

- [x] 6.1 `backend/app/monitor/service.py`：system_health, llm_cost_stats, push_stats, user_overview
- [x] 6.2 `backend/app/monitor/router.py`：GET /admin/health, GET /admin/stats/overview, GET /admin/stats/llm, PUT /admin/settings/llm-budget, GET /admin/stats/push, GET /admin/users
- [x] 6.3 所有 `/admin/*` 路由加 `get_current_active_superuser` 依赖

## 7. 推送通知（notification/）

- [x] 7.1 `backend/app/notification/push_service.py`：subscribe, unsubscribe, send_push
- [x] 7.2 Push API Router：POST subscribe, DELETE subscription, PUT preferences, POST test
- [x] 7.3 `backend/app/notification/scheduler.py`：APScheduler 待办提醒每分钟、每日摘要 8:00
- [x] 7.4 静默时段过滤

---

## 8. 用户端 PWA 改造（基于 vue3-vant-mobile）

### 8.1 清理与认证

- [x] 8.1.1 删除示例页：`pages/counter/`, `charts/`, `mock/`, `scroll-cache/`, `keepalive/`, `unocss/`
- [x] 8.1.2 `pages/login/`：email → username + password（OAuth2 password grant）
- [x] 8.1.3 `pages/register/`：去邮箱，改 username + password + full_name
- [x] 8.1.4 `stores/modules/user.ts`：加 partner_id, wechat_id, push_enabled 字段

### 8.2 导航与首页

- [x] 8.2.1 `App.vue`：TabBar 改为今日/待办/日历/记录/我的
- [x] 8.2.2 `pages/index.vue`→ 改造为 TodayView：聚合今日待办+日程+快捷录入

### 8.3 业务页面

- [x] 8.3.1 `pages/todos/`（新增）：TodoView——创建/筛选/排序/勾选完成/分配给伴侣
- [x] 8.3.2 `pages/events/`（新增）：EventView——月/周/日三视图
- [x] 8.3.3 `pages/lifelog/`（新增）：LifeLogView——类型Tab+表单+列表
- [x] 8.3.4 `pages/share/`（新增）：ShareView——邀请码+伴侣数据
- [x] 8.3.5 `pages/settings/` + `pages/profile/`（已有改造）：推送偏好+静默时段+账号

### 8.4 日历组件（components/calendar/）

- [x] 8.4.1 `composables/useCalendar.ts`（新增）：日期状态+月/周/日切换+Vant Swipe
- [x] 8.4.2 `components/calendar/MonthGrid.vue`（新增）：7×N CSS Grid
- [x] 8.4.3 `components/calendar/WeekView.vue`（新增）：7列×24h 横向滚动
- [x] 8.4.4 `components/calendar/DayView.vue`（新增）：24h 纵向时间轴
- [x] 8.4.5 `components/calendar/EventDetailSheet.vue`（新增）：Vant ActionSheet——详情+记录时间线

### 8.5 PWA 离线与推送

- [x] 8.5.1 `utils/db.ts`（新增）：IndexedDB 离线存储，联网同步
- [x] 8.5.2 `sw.ts`（已有修改）：加 push 事件处理（通知弹出+点击跳转）
- [x] 8.5.3 `composables/usePush.ts`（新增）：Web Push 订阅+后端 API

### 8.6 API 层与配置

- [x] 8.6.1 `api/modules/todos.ts`, `events.ts`, `lifelogs.ts`, `push.ts`（新增）
- [x] 8.6.2 `.env.development`：`VITE_APP_API_BASE_URL=http://localhost:8000`

---

## 9. 管理后台改造（基于 vue3-element-admin）

### 9.1 清理

- [ ] 9.1.1 删除 `views/system/` 全部（用户/角色/菜单/部门/字典/日志/通知/租户管理）
- [ ] 9.1.2 删除 `views/codegen/`, `views/demo/`

### 9.2 系统概览（改造 dashboard）

- [ ] 9.2.1 `views/dashboard/`：改造为系统概览——服务状态卡片+用户数+今日待办/日程数+推送成功率
- [ ] 9.2.2 `api/admin/monitor.ts`（新增）：调用 /admin/health + /admin/stats/overview

### 9.3 LLM 监控（新增）

- [ ] 9.3.1 `views/admin/llm/`（新增）：月度 Token 消耗趋势图（ECharts 折线图）+ 按日明细表
- [ ] 9.3.2 预算告警阈值配置表单（PUT /admin/settings/llm-budget）

### 9.4 推送监控（新增）

- [ ] 9.4.1 `views/admin/push/`（新增）：每日推送成功/失败统计柱状图（ECharts）

### 9.5 用户列表（新增）

- [ ] 9.5.1 `views/admin/users/`（新增）：Element Plus 表格——用户名/伴侣状态/注册时间
- [ ] 9.5.2 `api/admin/users.ts`（新增）

### 9.6 路由与菜单

- [ ] 9.6.1 侧边栏菜单调整为：系统概览 / LLM监控 / 推送监控 / 用户列表
- [ ] 9.6.2 `.env.development`：`VITE_APP_API_URL=http://localhost:8000`

---

## 10. 冒烟测试

- [ ] 10.1 `backend/tests/test_smoke.py`（复用 conftest.py）：注册→登录→邀请→伴侣绑定→创建待办→完成→分配→创建日程→追加记录→推送→伴侣读共享
- [ ] 10.2 `backend/tests/test_admin.py`：superuser 登录→health/stats/llm/push/users 接口验证

---

# 阶段二：AI 接入（在已有 Service 上封装工具）

## 11. Agent 层

- [ ] 11.1 `backend/app/agent/core.py`：BaseSkill + AgentCore（意图路由）
- [ ] 11.2 `backend/app/agent/llm.py`：LLMClient（openai SDK→DeepSeek，Token计数）
- [ ] 11.3 `agent/skills/todo_skill.py` + `event_skill.py` + `lifelog_skill.py`：LLM 提取参数→Service→回复
- [ ] 11.4 `backend/app/agent/memory.py`：Redis 会话（20轮）+ Conversation 持久化

## 12. Bot 集成

- [ ] 12.1 `backend/app/utils/ilink_client.py`：长轮询+send_message
- [ ] 12.2 BotService 后台 asyncio 任务
- [ ] 12.3 POST /bot/webhook

## 13. AI 分析

- [ ] 13.1 `backend/app/analysis/service.py`：读 planning+lifelog→LLM 摘要+建议
- [ ] 13.2 `backend/app/analysis/router.py`：GET weekly/monthly, POST on-demand

## 14. 部署与运维

- [ ] 14.1 `backend/scripts/backup.sh` 数据库备份
- [ ] 14.2 `backend/scripts/seed.py` 测试数据填充
- [ ] 14.3 Capacitor 配置（预留）
- [ ] 14.4 验证 `docker compose up -d` 全栈部署
