## Why

当前系统几乎所有业务接口（lifelog、planning、identity、notification、items、users/me 等）都通过 `CurrentUser` 依赖注入强制要求认证，未登录用户访问任何受保护端点均返回 401。除此之外，存在 JWT 过期返回 403（而非 401）的语义错误，以及缺少公开健康检查端点等问题。需要梳理权限体系全貌，修复错误状态码，补齐必要的公开端点。

## What Changes

- 梳理并文档化当前系统的完整权限矩阵：哪些端点公开、哪些需认证、哪些需超管
- 修复 `get_current_user()` 中 JWT 过期/无效时返回 `403 FORBIDDEN` 的错误，改为 `401 UNAUTHORIZED`
- 为 `OAuth2PasswordBearer` 启用自定义 401 响应体（`auto_error=False` + 手动抛出），统一 401 时的错误格式，避免 FastAPI 默认空响应
- 添加 `GET /api/v1/health` 公开健康检查端点，不要求认证
- 可选：添加 `GET /api/v1/` 公开根端点，返回 API 基本信息

## Capabilities

### New Capabilities
- `auth-error-semantics`: 统一认证错误语义 —— 未认证（无 token / token 无效 / token 过期）统一返回 401 而非混用 403
- `health-endpoint`: 添加公开健康检查端点，供负载均衡和监控使用

### Modified Capabilities
<!-- 无现有主 spec 需要修改；所有 spec 目前仅存在于 changes 中 -->

## Impact

- **后端**: `app/api/deps.py`（修改 `get_current_user` 错误码）、`app/api/routes/utils.py` 或 `app/main.py`（添加 health 端点）
- **移动端**: 无需修改（`request.ts` 已正确处理 401/403）
- **管理后台**: 无需修改（`request.ts` 已正确处理 401/403，含自动 refresh 重试逻辑）
- 无新增依赖
