# 待办功能 实施计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 实现完整的待办功能，包括个人待办 CRUD、指派给伴侣、确认回复、首页最近 3 条展示、设置模板管理。

**Architecture:** 后端新增 todo 和 ack_template 两个领域模块，遵循 sharedrecord 的直接 `@Service` 模式。todo 表通过 `user_id`（创建者）和 `assigned_to`（被指派者）关联用户，`ack_status`/`ack_message` 记录确认状态。模板存储在服务端，首次自动初始化预设，每人最多 5 条。

**Tech Stack:** Spring Boot 3 + MyBatis-Plus + Flyway + Vue 3 + Vant 4 + Pinia

**参考文件：**
- 设计文档：`docs/superpowers/specs/2026-06-24-todo-feature-design.md`
- 后端模板：`backend/lifeassistant/lifeassistant-system/src/main/java/top/lifeassistant/sharedrecord/` 下所有文件
- 前端模板：`front/vue3-vant-mobile/src/pages/share/index.vue`（CRUD 交互模式）
- 现有前端 API：`front/vue3-vant-mobile/src/api/modules/todos.ts`（接口定义已就绪，需要调整）

---

### Task 1: Flyway 迁移 — todo 表加字段 + 建模板表

**文件：**
- Create: `backend/lifeassistant/lifeassistant-server/src/main/resources/db/migration/V2__add_todo_ack_and_template.sql`

```sql
ALTER TABLE todo
  ADD COLUMN ack_status  VARCHAR(16)  NOT NULL DEFAULT 'none'
    COMMENT '确认状态：none=个人待办 / unconfirmed=待确认 / confirmed=已确认';

ALTER TABLE todo
  ADD COLUMN ack_message VARCHAR(100) DEFAULT NULL
    COMMENT '确认回复文案';

CREATE TABLE todo_ack_template (
    id          CHAR(36)     NOT NULL COMMENT 'UUID 主键',
    user_id     CHAR(36)     NOT NULL COMMENT '所属用户',
    content     VARCHAR(100) NOT NULL COMMENT '文案内容',
    sort_order  INT          NOT NULL DEFAULT 0 COMMENT '排序顺序',
    created_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    KEY idx_user_id (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='确认回复文案模板';
```

- [ ] **Step 1: 创建 V2 迁移文件**
- [ ] **Step 2: 在本地 MySQL 执行验证迁移成功**
- [ ] **Step 3: Commit**

---

### Task 2: 后端 — Todo Entity + Mapper

**文件：**
- Create: `backend/lifeassistant/lifeassistant-system/src/main/java/top/lifeassistant/todo/model/entity/TodoDO.java`
- Create: `backend/lifeassistant/lifeassistant-system/src/main/java/top/lifeassistant/todo/mapper/TodoMapper.java`

TodoDO.java — 继承 BaseDO，字段对应 todo 表所有列（id, userId, title, description, isCompleted, priority, category, dueDate, assignedTo, assignedBy, ackStatus, ackMessage, completedAt, cancelledAt）。

TodoMapper.java — 继承 BaseMapper<TodoDO>，加两个自定义查询：
- `@Select("SELECT * FROM todo WHERE user_id = #{userId} OR assigned_to = #{userId} ORDER BY due_date ASC LIMIT 3")` — 首页最近 3 条
- `@Select("SELECT * FROM todo WHERE (user_id = #{userId} OR assigned_to = #{userId}) AND is_completed = 0 AND due_date IS NOT NULL ORDER BY due_date ASC")` — 未完成的待办按截止日期升序

- [ ] **Step 1: 创建 TodoDO.java**
- [ ] **Step 2: 创建 TodoMapper.java**
- [ ] **Step 3: Commit**

---

### Task 3: 后端 — Todo Service + Controller

**文件：**
- Create: `backend/lifeassistant/lifeassistant-system/src/main/java/top/lifeassistant/todo/service/TodoService.java`
- Create: `backend/lifeassistant/lifeassistant-system/src/main/java/top/lifeassistant/todo/controller/TodoController.java`
- Create: `backend/lifeassistant/lifeassistant-system/src/main/java/top/lifeassistant/todo/model/req/TodoCreateReq.java`
- Create: `backend/lifeassistant/lifeassistant-system/src/main/java/top/lifeassistant/todo/model/req/TodoUpdateReq.java`
- Create: `backend/lifeassistant/lifeassistant-system/src/main/java/top/lifeassistant/todo/model/resp/TodoResp.java`

