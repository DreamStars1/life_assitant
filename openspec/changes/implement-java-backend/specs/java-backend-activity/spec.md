# java-backend-activity

## Purpose

定义时间块活动（Activity）的端点契约——记录"谁在什么时间段做了什么"，在日历/时间轴上展示，伴侣全量可见。

## ADDED Requirements

### Requirement: Activity CRUD

系统 SHALL 提供 Activity 的创建、列表、详情、更新、删除端点。Activity 表示一个时间段内的行为，`start_time` 必填，`end_time` 可选。

#### Scenario: 创建带结束时间的活动

- **WHEN** 用户提交 `POST /api/v1/activities` 且 `{"title":"晨跑","start_time":"2026-06-20T06:30:00Z","end_time":"2026-06-20T07:15:00Z","category":"运动","color":"#4CAF50"}`
- **THEN** 返回 200，`user_id` 为当前用户

#### Scenario: 创建时间点活动（无 end_time）

- **WHEN** 用户提交 `POST /api/v1/activities` 且 `{"title":"吃药","start_time":"2026-06-20T12:00:00Z"}`
- **THEN** 返回 200，`end_time` 为 null

### Requirement: 按时间范围查询

系统 SHALL 提供 `GET /api/v1/activities`，支持时间范围和分类过滤，用于日历/时间轴展示。

#### Scenario: 查询某周的活动

- **WHEN** 用户请求 `GET /api/v1/activities?start=2026-06-15T00:00:00Z&end=2026-06-22T00:00:00Z`
- **THEN** 返回 `start_time` 在该范围内的所有 Activity，按 `start_time` 升序

#### Scenario: 按分类过滤

- **WHEN** 用户请求 `GET /api/v1/activities?category=运动`
- **THEN** 仅返回分类为"运动"的活动

#### Scenario: 组合过滤

- **WHEN** 用户同时传 `start&end&category`
- **THEN** 同时满足时间和分类条件

### Requirement: 用户隔离

Activity 列表 SHALL 仅返回当前用户自己的数据（伴侣查询通过单独端点）。

#### Scenario: 查询自己的活动

- **WHEN** 用户请求 `GET /api/v1/activities`
- **THEN** 系统注入 `user_id` 过滤条件，不返回其他用户的 Activity

### Requirement: 伴侣全量可见

绑定伴侣后，用户 SHALL 可以通过 `GET /api/v1/partner/activities` 查看伴侣的全部活动。

#### Scenario: 查看伴侣活动

- **WHEN** 有伴侣的用户请求 `GET /api/v1/partner/activities?start=...&end=...`
- **THEN** 返回伴侣在时间范围内的全部 Activity

#### Scenario: 未绑定伴侣时查询

- **WHEN** 未绑定伴侣的用户请求 `GET /api/v1/partner/activities`
- **THEN** 返回 400
