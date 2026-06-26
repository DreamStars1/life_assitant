# 待办功能：个人待办 + 指派伴侣 + 确认回复

## 背景

当前项目已完成伴侣绑定和共享记录功能，todo 相关的前端 API 和后端数据库表已预先就绪但实际功能未实现。需要新增完整的待办功能，支持个人待办管理和伴侣间待办指派。

## 方案

### 数据库变更

#### todo 表 — 新增 2 个字段

```sql
ALTER TABLE todo
  ADD COLUMN ack_status  VARCHAR(16)  NOT NULL DEFAULT 'unconfirmed'
    COMMENT '确认状态：unconfirmed / confirmed';
ALTER TABLE todo
  ADD COLUMN ack_message VARCHAR(100) DEFAULT NULL
    COMMENT '确认回复文案';
```

#### 新建 todo_ack_template 表

```sql
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

模板表不含 `is_preset` 字段。用户首次请求模板时，服务端检测到该用户无记录，自动插入 3 条预设：**收到**、**朕知道了**、**臣遵旨**。预设和用户自定义在数据层面无区别，用户可以编辑、删除、新增。全部删光时下次获取重新初始化预设。每人最多保留 5 条。

### 后端 API

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/todos` | 待办列表，支持 `is_completed`、`priority`、`start_due_date`、`end_due_date` 筛选 |
| GET | `/todos/:id` | 待办详情 |
| POST | `/todos` | 创建待办。body 含 `title`、`description`、`priority`、`due_date`。前端开启"交给 TA"时传 `assigned_to` 为伴侣 ID |
| PATCH | `/todos/:id` | 更新待办，可修改标题/描述/优先级/截止日期 |
| DELETE | `/todos/:id` | 删除待办 |
| POST | `/todos/:id/acknowledge` | 确认收到，body: `{ message }`。仅指派的目标用户可操作 |
| GET | `/todos/upcoming` | 首页最近 3 条未完成的待办（按截止日期升序） |
| GET | `/ack-templates` | 获取当前用户模板列表。首次无数据时自动创建 3 条预设 |
| POST | `/ack-templates` | 添加模板，body: `{ content }`。超过 5 条时返回错误 |
| PUT | `/ack-templates/:id` | 编辑模板内容 |
| DELETE | `/ack-templates/:id` | 删除模板 |
| PUT | `/ack-templates/reorder` | 排序，body: `{ ids: string[] }` 按 order 更新 sort_order |

### 前端

#### 入口

- TabBar 底部导航新增「待办」Tab，TabBar 变为 4 个：今日 / 待办 / 伴侣 / 我的
- 首页（`/`）在问候语下方展示截止日期最近的 **3 条未完成待办**（个人 + 对方指派混合），超过 3 条显示"查看全部 →"跳转到待办页

#### 待办列表（`/todos`）

统一列表混排不拆分 Tab。列表顶部有筛选栏：

- 完成状态：全部 / 进行中 / 已完成
- 截止日期范围选择

每条待办展示：

- 左侧圆圈点击即标记完成/取消完成
- 标题、优先级、截止日期
- 对方指派来的额外显示：来自 TA / 确认状态（待确认/已确认+回复文案）

底部浮动按钮打开创建表单。

#### 添加待办

表单字段：标题 · 描述（可选）· 优先级 · 「交给 TA」开关 · 截止日期（开关开启后显示）

开启「交给 TA」时，创建请求自动将 `assigned_to` 设为当前用户的伴侣 ID。

#### 确认收到

对方在待办详情页点击「确认收到」按钮 → 取该用户模板列表第一条文案作为回复 → 调用 `POST /todos/:id/acknowledge` 更新 `ack_status=confirmed`、`ack_message=文案`。一步完成，无中间选择步骤。如需更换文案，去设置中编辑模板排序，下次确认即使用新第一条。

#### 设置 — 模板管理

设置页新增「确认回复模板」入口 → 进入后显示模板列表，支持：

- 编辑文案
- 拖动排序
- 新增（最多 5 条）
- 删除（全部删光时重新初始化 3 条预设）

### ack_status 在前后端的语义

| 值 | 场景 | 说明 |
|---|---|---|
| `none` | 个人待办 | 未指派给任何人，无需确认（默认值） |
| `unconfirmed` | 对方指派 | 已指派给伴侣，等待对方确认 |
| `confirmed` | 已确认 | 对方已确认收到 |

`ack_status` 为 NOT NULL，创建个人待办时默认 `none`，前端据此不展示确认 UI。指派给伴侣时设为 `unconfirmed`，对方调用 acknowledge 后变为 `confirmed`。