TodoService.java：
- `@Service`，`@RequiredArgsConstructor` 注入 TodoMapper
- `listMyTodos(userId, isCompleted, priority, startDueDate, endDueDate)` — 查询 `user_id = userId OR assigned_to = userId`，支持筛选
- `getById(id)` — 查单条
- `create(userId, req)` — 创建，如果 `req.assignedTo` 不为空则同时设置 `assignedBy`
- `update(id, userId, req)` — 只能更新自己的待办（user_id == userId）
- `delete(id, userId)` — 只能删除自己的
- `acknowledge(id, userId, message)` — 仅 `assigned_to == userId` 可操作，更新 `ackStatus=confirmed, ackMessage=message`
- `getUpcoming(userId)` — 调用 mapper 查最近 3 条未完成

TodoController.java：
- `@RequiredArgsConstructor`，遵循 sharedrecord 的模式
- GET `/todos` → `listMyTodos`
- GET `/todos/upcoming` → `getUpcoming`
- GET `/todos/{id}` → `getById`
- POST `/todos` → `create`
- PATCH `/todos/{id}` → `update`
- DELETE `/todos/{id}` → `delete`
- POST `/todos/{id}/acknowledge` → `acknowledge`

- [ ] **Step 1: 创建 TodoCreateReq.java**（title @NotBlank, description, priority @NotBlank, dueDate, assignedTo）
- [ ] **Step 2: 创建 TodoUpdateReq.java**（所有字段 optional）
- [ ] **Step 3: 创建 TodoResp.java**（@Builder, from(DO)）
- [ ] **Step 4: 创建 TodoService.java**
- [ ] **Step 5: 创建 TodoController.java**
- [ ] **Step 6: 编译验证无错误**
- [ ] **Step 7: Commit**

---

### Task 4: 后端 — AckTemplate Entity + Mapper + Service + Controller

**文件：**
- Create: `backend/lifeassistant/lifeassistant-system/src/main/java/top/lifeassistant/todo/model/entity/TodoAckTemplateDO.java`
- Create: `backend/lifeassistant/lifeassistant-system/src/main/java/top/lifeassistant/todo/mapper/TodoAckTemplateMapper.java`
- Create: `backend/lifeassistant/lifeassistant-system/src/main/java/top/lifeassistant/todo/service/TodoAckTemplateService.java`
- Create: `backend/lifeassistant/lifeassistant-system/src/main/java/top/lifeassistant/todo/controller/TodoAckTemplateController.java`
- Create: `backend/lifeassistant/lifeassistant-system/src/main/java/top/lifeassistant/todo/model/req/TodoAckTemplateCreateReq.java`
- Create: `backend/lifeassistant/lifeassistant-system/src/main/java/top/lifeassistant/todo/model/req/TodoAckTemplateUpdateReq.java`
- Create: `backend/lifeassistant/lifeassistant-system/src/main/java/top/lifeassistant/todo/model/req/TodoAckTemplateReorderReq.java`
- Create: `backend/lifeassistant/lifeassistant-system/src/main/java/top/lifeassistant/todo/model/resp/TodoAckTemplateResp.java`

TodoAckTemplateService.java：
- `listByUser(userId)` — 查询用户模板，按 sort_order 升序。如果无数据，自动创建 3 条预设（收到、朕知道了、臣遵旨）后返回
- `create(userId, content)` — 新增，检查不超过 5 条
- `update(id, userId, content)` — 编辑
- `delete(id, userId)` — 删除，如果全部删光则标记"下次可重新初始化"
- `reorder(userId, ids)` — 按 ids 顺序更新 sort_order

