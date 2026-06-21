# java-backend-shared-record

## Purpose

定义"一起做过的事"的端点契约——伴侣绑定后的共享记录本，双方均可添加和查看，两人看到完全一致的列表。

## ADDED Requirements

### Requirement: 添加共享记录

系统 SHALL 允许已绑定伴侣的用户创建共享记录。

#### Scenario: 成功添加

- **WHEN** 用户提交 `POST /api/v1/shared-records` 且 `{"title":"一起去看了电影","content":"《流浪地球3》，IMAX","occurred_at":"2026-06-15T19:00:00Z"}`
- **THEN** 返回 200，`created_by` 为当前用户

#### Scenario: 未绑定伴侣时添加

- **WHEN** 未绑定伴侣的用户尝试添加
- **THEN** 返回 400，提示需要先绑定伴侣

### Requirement: 查看共享记录列表

系统 SHALL 返回伴侣双方的全部共享记录，两人看到完全相同的列表。

#### Scenario: 查看列表

- **WHEN** 已绑定伴侣的用户请求 `GET /api/v1/shared-records`
- **THEN** 返回两人创建的所有记录，按 `occurred_at` 降序排列

#### Scenario: 按时间范围过滤

- **WHEN** 用户请求 `GET /api/v1/shared-records?start=2026-06-01&end=2026-06-30`
- **THEN** 仅返回 `occurred_at` 在范围内的两人记录

#### Scenario: 未绑定伴侣时查看

- **WHEN** 未绑定伴侣的用户请求
- **THEN** 返回 400

### Requirement: 查看单条详情

系统 SHALL 允许查看共享记录详情（双方均可查看任意记录）。

#### Scenario: 查看任意记录

- **WHEN** 已绑定用户请求 `GET /api/v1/shared-records/{id}` 且该记录属于自己或伴侣
- **THEN** 返回记录详情

#### Scenario: 查看不属于两人的记录

- **WHEN** 请求的 id 不属于自己或伴侣
- **THEN** 返回 404

### Requirement: 更新自己的记录

系统 SHALL 仅允许创建者修改自己的共享记录。

#### Scenario: 修改自己的记录

- **WHEN** 创建者请求 `PATCH /api/v1/shared-records/{id}`
- **THEN** 记录更新成功

#### Scenario: 修改伴侣的记录

- **WHEN** 用户尝试修改伴侣创建的记录
- **THEN** 返回 403

### Requirement: 删除自己的记录

系统 SHALL 仅允许创建者删除自己的共享记录。

#### Scenario: 删除自己的记录

- **WHEN** 创建者请求 `DELETE /api/v1/shared-records/{id}`
- **THEN** 记录被删除

#### Scenario: 删除伴侣的记录

- **WHEN** 用户尝试删除伴侣创建的记录
- **THEN** 返回 403
