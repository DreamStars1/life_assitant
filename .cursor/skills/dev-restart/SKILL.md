---
name: dev-restart
description: Restart backend (Java 17 Spring Boot) and frontend (Vite) dev servers for the Life Assistant project. Use when user asks to restart/reboot development servers, frontend, backend, or both.
---

# Dev Environment Restart

## Quick commands

| Action | Command |
|--------|---------|
| 重启后端 | `.\scripts\restart-backend.ps1` |
| 重启前端 | `.\scripts\restart-frontend.ps1` |
| 两者都重启 | `.\scripts\restart-all.ps1` |
| 查看容器状态 | `docker ps --format "table {{.Names}}\t{{.Image}}\t{{.Ports}}\t{{.Status}}"` |

脚本位置：`.cursor/skills/dev-restart/scripts/`

## 环境总览

### Docker 容器

| 容器 | 镜像 | 端口 | 作用 |
|------|------|------|------|
| `life_assistant-db-1` | mysql:8.4 | 33062→3306 | MySQL 数据库，root/rootpassword，库名 app |
| `life_assistant-redis-1` | redis:7 | **无宿主机端口** | Redis 缓存/SaToken 会话 |
| `life_assistant-adminer-1` | adminer | 8080→8080 | 数据库管理界面 |
| `life_assistant-mailcatcher-1` | schickling/mailcatcher | 1026/1081 | 邮件拦截查看 |
| `life_assistant-front-1` | front:latest | 80 | 生产前端（Nginx） |
| `life_assistant-admin-1` | admin:latest | 80 | 管理后台 |

**Redis 密码**：`123456`（Redis 容器启动时设置）。

### 后端 (Java 17 Spring Boot)

- **JAR 路径**: `backend/lifeassistant/lifeassistant-server/target/app/bin/lifeassistant.jar`
- **端口**: 8000
- **构建命令**: `mvn package -DskipTests`（在 `backend/lifeassistant` 下执行）
- **启动所需环境变量**:

| 变量 | 值 | 说明 |
|------|-----|------|
| DB_HOST | localhost | MySQL 地址 |
| DB_PORT | 33062 | MySQL 端口 |
| DB_NAME | app | 数据库名 |
| DB_USER | root | MySQL 用户 |
| DB_PWD | rootpassword | MySQL 密码 |
| REDIS_HOST | localhost | Redis 地址 |
| REDIS_PORT | 6379 | Redis 端口 |
| REDIS_PWD | 123456 | Redis 密码 |

### 前端 (Vue3 + Vant + Vite)

- **目录**: `front/vue3-vant-mobile`
- **端口**: 3000
- **启动命令**: `pnpm dev`
- **代理**: `/api` → `http://localhost:8000`

## 一键重启流程

### 1. 重启 Docker 容器（如需要）

```powershell
docker compose -f <compose-file> up -d db redis
```

### 2. 从零构建并启动

```powershell
# 后端（一键启动，完整环境变量）
$env:DB_HOST = "localhost"; $env:DB_PORT = "33062"; $env:DB_NAME = "app"
$env:DB_USER = "root"; $env:DB_PWD = "rootpassword"
$env:REDIS_HOST = "localhost"; $env:REDIS_PORT = "6379"; $env:REDIS_PWD = "123456"
$env:PROFILES_ACTIVE = "dev"
cd backend\lifeassistant\lifeassistant-server\target\app
& "D:\soft\Java\jdk-17.0.2\bin\java.exe" -jar bin\lifeassistant.jar
```

## 常见问题

### Maven clean 失败
