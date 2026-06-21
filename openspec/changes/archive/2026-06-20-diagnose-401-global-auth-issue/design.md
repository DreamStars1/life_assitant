## Context

项目基于 FastAPI + Full Stack FastAPI Template 构建，三套前端（移动端 vant、管理后台 element-admin、桌面 PWA）通过 nginx 或直连方式访问后端。

**当前架构：**

```
nginx :80 (dev)
  api.*    → backend:8000
  app.*    → front:80
  admin.*  → admin:80
  adminer.* → adminer:8080

直连: localhost:8000/api/v1 (移动端 + 管理后台 dev 模式)
```

**认证链路：**

```
OAuth2PasswordBearer (提取 Authorization: Bearer <token>)
    ↓ 无 token → 401（FastAPI 默认空响应体）
    ↓ 有 token → 返回 token 字符串
get_current_user() 依赖注入
    ↓ JWT 解码 + sub 校验 + UUID 转换 + DB 查询
    ↓ 失败 → 403（Bug，应为 401）
    ↓ 成功 → User 对象
CurrentUser = Annotated[User, Depends(get_current_user)]
    ↓ 注入到业务端点
```

**权限分级：**

| 级别 | 机制 | 典型端点 |
|------|------|----------|
| 公开 | 无 `CurrentUser` 依赖 | `/login/access-token`、`/auth/login`、`/users/signup`、`/utils/health-check/` |
| 需登录 | `CurrentUser` 依赖 | `/users/me`、所有 lifelog/planning/identity/notification/items 端点 |
| 需超管 | `get_current_active_superuser` 依赖 | `/users/` (GET/POST)、`/monitor/*`、`/utils/test-email/` |

## Goals / Non-Goals

**Goals:**
- 完整梳理并文档化权限矩阵，明确哪些端点需要认证、哪些公开
- 修复 `get_current_user()` 中 JWT 过期/无效时返回 403 的错误语义，改为 401
- 让 401 响应包含可读的 JSON 错误体（`{"detail": "..."}`），而非 FastAPI 默认空 body
- 不影响现有的前端 token 刷新逻辑（管理后台已在 401/403 时触发 refresh）

**Non-Goals:**
- 不实现 RBAC 角色权限系统
- 不修改 OAuth2 登录流程（OAuth2PasswordRequestForm 标准流程不动）
- 不引入新的认证方式（API Key、Session Cookie 等）
- 不修改前端代码（前端已正确适配 FastAPI 错误格式）

## Decisions

### 决策 1: 修复 403 → 401 错误语义

**现状问题：** `get_current_user()` 在 JWT 无效/过期时返回 `HTTP_403_FORBIDDEN`。

```python
# deps.py 第 37-41 行（Bug）
except (InvalidTokenError, ValidationError):
    raise HTTPException(
        status_code=status.HTTP_403_FORBIDDEN,  # 应为 401
        detail="Could not validate credentials",
    )
```

**HTTP 语义标准：**
- **401 Unauthorized**: "未认证" —— token 缺失、无效、过期。客户端应重新登录。
- **403 Forbidden**: "已认证但无权限" —— 用户身份有效但不具备访问该资源的权限。

**修复方案：**
- 将 `get_current_user()` 中所有 JWT 相关错误（`InvalidTokenError`、`ValidationError`、`sub` 缺失、UUID 转换失败）的 HTTP 状态码从 `403` 改为 `401`
- 保留 `is_superuser` 检查使用 `403`（用户已认证但权限不足）
- 保留 `is_active` 检查使用 `400`（语义：账户已禁用）

**影响分析：**
- 管理后台 `request.ts` 第 55 行 `if (status === 401 || status === 403)` 同时处理两者，行为不变
- 移动端 `request.ts` 第 31 行同样同时处理两者，行为不变
- 正确的 401 语义让调试更清晰

### 决策 2: OAuth2PasswordBearer 自定义 401 响应体

**现状问题：** `OAuth2PasswordBearer(auto_error=True)` 在无 token 时自动抛出 401，但响应体为空（只有 `WWW-Authenticate` 头），客户端无法获得可读的错误描述。

**方案：** 使用 `auto_error=False` 获取 Optional 返回值，手动检查并抛出统一格式的 `HTTPException`。

