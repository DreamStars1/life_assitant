# java-backend-notification

## Purpose

定义 Web Push 订阅管理、推送发送、静默时段控制和偏好设置的端点契约。

## ADDED Requirements

### Requirement: 推送订阅管理

系统 SHALL 提供 Web Push 订阅的注册（`POST /api/v1/push/subscribe`）和取消（`DELETE /api/v1/push/subscription`）端点。

#### Scenario: 注册推送订阅

- **WHEN** 客户端提交 `{"endpoint": "https://...", "p256dh": "base64...", "auth": "base64..."}`
- **THEN** 系统创建或更新 PushSubscription 记录，`is_active=true`

#### Scenario: 重复订阅同一 endpoint

- **WHEN** 使用相同的 endpoint 再次注册
- **THEN** 系统更新已有的订阅记录，更新 p256dh 和 auth 密钥，`is_active=true`

#### Scenario: 取消订阅

- **WHEN** 客户端提交 `DELETE /api/v1/push/subscription` 且 `{"endpoint": "https://..."}`
- **THEN** 对应订阅标记为 `is_active=false`（软删除）

### Requirement: 推送发送

系统 SHALL 支持向指定用户的所有活跃订阅发送 VAPID Web Push 通知，包含静默时段检查。

#### Scenario: 发送推送通知

- **WHEN** 系统调用推送服务发送 `{"title": "提醒", "body": "会议还有15分钟"}`
- **THEN** 向用户所有 `is_active=true` 的订阅发送通知，返回成功数

#### Scenario: 静默时段内不推送

- **WHEN** 当前时间在用户的 `quiet_hours_start` ~ `quiet_hours_end` 范围内
- **THEN** 推送被跳过，返回 0

#### Scenario: 推送已关闭时跳过

- **WHEN** 用户的 `push_enabled=false`
- **THEN** 推送被跳过

#### Scenario: VAPID 密钥未配置

- **WHEN** `VAPID_PRIVATE_KEY` 为空
- **THEN** 推送被跳过，记录警告日志

#### Scenario: 订阅 410 Gone 处理

- **WHEN** Push Service 返回 410 Gone
- **THEN** 对应订阅标记为 `is_active=false`

### Requirement: 推送偏好设置

系统 SHALL 提供 `PUT /api/v1/push/preferences` 端点，允许用户设置推送开关和静默时段。

#### Scenario: 更新推送偏好

- **WHEN** 用户提交 `{"push_enabled": false, "quiet_hours_start": "22:00", "quiet_hours_end": "06:00"}`
- **THEN** User 表的对应字段更新

### Requirement: 测试推送

系统 SHALL 提供 `POST /api/v1/push/test` 端点，向当前用户发送测试推送。

#### Scenario: 成功发送测试推送

- **WHEN** 已订阅的用户请求测试推送 `{"title": "Test", "body": "Hello"}`
- **THEN** 返回 `{"sent": N}`，N 为实际发送的推送数

#### Scenario: 无活跃订阅时失败

- **WHEN** 用户无活跃订阅
- **THEN** 返回 400，`{"detail": "No active subscriptions or push not sent"}`