- [ ] **Step 1: 创建 TodoAckTemplateDO.java**
- [ ] **Step 2: 创建 TodoAckTemplateMapper.java**
- [ ] **Step 3: 创建三个 Req 文件**
- [ ] **Step 4: 创建 TodoAckTemplateResp.java**
- [ ] **Step 5: 创建 TodoAckTemplateService.java**
- [ ] **Step 6: 创建 TodoAckTemplateController.java**
- [ ] **Step 7: 编译验证无错误**
- [ ] **Step 8: Commit**

---

### Task 5: 前端 — 更新 API 层

**文件：**
- Modify: `front/vue3-vant-mobile/src/api/modules/todos.ts`

现有 API 定义已基本完整，需要：
1. `TodoItem` 接口增加 `ack_status` 和 `ack_message` 字段
2. 新增 `acknowledgeTodo(id, message)` 函数
3. 新增 `fetchUpcomingTodos()` 函数
4. 新建 `front/vue3-vant-mobile/src/api/modules/ack-templates.ts` 文件

```typescript
// ack-templates.ts
import request from '@/utils/request'
import type { ApiResponse } from '@/types/api'

export interface AckTemplate {
  id: string
  content: string
  sort_order: number
}

export function fetchTemplates() {
  return request.get<ApiResponse<AckTemplate[]>>('/ack-templates')
}

export function createTemplate(content: string) {
  return request.post<ApiResponse<AckTemplate>>('/ack-templates', { content })
}

export function updateTemplate(id: string, content: string) {
  return request.put<ApiResponse<AckTemplate>>(`/ack-templates/${id}`, { content })
}

export function deleteTemplate(id: string) {
  return request.delete<ApiResponse<void>>(`/ack-templates/${id}`)
}

export function reorderTemplates(ids: string[]) {
  return request.put<ApiResponse<void>>('/ack-templates/reorder', { ids })
}
```

- [ ] **Step 1: 更新 TodoItem 接口和 todos.ts**
- [ ] **Step 2: 创建 ack-templates.ts**
- [ ] **Step 3: Commit**

---

### Task 6: 前端 — TabBar 新增「待办」Tab

**文件：**
- Modify: `front/vue3-vant-mobile/src/components/TabBar.vue`

在现有的 3 个 Tab 中插入「待办」Tab，图标用 Vant 的 `todo-list-o` 或 `pending-orders` 图标。需要同时更新 `config/routes.ts` 中的 `rootRouteList`（如有影响）。

- [ ] **Step 1: 修改 TabBar.vue 新增待办 Tab**
- [ ] **Step 2: 验证导航正确**
- [ ] **Step 3: 更新国际化文件（zh-CN.json, en-US.json）中的 Tab 名称**
- [ ] **Step 4: Commit**

---

### Task 7: 前端 — 待办列表页面

**文件：**
- Rewrite: `front/vue3-vant-mobile/src/pages/todos/index.vue`（当前是 ENABLED=false 占位页）

页面结构（参考 share/index.vue 的 CRUD 模式）：
1. 顶部 NavBar 标题"待办"
2. 筛选栏：全部 / 进行中 / 已完成（van-tabs 或 van-segmented）+ 日期选择（van-calendar 或 van-date-picker）
3. 列表使用 van-checkbox 或自定义圆圈做完成切换
4. 每条展示：完成勾选框、标题、优先级标签、截止日期、确认状态（对方指派时）
5. 底部浮动按钮（van-floating-button 或 van-button fixed）打开添加对话框
6. 点击待办项进入详情页
7. 右侧滑动删除

使用 Pinia store 或直接在组件内管理状态。参考 share/index.vue 的 `async onLoad` 下拉加载模式。

新建 `front/vue3-vant-mobile/src/stores/modules/todo.ts` Pinia store：
- state: todos, loading, filter 等
- actions: fetchTodos, create, update, delete, toggleComplete, acknowledge

- [ ] **Step 1: 创建 todo Pinia store**
- [ ] **Step 2: 重写 todos/index.vue 列表页**
- [ ] **Step 3: 验证列表、筛选、完成切换、删除功能**
- [ ] **Step 4: Commit**

---

### Task 8: 前端 — 今日首页展示最近 3 条待办

**文件：**
- Modify: `front/vue3-vant-mobile/src/pages/index.vue`

