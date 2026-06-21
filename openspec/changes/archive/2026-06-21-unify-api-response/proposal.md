## Why

当前 API 响应格式不统一：正常响应 4 种形状（`LoginResp`、`UserPublicResp`、`List<T>`、`Map<String, String>`、`void`），异常响应又是另一种（`Map.of("detail", ...)`），前端需要 5+ 套解析逻辑。同时 `GlobalExceptionHandler` 存在 3 个确认的 bug（BusinessException 误返回 500、MultipartException 逻辑反转、parseLong 无保护），缺少 2 个必要的 handler（ConstraintViolationException、NotLoginException 场景区分），且所有异常无论 4xx/5xx 都用 `log.error` 污染告警。修复 bugs 并统一格式，一次性解决后端 bug + 前端对接成本。

## What Changes

- **BREAKING**: 所有 Controller 返回类型从裸 DTO/Map/void 改为 `ApiResponse<T>` 包装
- **BREAKING**: 异常响应从 `Map.of("detail", ...)` 改为 `ApiResponse<Void>`（含 `code`/`message`/`errors`/`traceId`）
- **BREAKING**: 前端需同步更新所有 API 调用的响应解析逻辑
- 新增 `ApiResponse<T>` 统一响应类（含 `FieldError` 子类）
- 修复 `handleBusinessException` 返回 500 → 400，4xx handler 改用 `log.warn`
- 修复 `handleMultipartException` 逻辑反转（`isBlank` → `isNotBlank`）、parseLong 加 try-catch
- 修复 `handleBindException` 从 `findFirst()` 改为返回全部校验错误
- 新增 `ConstraintViolationException` handler
- 新增 `NotLoginException` 按 `getType()` 区分场景
- 兜底 handler 返回 `traceId`（通过 MDC）

## Capabilities

### New Capabilities

- `api-response-format`: 统一 API 响应结构 `ApiResponse<T>`，适配成功/失败/校验三种场景
- `global-exception-handler`: 修复 bugs、补缺失 handler、日志分级

### Modified Capabilities

<!-- 无现有 specs，无需修改 -->

## Impact

- **后端**: `GlobalExceptionHandler.java`（重写）、新增 `ApiResponse.java`、4 个 Controller（`AuthController`、`UserController`、`IdentityController`、`SharedRecordController`）共 20 个方法改返回类型
- **前端**: 所有 API 调用处需从裸数据/`detail` 字段改为 `ApiResponse` 统一结构（`code`/`message`/`data`/`errors`）
- **依赖**: 无新增外部依赖（基于现有 `lombok`、`jackson`、`spring-web`）
