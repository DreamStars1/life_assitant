# 小助手项目开发文档

> 版本：v1.0 | 日期：2026-06-18 | 技术栈：Python + Vue3 + MySQL

---

## 一、项目概述

### 1.1 项目定位

面向情侣/个人的智能生活助手，支持微信对话交互、手机推送提醒、Web管理配置。

### 1.2 核心能力

| 能力 | 说明 |
| --- | --- |
| 对话理解 | 微信自然语言交互，LLM解析意图 |
| 待办管理 | 创建/查看/完成/分配待办 |
| 日程管理 | 日程CRUD + 定时提醒推送 |
| 生活记录 | 饮食/运动/工作日志 |
| AI分析 | 基于生活数据生成建议和周报 |
| 协作共享 | 两人共享待办/日程，互相分配 |
| 推送通知 | Android系统级推送 |

### 1.3 用户规模

2人起步，未来扩展至10人以下。

---

## 二、系统架构

### 2.1 整体架构

```
┌─────────────────────────────────────────────────────────────┐
│                        客户端                                │
│                                                             │
│   ┌──────────────┐              ┌──────────────────────┐    │
│   │   微信 Bot    │              │    PWA (Vue3+Vant)   │    │
│   │  iLink 长轮询  │              │  Service Worker      │    │
│   │  收发消息      │              │  Web Push 订阅       │    │
│   └──────┬───────┘              └──────────┬───────────┘    │
│          │                                  │               │
└──────────┼──────────────────────────────────┼───────────────┘
           │ HTTP                              │ HTTP / WS
           ▼                                  ▼
┌─────────────────────────────────────────────────────────────┐
│                        后端服务                              │
│                                                             │
│   ┌──────────────────────────────────────────────────────┐  │
│   │                 FastAPI Application                   │  │
│   │                                                      │  │
│   │  ┌────────────┐  ┌────────────┐  ┌────────────────┐ │  │
│   │  │ Bot Router  │  │ API Router │  │ Push Router    │ │  │
│   │  │ /bot/*      │  │ /api/*     │  │ /push/*        │ │  │
│   │  └──────┬─────┘  └──────┬─────┘  └───────┬────────┘ │  │
│   │         │               │                 │          │  │
│   │  ┌──────┴───────────────┴─────────────────┴────────┐ │  │
│   │  │              Service Layer                       │ │  │
│   │  │  TodoService | EventService | LifeLogService     │ │  │
│   │  │  UserService | ShareService | PushService        │ │  │
│   │  │  AnalysisService | SchedulerService              │ │  │
│   │  └──────────────────────┬──────────────────────────┘ │  │
│   └─────────────────────────┼────────────────────────────┘  │
│                             │                               │
│   ┌─────────────────────────┼────────────────────────────┐  │
│   │        Agent Layer      │                            │  │
│   │  ┌──────────────────────┴─────────────────────────┐  │  │
│   │  │  CowAgent / LangGraph                          │  │  │
│   │  │  ├─ 意图识别 → 路由到对应Service               │  │  │
│   │  │  ├─ 对话生成 → 回复用户                        │  │  │
│   │  │  ├─ 数据分析 → 生成建议                        │  │  │
│   │  │  └─ Skills: todo/event/lifelog/analysis        │  │  │
│   │  └────────────────────────────────────────────────┘  │  │
│   └──────────────────────────────────────────────────────┘  │
│                                                             │
│   ┌──────────────┐  ┌──────────────┐  ┌──────────────────┐ │
│   │   MySQL      │  │    Redis     │  │   APScheduler    │ │
│   │   业务数据    │  │  会话/缓存   │  │   定时任务调度    │ │
│   └──────────────┘  └──────────────┘  └──────────────────┘ │
└─────────────────────────────────────────────────────────────┘
           │
           ▼
┌─────────────────────────────────────────────────────────────┐
│                     外部服务                                 │
│   ┌──────────────┐  ┌──────────────┐  ┌──────────────────┐ │
│   │ iLink API    │  │ DeepSeek API │  │ Web Push Service │ │
│   │ 微信消息收发  │  │ LLM推理      │  │ FCM 推送         │ │
│   └──────────────┘  └──────────────┘  └──────────────────┘ │
└─────────────────────────────────────────────────────────────┘

```

### 2.2 请求流转

```
微信消息流：
  用户发消息 → iLink长轮询获取 → Bot Router → Agent意图识别
  → 调用Service → 写MySQL → Agent生成回复 → iLink发送消息
  → (如有定时) APScheduler注册推送任务

PWA请求流：
  用户操作 → API Router → Service → MySQL → 返回JSON
  → (如需通知对方) PushService → Web Push

推送流：
  APScheduler触发 → PushService查询待推送项
  → 查MySQL获取PushSubscription → 调用Web Push API → 手机弹出通知

```

---

## 三、项目结构

