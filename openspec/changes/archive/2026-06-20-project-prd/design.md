## Context

本设计基于 [项目需求规格说明书](specs/requirements/spec.md)，在三个已有基座上扩展：

- **后端**：`backend/`（Full Stack FastAPI Template——SQLModel、JWT OAuth2、MySQL 8.4 + Traefik、pytest 52 用例）
- **用户端 PWA**：`front/vue3-vant-mobile/`（移动端模板——Vant 4、Pinia、PWA 已配、登录/注册页面骨架）
- **管理后台**：`admin_front/vue3-element-admin/`（PC 后台模板——Element Plus、ECharts、动态路由、权限体系）

## Goals / Non-Goals

**Goals:**
- 后端：在现有 User 模型基础上扩展 partner/wechat/push 字段，按 DDD 上下文新增 planning/lifelog 模块
- 用户端：基于 vue3-vant-mobile 改造，删示例页，新增业务页 + 日历组件 + PWA 离线
- 管理后台：基于 vue3-element-admin 改造，删系统管理示例，新增 LLM 监控/推送监控/系统概览
- Redis 作为 Agent 对话记忆（阶段二）
- Web Push 闭环

**Non-Goals:**
- 不改现有 User OAuth2 认证流程
- 不实现原生 APP / 多租户 / 国际化
- 管理员功能不面向普通用户开放（需 superuser 权限）

## 双前端架构

```
                    ┌─────────────────────┐
                    │    Traefik (TLS)     │
                    └──────────┬──────────┘
                               │
              ┌────────────────┼────────────────┐
              │                │                │
         app.xxx.cn       admin.xxx.cn     api.xxx.cn
              │                │                │
     ┌────────▼────────┐ ┌────▼──────────┐ ┌───▼───────────┐
     │  用户端 PWA      │ │  管理后台 PC   │ │  FastAPI 后端   │
     │  vue3-vant-mobile│ │vue3-element-admin│ │  backend/     │
     │  (移动端安装)     │ │  (桌面浏览器)    │ │  (API+Bot)    │
     └─────────────────┘ └───────────────┘ └───────────────┘
```

| 前端 | 基座 | 域名 | 受众 | 用途 |
|------|------|------|------|------|
| 用户端 PWA | `front/vue3-vant-mobile/` | `app.xxx.cn` | 情侣用户 | 日常待办/日程/记录/共享 |
| 管理后台 | `admin_front/vue3-element-admin/` | `admin.xxx.cn` | 管理员（superuser） | 系统监控/LLM 成本/推送统计 |

## 现有基座分析

### 后端基座：`backend/`（直接继承）

| 组件 | 位置 | 状态 |
|------|------|------|
| FastAPI + CORS + Sentry | `app/main.py` | 已有 |
| SQLModel ORM（UUID） | `app/models.py`（User + Item） | 已有，扩展 User + 新增 7 表 |
| JWT OAuth2（argon2+bcrypt） | `app/api/deps.py`, `app/core/security.py` | 已有，复用 `CurrentUser` + `SessionDep` |
| pydantic-settings | `app/core/config.py` | 已有，新增 Redis/LLM/push 项 |
| MySQL 8.4 | `app/core/db.py` | 已有 |
| Alembic | `app/alembic/` | 已有初始迁移，新增迁移 |
| Docker Compose | `compose.yml`（MySQL + Adminer + Traefik + backend） | 已有，加 Redis + 两个前端 |
| Traefik + TLS | `compose.yml` labels | 已有 |
| pytest 52 用例 + CI | `tests/`, `.github/workflows/` | 已有，复用 conftest.py |

### 用户端 PWA 基座：`front/vue3-vant-mobile/`

| 能力 | 版本 | 状态 |
|------|------|------|
| Vue | 3.5.35，`<script setup lang="ts">` | 已有 |
| UI | Vant 4.9.24（按需导入 + 触摸模拟） | 已有 |
| 状态管理 | Pinia 3.0.4 + persistedstate | 已有 |
| 路由 | vue-router 5.1，文件系统路由，History 模式 | 已有 |
| HTTP | axios 1.16.1（Access-Token 拦截器） | 已有 |
| PWA | `vite-plugin-pwa` 1.3（autoUpdate + manifest + SW） | **已有** |
| 认证页面 | 登录/注册/忘记密码 | 已有骨架 |
| 图表 | ECharts 6.1 | 已有 |

### 管理后台基座：`admin_front/vue3-element-admin/`

