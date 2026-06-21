## 1. 修复 OAuth2PasswordBearer 401 响应体为空的问题

- [ ] 1.1 在 `backend/app/api/deps.py` 中将 `reusable_oauth2` 改为 `auto_error=False`
- [ ] 1.2 新增 `get_token()` 函数，手动检查 token 存在性，缺失时抛出 HTTP_401_UNAUTHORIZED 并携带 `detail: "Not authenticated"`
- [ ] 1.3 将 `TokenDep` 类型从 `Annotated[str, Depends(reusable_oauth2)]` 改为 `Annotated[str, Depends(get_token)]`

## 2. 修复 403 → 401 错误语义

- [ ] 2.1 在 `backend/app/api/deps.py` 的 `get_current_user()` 中，将 `InvalidTokenError` / `ValidationError` 捕获块的 HTTP_403_FORBIDDEN 改为 HTTP_401_UNAUTHORIZED
- [ ] 2.2 将 `token_data.sub` 缺失的 HTTP_403_FORBIDDEN 改为 HTTP_401_UNAUTHORIZED
- [ ] 2.3 将 `uuid.UUID(token_data.sub)` 转换失败的 HTTP_403_FORBIDDEN 改为 HTTP_401_UNAUTHORIZED
- [ ] 2.4 确认 `get_current_active_superuser()` 中的 403 不变（已认证但权限不足，语义正确）
- [ ] 2.5 确认 `user.is_active` 检查的 400 不变（账户已禁用，语义正确）

## 3. 验证

- [ ] 3.1 无 token 访问 `/api/v1/users/me` → 返回 401 + `{"detail": "Not authenticated"}`
- [ ] 3.2 使用过期/无效 token 访问 `/api/v1/users/me` → 返回 401 + `{"detail": "Could not validate credentials"}`
- [ ] 3.3 使用有效 token 访问 `/api/v1/users/me` → 返回 200 + 用户信息
- [ ] 3.4 普通用户访问 `/api/v1/users/`（超管端点）→ 返回 403
- [ ] 3.5 匿名访问 `/api/v1/utils/health-check/` → 返回 200 + `true`
- [ ] 3.6 启动管理后台登录流程 → 登录成功 → 访问首页 → 正常显示数据
- [ ] 3.7 启动移动端登录流程 → 登录成功 → 访问首页 → 正常显示数据

> **验证命令**: `docker compose up -d --build` 或直接运行后端 `fastapi run backend/app/main.py`。