```
assistant/
├── docker-compose.yml
├── .env.example
├── nginx/
│   └── assistant.conf
│
├── backend/                          # FastAPI 后端
│   ├── Dockerfile
│   ├── requirements.txt
│   ├── alembic/                      # 数据库迁移
│   │   ├── env.py
│   │   └── versions/
│   ├── app/
│   │   ├── __init__.py
│   │   ├── main.py                   # FastAPI 入口
│   │   ├── config.py                 # 配置管理
│   │   ├── database.py               # MySQL 连接
│   │   │
│   │   ├── models/                   # SQLAlchemy 模型
│   │   │   ├── __init__.py
│   │   │   ├── user.py
│   │   │   ├── todo.py
│   │   │   ├── event.py
│   │   │   ├── life_log.py
│   │   │   ├── conversation.py
│   │   │   ├── push_subscription.py
│   │   │   └── shared_data.py
│   │   │
│   │   ├── schemas/                  # Pydantic Schema
│   │   │   ├── __init__.py
│   │   │   ├── user.py
│   │   │   ├── todo.py
│   │   │   ├── event.py
│   │   │   ├── life_log.py
│   │   │   └── push.py
│   │   │
│   │   ├── services/                 # 业务逻辑
│   │   │   ├── __init__.py
│   │   │   ├── todo_service.py
│   │   │   ├── event_service.py
│   │   │   ├── life_log_service.py
│   │   │   ├── user_service.py
│   │   │   ├── share_service.py
│   │   │   ├── push_service.py
│   │   │   ├── analysis_service.py
│   │   │   └── scheduler_service.py
│   │   │
│   │   ├── routers/                  # API 路由
│   │   │   ├── __init__.py
│   │   │   ├── bot.py               # iLink Bot 回调
│   │   │   ├── todo.py
│   │   │   ├── event.py
│   │   │   ├── life_log.py
│   │   │   ├── user.py
│   │   │   ├── push.py
│   │   │   └── analysis.py
│   │   │
│   │   ├── agent/                    # Agent 层
│   │   │   ├── __init__.py
│   │   │   ├── core.py              # Agent 核心（意图路由）
│   │   │   ├── llm.py               # LLM 调用封装
│   │   │   ├── memory.py            # 对话记忆管理
│   │   │   └── skills/              # 自定义技能
│   │   │       ├── __init__.py
│   │   │       ├── todo_skill.py
│   │   │       ├── event_skill.py
│   │   │       ├── lifelog_skill.py
│   │   │       └── analysis_skill.py
│   │   │
│   │   └── utils/
│   │       ├── __init__.py
│   │       ├── ilink_client.py      # iLink API 客户端
│   │       ├── push_client.py       # Web Push 客户端
│   │       └── date_parser.py       # 自然语言日期解析
│   │
│   └── tests/
│       ├── test_todo.py
│       ├── test_event.py
│       └── test_agent.py
│
├── frontend/                         # Vue3 PWA
│   ├── Dockerfile
│   ├── package.json
│   ├── vite.config.ts
│   ├── src/
│   │   ├── main.ts
│   │   ├── App.vue
│   │   ├── router/
│   │   │   └── index.ts
│   │   ├── stores/                   # Pinia
│   │   │   ├── todo.ts
│   │   │   ├── event.ts
│   │   │   ├── user.ts
│   │   │   └── push.ts
│   │   ├── api/                      # 后端API调用
│   │   │   ├── client.ts
│   │   │   ├── todo.ts
│   │   │   ├── event.ts
│   │   │   └── push.ts
│   │   ├── views/
│   │   │   ├── TodayView.vue        # 今日首页
│   │   │   ├── TodoView.vue         # 待办列表
│   │   │   ├── EventView.vue        # 日程日历
│   │   │   ├── AnalysisView.vue     # 数据分析
│   │   │   ├── ShareView.vue        # 协作
│   │   │   └── SettingsView.vue     # 设置
│   │   ├── components/
│   │   │   ├── TodoItem.vue
│   │   │   ├── EventCard.vue
│   │   │   ├── AnalysisChart.vue
│   │   │   └── PushPermission.vue
│   │   ├── composables/
│   │   │   ├── usePush.ts           # Web Push 逻辑
│   │   │   └── useWebSocket.ts      # 实时更新
│   │   └── sw.ts                     # Service Worker
│   ├── public/
│   │   ├── manifest.json            # PWA manifest
│   │   └── icons/                   # APP图标
│   └── capacitor.config.ts          # Capacitor配置（未来打包APK）
│
└── scripts/
    ├── deploy.sh                     # 部署脚本
    ├── backup.sh                     # 数据库备份
    └── seed.py                       # 测试数据

```

---

## 四、数据库设计

### 4.1 MySQL 配置

```sql
-- 字符集：utf8mb4，支持 emoji 和中文
-- 排序规则：utf8mb4_unicode_ci
-- 引擎：InnoDB

```

### 4.2 ER 关系

```
users ──1:N──> todos
users ──1:N──> events
users ──1:N──> life_logs
users ──1:N──> conversations
users ──1:N──> push_subscriptions
users ──1:1──> users (partner_id, 互相关联)
todos ──N:N──> shared_data
events ──N:N──> shared_data

```

### 4.3 表结构

