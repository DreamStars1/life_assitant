## 1. 后端：新增 ApiResponse 统一响应类

- [x] 1.1 在 `lifeassistant-common` 模块创建 `ApiResponse.java`

## 2. 后端：重写 GlobalExceptionHandler

- [x] 2.1 异常响应格式改为 `ApiResponse`（不再用 `Map.of("detail", ...)`），删除旧的 `error()` 私有方法
- [x] 2.2 `handleBusinessException` 改为返回 400 + `log.warn`
- [x] 2.3 `handleBadRequestException` 保持 400，改用 `log.warn`
- [x] 2.4 `handleMultipartException` 修复逻辑反转（`isBlank`→`isNotBlank`），parseLong 加 try-catch 兜底，文件大小无法解析时返回 `"文件大小超出限制"`
- [x] 2.5 `handleBindException` 从 `findFirst()` 改为返回全部 `FieldError` 列表，改用 `log.warn`
- [x] 2.6 新增 `@ExceptionHandler(ConstraintViolationException.class)` 返回 400 + `log.warn`
- [x] 2.7 `handleNotLogin` 按 `e.getType()` (NOT_TOKEN/INVALID_TOKEN/TOKEN_TIMEOUT/BE_REPLACED/KICK_OUT) 返回不同 message
- [x] 2.8 `handleTypeMismatch`、`handleNotReadable`、`handleMissingParam` 改用 `log.warn`
- [x] 2.9 `handleNotFound`、`handleMethodNotSupported` 改用 `log.warn`
- [x] 2.10 `handleBaseException` 和 `handleAll` 增加 traceId：通过 `MDC.put("traceId", uuid8)` 注入，响应中包含 traceId；保留 `log.error`

## 3. 后端：Controller 返回值统一为 ApiResponse

- [x] 3.1 `AuthController`（6 个方法）：`LoginResp` 包 `ApiResponse.ok()`，`Map<String,String>` 改 `ApiResponse.ok(msg)`
- [x] 3.2 `UserController`（6 个方法）：`UserPublicResp` 包 `ApiResponse.ok()`，`Map<String,String>` 改 `ApiResponse.ok(msg)`
- [x] 3.3 `IdentityController`（3 个方法）：`UserPublicResp` 包 `ApiResponse.ok()`，`Map<String,String>` 改 `ApiResponse.ok(msg)`
- [x] 3.4 `SharedRecordController`（5 个方法）：`SharedRecordResp`/`List<SharedRecordResp>` 包 `ApiResponse.ok()`，`void` 改 `ApiResponse.ok()`

## 4. 前端：适配 ApiResponse 格式

- [x] 4.1 新增 `src/types/api.ts`：定义 `ApiResponse<T>` 接口（`code`/`message`/`data?`/`errors?`/`traceId?`）和 `FieldError` 接口
- [x] 4.2 `src/utils/request.ts`：`RequestError` 类型增加 `traceId?: string`、`errors?: FieldError[]`；错误拦截器 `data.detail` 改为 `data.message`
- [x] 4.3 `src/api/user.ts`：`LoginRes` → 嵌套为 `ApiResponse<LoginRes>`，调用方改 `res.accessToken` → `res.data.accessToken`；`register`/`fetchUser` 同样适配
- [x] 4.4 `src/api/modules/lifelogs.ts`：`request.get<LifeLogItem[]>` → `request.get<ApiResponse<LifeLogItem[]>>`，`request.post<LifeLogItem>` → `request.post<ApiResponse<LifeLogItem>>`
- [x] 4.5 `src/api/modules/todos.ts`：所有 `request.get/post/patch/delete<T>` 泛型改为 `ApiResponse<T>` 包装
- [x] 4.6 `src/api/modules/events.ts`：同上
- [x] 4.7 `src/api/modules/push.ts`：同上
- [x] 4.8 `src/stores/modules/user.ts`：`login()` / `info()` / `register()` 等调用结果适配 `.data` 解包
- [x] 4.9 裸调 request 的页面适配：`src/pages/share/index.vue`、`src/pages/profile/index.vue`、`src/pages/profile/edit.vue`、`src/pages/partner/detail.vue`——`res.xxx` → `res.data.xxx`
- [x] 4.10 UI 层：登录过期（401 + `message: "登录已过期"`）自动跳转登录页；被顶下线（"账号已在其他设备登录"）弹窗提示
- [x] 4.11 表单校验错误：从 `errors` 数组一次性标红所有问题字段

## 5. 验证

- [x] 5.1 启动后端确认编译通过
- [ ] 5.2 用 Swagger/Postman 调各接口验证响应格式
- [ ] 5.3 构造业务异常验证返回 400（非 500）
- [ ] 5.4 构造多字段校验失败验证 errors 列表完整
- [ ] 5.5 构造未知异常验证 traceId 出现在响应中
- [ ] 5.6 前端编译通过，核心流程走通
