## Context

项目包含三套独立开发的认证体系：

1. **后端 (FastAPI)**: 标准 FastAPI OAuth2 流程，`POST /api/v1/login/access-token`（form-encoded），返回 `{access_token, token_type}`；注册端点 `POST /api/v1/users/signup`（JSON body）；`GET /api/v1/users/me` 获取当前用户。无需验证码、Token 刷新、登出端点。

2. **移动端 (vue3-vant-mobile)**: 主要功能正确，但存在：
   - `api/user.ts` 中导出的 `register()` 调用 `/users/register`（后端实际端点为 `/users/signup`）
   - 使用自定义 `Access-Token` 请求头而非标准 `Authorization: Bearer`

3. **管理后台 (vue3-element-admin)**: 基于 youlai 模板，设计为对接 Java 后端（youlai-boot 风格的 `/api/v1/auth/*` 接口族），与当前 FastAPI 后端完全不兼容：
   - 所有 API 路径错误：`/auth/login`、`/auth/captcha`、`/auth/refresh-token`、`/auth/logout`
   - 响应格式期望 `{code: "00000", data: {...}, msg: "..."}` 包装，而 FastAPI 直接返回 JSON
   - 使用 mock 数据开发，mock 数据模拟了不存在的端点

**当前状态**: 无 Docker Compose 中 nginx 代理配置可见，但后端通过 `compose.override.yml` 暴露 `8000` 端口，移动端 dev 模式直连 `localhost:8000/api/v1`，管理后台配置指向 `https://api.youlai.tech`（外部不相关 API）。

## Goals / Non-Goals

**Goals:**
- 移动端登录/注册功能可正常使用
- 管理后台至少登录功能可正常工作（对接真实后端）
- 统一 Token 传递方式为标准 `Authorization: Bearer` 头
- 后端添加管理后台所需的 `/auth/*` 过渡端点

**Non-Goals:**
- 不实现完整的 RBAC 权限系统（角色、菜单、按钮权限）
- 不实现多租户支持
- 不实现验证码（captcha）后端生成（管理后台暂时移除验证码依赖或使用简化方案）
- 不实现 Token 黑名单/服务端登出（登出仅客户端丢弃 Token）

## Decisions

### 决策 1: 后端同时保留现有端点 + 添加 `/auth/*` 适配端点

**方案**: 在 `app/api/routes/login.py` 中添加新的 `/auth/login`、`/auth/refresh-token`、`/auth/logout` 端点，内部复用现有认证逻辑。

**替代方案考虑**:
- 方案 A: 修改管理后台代码去适配后端现有端点。工作量更大（修改管理后台所有 API 调用、拦截器、类型定义），且管理后台依赖 `refreshToken`、`captcha` 等功能。
- 方案 B: 完全重写后端认证为管理后台的格式。破坏现有移动端，风险过大。

**选择**: 后端添加适配端点是侵入性最小的方案，只需添加几个薄端点。

### 决策 2: 后端新增 Token 响应格式

**方案**: 后端 `/auth/login` 返回格式与现有 `/login/access-token` 不同，新增 `refresh_token` 字段。`refresh_token` 使用相同的 JWT 签名方式但有效期更长（如 7 天）。

```json
{
  "access_token": "eyJ...",
  "token_type": "bearer",
  "refresh_token": "eyJ...",
  "expires_in": 7200
}
```

管理后台响应拦截器 `src/utils/request.ts` 需调整为兼容 FastAPI 直接返回格式（无 `code` 包装）。

### 决策 3: 管理后台响应拦截器适配

**方案**: 修改管理后台 `src/utils/request.ts` 的响应拦截器，移除对 `{code, data, msg}` 包装的强制依赖。成功响应直接透传，HTTP 错误状态码触发错误处理。

**替代方案考虑**:
- 方案 A: 后端包装所有响应。需要中间件包装所有端点，影响面大，且 FastAPI 自动生成的 OpenAPI schema 会不准确。

**选择**: 前端适配后端。FastAPI 生态更自然，OpenAPI 文档保持准确。

### 决策 4: 管理后台移除验证码依赖

**方案**: 管理后台登录表单移除验证码字段，或将其设为可选。`compose.override.yml` 中 mailcatcher 可用于密码重置邮件，但验证码不在范围内。

**ponytail**: 验证码属于反爬/安全增强，当前阶段不需要。如需在后续添加，使用简单算式验证码或复用 mailcatcher 邮件发送。

## Risks / Trade-offs

- **[风险] Token 刷新端点缺少真正的 refresh token 安全性**: refresh token 使用与 access token 相同的 JWT 签名，仅有效期更长。→ 缓解：当前阶段可接受，后续可引入独立的 refresh token secret 或使用 opaque token。
- **[风险] 管理后台登出仅为客户端行为**: 后端不维护 Token 黑名单，登出后 Token 仍有效直到过期。→ 缓解：Token 有效期短（默认 8 天），可接受。
- **[风险] 管理后台移除验证码降低安全性**: 登录表单无验证码保护。→ 缓解：当前为本地开发环境，部署时需添加验证码或限流保护。
- **[权衡] 后端同时维护两套登录端点**: 增加维护负担。→ 缓解：`/auth/*` 端点为薄包装，复用现有 `crud.authenticate()`。
