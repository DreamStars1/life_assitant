## Purpose

定义系统 API 统一响应信封 `ApiResponse<T>`，统一成功/失败/校验三种场景的响应结构，消除前端多套解析逻辑。

## Requirements

### Requirement: 统一成功响应格式

所有 Controller 成功响应 SHALL 使用 `ApiResponse<T>` 包装，返回 `code=200`、`message` 和 `data` 三个字段。

#### Scenario: 返回领域对象

- **WHEN** Controller 返回单个业务对象（如 `UserPublicResp`）
- **THEN** 响应体为 `{ code: 200, message: "ok", data: { ... } }`

#### Scenario: 返回列表

- **WHEN** Controller 返回对象列表（如 `List<SharedRecordResp>`）
- **THEN** 响应体为 `{ code: 200, message: "ok", data: [ ... ] }`

#### Scenario: 返回简单消息无数据

- **WHEN** Controller 仅返回确认消息（如 "删除成功"）
- **THEN** 响应体为 `{ code: 200, message: "删除成功" }`，`data` 字段 JSON 序列化时省略（null 不序列化）

#### Scenario: 返回空数据

- **WHEN** Controller 方法返回 void（如删除操作）
- **THEN** 响应体为 `{ code: 200, message: "ok" }`

### Requirement: 统一错误响应格式

所有 `GlobalExceptionHandler` 捕获的异常 SHALL 返回 `ApiResponse<Void>`，包含 `code`（HTTP 状态码）、`message`（人类可读描述）、可选的 `errors`（校验错误列表）和 `traceId`（5xx 时必填）。

#### Scenario: 客户端错误（4xx）

- **WHEN** 发生 4xx 异常（如参数缺失 400、未授权 401、未找到 404）
- **THEN** 响应体为 `{ code: 4xx, message: "具体错误描述", errors: null, traceId: null }`

#### Scenario: 校验错误返回全部字段

- **WHEN** `@Valid` 校验失败产生多个字段错误
- **THEN** 响应体 `errors` 数组包含所有字段错误 `[{ field: "email", message: "格式不正确" }, { field: "password", message: "长度至少6位" }]`

#### Scenario: 服务端错误（5xx）带 traceId

- **WHEN** 发生未预期的 5xx 异常
- **THEN** 响应体 `traceId` 字段返回 UUID，与日志 MDC 中 traceId 一致，供前端展示和用户反馈使用

### Requirement: ApiResponse 结构定义

`ApiResponse<T>` SHALL 定义以下字段：

| 字段 | 类型 | 说明 |
|------|------|------|
| `code` | `int` | HTTP 状态码 |
| `message` | `String` | 人类可读描述 |
| `data` | `T` | 业务数据，`@JsonInclude(NON_NULL)` |
| `errors` | `List<FieldError>` | 校验错误，`@JsonInclude(NON_NULL)` |
| `traceId` | `String` | 排查用 ID，`@JsonInclude(NON_NULL)` |

`FieldError` 内部类 SHALL 包含 `field`（字段名）和 `message`（错误描述）。

#### Scenario: JSON 序列化忽略 null 字段

- **WHEN** `ApiResponse.error(400, "参数缺失")` 不含 data/errors/traceId
- **THEN** 序列化后 JSON 仅含 `{ code: 400, message: "参数缺失" }`