```sql
-- ============================================================
-- 用户表
-- ============================================================
CREATE TABLE users (
    id              BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    name            VARCHAR(50)       NOT NULL COMMENT '昵称',
    wechat_id       VARCHAR(100)      DEFAULT NULL COMMENT 'iLink绑定的微信ID',
    partner_id      BIGINT UNSIGNED   DEFAULT NULL COMMENT '伴侣用户ID',
    timezone        VARCHAR(50)       DEFAULT 'Asia/Shanghai',
    push_enabled    TINYINT(1)        DEFAULT 1 COMMENT '是否开启推送',
    quiet_hours_start TIME            DEFAULT '23:00' COMMENT '静默时段开始',
    quiet_hours_end   TIME            DEFAULT '08:00' COMMENT '静默时段结束',
    created_at      DATETIME          DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME          DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_wechat_id (wechat_id),
    KEY idx_partner_id (partner_id),
    FOREIGN KEY (partner_id) REFERENCES users(id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;


-- ============================================================
-- 待办事项
-- ============================================================
CREATE TABLE todos (
    id              BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    user_id         BIGINT UNSIGNED   NOT NULL COMMENT '所属用户',
    title           VARCHAR(200)      NOT NULL COMMENT '待办标题',
    description     TEXT              DEFAULT NULL COMMENT '详细描述',
    due_date        DATETIME          DEFAULT NULL COMMENT '截止时间',
    remind_at       DATETIME          DEFAULT NULL COMMENT '提醒时间',
    priority        ENUM('low','medium','high','urgent') DEFAULT 'medium',
    status          ENUM('pending','in_progress','done','cancelled') DEFAULT 'pending',
    category        VARCHAR(50)       DEFAULT NULL COMMENT '分类：work/life/health等',
    assigned_by     BIGINT UNSIGNED   DEFAULT NULL COMMENT '分配者ID（协作场景）',
    source          ENUM('wechat','pwa','api') DEFAULT 'wechat' COMMENT '创建来源',
    created_at      DATETIME          DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME          DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    done_at         DATETIME          DEFAULT NULL COMMENT '完成时间',
    KEY idx_user_status (user_id, status),
    KEY idx_due_date (due_date),
    KEY idx_remind_at (remind_at),
    FOREIGN KEY (user_id) REFERENCES users(id),
    FOREIGN KEY (assigned_by) REFERENCES users(id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;


-- ============================================================
-- 日程事件
-- ============================================================
CREATE TABLE events (
    id              BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    user_id         BIGINT UNSIGNED   NOT NULL,
    title           VARCHAR(200)      NOT NULL,
    description     TEXT              DEFAULT NULL,
    start_time      DATETIME          NOT NULL,
    end_time        DATETIME          DEFAULT NULL,
    recurrence      VARCHAR(100)      DEFAULT NULL COMMENT '重复规则：RRULE格式',
    category        VARCHAR(50)       DEFAULT NULL,
    remind_minutes  INT               DEFAULT 15 COMMENT '提前多少分钟提醒',
    is_shared       TINYINT(1)        DEFAULT 0 COMMENT '是否共享给伴侣',
    source          ENUM('wechat','pwa','api') DEFAULT 'wechat',
    created_at      DATETIME          DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME          DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    KEY idx_user_start (user_id, start_time),
    KEY idx_remind (start_time, remind_minutes),
    FOREIGN KEY (user_id) REFERENCES users(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;


-- ============================================================
-- 生活记录
-- ============================================================
CREATE TABLE life_logs (
    id              BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    user_id         BIGINT UNSIGNED   NOT NULL,
    log_type        ENUM('diet','exercise','work','mood','sleep') NOT NULL COMMENT '记录类型',
    content         TEXT              NOT NULL COMMENT '记录内容',
    metadata_json   JSON              DEFAULT NULL COMMENT '结构化元数据',
    logged_at       DATETIME          NOT NULL COMMENT '记录发生时间',
    source          ENUM('wechat','pwa','api') DEFAULT 'wechat',
    created_at      DATETIME          DEFAULT CURRENT_TIMESTAMP,
    KEY idx_user_type_date (user_id, log_type, logged_at),
    FOREIGN KEY (user_id) REFERENCES users(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- metadata_json 示例：
-- diet:      {"meal": "lunch", "calories": 650, "items": ["米饭", "鸡胸肉", "西兰花"]}
-- exercise:  {"type": "running", "duration_min": 30, "distance_km": 5.0, "calories": 300}
-- work:      {"project": "xxx", "hours": 4, "focus_score": 7}
-- sleep:     {"hours": 7.5, "quality": "good", "bedtime": "23:30", "wakeup": "07:00"}
-- mood:      {"score": 8, "tags": ["开心", "充实"]}


-- ============================================================
-- 对话记录
-- ============================================================
CREATE TABLE conversations (
    id              BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    user_id         BIGINT UNSIGNED   NOT NULL,
    channel         ENUM('wechat','pwa') DEFAULT 'wechat',
    role            ENUM('user','assistant','system') NOT NULL,
    content         TEXT              NOT NULL,
    intent          VARCHAR(50)       DEFAULT NULL COMMENT '识别的意图',
    skill_used      VARCHAR(50)       DEFAULT NULL COMMENT '调用的技能',
    tokens_used     INT               DEFAULT 0 COMMENT '消耗的Token数',
    created_at      DATETIME          DEFAULT CURRENT_TIMESTAMP,
    KEY idx_user_time (user_id, created_at),
    FOREIGN KEY (user_id) REFERENCES users(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;


-- ============================================================
-- 推送订阅
-- ============================================================
CREATE TABLE push_subscriptions (
    id              BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    user_id         BIGINT UNSIGNED   NOT NULL,
    endpoint        VARCHAR(500)      NOT NULL COMMENT '推送服务端点URL',
    p256dh_key      VARCHAR(200)      NOT NULL COMMENT '公钥',
    auth_key        VARCHAR(100)      NOT NULL COMMENT '认证密钥',
    user_agent      VARCHAR(500)      DEFAULT NULL COMMENT '浏览器UA',
    is_active       TINYINT(1)        DEFAULT 1,
    created_at      DATETIME          DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME          DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_endpoint (endpoint(255)),
    KEY idx_user_active (user_id, is_active),
    FOREIGN KEY (user_id) REFERENCES users(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;


-- ============================================================
-- 推送记录
-- ============================================================
CREATE TABLE push_logs (
    id              BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    user_id         BIGINT UNSIGNED   NOT NULL,
    push_type       ENUM('todo_remind','event_remind','daily_summary',
                         'weekly_report','partner_assign','custom') NOT NULL,
    title           VARCHAR(200)      NOT NULL,
    body            TEXT              NOT NULL,
    related_id      BIGINT UNSIGNED   DEFAULT NULL COMMENT '关联的todo/event ID',
    status          ENUM('pending','sent','failed') DEFAULT 'pending',
    sent_at         DATETIME          DEFAULT NULL,
    created_at      DATETIME          DEFAULT CURRENT_TIMESTAMP,
    KEY idx_user_status (user_id, status),
    KEY idx_push_type_time (push_type, created_at),
    FOREIGN KEY (user_id) REFERENCES users(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;


-- ============================================================
-- 共享数据权限
-- ============================================================
CREATE TABLE shared_data (
    id              BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    owner_id        BIGINT UNSIGNED   NOT NULL COMMENT '数据所有者',
    shared_with_id  BIGINT UNSIGNED   NOT NULL COMMENT '被共享者',
    data_type       ENUM('todo','event','life_log') NOT NULL,
    data_id         BIGINT UNSIGNED   NOT NULL COMMENT '数据记录ID',
    permission      ENUM('view','edit') DEFAULT 'view',
    created_at      DATETIME          DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_share (owner_id, shared_with_id, data_type, data_id),
    FOREIGN KEY (owner_id) REFERENCES users(id),
    FOREIGN KEY (shared_with_id) REFERENCES users(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;


-- ============================================================
-- AI分析结果
-- ============================================================
CREATE TABLE analysis_results (
    id              BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    user_id         BIGINT UNSIGNED   NOT NULL,
    analysis_type   ENUM('daily','weekly','monthly','on_demand') NOT NULL,
    period_start    DATE              NOT NULL,
    period_end      DATE              NOT NULL,
    summary         TEXT              NOT NULL COMMENT 'AI生成的分析摘要',
    suggestions     JSON              DEFAULT NULL COMMENT '建议列表',
    accepted        TINYINT(1)        DEFAULT NULL COMMENT '用户是否采纳',
    created_at      DATETIME          DEFAULT CURRENT_TIMESTAMP,
    KEY idx_user_type (user_id, analysis_type, period_start),
    FOREIGN KEY (user_id) REFERENCES users(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

```