| 能力 | 版本 | 状态 |
|------|------|------|
| Vue | 3.5.30，`<script setup lang="ts">` | 已有 |
| UI | Element Plus 2.13.6（按需导入） | 已有 |
| 状态管理 | Pinia 3.0.4 | 已有 |
| 路由 | vue-router 5.0.3，Hash History，动态路由 | 已有 |
| HTTP | axios 1.13.6（Token 刷新"单飞模式"） | 已有 |
| 认证 | JWT 双 token + 路由守卫 + 权限指令（v-hasPerm） | 已有 |
| 图表 | ECharts 6.0 + 封装组件 | 已有 |
| 布局 | 4 种布局（左侧菜单/顶部菜单/混合） | 已有 |

---

## 后端项目结构（DDD 扩展）

在现有 `app/` 下新增上下文目录：

```
backend/app/
├── main.py / models.py / crud.py    # 已有，不动
├── core/                             # config.py 新增配置项
├── api/                              # main.py 挂载新 router
│
├── identity/                         # BC1: 账号（User 扩展 + PushSubscription）
│   ├── models.py                     # PushSubscription 新表
│   ├── service.py                    # invite, bind_partner, push_prefs
│   └── router.py                     # /identity/*
│
├── planning/                         # BC2: 规划（Todo + Event + EventRecord）
│   ├── models.py                     # 3 张新表
│   ├── service.py                    # CRUD + assign + record + recurrence
│   └── router.py                     # /todos/*, /events/*, /partner/*
│
├── lifelog/                          # BC3: 生活记录
│   ├── models.py                     # LifeLog
│   ├── service.py                    # create + list + stats
│   └── router.py                     # /life-logs/*
│
├── monitor/                          # 管理后台专用：监控与统计
│   ├── service.py                    # system_health, llm_cost_stats, push_stats, user_overview
│   └── router.py                     # /admin/*（需 superuser 权限）
│
├── agent/      (阶段二)              # Agent 技能 + 对话记忆
├── analysis/   (阶段二)              # AI 分析
├── notification/                     # 推送 + 调度
└── utils/                            # ilink_client, date_parser
```

---

## 用户端 PWA 改造（基于 vue3-vant-mobile）

### 模板页面处理策略

| 模板页面 | 路径 | 处理 |
|---------|------|------|
| 登录 | `pages/login/` | **保留改造**：email→username |
| 注册 | `pages/register/` | **保留改造**：去邮箱，username+password |
| 忘记密码 | `pages/forgot-password/` | **保留** |
| 个人中心 | `pages/profile/` | **改造**为设置页入口 |
| 设置 | `pages/settings/` | **改造**为推送偏好+静默时段 |
| 示例页（counter/charts/mock/scroll-cache/keepalive/unocss） | - | **删除** |

### 改造后结构（新增部分标 ★）

```
front/vue3-vant-mobile/src/
├── api/modules/
│   ├── auth.ts        # 已有，改 email→username
│   ├── todos.ts       # ★
│   ├── events.ts      # ★
│   ├── lifelogs.ts    # ★
│   └── push.ts        # ★
│
├── components/calendar/   # ★ 日历组件
│   ├── MonthGrid.vue      # ★ 月视图
│   ├── WeekView.vue       # ★ 周视图
│   ├── DayView.vue        # ★ 日视图
│   └── EventDetailSheet.vue  # ★ 日程详情弹窗
│
├── composables/
│   ├── dark.ts             # 已有
│   ├── usePush.ts          # ★ Web Push 订阅
│   └── useCalendar.ts      # ★ 日期状态+视图切换
│
├── pages/                   # 文件系统路由
│   ├── index.vue            # 已有 → ★ 今日首页
│   ├── login/               # 已有改造
│   ├── register/            # 已有改造
│   ├── forgot-password/     # 已有保留
│   ├── todos/               # ★ 待办列表
│   ├── events/              # ★ 日程日历
│   ├── lifelog/             # ★ 生活记录
│   ├── share/               # ★ 协作共享
│   ├── settings/            # 已有改造
│   └── profile/             # 已有改造
│
├── stores/modules/
│   ├── user.ts              # 已有，加 partner/wechat 字段
│   ├── todos.ts             # ★
│   └── events.ts            # ★
│
├── utils/
│   ├── auth.ts              # 已有
│   ├── request.ts           # 已有，升级 Token 刷新
│   └── db.ts                # ★ IndexedDB 离线存储
│
├── App.vue                  # TabBar: 今日/待办/日历/记录/我的
├── main.ts / sw.ts          # sw.ts ★ 加 push 事件处理
```

---

## 管理后台改造（基于 vue3-element-admin）

### 模板页面处理策略

