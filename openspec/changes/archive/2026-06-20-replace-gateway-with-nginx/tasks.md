## 1. Nginx 配置文件

- [x] 1.1 创建 `backend/nginx/nginx.conf`：生产配置，包含 api/app/admin/adminer 四个 server block、TLS 终结 (443)、HTTP→HTTPS 重定向 (80)、`X-Forwarded-*` 头转发
- [x] 1.2 创建 `backend/nginx/nginx.dev.conf`：开发配置，仅监听 80 端口，无 TLS，与生产保持相同路由规则
- [x] 1.3 创建 `backend/nginx/Dockerfile`：基于 `nginx:alpine`，通过 envsubst 注入 DOMAIN

## 2. Docker Compose 改造

- [x] 2.1 `compose.yml`（根目录）：删除所有服务的 `traefik.*` labels，移除 `traefik-public` 网络定义，新增 `nginx` 服务，`env_file` 指向 `backend/.env`
- [x] 2.2 `compose.override.yml`（根目录）：将 `proxy`（Traefik）服务替换为 Nginx 开发覆写（挂载 `nginx.dev.conf`，端口 80:80）
- [x] 2.3 删除 `backend/compose.traefik.yml` 文件

## 3. 清理与验证

- [x] 3.1 `docker compose config` 验证 Compose 文件语法正确
- [x] 3.2 `nginx -t` 验证 nginx 配置语法正确
- [ ] 3.3 本地启动：`docker compose up -d` 确认所有服务正常启动（需本地 Docker 环境）
- [ ] 3.4 验证路由：`curl -H "Host: api.localhost" http://localhost/api/v1/health` 返回 200（本地启动后）
- [ ] 3.5 验证 SPA 回退：`curl -H "Host: app.localhost" http://localhost/todos` 返回 index.html（本地启动后）
- [x] 3.6 `nginx -t` 配置校验加入 pre-commit hook
