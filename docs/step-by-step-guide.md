# 逐步最小版本开发计划

> 核心原则：**每一步都能跑起来、能看到结果**，用 Cursor + DeepSeek 一步步搭建
> 关键策略：**站在模版基座的肩膀上**，不从零手搓
> 开发哲学：**对话优先** — 先做一个能聊天的 AI 助手，再让 AI 逐步"学会"操作待办/日程/记录

## 模版基座总览


| 模版                              | 用途                                 | 仓库                                                                                                               |
| ------------------------------- | ---------------------------------- | ---------------------------------------------------------------------------------------------------------------- |
| **Full Stack FastAPI Template** | 后端骨架（项目结构+JWT+Docker+Alembic）      | [https://github.com/fastapi/full-stack-fastapi-template](https://github.com/fastapi/full-stack-fastapi-template) |
| **vue3-vant-mobile**            | APP 前端（Vant4+PWA+Pinia+TS）         | [https://github.com/vue-zone/vue3-vant-mobile](https://github.com/vue-zone/vue3-vant-mobile)                     |
| **vue3-element-admin**          | Admin 管理面板（Element Plus+布局+权限）     | [https://github.com/youlaitech/vue3-element-admin](https://github.com/youlaitech/vue3-element-admin)             |
| **OpeniLink Hub**               | 微信 Bot 托管（iLink 协议+App 市场+Webhook） | [https://github.com/openilink/openilink-hub](https://github.com/openilink/openilink-hub)                         |
| **CowAgent**                    | Agent 框架（Skill 系统+微信原生支持）          | [https://github.com/zhayujie/CowAgent](https://github.com/zhayujie/CowAgent)                                     |


---

## 开发环境准备

### 工具安装

- [Cursor](https://cursor.sh) — AI 代码编辑器
- [Docker Desktop](https://www.docker.com/products/docker-desktop/) — 本地数据库 + 模版服务
- [Node.js 20+](https://nodejs.org/) — 前端构建
- [Python 3.11+](https://www.python.org/) — 后端运行
- Git — 版本管理

### Cursor 配置

1. 打开 Cursor → Settings → Models → 选择 DeepSeek 作为默认模型
2. 项目根目录已有 `.cursor/rules/` 规则文件，Cursor 会自动加载
3. 每次开发新步骤时，在 Cursor Chat 中说明当前步骤目标即可

---

## Step 1：后端骨架 — 基于 Full Stack FastAPI Template

**目标**：从官方模版起步，改 MySQL，跑通 API + Swagger + JWT 认证

**预计时间**：1 小时

### 为什么用模版

Full Stack FastAPI Template 自带：

- ✅ 完整项目结构（api/core/models/schemas/crud 分层）
- ✅ JWT 认证（注册/登录/Token 刷新）
- ✅ Alembic 数据库迁移
- ✅ Docker Compose（backend + db）
- ✅ 环境变量管理 + .env 配置
- ✅ CORS、异常处理、日志等基础设施

### 操作

```bash
# 1. 克隆模版
git clone https://github.com/fastapi/full-stack-fastapi-template.git
rm -rf .git
git init

# 2. 删除不需要的前端（模版自带 React 前端，我们用自己的）
rm -rf frontend

# 3. 在 Cursor 中打开项目

```

在 Cursor Chat 中：

```
参考 .cursor/rules/ 规范，对这个 FastAPI 模版做以下改造：
1. 把 PostgreSQL 替换为 MySQL 8.0（驱动改 aiomysql，连接串改 mysql+aiomysql://）
2. 保留 JWT 认证、Alembic 迁移、Docker Compose 等基础设施
3. 把 docker-compose.yml 中的 postgres 服务替换为 mysql:8.0，配置 utf8mb4
4. 把 pgadmin 服务删掉
5. 确认 /api/v1/utils/health-check 能正常返回
6. 确认 /docs Swagger 页面可访问

```

### 关键改动点


| 文件                         | 改动                                             |
| -------------------------- | ---------------------------------------------- |
| `backend/app/core/db.py`   | `create_async_engine` 连接串改 `mysql+aiomysql://` |
| `backend/requirements.txt` | 删 `asyncpg`，加 `aiomysql`                       |
| `docker-compose.yml`       | postgres → mysql:8.0，加 charset 配置              |
| `backend/app/models.py`    | 检查字段类型兼容性（JSON、ARRAY 等）                        |
| `alembic/env.py`           | 确认连接串读取正确                                      |


### 验证

```bash
docker compose up -d db
cd backend
pip install -r requirements.txt
uvicorn app.main:app --reload --port 8000

# 测试健康检查
curl http://localhost:8000/api/v1/utils/health-check

# 测试 Swagger — 浏览器访问 http://localhost:8000/docs

# 测试注册登录（模版自带）
curl -X POST http://localhost:8000/api/v1/auth/register \
  -H "Content-Type: application/json" \
  -d '{"email": "test@example.com", "password": "test1234"}'

```

### 完成标志

- ✅ MySQL 容器运行正常
- ✅ 健康检查接口返回正常
- ✅ Swagger 文档可访问
- ✅ 注册/登录接口可用
- ✅ `git commit -m "step1: backend from FastAPI template, MySQL adapted"`

---

## Step 2：用户模型适配 — 改造模版 User 表

**目标**：把模版的 User 表改成项目需要的字段（手机号登录、昵称、头像等）

**预计时间**：30 分钟

### 操作

在 Cursor Chat 中：

```
改造 User 模型，适配项目需求：
1. 新增 phone 字段（VARCHAR(20)，唯一索引），作为登录凭证替代 email
2. 新增 nickname 字段（VARCHAR(50)）
3. 新增 avatar 字段（VARCHAR(500)，可选，头像 URL）
4. 保留原有的 id/created_at/updated_at/is_active/is_superuser
5. 登录接口改为手机号+密码登录
6. 注册接口改为手机号+密码+昵称
7. 生成 Alembic 迁移

```

### 验证

```bash
cd backend && alembic upgrade head

# 测试新注册接口
curl -X POST http://localhost:8000/api/v1/auth/register \
  -H "Content-Type: application/json" \
  -d '{"phone": "13800138000", "password": "test1234", "nickname": "海培"}'

# 测试新登录接口
curl -X POST http://localhost:8000/api/v1/auth/login \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "username=13800138000&password=test1234"

```

### 完成标志

- ✅ 手机号注册/登录可用
- ✅ users 表字段符合项目需求
- ✅ `git commit -m "step2: User model adapted for phone login"`

---

## Step 3：对话模块 — AI 聊天核心 ⭐

**目标**：接入 DeepSeek，实现基础 AI 对话，对话记录自存 MySQL

**预计时间**：1.5 小时

### 为什么对话优先

- Step 3 完成就能跟 AI 聊天了（虽然只会闲聊），**第一步就有可玩的东西**
- 后续 Skill 是"长"出来的：先有对话框架，再教 AI 学会操作待办/日程
- 对话记录是上游数据，待办/日程/记录是下游操作，先有上游

### 对话存储设计

**自存 MySQL**，不托管给 CowAgent/DeepSeek：


| 原因           | 说明                           |
| ------------ | ---------------------------- |
| 对话是上游数据      | "帮我添加待办"这条对话 → 触发创建待办，需要追溯来源 |
| AI 分析需要历史    | "分析我最近的饮食/运动"需要回溯对话记录        |
| Admin 面板要展示  | 管理面板查看对话历史、统计活跃度             |
| 数据量极小        | 只有 2 个用户，存储成本忽略不计            |
| DeepSeek 无状态 | API 不存历史，每次要发完整上下文，必须自己存     |


### 数据流

```
用户消息 → 后端 API → 存入 conversations 表 → 拼接历史发给 DeepSeek → 回复也存入表 → 返回用户

```

### 操作

在 Cursor Chat 中：

```
创建对话模块，接入 DeepSeek API：
1. 创建 Conversation 模型：
   - id, user_id, role(user/assistant/system), content, created_at
   - 同一用户的多轮对话按时间排序
2. 创建 ConversationSchema：MessageCreate / MessageResponse / ChatRequest / ChatResponse
3. 创建 ConversationService：
   - save_message(user_id, role, content) — 存消息
   - get_history(user_id, limit=20) — 取最近 N 条历史
   - chat(user_id, message) — 核心方法：存用户消息 → 拼历史 → 调 DeepSeek → 存回复 → 返回
4. 创建 app/core/llm.py — DeepSeek 客户端封装：
   - AsyncOpenAI(api_key=DEEPSEEK_API_KEY, base_url="https://api.deepseek.com/v1")
   - chat_completion(messages, model="deepseek-v4-flash", temperature=0.7)
5. 创建 app/api/chat.py：
   - POST /api/v1/chat — 发送消息，返回 AI 回复
   - GET /api/v1/chat/history — 获取对话历史
6. System Prompt：你是一个贴心的个人助手，帮助用户管理待办、日程和生活记录。目前你只能聊天，后续会学会操作待办等功能。
7. 生成 Alembic 迁移

```

### conversations 表结构

```sql
CREATE TABLE conversations (
    id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT UNSIGNED NOT NULL,
    role VARCHAR(20) NOT NULL COMMENT 'user/assistant/system',
    content TEXT NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_conversations_user_id (user_id),
    INDEX idx_conversations_user_created (user_id, created_at),
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

```

### 验证

```bash
cd backend && alembic upgrade head

TOKEN="eyJ..."

# 发送消息
curl -X POST http://localhost:8000/api/v1/chat \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"message": "你好，你是谁？"}'
# 期望：AI 回复自我介绍

# 多轮对话
curl -X POST http://localhost:8000/api/v1/chat \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"message": "我刚才说了什么？"}'
# 期望：AI 能回忆上一轮对话内容

# 查看历史
curl http://localhost:8000/api/v1/chat/history \
  -H "Authorization: Bearer $TOKEN"
# 期望：返回刚才的对话记录列表

```

### 完成标志

- ✅ AI 能正常回复消息
- ✅ 多轮对话有上下文记忆
- ✅ 对话记录存入 MySQL
- ✅ 历史记录接口可用
- ✅ **🎉 AI 能聊天了！**
- ✅ `git commit -m "step3: conversation module with DeepSeek chat"`

---

## Step 4：APP 前端 — 基于 vue3-vant-mobile + 聊天页面

**目标**：从模版起步，登录 + 聊天页面，手机上能跟 AI 对话

**预计时间**：1.5 小时

### 操作

```bash
# 1. 克隆模版到 app/ 目录
cd personal-assistant
git clone --depth 1 https://github.com/vue-zone/vue3-vant-mobile.git app
cd app && rm -rf .git && npm install

```

在 Cursor Chat 中：

```
按照 .cursor/rules/vue3-frontend.mdc 规范，改造 vue3-vant-mobile 模版：
1. 删除模版自带的示例页面（about/dashboard 等）
2. 创建登录页（手机号+密码），对接后端 /api/v1/auth/login
3. 创建首页，底部 TabBar 三个 Tab：助手/待办/我的
   - "助手"Tab → 聊天页面（默认首页）
   - "待办"Tab → 占位页（Step 6 实现）
   - "我的"Tab → 个人信息页
4. 聊天页面 Chat.vue：
   - 消息气泡（用户右侧蓝色，AI 左侧灰色）
   - 底部输入框 + 发送按钮
   - 自动滚动到最新消息
   - 加载历史记录（进入页面时拉取）
   - 对接 POST /api/v1/chat 和 GET /api/v1/chat/history
5. 封装 Axios（自动带 Token、401 跳登录、统一错误处理）
6. 创建 Pinia 用户 Store（Token 管理、登录状态）
7. 配置 Vue Router 路由守卫（未登录跳转登录页）
8. 配置 Vite 代理 /api → http://localhost:8000

```

### 验证

```bash
# 同时启动后端和前端
cd backend && uvicorn app.main:app --reload --port 8000
cd app && npm run dev

# 浏览器访问 http://localhost:5173
# 1. 看到登录页面
# 2. 输入 Step 2 注册的账号登录
# 3. 跳转到聊天页面
# 4. 输入"你好"，AI 回复
# 5. 多轮对话正常
# 6. 刷新页面，历史记录保留

```

### 完成标志

- ✅ 登录页可正常登录
- ✅ 聊天页面能跟 AI 对话
- ✅ 多轮对话有上下文
- ✅ 历史记录持久化
- ✅ **🎉 手机上能跟 AI 聊天了！可以给女朋友看了**
- ✅ `git commit -m "step4: APP with chat page, talking to AI on phone"`

---

## Step 5：CowAgent Skill — AI 学会管待办

**目标**：接入 CowAgent 框架，让 AI 从"只会聊天"进化到"能操作待办"

**预计时间**：1.5 小时

### 为什么这一步才引入 CowAgent

- Step 3-4 已经有了对话框架和 DeepSeek 接入，但 AI 只会闲聊
- CowAgent 的 Skill 系统让 AI "学会"调用工具：识别意图 → 调用待办 API → 返回结果
- 先有对话框架，再长出能力，比一开始就搭完整 Agent 更容易调试

### 操作

```bash
# 安装 CowAgent
cd backend
pip install cowagent

```

在 Cursor Chat 中：

```
基于 CowAgent 框架，让 AI 学会操作待办：
1. 创建 Todo CRUD 后端模块（models/schemas/crud/api）：
   - Todo 模型：id/user_id/title/description/due_date/priority/is_completed/is_deleted/created_at/updated_at
   - 5 个路由：POST/GET/GET/{id}/PUT/{id}/DELETE/{id}
   - 生成 Alembic 迁移
2. 创建 app/skills/ 目录，按 CowAgent Skill 规范编写：
   - skills/todo_skill/SKILL.md — 待办意图识别和操作
   - 识别意图：创建待办/查看待办/完成待办/删除待办
   - 调用后端 TodoService 执行操作
3. 改造 app/core/agent.py：
   - 封装 CowAgent，注册 todo_skill
   - 用户消息 → CowAgent 意图识别 → 命中 Skill → 调用 Service → 返回结果
   - 未命中 Skill → 走普通聊天
4. 改造 app/api/chat.py：
   - chat() 方法改为先走 AgentCore，再 fallback 到纯聊天

```

### 验证

```bash
alembic upgrade head

# 在 APP 聊天界面输入：
"帮我添加一个待办：明天下午3点开会"
# 期望：AI 回复"✅ 已添加待办：明天下午3点开会"

"我有什么待办？"
# 期望：AI 列出未完成待办

"把第一个待办标记完成"
# 期望：AI 执行完成操作

# 普通聊天仍然正常
"今天天气怎么样？"
# 期望：AI 正常闲聊回复

```

### 完成标志

- ✅ AI 能识别待办意图并操作
- ✅ 待办数据写入 MySQL
- ✅ 普通聊天不受影响
- ✅ **🎉 AI 从聊天助手进化为待办助手！**
- ✅ `git commit -m "step5: CowAgent todo skill - AI learns to manage todos"`

---

## Step 6：APP 待办页面 — 前后端打通

**目标**：APP 中能直接管理待办（列表/添加/完成/删除），和 AI 对话创建的待办共享数据

**预计时间**：45 分钟

### 操作

在 Cursor Chat 中：

```
实现待办模块前端页面：
1. api/todo.ts — 封装待办 API（对接 Step 5 的后端接口）
2. stores/todo.ts — Pinia Store（列表、加载状态、分页）
3. views/TodoList.vue — 待办列表页（下拉刷新+上拉加载+左滑删除+点击完成）
4. components/TodoItem.vue — 待办项组件
5. components/TodoAddPopup.vue — 添加待办弹窗（标题+优先级+日期选择）
6. types/todo.ts — TypeScript 类型定义
7. TabBar "待办"Tab 从占位页改为 TodoList.vue

```

### 验证

```bash
# 1. 在聊天中让 AI 添加待办 "买牛奶"
# 2. 切到"待办"Tab，看到 "买牛奶" 出现在列表中
# 3. 在待办页面手动添加 "遛狗"
# 4. 回到聊天问"我有什么待办？"，AI 列出包含 "遛狗" 的列表
# 5. 在待办页面点击完成/左滑删除

```

### 完成标志

- ✅ 待办页面增删改查可用
- ✅ AI 创建的待办在页面可见，页面操作的待办 AI 也能查到
- ✅ **🎉 对话和页面两条路径打通，数据一致**
- ✅ `git commit -m "step6: todo pages - chat and UI share same data"`

---

## Step 7：CowAgent Skill — 日程 + 生活记录

**目标**：AI 学会操作日程和生活记录，能力继续扩展

**预计时间**：1.5 小时

### 操作

1. 后端：`创建 Event 和 LifeLog 两个 CRUD 模块`
  - Event：id/userid/title/description/starttime/endtime/location/remindbefore/isdeleted/createdat/updatedat
  - LifeLog：id/userid/logtype(diet/exercise/mood/other)/content/tags(JSON)/logdate/isdeleted/createdat/updatedat
2. CowAgent Skill：
  - `创建 skills/event_skill/SKILL.md — 日程意图识别和操作`
  - `创建 skills/lifelog_skill/SKILL.md — 生活记录意图识别和操作`
  - 注册到 AgentCore

### 验证

```bash
# 在 APP 聊天界面输入：
"明天下午2点有个会议"        # → 创建日程
"我今天跑了5公里"            # → 创建运动记录
"中午吃了一碗牛肉面"         # → 创建饮食记录
"这周有什么安排？"            # → 列出日程
"我最近运动情况怎么样？"      # → 查询运动记录

```

### 完成标志

- ✅ AI 能识别日程/生活记录意图
- ✅ 数据写入 MySQL
- ✅ `git commit -m "step7: event + lifelog skills - AI capability expansion"`

---

## Step 8：APP 日程/记录页面

**目标**：日程日历视图 + 生活记录时间线

**预计时间**：1 小时

### 操作

1. 日程页面：`创建日程页面，Vant Calendar 组件展示日历，点击日期查看/添加日程`
2. 生活记录页面：`创建生活记录页面，按日期时间线展示，快速添加记录`
3. TabBar 调整：助手/待办/日程/我的（4 个 Tab）

### 完成标志

- ✅ 日程日历页面可用
- ✅ 生活记录时间线可用
- ✅ **🎉 全部核心功能页面就位**
- ✅ `git commit -m "step8: event calendar + lifelog timeline pages"`

---

## Step 9：微信机器人 — 基于 OpeniLink Hub

**目标**：微信中跟机器人对话，和 APP 聊天功能一致

**预计时间**：1.5 小时

### 操作

```bash
# 部署 OpeniLink Hub
docker run -d -p 9800:9800 openilink/openilink-hub:latest
# 访问 http://localhost:9800 配置 iLink Bot

```

在 Cursor Chat 中：

```
对接 OpeniLink Hub 的 Webhook 通道：
1. 创建 app/api/webhook.py — POST /api/v1/webhook/ilink，接收 Hub 推送的微信消息
2. 创建 app/services/bot.py — BotService，解析消息，调用 AgentCore 处理，返回回复
3. 在 OpeniLink Hub 中配置 Webhook URL 指向后端
4. 用户消息 → Hub Webhook → 后端 AgentCore → 回复 → Hub → 微信
5. 微信用户和 APP 用户通过 phone 字段关联，共享数据

```

### 完成标志

- ✅ 微信中给机器人发消息能收到回复
- ✅ "添加待办" 能创建待办
- ✅ 微信和 APP 数据同步
- ✅ **🎉 女朋友不用装 APP 也能用了**
- ✅ `git commit -m "step9: WeChat Bot via OpeniLink Hub"`

---

## Step 10：推送通知 — PWA Web Push

**目标**：待办提醒、日程提醒推送到手机通知栏

**预计时间**：1 小时

### 操作

1. 后端：`实现 Web Push 订阅和推送，push_subscriptions 表，APScheduler 定时检查提醒`
2. 前端：`vue3-vant-mobile 已内置 vite-plugin-pwa，注册 Service Worker，请求通知权限`

### 完成标志

- ✅ APP 请求通知权限成功
- ✅ 创建带提醒的待办，到时间收到推送
- ✅ `git commit -m "step10: PWA Web Push notifications"`

---

## Step 11：管理面板 — 基于 vue3-element-admin

**目标**：桌面端管理界面，查看数据统计、对话历史、管理用户

**预计时间**：1 小时

### 操作

```bash
cd personal-assistant
git clone --depth 1 https://github.com/youlaitech/vue3-element-admin.git admin
cd admin && rm -rf .git && npm install

```

在 Cursor Chat 中：

```
改造 vue3-element-admin 模版：
1. 删除模版自带的 Java 后端对接代码，改为对接 FastAPI 后端
2. 保留登录页，改为手机号+密码登录
3. 左侧菜单：仪表盘/用户管理/对话记录/待办管理/日程管理/生活记录/系统设置
4. 仪表盘：统计卡片（用户数/待办数/日程数/对话数）+ 近7天活动折线图
5. 对话记录页面：按用户查看对话历史（关键页面，对话是核心数据）
6. 配置 Vite 代理 /api → http://localhost:8000

```

### 完成标志

- ✅ Admin 登录后看到仪表盘
- ✅ 对话记录可查看
- ✅ `git commit -m "step11: Admin panel from vue3-element-admin template"`

---

## Step 12：Docker 部署 — 上线

**目标**：一键部署到阿里云，外网可访问

**预计时间**：1 小时

### 操作

在 Cursor Chat 中：

```
完善 docker-compose.yml 部署配置：
1. 保留模版已有的 backend + mysql + redis 服务
2. 新增 app 服务（vue3-vant-mobile 构建后用 nginx 托管）
3. 新增 admin 服务（vue3-element-admin 构建后用 nginx 托管）
4. 新增 ilink-hub 服务（OpeniLink Hub）
5. 新增 nginx 反向代理（/ → app, /admin → admin, /api → backend）
6. 配置 .env.example 包含所有环境变量

```

### 完成标志

- ✅ 外网可访问 APP 和 Admin
- ✅ API 健康检查正常
- ✅ **🎉 正式上线！**
- ✅ `git commit -m "step12: Docker deployment"`

---

## Step 13（可选）：APK 打包

**目标**：用 Capacitor 把 PWA 打包成 APK

**预计时间**：45 分钟

### 操作

```bash
cd app
npm install @capacitor/core @capacitor/cli
npx cap init "个人助手" com.personal.assistant
npx cap add android
npm run build
npx cap copy
npx cap open android

```

### 完成标志

- ✅ APK 安装到安卓手机
- ✅ 推送通知正常
- ✅ `git commit -m "step13: Capacitor APK packaging"`

---

## 开发节奏


| 步骤      | 内容                       | 时间         | 里程碑            |
| ------- | ------------------------ | ---------- | -------------- |
| Step 1  | 后端骨架（FastAPI Template）   | 1h         | 🟢 API 能跑      |
| Step 2  | 用户适配（手机号登录）              | 30min      | 🟢 能登录         |
| Step 3  | **对话模块（DeepSeek）**       | 1.5h       | 🟢 **AI 能聊天**  |
| Step 4  | **APP 前端（聊天页面）**         | 1.5h       | 🟢 **手机上能聊**   |
| Step 5  | CowAgent Skill：待办        | 1.5h       | 🟡 AI 学会管待办    |
| Step 6  | APP 待办页面                 | 45min      | 🟡 **对话+页面打通** |
| Step 7  | CowAgent Skill：日程+记录     | 1.5h       | 🟡 AI 能力扩展     |
| Step 8  | APP 日程/记录页面              | 1h         | 🟡 全部页面就位      |
| Step 9  | 微信机器人（OpeniLink Hub）     | 1.5h       | 🟠 微信入口        |
| Step 10 | 推送通知                     | 1h         | 🟠 主动触达        |
| Step 11 | 管理面板（vue3-element-admin） | 1h         | 🟠 管理能力        |
| Step 12 | Docker 部署                | 1h         | 🔴 **上线**      |
| Step 13 | APK 打包                   | 45min      | 🔴 原生体验        |
| **总计**  |                          | **~13.5h** |                |


### 关键里程碑


| 步骤          | 里程碑                | 你能做什么                       |
| ----------- | ------------------ | --------------------------- |
| **Step 4**  | 🟢 **手机上能跟 AI 聊天** | 可以给女朋友看了！虽然只会闲聊             |
| **Step 6**  | 🟡 **对话+页面打通**     | AI 说的待办在页面能看到，页面加的待办 AI 也能查 |
| **Step 9**  | 🟠 **微信入口**        | 女朋友不用装 APP，微信直接跟 AI 对话      |
| **Step 12** | 🔴 **正式上线**        | 外网可访问                       |


### 对话优先 vs 待办优先对比


| 维度         | 对话优先（本方案）          | 待办优先（旧方案）      |
| ---------- | ------------------ | -------------- |
| Step 4 就能玩 | ✅ 跟 AI 聊天          | ❌ 只有登录页        |
| 给女朋友看的时机   | Step 4（~4h）        | Step 6（~6h）    |
| AI 能力增长感   | ✅ 从闲聊→管待办→管日程，逐步进化 | ❌ AI 一次性全上     |
| 调试难度       | ✅ 先调通对话，再加 Skill   | ❌ 对话+Skill 一起调 |
| 功能完整度      | 一样                 | 一样             |


---

## 每步开发流程

```
1. 在 Cursor 中打开项目
2. 在 Chat 中描述当前步骤目标（参考上面的"操作"描述）
3. 明确告诉 Cursor 基于哪个模版改
4. Cursor 生成代码 → 检查是否符合 .cursor/rules/ 规范
5. 运行验证命令
6. 确认通过 → git commit
7. 进入下一步

```

## 遇到问题怎么办

1. **模版代码和项目规范冲突** → 以 `.cursor/rules/` 为准，让 Cursor 改模版代码
2. **FastAPI Template 的 PostgreSQL 特性** → 搜索替换为 MySQL 等价写法
3. **DeepSeek API 报错** → 检查 API Key、余额、网络连接
4. **CowAgent Skill 不生效** → 检查 SKILL.md 格式，参考技能广场示例
5. **对话上下文丢失** → 检查 gethistory 是否正确返回，确认传给 DeepSeek 的 messages 格式
6. **前端连不上后端** → 确认 Vite 代理配置、后端端口、CORS 设置
7. **数据库迁移失败** → `alembic downgrade base && alembic upgrade head` 重置
8. **OpeniLink Hub 对接问题** → 先用自研 iLink 轮询兜底