| 模板页面 | 处理 |
|---------|------|
| 登录页 | **保留**（后台管理员登录，复用 JWT + superuser 校验） |
| 仪表盘（dashboard） | **改造**为系统概览（服务状态+用户数+今日统计） |
| 系统管理（用户/角色/菜单/部门/字典/日志/租户/通知） | **删除**（2 人不需要 RBAC） |
| 代码生成器（codegen） | **删除** |
| 示例（demo/CRUD/富文本/拖拽） | **删除** |

### 改造后新增的页面

| 页面 | 路由 | 说明 |
|------|------|------|
| 系统概览 | `/dashboard` | 服务健康状态、活跃用户数、今日待办/日程数、推送成功率 |
| LLM 监控 | `/llm/monitor` | 月度 Token 消耗趋势图（ECharts）、按日统计、预算告警阈值配置 |
| 推送监控 | `/push/monitor` | 推送成功/失败统计、每日推送量趋势 |
| 用户列表 | `/users` | 用户列表（含伴侣关联状态、注册时间、活跃状态）——极简版 |

### 改造后结构

```
admin_front/vue3-element-admin/src/
├── api/
│   ├── auth/                  # 已有，保留
│   └── admin/                 # ★ 新增
│       ├── monitor.ts         # ★ LLM 统计 + 推送统计 + 健康检查
│       └── users.ts           # ★ 用户列表
│
├── views/
│   ├── login/                 # 已有，保留
│   ├── dashboard/             # 已有 → ★ 改造为系统概览
│   ├── redirect/ / error/     # 已有，保留
│   └── admin/                 # ★ 新增
│       ├── llm/               # ★ LLM 监控页（ECharts 趋势图）
│       ├── push/              # ★ 推送监控页
│       └── users/             # ★ 用户列表页
│
├── components/
│   └── ECharts/               # 已有，直接用于 LLM/推送图表
│
└── stores/modules/
    └── user.ts                # 已有，加 superuser 权限判断
```

---

## 后端新增 `monitor/` 上下文（管理后台专用 API）

```python
# backend/app/monitor/router.py（所有接口需 superuser 权限）

GET  /api/v1/admin/health         # 系统健康状态（MySQL/Redis/iLink/DeepSeek）
GET  /api/v1/admin/stats/overview  # 概览：用户数、今日待办数、今日日程数
GET  /api/v1/admin/stats/llm      # LLM：月度 Token 消耗趋势、按日明细
PUT  /api/v1/admin/settings/llm-budget  # 设置 LLM 月度预算上限
GET  /api/v1/admin/stats/push     # 推送：按日统计成功/失败数
GET  /api/v1/admin/users           # 用户列表（含伴侣关联状态）
```

---

## Decisions

### D-1: 双前端分工

**选择**：用户日常操作走 PWA（手机），系统管理与监控走管理后台（PC）

**理由**：
- 手机端聚焦日常交互（Vant 移动组件），PC 端聚焦数据看板（Element Plus 表格+图表）
- 管理后台已有 4 种布局、ECharts 封装、权限指令——天然适合做监控面板
- 管理后台域名独立（admin.xxx.cn），方便 Nginx/Traefik 做 IP 白名单

### D-2: 管理后台不需要完整 RBAC

**选择**：只保留 superuser 权限判断（`v-hasPerm` 指令已有），不引入角色/部门/菜单管理

**理由**：2-10 人系统不需要多级管理员，superuser 一个角色足够

**改造**：删除 `views/system/` 下所有管理页面（用户/角色/菜单/部门/字典/日志），保留动态路由框架

### D-3: 用户端 PWA 基于 vue3-vant-mobile 改造

（与之前一致）

### D-4~D-12: Agent / Bot / Push / 邀请码 / LLM / 日程 / 日历 / Redis / 重复规则

（与之前一致）

### D-13: Traefik 多域名路由

```
app.xxx.cn    → frontend  (用户端 PWA)
admin.xxx.cn  → admin     (管理后台)
api.xxx.cn    → backend   (API)
```

## 需要新增的依赖

```toml
# backend/pyproject.toml 新增
"apscheduler>=3.10",
"redis>=5.0",
"pywebpush>=2.0",
"openai>=1.0",
"python-dateutil>=2.8",
```

管理后台无需新增依赖（ECharts + Element Plus 模板中已有）。用户端 PWA 无需新增依赖。

## Risks / Trade-offs

| 风险 | 缓解 |
|------|------|
| 两个前端项目维护成本 | 两套模板都是已有成熟项目，改造量小；API 共享同一后端 |
| 模板前端 Vue 3.5 / Vite 8 版本较新 | lock 文件锁定版本 |
| 管理员访问控制 | /admin/* API 全部校验 superuser；管理后台路由守卫检查 is_superuser |
| iLink API 不可用 | Bot 解耦 |
