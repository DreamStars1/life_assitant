# java-backend-todo

## Purpose

定义待办（Todo）的端点契约——点状任务，支持标记完成、伴侣分配和时间线整合展示。

## ADDED Requirements

### Requirement: 待办 CRUD

系统 SHALL 提供 Todo 的创建、列表、详情、更新、删除端点。

#### Scenario: 创建待办

- **WHEN** 用户提交 `POST /api/v1/todos` 且 `{"title": "买菜", "priority": "high"}`
- **THEN** 返回 200，`is_completed=false`，`user_id` 为当前用户

#### Scenario: 列表过滤

- **WHEN** 用户请求 `GET /api/v1/todos?is_completed=false&priority=high`
- **THEN** 返回当前用户符合条件的 Todo，不包含其他用户的数据

#### Scenario: 标记完成

- **WHEN** 用户请求 `PATCH /api/v1/todos/{id}` 且 `{"is_completed": true}`
- **THEN** Todo 的 `is_completed` 变为 true，`completed_at` 自动设为当前时间

#### Scenario: 取消完成

- **WHEN** 用户请求 `PATCH /api/v1/todos/{id}` 且 `{"is_completed": false}`
- **THEN** `completed_at` 置为 null

### Requirement: 待办分配

系统 SHALL 支持将待办分配给伴侣用户。

#### Scenario: 分配待办

- **WHEN** 用户请求 `POST /api/v1/todos/{todo_id}/assign` 且 `{"assigned_to_id": "<partner_id>"}`
- **THEN** Todo 的 `assigned_to` 和 `assigned_by` 更新，返回完整 Todo

#### Scenario: 未绑定伴侣时分配

- **WHEN** 未绑定伴侣的用户尝试分配
- **THEN** 返回 400

### Requirement: 伴侣待办查询

系统 SHALL 提供 `GET /api/v1/partner/todos`，查询已绑定伴侣创建的 Todo。

#### Scenario: 查看伴侣待办

- **WHEN** 有伴侣的用户请求 `GET /api/v1/partner/todos`
- **THEN** 返回伴侣的所有 Todo（全部可见，全有或全无共享）

#### Scenario: 未绑定伴侣时查询

- **WHEN** 未绑定伴侣的用户请求
- **THEN** 返回 400
