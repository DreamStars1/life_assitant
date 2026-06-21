# java-backend-monitor

## Purpose

定义管理后台（superuser 准入）的监控端点契约：系统健康、概览统计、推送统计、用户列表。

## ADDED Requirements

### Requirement: 管理员权限控制

所有 `/api/v1/admin/*` 端点 SHALL 仅允许 `is_superuser=true` 的用户访问。

#### Scenario: 超级管理员访问

- **WHEN** 超级管理员请求 admin 端点
- **THEN** 正常返回数据

#### Scenario: 非管理员访问被拒绝

- **WHEN** 普通用户请求 admin 端点
- **THEN** 返回 403

### Requirement: 系统健康检查

系统 SHALL 提供 `GET /api/v1/admin/health` 端点，返回各组件健康状态。

#### Scenario: 查询系统健康

- **WHEN** 管理员请求 health
- **THEN** 返回数据库连接、Redis 连接等组件状态

### Requirement: 概览统计

系统 SHALL 提供 `GET /api/v1/admin/stats/overview` 端点，返回用户总数、活跃用户数、今日事件数等。

#### Scenario: 查询概览

- **WHEN** 管理员请求 overview 统计
- **THEN** 返回 `{total_users, active_users, today_events, total_lifelogs, ...}`

### Requirement: 推送统计

系统 SHALL 提供 `GET /api/v1/admin/stats/push` 端点，返回推送订阅数和发送成功率。

#### Scenario: 推送统计查询

- **WHEN** 管理员请求推送统计
- **THEN** 返回 `{total_subscriptions, active_subscriptions, push_success_rate, ...}`

### Requirement: 用户列表

系统 SHALL 提供 `GET /api/v1/admin/users` 端点，返回所有用户（含伴侣绑定状态）。

#### Scenario: 管理员查看用户列表

- **WHEN** 管理员请求 admin users
- **THEN** 返回 `[{id, username, full_name, is_active, has_partner, created_at}, ...]`