在问候语下方增加「待办」区域：
- 加载 `fetchUpcomingTodos()` 获取数据
- 最多展示 3 条，折叠样式
- 超过 3 条显示「查看全部 →」跳转到 `/todos`
- 点击某条跳转到待办详情
- 无待办时隐藏或显示「暂无待办」空状态

- [ ] **Step 1: 在首页添加待办区域**
- [ ] **Step 2: 验证显示和跳转**
- [ ] **Step 3: Commit**

---

### Task 9: 前端 — 添加/编辑待办表单

**文件：**
- Create: `front/vue3-vant-mobile/src/pages/todos/form.vue`（或作为弹窗/底部弹出层）

使用 Vant Form + Field + Picker 组件：
- 标题（van-field，必填）
- 描述（van-field textarea，可选）
- 优先级（van-radio-group：低/中/高/紧急 或 van-picker）
- 交给 TA（van-switch）
- 截止日期（van-calendar 或 van-field + van-date-picker，交给 TA 开启后必填）
- 保存按钮

如果是独立页面，通过路由 query 或 params 传编辑模式和数据。如果是弹窗，使用 van-action-sheet 或 van-dialog。

- [ ] **Step 1: 创建添加/编辑表单页面或组件**
- [ ] **Step 2: 集成到待办列表的添加浮动按钮和点击编辑**
- [ ] **Step 3: 验证创建和更新流程**
- [ ] **Step 4: Commit**

---

### Task 10: 前端 — 待办详情 + 确认收到

**文件：**
- Create: `front/vue3-vant-mobile/src/pages/todos/detail.vue`

详情展示：
- 标题、描述、优先级、截止日期
- 如果是对方指派：显示"来自 TA" + 确认状态
- 如果是自己指派给对方的：显示"已指派给 TA" + 对方确认状态
- 操作按钮：编辑（自己的）、删除（自己的）
- 如果是对方指派且未确认：底部「确认收到」按钮 → 调用 `acknowledgeTodo(id, message)`
- 如果是已确认：显示确认文案

- [ ] **Step 1: 创建详情页**
- [ ] **Step 2: 实现确认收到按钮逻辑（自动取模板第一条）**
- [ ] **Step 3: 验证详情和确认流程**
- [ ] **Step 4: Commit**

---

### Task 11: 前端 — 设置页模板管理

**文件：**
- Modify: `front/vue3-vant-mobile/src/pages/settings/index.vue`（增加入口）
- Create: `front/vue3-vant-mobile/src/pages/settings/ack-templates.vue`

settings/index.vue：
- 在退出登录上方增加「确认回复模板」cell 导航

ack-templates.vue：
- 列表展示所有模板，右侧编辑/删除按钮
- 支持拖动排序（van-swipe-cell 或 sortablejs）
- 底部「+ 添加」按钮，最多 5 条
- 编辑弹出 van-dialog 修改文案
- 删除确认弹窗
- 用户无模板时自动从服务端获取预设

- [ ] **Step 1: 修改 settings/index.vue 增加入口**
- [ ] **Step 2: 创建 settings/ack-templates.vue**
- [ ] **Step 3: 验证 CRUD 和排序**
- [ ] **Step 4: Commit**

---

### Task 12: 国际化更新

**文件：**
- Modify: `front/vue3-vant-mobile/src/locales/zh-CN.json`
- Modify: `front/vue3-vant-mobile/src/locales/en-US.json`

确认现有翻译覆盖情况，补充缺失的 key：
- Tab 名称「待办」
- 确认相关文案：「确认收到」「待确认」「已确认」
- 模板管理：「确认回复模板」「添加模板」「编辑模板」
- 筛选：「全部」「进行中」「已完成」

- [ ] **Step 1: 补充中文翻译**
- [ ] **Step 2: 补充英文翻译**
- [ ] **Step 3: Commit**

---

### Task 13: 端到端验证

- [ ] **Step 1: 重启后端 + 前端开发服务器**
- [ ] **Step 2: 验证完整流程：注册两个用户 → 绑定伴侣 → 创建个人待办 → 指派待办给对方 → 对方确认 → 查看首页最近 3 条 → 编辑模板**
- [ ] **Step 3: 检查边界情况：删光模板后重新获取、最多 5 条限制、筛选切换**
