## Context

当前架构使用 Traefik 3.6 作为反向代理，通过 Docker Compose labels 配置路由规则。Traefik 运行在单独的 `compose.traefik.yml` 中，开发环境通过 `compose.override.yml` 覆盖。

**现有路由规则：**

| 域名 | 目标服务 | 端口 |
|------|----------|------|
| `api.${DOMAIN}` | backend | 8000 |
| `app.${DOMAIN}` | front | 80 |
| `admin.${DOMAIN}` | admin | 80 |
| `adminer.${DOMAIN}` | adminer | 8080 |
| `traefik.${DOMAIN}` | Traefik Dashboard | 8080 |

**问题：**
1. Traefik 需要挂载 Docker socket（`/var/run/docker.sock:ro`），扩大攻击面
2. Docker labels 配置不易版本评审、不可独立测试
3. 需要预先创建 `traefik-public` 外部网络，增加部署步骤
4. Traefik 在资源受限环境（如 1GB RAM 实例）下比 Nginx 占用更多内存

## Goals / Non-Goals

**Goals:**
- 用 Nginx `nginx:alpine` 替换 Traefik 3.6
- 保持完全相同的路由规则和 TLS 行为
- 移除 Docker socket 依赖
- 移除 `traefik-public` 外部网络依赖
- 生产环境支持 Let's Encrypt TLS（通过 certbot 或手动证书挂载）
- 开发环境支持无 TLS 的热重载

**Non-Goals:**
- 不改动后端 API 路由（`/api/v1/*` 路径不变）
- 不改动前端构建流程（Dockerfile 不变）
- 不改动任何应用层代码
- 不引入 Nginx Plus（使用开源版）
- 不做性能基准对比测试

## Decisions

### D-1: 使用 static `nginx.conf` 而非 Docker labels

**选择**：所有路由规则写在 `nginx.conf` 中，通过 `include` 分隔生产/开发配置。

**理由**：
- Nginx 的声明式配置文件比 Docker labels 更直观、可版本控制、可独立测试
- 移除 Docker socket 挂载（Traefik 需要 socket 读取其他容器的 labels）
- Nginx 重启/reload 即可生效，无需重新部署整个 stack

**替代方案**：使用 `nginx-proxy` / `jwilder/nginx-proxy` 自动发现容器。否决：引入额外依赖，且同样需要 Docker socket。

### D-2: 使用 `nginx:alpine` 基础镜像

**选择**：`nginx:alpine`（约 8MB），而非 `nginx:latest`（约 187MB）。

**理由**：
- 体积减少 96%，适合资源受限部署
- 安全性更好（攻击面小）
- 功能完全满足反向代理需求

### D-3: TLS 通过 certbot + Let's Encrypt 或手动挂载

**选择**：不内置自动证书管理（Traefik 的 Let's Encrypt 自动处理不再可用），提供两种方案：

- **方案 A（推荐）**：在宿主机上用 certbot 获取证书，通过 volume 挂载到 Nginx 容器
- **方案 B**：在 compose 中加 sidecar certbot 容器，定时续期

**理由**：
- 方案 A 最简单，适合个人/情侣项目（证书 90 天续期一次，可 cron 自动化）
- 方案 B 增加了复杂度，但完全容器化。需要时再引入

### D-4: 内部网络替代 `traefik-public`

**选择**：Nginx 加入 `default` 网络即可与其他服务通信，不再需要 `traefik-public` 外部网络。

**理由**：
- Compose 默认会创建 `default` 网络，所有服务默认加入
- Nginx 通过服务名（`backend`、`front`、`admin`）代理请求
- 移除对外部网络的依赖，简化部署流程（`docker compose up` 即可）

### D-5: 开发环境使用独立 nginx.dev.conf

**选择**：开发环境用 `nginx.dev.conf`（无 TLS），通过 `compose.override.yml` 覆盖 Nginx 配置卷挂载。

**理由**：
- 开发环境不需要 HTTPS，减少证书管理负担
- 配置差异足够大（无 TLS、无 certbot），用单独文件比条件判断更清晰
- 保持和生产配置结构一致，迁移时只需替换配置文件

## Risks / Trade-offs

| 风险 | 缓解 |
|------|------|
| 缺少 Traefik Dashboard 那样直观的路由可视化 | Nginx 无官方 Dashboard。可用 `nginx -T` 转储配置，或引入 `nginx-prometheus-exporter` + Grafana（按需） |
| TLS 证书需手动续期（Traefik 自动处理） | 方案 A（宿主机 certbot + cron）或方案 B（sidecar certbot 容器）可自动化 |
| 配置错误可能导致服务不可用 | 先 `nginx -t` 验证配置再 reload；保留 Traefik 配置作为回退方案 |
| 迁移期间服务中断 | compose.yml 一次性替换，不涉及蓝绿部署；回退只需恢复 compose 文件 + `docker compose up -d` |
| `nginx:alpine` 缺少调试工具 | 仅在必要时用 `docker exec -it` 进入容器；日志用 `docker compose logs nginx` |
