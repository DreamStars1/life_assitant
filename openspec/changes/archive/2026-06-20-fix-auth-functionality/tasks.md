## 1. Backend Auth Endpoints

- [x] 1.1 在 `app/api/routes/login.py` 中添加 `POST /auth/login` 端点，接受 JSON 格式 `{username, password}`，复用 `crud.authenticate()`，返回 `{access_token, token_type, refresh_token, expires_in}`
- [x] 1.2 在 `app/core/security.py` 中添加 `create_refresh_token()` 函数，JWT 有效期 7 天
- [x] 1.3 在 `app/api/routes/login.py` 中添加 `POST /auth/refresh-token` 端点，验证 refresh token 并返回新的 token pair
- [x] 1.4 在 `app/api/routes/login.py` 中添加 `DELETE /auth/logout` 端点，接受已认证用户请求，返回成功消息
- [x] 1.5 在 `app/models.py` 中添加 `AuthToken` 响应模型（含 `access_token`, `token_type`, `refresh_token`, `expires_in`）

## 2. Mobile Client Fixes

- [x] 2.1 修复 `front/vue3-vant-mobile/src/api/user.ts` 中 `register()` 函数的端点：`/users/register` → `/users/signup`
- [x] 2.2 修改 `front/vue3-vant-mobile/src/utils/request.ts`，将 `REQUEST_TOKEN_KEY` 从 `Access-Token` 改为 `Authorization`，值格式为 `Bearer <token>`
- [x] 2.3 更新 `front/vue3-vant-mobile/src/utils/auth.ts` 中的 Token 读写逻辑，适配新的 header 格式

## 3. Admin Panel Fixes

- [x] 3.1 修改 `admin_front/vue3-element-admin/src/utils/request.ts` 响应拦截器，移除对 `{code, data, msg}` 包装格式的强制依赖，直接透传 FastAPI JSON 响应
- [x] 3.2 修改 `admin_front/vue3-element-admin/src/utils/request.ts` 错误处理逻辑，HTTP 错误（400/401/403）通过 `response.data.detail` 获取错误信息并展示
- [x] 3.3 修改 `admin_front/vue3-element-admin/src/api/auth/index.ts`，更新 `AuthAPI.login()` 适配后端 `/auth/login` 返回格式（字段名映射：后端 `access_token` → 前端 `accessToken`）
- [x] 3.4 移除或标记为可选：`admin_front/vue3-element-admin/src/views/login/index.vue` 中的验证码字段（captchaId/captchaCode），暂时禁用验证码获取逻辑
- [x] 3.5 修改 `admin_front/vue3-element-admin/.env.development`，将 `VITE_APP_API_URL` 指向本地后端 `http://localhost:8000`
- [x] 3.6 更新 `admin_front/vue3-element-admin/src/api/auth/types.ts` 中 `LoginResponse` 类型，对齐后端 `AuthToken` 模型字段

## 4. Verification

- [ ] 4.1 验证移动端注册流程（需启动 backend + mobile 容器）：`POST /api/v1/users/signup` → 注册成功 → 跳转登录页
- [ ] 4.2 验证移动端登录流程（需启动 backend + mobile 容器）：`POST /api/v1/login/access-token` → 获取 token → 访问 `/users/me`
- [ ] 4.3 验证管理后台登录（需启动 backend + admin 容器）：`POST /api/v1/auth/login` → 获取 token pair → 跳转首页
- [ ] 4.4 验证 Token 刷新（需启动 backend + admin 容器）：管理后台 401 → 自动刷新 → 重试请求成功
- [ ] 4.5 验证 Token 过期处理（需启动 backend + admin 容器）：刷新失败 → 跳转登录页

> **验证命令**: `docker compose up -d --build` 启动全部服务，然后用浏览器访问 http://localhost:5173（移动端）或 http://localhost:3000（管理后台）。