---

## 五、API 设计

### 5.1 通用约定

```
基础路径: /api/v1
认证方式: Bearer Token (JWT)
响应格式: { "code": 0, "data": {...}, "message": "ok" }
分页参数: ?page=1&page_size=20
时间格式: ISO 8601 (2026-06-18T15:00:00+08:00)

```

### 5.2 API 列表

#### 认证

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| POST | `/api/v1/auth/register` | 注册 |
| POST | `/api/v1/auth/login` | 登录 |
| POST | `/api/v1/auth/bind-wechat` | 绑定微信ID |

#### 待办

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| GET | `/api/v1/todos` | 待办列表（支持?status=&category=&due\_before=） |
| POST | `/api/v1/todos` | 创建待办 |
| GET | `/api/v1/todos/{id}` | 待办详情 |
| PUT | `/api/v1/todos/{id}` | 更新待办 |
| PATCH | `/api/v1/todos/{id}/done` | 标记完成 |
| DELETE | `/api/v1/todos/{id}` | 删除待办 |
| POST | `/api/v1/todos/{id}/assign` | 分配给伴侣 |

#### 日程

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| GET | `/api/v1/events` | 日程列表（支持?start=&end=） |
| POST | `/api/v1/events` | 创建日程 |
| GET | `/api/v1/events/{id}` | 日程详情 |
| PUT | `/api/v1/events/{id}` | 更新日程 |
| DELETE | `/api/v1/events/{id}` | 删除日程 |

#### 生活记录

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| GET | `/api/v1/life-logs` | 记录列表（支持?log\_type=&start=&end=） |
| POST | `/api/v1/life-logs` | 新增记录 |
| GET | `/api/v1/life-logs/stats` | 统计数据（按周/月） |

#### 分析

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| GET | `/api/v1/analysis/weekly` | 本周分析 |
| GET | `/api/v1/analysis/monthly` | 本月分析 |
| POST | `/api/v1/analysis/on-demand` | 按需分析 |
| GET | `/api/v1/analysis/suggestions` | AI建议列表 |
| POST | `/api/v1/analysis/suggestions/{id}/accept` | 采纳建议 |

#### 推送

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| POST | `/api/v1/push/subscribe` | 注册推送订阅 |
| DELETE | `/api/v1/push/subscription` | 取消订阅 |
| PUT | `/api/v1/push/preferences` | 推送偏好设置 |
| POST | `/api/v1/push/test` | 发送测试推送 |

#### 协作

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| GET | `/api/v1/partner/todos` | 伴侣的共享待办 |
| GET | `/api/v1/partner/events` | 伴侣的共享日程 |
| PUT | `/api/v1/share/settings` | 共享范围设置 |

#### Bot（iLink回调）

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| POST | `/bot/webhook` | iLink消息回调 |
| GET | `/bot/status` | Bot运行状态 |

---

## 六、Agent 技能开发

### 6.1 技能注册机制

```python
# app/agent/skills/base.py

from typing import Optional
from pydantic import BaseModel


class SkillResult(BaseModel):
    """技能执行结果"""
    success: bool
    message: str                    # 回复用户的文本
    data: Optional[dict] = None     # 结构化数据（可选）


class BaseSkill:
    """技能基类"""
    name: str = ""
    description: str = ""
    # 意图关键词，Agent用于路由
    trigger_intents: list[str] = []

    async def execute(self, user_id: int, params: dict) -> SkillResult:
        raise NotImplementedError

```

### 6.2 待办技能示例

```python
# app/agent/skills/todo_skill.py

from .base import BaseSkill, SkillResult
from app.services.todo_service import TodoService
from app.utils.date_parser import parse_natural_date


class TodoSkill(BaseSkill):
    name = "todo"
    description = "待办事项管理：创建、查看、完成待办"
    trigger_intents = ["create_todo", "list_todo", "complete_todo", "delete_todo"]

    async def execute(self, user_id: int, params: dict) -> SkillResult:
        intent = params.get("intent")
        todo_service = TodoService()

        if intent == "create_todo":
            title = params.get("title")
            due_date = parse_natural_date(params.get("due_date_text"))
            todo = await todo_service.create(
                user_id=user_id,
                title=title,
                due_date=due_date,
                source="wechat"
            )
            return SkillResult(
                success=True,
                message=f"✅ 已添加：{title}" +
                        (f"（{due_date.strftime('%m月%d日%H:%M')}）" if due_date else ""),
                data={"todo_id": todo.id}
            )

        elif intent == "list_todo":
            todos = await todo_service.list_pending(user_id)
            if not todos:
                return SkillResult(success=True, message="📋 当前没有待办事项")
            lines = ["📋 待办事项："]
            for t in todos:
                due = f" ({t.due_date.strftime('%m/%d %H:%M')})" if t.due_date else ""
                lines.append(f"  {'🔴' if t.priority == 'urgent' else '🟡' if t.priority == 'high' else '⚪'} {t.title}{due}")
            return SkillResult(success=True, message="\n".join(lines))

        elif intent == "complete_todo":
            keyword = params.get("keyword")
            todo = await todo_service.find_and_complete(user_id, keyword)
            if todo:
                return SkillResult(success=True, message=f"🎉 已完成：{todo.title}")
            return SkillResult(success=False, message="没找到这个待办，能说得更具体些吗？")

        return SkillResult(success=False, message="我不太明白，能再说一次吗？")

```

