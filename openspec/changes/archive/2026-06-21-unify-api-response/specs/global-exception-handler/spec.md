## ADDED Requirements

### Requirement: BusinessException 返回 400

`handleBusinessException` SHALL 返回 `HTTP 400 Bad Request`，MUST NOT 返回 500。

#### Scenario: 业务校验失败

- **WHEN** Service 层抛出 `new BusinessException("订单不存在")`
- **THEN** HTTP 状态码为 400，响应 `message` 为 "订单不存在"

### Requirement: MultipartException 逻辑修复

`handleMultipartException` SHALL 在消息空白时返回固定兜底提示，MUST NOT 返回空 detail。

#### Scenario: 异常消息为空

- **WHEN** `MultipartException.getMessage()` 返回 null 或空白
- **THEN** 返回 `message: "文件上传失败"`

#### Scenario: parseLong 解析失败

- **WHEN** 从异常消息提取的 sizeLimit 字符串无法解析为 Long（如含非数字字符）
- **THEN** 返回 `message: "文件大小超出限制"`，不抛 `NumberFormatException`

### Requirement: BindException 返回全部校验错误

`handleBindException` SHALL 返回所有字段校验错误，MUST NOT 仅返回 `findFirst()`。

#### Scenario: 多个字段校验失败

- **WHEN** 请求体有 3 个字段校验失败（email 格式错、password 太短、name 为 null）
- **THEN** 响应 `errors` 数组包含 3 个 `FieldError` 对象

### Requirement: 新增 ConstraintViolationException 处理

系统 SHALL 处理 `ConstraintViolationException`（`@Validated` 在方法参数上产生的校验异常），返回 400 和具体错误信息。

#### Scenario: 路径参数校验失败

- **WHEN** 用户在路径参数中传入非法值触发 `ConstraintViolationException`
- **THEN** 返回 `code: 400, message: "参数校验失败"`，不在兜底 handler 中丢失信息

### Requirement: NotLoginException 按类型区分

`handleNotLogin` SHALL 根据 `e.getType()` 区分返回不同 message：

| type | 场景 | message |
|------|------|---------|
| -1 | 未登录 | "未登录" |
| -2 | Token 过期 | "登录已过期，请重新登录" |
| -3 | 被踢下线 | "账号已被管理员踢下线" |
| -4 | 被顶下线 | "账号已在其他设备登录" |
| other | 未知 | "未认证" |

#### Scenario: Token 过期

- **WHEN** Sa-Token 检测到 Token 已过期
- **THEN** 返回 `code: 401, message: "登录已过期，请重新登录"`

#### Scenario: 被顶下线

- **WHEN** 用户在另一设备登录导致当前 Token 失效
- **THEN** 返回 `code: 401, message: "账号已在其他设备登录"`

### Requirement: 日志按 HTTP 状态分级

4xx 异常 SHALL 使用 `log.warn`，5xx 异常 SHALL 使用 `log.error`。

#### Scenario: 客户端错误日志级别

- **WHEN** 发生 400 Bad Request 异常
- **THEN** 日志级别为 WARN

#### Scenario: 服务端错误日志级别

- **WHEN** 发生 500 Internal Server Error
- **THEN** 日志级别为 ERROR

### Requirement: 5xx 响应包含 traceId

兜底异常处理器 `handleAll` SHALL 在 5xx 响应中返回 `traceId`，traceId 通过 `MDC.put("traceId", UUID)` 注入。

#### Scenario: 未知异常带 traceId

- **WHEN** 发生未捕获的 Exception
- **THEN** 响应包含 `traceId` 字段，且与后端日志中 MDC traceId 一致
