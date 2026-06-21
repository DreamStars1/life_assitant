## Why

当前项目存在三套认证体系：后端 FastAPI 标准 OAuth2 认证、移动端（vue3-vant-mobile）认证对接和管理后台（vue3-element-admin）认证对接。三者之间存在严重的 API 路径不匹配、响应格式不一致、Token 传递方式不同等问题，导致登录和注册功能在前后端联调时完全不可用。管理后台的 API 路径引用了一套后端根本不存在的 `/auth/*` 接口族，移动端则存在注册端点调用错误和 Token 头格式不标准的问题。

## What Changes

### 后端修复
- 添加 `/auth/login` 适配端点（或调整管理后台对接现有 `/login/access-token` 接口）
- **BREAKING** 统一 Token 响应格式为含 `accessToken` + `refreshToken` 的标准结构
- 添加 `/auth/refresh-token` Token 刷新端点
- 添加 `/auth/logout` 登出端点（可选实现 Token 黑名单或仅客户端丢弃）
- 添加验证码生成端点 `/auth/captcha`（可选，或移除管理后台验证码依赖）

### 移动端修复
- 修复 `api/user.ts` 中 `register()` 导出的错误端点 `/users/register` → `/users/signup`
- **BREAKING** 将 Token 传递方式从自定义 `Access-Token` 头改为标准 `Authorization: Bearer <token>`

### 管理后台修复
- 修改 `src/api/auth/index.ts` 中所有端点路径，对齐后端实际 API
- 修改响应拦截器 `src/utils/request.ts`，兼容 FastAPI 直接响应（无 `{code, data, msg}` 包装）
- 移除或调整为后端实际存在的 API 路径和响应结构

## Capabilities

### New Capabilities
- `auth-api-alignment`: 统一前后端认证 API 路径、请求/响应格式和 Token 传递方式
- `auth-token-refresh`: Token 刷新机制，支持 access_token 过期后自动续期

### Modified Capabilities
<!-- 无现有 spec 文件需要修改 -->

## Impact

- **后端**: `app/api/routes/login.py`（添加端点）、`app/core/security.py`（可能需要添加 refresh token 生成）、`app/models.py`（可能需要调整 Token 模型）
- **移动端**: `src/api/user.ts`、`src/utils/request.ts`、`src/utils/auth.ts`、`src/stores/modules/user.ts`
- **管理后台**: `src/api/auth/index.ts`、`src/api/auth/types.ts`、`src/utils/request.ts`、`src/enums/api.ts`
- 无新增依赖