### 6.3 Agent 核心（意图路由）

```python
# app/agent/core.py

from app.agent.llm import LLMClient
from app.agent.skills import SKILL_REGISTRY


INTENT_PROMPT = """分析用户消息的意图，返回JSON：
{intent, params}

可选意图：
- create_todo: 创建待办 → params: {title, due_date_text}
- list_todo: 查看待办
- complete_todo: 完成待办 → params: {keyword}
- create_event: 创建日程 → params: {title, start_time_text, end_time_text}
- list_event: 查看日程
- log_diet: 记录饮食 → params: {content}
- log_exercise: 记录运动 → params: {content}
- log_work: 记录工作 → params: {content}
- analysis: 分析数据 → params: {analysis_type}
- chat: 普通聊天

用户消息：{message}
当前时间：{now}"""


class AgentCore:
    def __init__(self):
        self.llm = LLMClient()

    async def handle_message(self, user_id: int, message: str) -> str:
        # 1. 意图识别
        intent_result = await self.llm.parse_intent(
            prompt=INTENT_PROMPT.format(message=message, now=datetime.now()),
            user_id=user_id
        )

        intent = intent_result.get("intent", "chat")
        params = intent_result.get("params", {})

        # 2. 路由到技能
        if intent != "chat":
            for skill in SKILL_REGISTRY:
                if intent in skill.trigger_intents:
                    result = await skill.execute(user_id, {**params, "intent": intent})
                    if result.success:
                        return result.message

        # 3. 普通对话
        reply = await self.llm.chat(user_id=user_id, message=message)
        return reply

```

---

## 七、推送实现

### 7.1 后端推送服务

```python
# app/services/push_service.py

from pywebpush import webpush, WebPushException
from app.models.push_subscription import PushSubscription
from app.config import settings


class PushService:

    async def subscribe(self, user_id: int, subscription_data: dict):
        """保存推送订阅"""
        sub = await PushSubscription.create(
            user_id=user_id,
            endpoint=subscription_data["endpoint"],
            p256dh_key=subscription_data["keys"]["p256dh"],
            auth_key=subscription_data["keys"]["auth"],
        )
        return sub

    async def send_push(self, user_id: int, title: str, body: str,
                        url: str = None, data: dict = None):
        """发送推送通知"""
        subscriptions = await PushSubscription.filter(
            user_id=user_id, is_active=True
        ).all()

        payload = {
            "title": title,
            "body": body,
            "icon": "/icons/icon-192.png",
            "badge": "/icons/badge.png",
            "data": {"url": url, **(data or {})}
        }

        for sub in subscriptions:
            try:
                webpush(
                    subscription_info={
                        "endpoint": sub.endpoint,
                        "keys": {"p256dh": sub.p256dh_key, "auth": sub.auth_key}
                    },
                    data=json.dumps(payload),
                    vapid_private_key=settings.VAPID_PRIVATE_KEY,
                    vapid_claims={"sub": f"mailto:{settings.VAPID_EMAIL}"}
                )
            except WebPushException:
                sub.is_active = False
                await sub.save()

```

### 7.2 定时调度

```python
# app/services/scheduler_service.py

from apscheduler.schedulers.asyncio import AsyncIOScheduler
from app.services.push_service import PushService
from app.services.todo_service import TodoService
from app.services.event_service import EventService


scheduler = AsyncIOScheduler()


async def check_todo_reminders():
    """每分钟检查待办提醒"""
    todos = await TodoService.get_due_reminders(minutes_ahead=15)
    push_service = PushService()
    for todo in todos:
        await push_service.send_push(
            user_id=todo.user_id,
            title="⏰ 待办提醒",
            body=f"{todo.title}（{todo.due_date.strftime('%H:%M')}）",
            url=f"/todo/{todo.id}"
        )


async def daily_summary():
    """每天8:00推送今日安排"""
    push_service = PushService()
    # ... 查询今日待办+日程，生成摘要推送


async def weekly_analysis():
    """每周日20:00推送周报"""
    # ... 调用AnalysisService生成分析，推送


def setup_scheduler():
    scheduler.add_job(check_todo_reminders, "cron", minute="*")
    scheduler.add_job(daily_summary, "cron", hour=8, minute=0)
    scheduler.add_job(weekly_analysis, "cron", day_of_week="sun", hour=20)
    scheduler.start()

```

### 7.3 前端推送注册

```typescript
// frontend/src/composables/usePush.ts

export function usePush() {
  const registerPush = async () => {
    if (!('Notification' in window)) return
    const permission = await Notification.requestPermission()
    if (permission !== 'granted') return

    const registration = await navigator.serviceWorker.ready
    const subscription = await registration.pushManager.subscribe({
      userVisibleOnly: true,
      applicationServerKey: urlBase64ToUint8Array(VAPID_PUBLIC_KEY)
    })

    // 发送给后端保存
    await api.post('/api/v1/push/subscribe', {
      endpoint: subscription.endpoint,
      keys: {
        p256dh: btoa(String.fromCharCode(...subscription.getKey('p256dh')!)),
        auth: btoa(String.fromCharCode(...subscription.getKey('auth')!))
      }
    })
  }

  return { registerPush }
}

```

