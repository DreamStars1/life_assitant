## ADDED Requirements

### Requirement: API 路由

Nginx SHALL 将 `api.${DOMAIN}` 的请求代理到 `backend` 服务的 8000 端口，并保持原始 Host 头。

#### Scenario: API 请求正确转发
- **WHEN** 客户端请求 `GET https://api.xxx.cn/api/v1/health`
- **THEN** Nginx 将请求转发到 `http://backend:8000/api/v1/health`，并返回 backend 的响应

#### Scenario: API 请求 404
- **WHEN** 客户端请求不存在的 API 路径
- **THEN** Nginx 返回 backend 的 404 响应

### Requirement: 用户端 PWA 路由

Nginx SHALL 将 `app.${DOMAIN}` 的请求代理到 `front` 服务的 80 端口，并支持 SPA 历史模式回退。

#### Scenario: PWA 页面请求正确转发
- **WHEN** 客户端请求 `GET https://app.xxx.cn/todos`
- **THEN** Nginx 将请求转发到 `http://front:80/todos`，front 返回 index.html（SPA 回退）

#### Scenario: PWA 静态资源请求
- **WHEN** 客户端请求 `GET https://app.xxx.cn/assets/index.[hash].js`
- **THEN** Nginx 直接将请求代理到 `http://front:80/assets/index.[hash].js`

### Requirement: 管理后台路由

Nginx SHALL 将 `admin.${DOMAIN}` 的请求代理到 `admin` 服务的 80 端口，支持 SPA 回退。

#### Scenario: 管理后台页面请求正确转发
- **WHEN** 客户端请求 `GET https://admin.xxx.cn/dashboard`
- **THEN** Nginx 将请求转发到 `http://admin:80/dashboard`，admin 返回 index.html（SPA 回退）

### Requirement: Adminer 路由

Nginx SHALL 将 `adminer.${DOMAIN}` 的请求代理到 `adminer` 服务的 8080 端口。

#### Scenario: Adminer 页面请求
- **WHEN** 客户端请求 `GET https://adminer.xxx.cn`
- **THEN** Nginx 将请求转发到 `http://adminer:8080`

### Requirement: TLS 终结

Nginx SHALL 在生产环境监听 443 端口并提供 TLS 终结，将 80 端口 HTTP 请求重定向到 HTTPS。

#### Scenario: HTTP 重定向到 HTTPS
- **WHEN** 客户端请求 `http://app.xxx.cn`
- **THEN** Nginx 返回 301 重定向到 `https://app.xxx.cn`

#### Scenario: HTTPS 请求正常处理
- **WHEN** 客户端请求 `https://api.xxx.cn/api/v1/health`
- **THEN** Nginx 使用配置的 TLS 证书处理请求，成功代理到后端

### Requirement: HTTP 头部处理

Nginx SHALL 在代理请求时设置正确的 `X-Forwarded-For`、`X-Forwarded-Proto` 和 `Host` 头。

#### Scenario: 后端收到正确的转发头
- **WHEN** 客户端通过 HTTPS 请求 API
- **THEN** Nginx 设置 `X-Forwarded-Proto: https`、`X-Forwarded-For: <客户端IP>`，后端识别请求来源

### Requirement: 开发环境无 TLS

Nginx SHALL 在开发环境（`nginx.dev.conf`）仅监听 80 端口，不配置 TLS。

#### Scenario: 开发环境 HTTP 请求
- **WHEN** 开发环境请求 `http://localhost:80`
- **THEN** Nginx 正常转发请求，不执行任何重定向

### Requirement: 配置校验

Nginx 配置在加载前 SHALL 通过 `nginx -t` 校验。

#### Scenario: 无效配置阻止启动
- **WHEN** nginx.conf 存在语法错误
- **THEN** `nginx -t` 返回非零退出码，Nginx 不启动
