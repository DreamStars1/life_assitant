## Context

当前项目使用 continew-starter 2.15.0（基于 Spring Boot 3），外部异常类 `BaseException`、`BusinessException`、`BadRequestException` 均不含 `httpStatus` 字段。Controller 返回类型不统一（DTO/List/Map/void 混用），异常响应格式与正常响应完全不同。

## Goals / Non-Goals

**Goals:**
- 后端统一所有 API 返回为 `ApiResponse<T>` 格式
- 修复 `GlobalExceptionHandler` 中 3 个确认 bug
- 补齐缺失的 handler（`ConstraintViolationException`、`NotLoginException` 场景区分）
- 日志按 4xx/5xx 分级
- 5xx 异常响应包含 `traceId` 便于排查
- 前端同步更新 API 调用层

**Non-Goals:**
- 不引入错误码枚举（YAGNI，业务规模未到）
- 不采用 RFC 7807 ProblemDetail（与外部异常类不兼容，引入复杂度）
- 不引入 Spring Boot `ErrorAttributes` / `ErrorController` 自定义（项目规模不需要）
- 不改动 Service 层

## Decisions

### 1. `ApiResponse<T>` 用 class + Lombok，不走 record

**选 class 而非 record**：需要 `@JsonInclude(NON_NULL)` 按字段粒度控制序列化，record 的 `canonical constructor` 不适合这种差异化处理。用 `@Data` 简化 getter/setter，`@NoArgsConstructor` + `@AllArgsConstructor` 保证 Jackson 反序列化兼容。

**位置**: `top.lifeassistant.common.base.model.resp.ApiResponse`，与现有的 `BaseResp`、`BaseDetailResp` 同包，语义清晰（都是响应模型）。

```java
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ApiResponse<T> {
    private int code;
    private String message;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private T data;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private List<FieldError> errors;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String traceId;

    // ---- 工厂方法 ----
    public static <T> ApiResponse<T> ok(T data) {
        return new ApiResponse<>(200, "ok", data, null, null);
    }

    public static <T> ApiResponse<T> ok(String message, T data) {
        return new ApiResponse<>(200, message, data, null, null);
    }

    public static ApiResponse<Void> ok() {
        return new ApiResponse<>(200, "ok", null, null, null);
    }

    public static <T> ApiResponse<T> error(int code, String message) {
        return new ApiResponse<>(code, message, null, null, null);
    }

    public static <T> ApiResponse<T> error(int code, String message, String traceId) {
        return new ApiResponse<>(code, message, null, null, traceId);
    }

    @Data
    @AllArgsConstructor
    public static class FieldError {
        private String field;
        private String message;
    }
}
```

**替代方案**: 用 `record` — 拒绝，因为 `@JsonInclude(NON_NULL)` 无法按字段区分。

### 2. GlobalExceptionHandler 改用 `ApiResponse` 返回

原先返回 `ResponseEntity<Map<String, String>>`，改为返回 `ApiResponse<Void>`，不再手动构造 `ResponseEntity`：

```java
// 原先
return ResponseEntity.status(400).body(Map.of("detail", "参数缺失"));

// 改后
return ResponseEntity.status(400).body(ApiResponse.error(400, "参数缺失"));
```

删除 `private static error()` 工具方法，直接在各 handler 中调用 `ApiResponse.error()` 工厂。

### 3. Controller 改返回值：静态方法包裹

从原本直接返回 DTO：

```java
@GetMapping("/me")
public UserPublicResp me(@CurrentUser UserDO user) { ... }
```

改为返回 `ApiResponse<UserPublicResp>`：

```java
@GetMapping("/me")
public ApiResponse<UserPublicResp> me(@CurrentUser UserDO user) {
    return ApiResponse.ok(userService.getCurrentUser());
}
```

`Map.of("message", "删除成功")` 改为 `ApiResponse.ok("删除成功")`。

`void` 返回改为 `ApiResponse<Void>` 并 `return ApiResponse.ok()`。

### 4. 异常路由表

| 异常 | 原状态码 | 新状态码 | 日志 |
|------|---------|---------|------|
| `BusinessException` | 500 | **400** | warn |
| `BaseException` | 500 | 500（不变） | error |
| `BadRequestException` | 400 | 400（不变） | warn |
| `MultipartException` | 400 | 400（不变） | warn |
| `BindException` / `MethodArgumentNotValidException` | 422 | 422（不变） | warn |
| `ConstraintViolationException` | 无处理→500 | **400** | warn |
| `MissingServletRequestParameterException` | 400 | 400（不变） | warn |
| `MethodArgumentTypeMismatchException` | 400 | 400（不变） | warn |
| `HttpMessageNotReadableException` | 400 | 400（不变） | warn |
| `NoHandlerFoundException` | 404 | 404（不变） | warn |
| `HttpRequestMethodNotSupportedException` | 405 | 405（不变） | warn |
| `NotLoginException` | 401 | 401（不变） | warn |
| `Exception` | 500 | 500（不变） | error |