---

## 八、iLink Bot 集成

### 8.1 Bot 客户端

```python
# app/utils/ilink_client.py

import httpx
from app.config import settings


class iLinkClient:
    BASE_URL = "https://ilinkai.weixin.qq.com"

    def __init__(self):
        self.token = settings.ILINK_TOKEN
        self.client = httpx.AsyncClient(timeout=40)

    async def get_updates(self) -> list[dict]:
        """长轮询获取消息（35s超时）"""
        resp = await self.client.post(
            f"{self.BASE_URL}/ilink/bot/getupdates",
            json={"token": self.token}
        )
        return resp.json().get("data", [])

    async def send_message(self, user_id: str, content: str, msg_type: str = "text"):
        """发送消息"""
        resp = await self.client.post(
            f"{self.BASE_URL}/ilink/bot/sendmessage",
            json={
                "token": self.token,
                "user_id": user_id,
                "msg_type": msg_type,
                "content": content
            }
        )
        return resp.json()

    async def send_typing(self, user_id: str):
        """发送"正在输入"状态"""
        await self.client.post(
            f"{self.BASE_URL}/ilink/bot/sendtyping",
            json={"token": self.token, "user_id": user_id}
        )

```

### 8.2 Bot 轮询服务

```python
# app/services/bot_service.py

import asyncio
from app.utils.ilink_client import iLinkClient
from app.agent.core import AgentCore
from app.services.user_service import UserService


class BotService:
    def __init__(self):
        self.ilink = iLinkClient()
        self.agent = AgentCore()

    async def run(self):
        """启动Bot轮询"""
        while True:
            try:
                updates = await self.ilink.get_updates()
                for update in updates:
                    asyncio.create_task(self._handle_update(update))
            except Exception as e:
                logger.error(f"Bot poll error: {e}")
                await asyncio.sleep(5)

    async def _handle_update(self, update: dict):
        user_id = update.get("user_id")
        content = update.get("content", "")

        # 微信ID → 系统用户
        user = await UserService.get_by_wechat_id(user_id)
        if not user:
            await self.ilink.send_message(user_id, "请先在管理页面绑定微信账号～")
            return

        # 发送"正在输入"
        await self.ilink.send_typing(user_id)

        # Agent处理
        reply = await self.agent.handle_message(user.id, content)

        # 回复
        await self.ilink.send_message(user_id, reply)

```

---

## 九、部署

### 9.1 Docker Compose

```yaml
# docker-compose.yml

version: "3.8"

services:
  backend:
    build: ./backend
    container_name: assistant-backend
    restart: always
    ports:
      - "8000:8000"
    environment:
      - DATABASE_URL=mysql+aiomysql://assistant:${DB_PASSWORD}@mysql:3306/assistant
      - REDIS_URL=redis://redis:6379/0
      - DEEPSEEK_API_KEY=${DEEPSEEK_API_KEY}
      - ILINK_TOKEN=${ILINK_TOKEN}
      - VAPID_PRIVATE_KEY=${VAPID_PRIVATE_KEY}
      - VAPID_PUBLIC_KEY=${VAPID_PUBLIC_KEY}
      - VAPID_EMAIL=${VAPID_EMAIL}
      - JWT_SECRET=${JWT_SECRET}
    depends_on:
      mysql:
        condition: service_healthy
      redis:
        condition: service_started

  frontend:
    build: ./frontend
    container_name: assistant-frontend
    restart: always

  mysql:
    image: mysql:8.0
    container_name: assistant-mysql
    restart: always
    environment:
      MYSQL_ROOT_PASSWORD: ${MYSQL_ROOT_PASSWORD}
      MYSQL_DATABASE: assistant
      MYSQL_USER: assistant
      MYSQL_PASSWORD: ${DB_PASSWORD}
    volumes:
      - mysql_data:/var/lib/mysql
      - ./scripts/init.sql:/docker-entrypoint-initdb.d/init.sql
    command: >
      --character-set-server=utf8mb4
      --collation-server=utf8mb4_unicode_ci
      --default-authentication-plugin=mysql_native_password
    healthcheck:
      test: ["CMD", "mysqladmin", "ping", "-h", "localhost"]
      interval: 10s
      timeout: 5s
      retries: 5

  redis:
    image: redis:7-alpine
    container_name: assistant-redis
    restart: always
    volumes:
      - redis_data:/data

  nginx:
    image: nginx:alpine
    container_name: assistant-nginx
    restart: always
    ports:
      - "80:80"
      - "443:443"
    volumes:
      - ./nginx/assistant.conf:/etc/nginx/conf.d/default.conf
      - certbot_data:/etc/letsencrypt
    depends_on:
      - backend
      - frontend

volumes:
  mysql_data:
  redis_data:
  certbot_data:

```

### 9.2 Nginx 配置

```nginx
# nginx/assistant.conf

server {
    listen 80;
    server_name your-domain.cn;
    return 301 https://$server_name$request_uri;
}

server {
    listen 443 ssl http2;
    server_name your-domain.cn;

    ssl_certificate     /etc/letsencrypt/live/your-domain.cn/fullchain.pem;
    ssl_certificate_key /etc/letsencrypt/live/your-domain.cn/privkey.pem;

    # PWA 前端
    location / {
        proxy_pass http://frontend:80;
        proxy_set_header Host $host;
    }

    # 后端 API
    location /api/ {
        proxy_pass http://backend:8000;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
    }

    # Bot Webhook
    location /bot/ {
        proxy_pass http://backend:8000;
        proxy_set_header Host $host;
    }

    # WebSocket
    location /ws {
        proxy_pass http://backend:8000;
        proxy_http_version 1.1;
        proxy_set_header Upgrade $http_upgrade;
        proxy_set_header Connection "upgrade";
    }
}

```

