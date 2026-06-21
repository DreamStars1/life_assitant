## Why

当前使用 Traefik 3.6 作为反向代理/API 网关，通过 Docker labels 配置路由。Traefik 依赖 Docker socket 挂载（安全风险），且其配置方式（Docker labels）在项目规模增长后难以管理和版本控制。Nginx 配置更标准化、资源占用更低、社区生态更成熟，且不需要 Docker socket 权限。

## What Changes

- **移除** `compose.traefik.yml` 中的 Traefik 服务定义
- **移除** `compose.override.yml` 中的 `proxy`（Traefik 开发实例）
- **移除** `compose.yml` 中所有服务的 `traefik.*` labels
- **新增** `compose.yml` 中用 Nginx 服务替代 Traefik，通过 `nginx.conf` 模板管理路由
- **新增** `nginx/nginx.conf` 生产 Nginx 配置（多域名路由 + TLS + HTTP→HTTPS 重定向）
- **新增** `nginx/nginx.dev.conf` 开发 Nginx 配置（无 TLS，简化路由）
- **新增** `nginx/Dockerfile` Nginx 镜像构建（复制配置模板）
- **移除** 对外部网络 `traefik-public` 的依赖，改为 Nginx 内部网络桥接
- **移除** `compose.traefik.yml` 文件
- **不再需要** `traefik-public` 外部 Docker network 的预先创建

## Capabilities

### New Capabilities
- `nginx-reverse-proxy`: 用 Nginx 替代 Traefik，提供反向代理、TLS 终结、HTTP→HTTPS 重定向、静态文件服务

### Modified Capabilities
- （无 — 不涉及 OpenSpec 定义的需求层变更，仅是基础设施实现替换）

## Impact

**删除：**
- `backend/compose.traefik.yml` — 整个文件删除
- `backend/compose.override.yml` 中 `proxy`（Traefik 开发实例）及相关网络配置
- `backend/compose.yml` 中所有服务的 `traefik.*` labels
- 部署文档中对 `traefik-public` 外部网络的依赖说明

**新增：**
- `backend/nginx/nginx.conf` — 生产配置
- `backend/nginx/nginx.dev.conf` — 开发配置
- `backend/nginx/Dockerfile` — 基于 `nginx:alpine` 的镜像构建
- `backend/compose.yml` 中 `nginx` 服务定义

**修改：**
- `backend/compose.yml` — 替换 Traefik labels 为 Nginx 服务
- `backend/compose.override.yml` — 替换 Traefik proxy 为 Nginx 开发配置
- 前端开发 `.env.development` 无需修改（仍指向 backend:8000 或 localhost:8000）
- 前端 Dockerfile 无需修改（Nginx 作为反向代理而非直接暴露）
