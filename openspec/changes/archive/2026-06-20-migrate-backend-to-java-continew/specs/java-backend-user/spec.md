# java-backend-user

## Purpose

定义用户管理、伴侣绑定的端点契约和数据模型。包括注册、个人信息管理、超管用户管理等端点规范。

## ADDED Requirements

### Requirement: 用户注册

系统 SHALL 提供 `POST /api/v1/users/signup` 端点，接受 `{email, password, full_name}`，返回 `UserPublic`。

#### Scenario: 成功注册

- **WHEN** 新邮箱注册
- **THEN** 系统创建用户，密码使用 bcrypt 编码，返回用户公开信息

#### Scenario: 重复邮箱

- **WHEN** 使用已注册邮箱注册
- **THEN** 返回 400，`{"detail": "The user with this email already exists in the system"}`

### Requirement: 获取当前用户

系统 SHALL 提供 `GET /api/v1/users/me` 端点，返回当前认证用户的公开信息。

#### Scenario: 获取自己的信息

- **WHEN** 已认证用户请求自己的信息
- **THEN** 返回 `{id, email, is_active, is_superuser, full_name, partner_id, ...}`

### Requirement: 更新当前用户信息

系统 SHALL 提供 `PATCH /api/v1/users/me` 端点，允许用户更新 `full_name` 和 `email`。

#### Scenario: 更新名称

- **WHEN** 用户提交 `{"full_name": "新名称"}`
- **THEN** 用户名称更新成功

#### Scenario: 邮箱冲突

- **WHEN** 用户要更新的邮箱已被他人使用
- **THEN** 返回 409，`{"detail": "User with this email already exists"}`

### Requirement: 修改密码

系统 SHALL 提供 `PATCH /api/v1/users/me/password` 端点，验证当前密码后更新为新密码。

#### Scenario: 正确当前密码

- **WHEN** 提交 `{"current_password": "正确", "new_password": "新密码"}`
- **THEN** 密码更新成功

#### Scenario: 错误当前密码

- **WHEN** 当前密码错误
- **THEN** 返回 400，`{"detail": "Incorrect password"}`

### Requirement: 删除当前用户

系统 SHALL 提供 `DELETE /api/v1/users/me` 端点，允许用户删除自己的账号（超级管理员不允许删除自己）。

#### Scenario: 普通用户删除自己

- **WHEN** 普通用户请求删除
- **THEN** 账号被删除，关联的 Items 被级联删除

#### Scenario: 超级管理员删除自己

- **WHEN** 超级管理员请求删除自己
- **THEN** 返回 403

### Requirement: 超级管理员用户管理

系统 SHALL 提供仅超级管理员可访问的用户管理端点：列表、创建、查看、更新、删除。

#### Scenario: 超级管理员列出用户

- **WHEN** 超级管理员请求 `GET /api/v1/users/?skip=0&limit=100`
- **THEN** 返回分页的用户列表

#### Scenario: 非管理员访问被拒绝

- **WHEN** 普通用户请求 `GET /api/v1/users/`
- **THEN** 返回 403

### Requirement: 伴侣邀请与绑定

系统 SHALL 提供伴侣绑定功能：生成邀请 Token → 对方接受绑定 → 建立双向 `partner_id` 关系。

#### Scenario: 生成邀请码

- **WHEN** 用户请求 `POST /api/v1/identity/invite`
- **THEN** 返回 `{"invite_token": "..."}` （24 小时有效 JWT）

#### Scenario: 通过邀请码绑定

- **WHEN** 另一方请求 `POST /api/v1/identity/bind-partner` 提交有效邀请码
- **THEN** 双方 `partner_id` 互指，返回更新后的 UserPublic