### 9.3 环境变量

```bash
# .env.example

# MySQL
MYSQL_ROOT_PASSWORD=your_root_password
DB_PASSWORD=your_db_password

# LLM
DEEPSEEK_API_KEY=sk-xxx

# iLink Bot
ILINK_TOKEN=your_ilink_token

# Web Push VAPID
VAPID_PRIVATE_KEY=xxx
VAPID_PUBLIC_KEY=xxx
VAPID_EMAIL=your@email.com

# JWT
JWT_SECRET=your_jwt_secret

# Domain
DOMAIN=your-domain.cn

```

### 9.4 部署步骤

```bash
# 1. 服务器初始化
ssh root@your-server
apt update && apt install -y docker.io docker-compose git

# 2. 克隆项目
git clone https://github.com/yourname/assistant.git
cd assistant

# 3. 配置环境变量
cp .env.example .env
vim .env  # 填入实际值

# 4. 生成 VAPID 密钥对
python -c "
from pywebpush import generate_vapid_keys
keys = generate_vapid_keys()
print('PUBLIC:', keys['public_key'])
print('PRIVATE:', keys['private_key'])
"

# 5. 启动
docker-compose up -d

# 6. 初始化数据库
docker-compose exec backend python -m alembic upgrade head

# 7. 申请 SSL 证书
apt install certbot
certbot certonly --standalone -d your-domain.cn

# 8. 重启 Nginx
docker-compose restart nginx

# 9. 验证
curl https://your-domain.cn/api/v1/health

```

---

## 十、Capacitor 打包 APK（未来）

### 10.1 初始化

```bash
cd frontend
npm install @capacitor/core @capacitor/cli
npx cap init "小助手" com.assistant.app --web-dir dist
npx cap add android

```

### 10.2 配置

```typescript
// capacitor.config.ts
import type { CapacitorConfig } from '@capacitor/cli';

const config: CapacitorConfig = {
  appId: 'com.assistant.app',
  appName: '小助手',
  webDir: 'dist',
  server: {
    // 开发时指向本地后端
    url: process.env.NODE_ENV === 'development'
      ? 'http://192.168.x.x:8000'
      : undefined,
  },
  plugins: {
    PushNotifications: {
      presentationOptions: ['badge', 'sound', 'alert'],
    },
    LocalNotifications: {
      smallIcon: 'ic_stat_icon',
      iconColor: '#488AFF',
    },
  },
};
export default config;

```

### 10.3 打包

```bash
# 构建 PWA
npm run build

# 同步到 Android 项目
npx cap sync android

# 用 Android Studio 打开打包
npx cap open android
# → Build → Generate Signed Bundle / APK

```

> 打包后APK可直接安装，也可上传到应用商店。PWA的Web Push在APK内同样可用，后续可切换为Capacitor的PushNotifications插件获得原生推送。

---

## 十一、监控与运维

### 11.1 日志

```bash
# 查看后端日志
docker-compose logs -f backend

# 查看 Bot 日志
docker-compose logs -f backend | grep "bot"

# 查看推送日志
docker-compose logs -f backend | grep "push"

```

### 11.2 数据库备份

```bash
# scripts/backup.sh
#!/bin/bash
DATE=$(date +%Y%m%d)
docker-compose exec -T mysql mysqldump -u root -p$MYSQL_ROOT_PASSWORD assistant \
  | gzip > /data/backups/assistant_$DATE.sql.gz
# 保留最近30天
find /data/backups -name "*.sql.gz" -mtime +30 -delete

```

### 11.3 健康检查

```python
# app/routers/health.py

@router.get("/api/v1/health")
async def health_check():
    checks = {
        "mysql": await check_mysql(),
        "redis": await check_redis(),
        "ilink": await check_ilink(),
        "deepseek": await check_deepseek(),
    }
    status = "ok" if all(checks.values()) else "degraded"
    return {"status": status, "checks": checks}

```

---

## 十二、开发规范

### 12.1 Git 分支

```
main          ← 生产分支
├── dev       ← 开发分支
├── feat/xxx  ← 功能分支
├── fix/xxx   ← 修复分支
└── chore/xxx ← 杂项

```

### 12.2 提交规范

```
feat: 新增待办技能
fix: 修复推送订阅重复问题
docs: 更新API文档
refactor: 重构Agent意图路由
chore: 升级依赖版本

```

### 12.3 代码风格

-   Python: Black + isort + flake8
-   TypeScript: ESLint + Prettier
-   SQL: 关键字大写，表名蛇形命名

### 12.4 测试

```bash
# 后端测试
cd backend && pytest

# 前端测试
cd frontend && npm run test

```

---

## 附录：官方文档地址

### 后端技术

