# java-backend-auth

## Purpose

定义 JWT 认证体系的端点契约：登录、Token 刷新、登出、密码哈希和密码重置的请求/响应规范。

## ADDED Requirements

### Requirement: Token 登录

系统 SHALL 提供 `POST /api/v1/login/access-token` 端点，接受 `application/x-www-form-urlencoded` 格式的 `username`（邮箱）和 `password`，返回 `{"access_token": "...", "token_type": "bearer"}`。

#### Scenario: 正确凭据登录

- **WHEN** 客户端发送 `POST /api/v1/login/access-token` 且 `username` 和 `password` 正确
- **THEN** 返回 200，`access_token` 为有效的 HS256 JWT，`sub` 为 UUID 字符串

#### Scenario: 错误凭据

- **WHEN** 客户端发送错误密码
- **THEN** 返回 400，`{"detail": "Incorrect email or password"}`

#### Scenario: 账号未激活

- **WHEN** 客户端发送已停用账号的凭据
- **THEN** 返回 400，`{"detail": "Inactive user"}`

### Requirement: Token 验证

系统 SHALL 对每个需要认证的请求验证 JWT Token：解码 Token → 提取 `sub` → 查找用户 → 检查激活状态。

#### Scenario: 有效 Token 访问

- **WHEN** 请求携带有效的 `Authorization: Bearer <token>` 头
- **THEN** 系统注入 `CurrentUser` 对象到 Controller 方法

#### Scenario: 缺失 Token

- **WHEN** 请求未携带 Authorization 头
- **THEN** 返回 401，`{"detail": "Not authenticated"}`

#### Scenario: 过期 Token

- **WHEN** Token 已过期
- **THEN** 返回 401，`{"detail": "Could not validate credentials"}`

#### Scenario: 用户不存在

- **WHEN** Token 有效但 sub 对应的用户不存在
- **THEN** 返回 404，`{"detail": "User not found"}`

### Requirement: 密码哈希

系统 SHALL 使用 BCrypt 对用户密码进行哈希存储。

#### Scenario: 创建新用户

- **WHEN** 创建新用户或重置密码
- **THEN** 存储的 `hashed_password` 使用 BCrypt 格式（`$2a$...`）

#### Scenario: 密码验证

- **WHEN** 用户登录
- **THEN** 系统使用 BCrypt 验证输入的明文密码与存储的哈希是否匹配

### Requirement: 管理后台认证

系统 SHALL 提供 `POST /api/v1/auth/login` 端点，接受 JSON `{"username": "...", "password": "..."}`，返回 `{"access_token": "...", "token_type": "bearer", "refresh_token": "...", "expires_in": 7200}`。

#### Scenario: 管理员 JSON 登录成功

- **WHEN** 客户端发送 `POST /api/v1/auth/login` 且凭据正确
- **THEN** 返回 200，含 access_token 和 refresh_token

#### Scenario: 管理员登录失败

- **WHEN** 凭据错误
- **THEN** 返回 400，`{"detail": "Incorrect email or password"}`

### Requirement: Token 刷新

系统 SHALL 提供 `POST /api/v1/auth/refresh-token` 端点，接受有效 refresh token 并返回新的 token pair。

#### Scenario: 成功刷新

- **WHEN** 客户端提交有效 refresh token
- **THEN** 返回 200 含新 access_token 和 refresh_token

#### Scenario: 过期或无效 refresh token

- **WHEN** refresh token 已过期或无效
- **THEN** 返回 401

### Requirement: 登出

系统 SHALL 提供 `DELETE /api/v1/auth/logout` 端点。

#### Scenario: 成功登出

- **WHEN** 已认证用户请求登出
- **THEN** 返回 200，`{"message": "Logged out successfully"}`

### Requirement: 密码重置

系统 SHALL 支持通过邮箱重置密码：生成重置 Token → 发送邮件 → 验证 Token → 更新密码。

#### Scenario: 请求密码重置

- **WHEN** 客户端发送 `POST /api/v1/password-recovery/{email}`
- **THEN** 系统发送重置邮件，统一返回 `{"message": "If that email is registered, we sent a password recovery link"}`（防止邮箱枚举）

#### Scenario: 执行密码重置

- **WHEN** 客户端发送 `POST /api/v1/reset-password/` 且 `{"token": "...", "new_password": "..."}` 且 Token 有效
- **THEN** 密码更新成功