### 5. MultipartException handler 简化

原先尝试解析 Spring 内部错误消息字符串（`subAfter`/`subBetween`/`parseLong`）以提取文件大小限制——这种耦合脆弱，Spring 版本升级后格式可能变。

**决策**: 直接返回固定提示 `"文件大小超出限制"`，文件大小校验改在 Controller 层 `@Valid` 后做。parseLong 用 try-catch 兜底。

### 6. NotLoginException 场景映射

使用 `switch (e.getType())` 根据 Sa-Token 常量区分：

```java
String msg = switch (e.getType()) {
    case NotLoginException.NOT_TOKEN         -> "未登录";
    case NotLoginException.INVALID_TOKEN     -> "登录已过期，请重新登录";
    case NotLoginException.TOKEN_TIMEOUT     -> "登录已过期，请重新登录";
    case NotLoginException.BE_REPLACED       -> "账号已在其他设备登录";
    case NotLoginException.KICK_OUT          -> "账号已被管理员踢下线";
    default                                  -> "未认证";
};
```

### 7. traceId 策略

不在 Filter 层注入（避免引入新组件），直接在 `handleAll` 兜底 handler 和 `handleBaseException` 中通过 MDC 生成：

```java
String traceId = UUID.randomUUID().toString().substring(0, 8);
MDC.put("traceId", traceId);
return ResponseEntity.status(500).body(ApiResponse.error(500, "服务器内部错误", traceId));
```

响应后 `MDC.clear()`。

### 8. 前端适配策略

**前端现状**: `src/utils/request.ts` 基于 axios，响应拦截器仅一行 `return response.data`（透传 axios 的 `response.data`，不做任何解包）。错误拦截器读取 `data.detail` 处理 401/403。没有 `ApiResponse<T>` 类型定义。

**两种调用模式并存**:
- **模式 A**（多数页面）：通过 `api/modules/*.ts` 封装，返回泛型如 `request.get<LifeLogItem[]>('/life-logs')`
- **模式 B**（share/profile/partner 页面）：页面直接 `import request` 裸调，如 `const res = await request.post('/identity/invite'); res.invite_token`

**选择策略 A：拦截器保持简单，调用方解包**

拦截器继续保持 `return response.data`，不做 code 检查。调用方拿到 `ApiResponse<T>` 后自己取 `.data`：

```typescript
// 改前
const user = await request.get<UserPublicResp>('/users/me')
user.id         // 直接用

// 改后
const res = await request.get<ApiResponse<UserPublicResp>>('/users/me')
res.data.id     // 通过 .data 取业务数据
res.message     // "ok"
res.code        // 200
```

**理由**: 调用方可以访问 `traceId` 和 `errors`（校验错误列表），不会被拦截器吞掉。且 4 个裸调页面改动清晰（`res.xxx` → `res.data.xxx`），不需要额外封装。

**具体改动**:
1. `src/utils/request.ts`：错误拦截器 `data.detail` → `data.message`；`RequestError` 类型新增 `traceId`、`errors` 字段
2. 新增 `src/types/api.ts`：定义 `ApiResponse<T>` 和 `FieldError` 接口
3. `api/modules/*.ts`（4 个文件）：泛型参数从 `LifeLogItem[]` 改为 `ApiResponse<LifeLogItem[]>`
4. 页面中直接调 `request` 的（4 个文件）：`res.xxx` → `res.data.xxx`
5. Store 层（`stores/modules/user.ts`）：`login()`/`info()` 等方法的返回值类型适配

## Risks / Trade-offs

| 风险 | 缓解 |
|------|------|
| Controller 改返回值是 BREAKING CHANGE，前端未同步会导致页面崩溃 | 前后端同一 PR/分支，一起部署 |
| `BusinessException` 从 500 改为 400 可能影响已有的前端错误处理逻辑 | 原 500 本身就是 bug，400 才是正确语义；前端若硬编码了 500 判断需要更新 |
| MultipartException 不再解析文件大小，用户体验略降 | 文件大小限制改为 Controller 层 `@Valid` 校验，提示更精准；parseLong 崩溃比信息缺失更差 |
| 前端 4 个页面裸调 request 改动碎片化 | 改动简单（`res.xxx` → `res.data.xxx`），逐一修改即可；未来可逐步迁移到 api/modules 封装 |