| 技术 | 官方文档 | 说明 |
| --- | --- | --- |
| **FastAPI** | [https://fastapi.tiangolo.com/zh/](https://fastapi.tiangolo.com/zh/) | Python 异步 Web 框架 |
| **SQLAlchemy** | [https://docs.sqlalchemy.org/](https://docs.sqlalchemy.org/) | Python ORM |
| **Alembic** | [https://alembic.sqlalchemy.org/](https://alembic.sqlalchemy.org/) | 数据库迁移工具 |
| **Pydantic** | [https://docs.pydantic.dev/](https://docs.pydantic.dev/) | 数据校验与序列化 |
| **aiomysql** | [https://aiomysql.readthedocs.io/](https://aiomysql.readthedocs.io/) | MySQL 异步驱动 |
| **APScheduler** | [https://apscheduler.readthedocs.io/](https://apscheduler.readthedocs.io/) | Python 定时任务调度 |
| **PyJWT** | [https://pyjwt.readthedocs.io/](https://pyjwt.readthedocs.io/) | JWT 认证 |
| **httpx** | [https://www.python-httpx.org/](https://www.python-httpx.org/) | 异步 HTTP 客户端 |
| **uvicorn** | [https://www.uvicorn.org/](https://www.uvicorn.org/) | ASGI 服务器 |

### 数据库与缓存

| 技术 | 官方文档 | 说明 |
| --- | --- | --- |
| **MySQL 8.0** | [https://dev.mysql.com/doc/refman/8.0/en/](https://dev.mysql.com/doc/refman/8.0/en/) | 关系型数据库 |
| **Redis** | [https://redis.io/docs/](https://redis.io/docs/) | 内存缓存与消息队列 |

### 前端技术

| 技术 | 官方文档 | 说明 |
| --- | --- | --- |
| **Vue 3** | [https://cn.vuejs.org/](https://cn.vuejs.org/) | 前端框架 |
| **Vite** | [https://cn.vitejs.dev/](https://cn.vitejs.dev/) | 构建工具 |
| **Vant 4** | [https://vant-ui.github.io/vant/#/zh-CN](https://vant-ui.github.io/vant/#/zh-CN) | 移动端 UI 组件库 |
| **Pinia** | [https://pinia.vuejs.org/zh/](https://pinia.vuejs.org/zh/) | Vue3 状态管理 |
| **Vue Router** | [https://router.vuejs.org/zh/](https://router.vuejs.org/zh/) | Vue 路由 |
| **ECharts** | [https://echarts.apache.org/zh/](https://echarts.apache.org/zh/) | 数据可视化图表 |

### PWA 与推送

| 技术 | 官方文档 | 说明 |
| --- | --- | --- |
| **PWA** | [https://web.dev/progressive-web-apps/](https://web.dev/progressive-web-apps/) | 渐进式 Web 应用 |
| **Service Worker** | [https://developer.mozilla.org/zh-CN/docs/Web/API/Service\_Worker\_API](https://developer.mozilla.org/zh-CN/docs/Web/API/Service_Worker_API) | 离线与后台能力 |
| **Web Push API** | [https://developer.mozilla.org/zh-CN/docs/Web/API/Push\_API](https://developer.mozilla.org/zh-CN/docs/Web/API/Push_API) | 浏览器推送通知 |
| **vite-plugin-pwa** | [https://vite-pwa-org.netlify.app/](https://vite-pwa-org.netlify.app/) | Vite PWA 插件 |
| **pywebpush** | [https://github.com/web-push-libs/pywebpush](https://github.com/web-push-libs/pywebpush) | Python Web Push 库 |
| **VAPID** | [https://datatracker.ietf.org/doc/html/draft-ietf-webpush-vapid-03](https://datatracker.ietf.org/doc/html/draft-ietf-webpush-vapid-03) | 推送认证协议 |

### Agent 与 LLM

| 技术 | 官方文档 | 说明 |
| --- | --- | --- |
| **CowAgent** | [https://github.com/zhayujie/chatgpt-on-wechat](https://github.com/zhayujie/chatgpt-on-wechat) | 微信+Agent 一体化框架 |
| **DeepSeek API** | [https://platform.deepseek.com/api-docs/](https://platform.deepseek.com/api-docs/) | LLM 推理 API |
| **Qwen API** | [https://help.aliyun.com/zh/model-studio/](https://help.aliyun.com/zh/model-studio/) | 通义千问 API |
| **LangGraph** | [https://langchain-ai.github.io/langgraph/](https://langchain-ai.github.io/langgraph/) | Agent 编排框架（备选） |

### 微信接入

| 技术 | 官方文档 | 说明 |
| --- | --- | --- |
| **iLink Bot API** | [https://wechatbot.dev/zh](https://wechatbot.dev/zh) | 微信官方 Bot API |
| **企业微信API** | [https://developer.work.weixin.qq.com/document/](https://developer.work.weixin.qq.com/document/) | 企微应用消息推送 |

### 部署与运维

| 技术 | 官方文档 | 说明 |
| --- | --- | --- |
| **Docker** | [https://docs.docker.com/](https://docs.docker.com/) | 容器引擎 |
| **Docker Compose** | [https://docs.docker.com/compose/](https://docs.docker.com/compose/) | 多容器编排 |
| **Nginx** | [https://nginx.org/en/docs/](https://nginx.org/en/docs/) | 反向代理与静态服务 |
| **Certbot** | [https://certbot.eff.org/](https://certbot.eff.org/) | Let's Encrypt SSL 证书 |
| **阿里云 ECS** | [https://help.aliyun.com/product/253659.html](https://help.aliyun.com/product/253659.html) | 云服务器 |

### APP 打包

| 技术 | 官方文档 | 说明 |
| --- | --- | --- |
| **Capacitor** | [https://capacitorjs.com/docs](https://capacitorjs.com/docs) | PWA → 原生 APP 打包 |
| **Android Studio** | [https://developer.android.com/studio](https://developer.android.com/studio) | Android 开发与签名 |

### 开发工具

| 技术 | 官方文档 | 说明 |
| --- | --- | --- |
| **Black** | [https://black.readthedocs.io/](https://black.readthedocs.io/) | Python 代码格式化 |
| **isort** | [https://pycqa.github.io/isort/](https://pycqa.github.io/isort/) | Python import 排序 |
| **flake8** | [https://flake8.pycqa.org/](https://flake8.pycqa.org/) | Python 代码检查 |
| **ESLint** | [https://eslint.org/](https://eslint.org/) | JS/TS 代码检查 |
| **Prettier** | [https://prettier.io/](https://prettier.io/) | 前端代码格式化 |
| **pytest** | [https://docs.pytest.org/](https://docs.pytest.org/) | Python 测试框架 |

---

_文档版本 v1.0 | 随项目迭代更新_