```python
# 变更前
reusable_oauth2 = OAuth2PasswordBearer(tokenUrl=..., auto_error=True)
TokenDep = Annotated[str, Depends(reusable_oauth2)]

# 变更后
reusable_oauth2 = OAuth2PasswordBearer(tokenUrl=..., auto_error=False)

def get_token(token: Annotated[str | None, Depends(reusable_oauth2)]) -> str:
    if not token:
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED,
            detail="Not authenticated",
        )
    return token

TokenDep = Annotated[str, Depends(get_token)]
```

**替代方案考虑：**
- 方案 A: 保留 `auto_error=True`，通过 exception_handler 拦截。缺点：FastAPI 的 `HTTPException` 已经是标准处理，不需要额外的 handler。
- 方案 B: 自定义 security scheme。过于复杂，无必要。

**选择：** `auto_error=False` + 手动检查，最小改动且语义清晰。

### 决策 3: Health Check 端点

**现状：** `GET /api/v1/utils/health-check/` 已存在且是公开端点。路径在 `/utils/` 下略显隐蔽，但功能完整。

**方案：** 不变动现有 health check 路径。系统已有公开端点供健康检查使用。

### 决策 4: 权限矩阵文档化

**完整权限矩阵：**

| 方法 | 路径 | 认证要求 | 权限级别 |
|------|------|----------|----------|
| `POST` | `/api/v1/login/access-token` | 无 | 公开 |
| `POST` | `/api/v1/login/test-token` | `CurrentUser` | 需登录 |
| `POST` | `/api/v1/password-recovery/{email}` | 无 | 公开 |
| `POST` | `/api/v1/reset-password/` | 无 | 公开 |
| `POST` | `/api/v1/auth/login` | 无 | 公开 |
| `POST` | `/api/v1/auth/refresh-token` | 无（query param） | 公开 |
| `DELETE` | `/api/v1/auth/logout` | `CurrentUser` | 需登录 |
| `POST` | `/api/v1/password-recovery-html-content/{email}` | 超管 | 需超管 |
| `GET` | `/api/v1/users/` | 超管 | 需超管 |
| `POST` | `/api/v1/users/` | 超管 | 需超管 |
| `GET` | `/api/v1/users/me` | `CurrentUser` | 需登录 |
| `PATCH` | `/api/v1/users/me` | `CurrentUser` | 需登录 |
| `PATCH` | `/api/v1/users/me/password` | `CurrentUser` | 需登录 |
| `DELETE` | `/api/v1/users/me` | `CurrentUser` | 需登录 |
| `POST` | `/api/v1/users/signup` | 无 | 公开 |
| `GET` | `/api/v1/users/{user_id}` | `CurrentUser` | 需登录 |
| `PATCH` | `/api/v1/users/{user_id}` | 超管 | 需超管 |
| `DELETE` | `/api/v1/users/{user_id}` | 超管 | 需超管 |
| `GET` | `/api/v1/utils/health-check/` | 无 | 公开 |
| `POST` | `/api/v1/utils/test-email/` | 超管 | 需超管 |
| 全部 | `/api/v1/items/*` | `CurrentUser` | 需登录 |
| 全部 | `/api/v1/identity/*` | `CurrentUser` | 需登录 |
| 全部 | `/api/v1/planning/*` | `CurrentUser` | 需登录 |
| 全部 | `/api/v1/lifelog/*` | `CurrentUser` | 需登录 |
| 全部 | `/api/v1/push/*` | `CurrentUser` | 需登录 |
| 全部 | `/api/v1/monitor/*` | 超管 | 需超管 |

## Risks / Trade-offs

- **[风险] `auto_error=False` 导致 fastapi.security 不再自动生成 WWW-Authenticate 头**：部分 OAuth2 客户端依赖此头进行自动登录 → **缓解**：当前所有前端均为手动 token 管理（localStorage + axios 拦截器），不受影响。Swagger UI 的 Authorize 按钮仍可通过 tokenUrl 正常工作。
- **[权衡] 仍无 RBAC 权限系统**：所有登录用户可访问所有 `CurrentUser` 端点，无角色/资源级别隔离 → **缓解**：当前业务阶段无需细粒度权限；升级路径已预留（`User` 模型含 `is_superuser` 字段，可按需扩展 `UserRole` 表）。

## Open Questions

- 无。当前分析已覆盖所有已知问题，修复范围明确。
