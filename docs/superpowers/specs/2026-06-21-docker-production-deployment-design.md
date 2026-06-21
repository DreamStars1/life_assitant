# Docker 生产级部署设计

> 日期：2026-06-21 | 状态：待实施

## 目标

将 Life Assistant 项目全栈容器化，支持单台云 VPS 通过 `docker compose` 一键部署。生产环境全栈 Docker 化，开发环境仅用 Docker 跑基础设施（MySQL + Redis）。

## 架构

```
                        VPS (your-server.com)
┌─────────────────────────────────────────────────────────────┐
│  nginx :80/:443          ← TLS 终止 + 反向路由              │
│    ├─ api.域名    → backend:8000                            │
│    ├─ app.域名    → front:80    (/api/ → backend:8000)      │
│    └─ adminer.域名 → adminer:8080                          │
│                                                              │
│  acme.sh (Let's Encrypt)  ← webroot 模式 HTTP-01 验证       │
│                                                              │
│  backend :8000 (Spring Boot fat-jar)                         │
│  front   :80   (nginx serving dist/)                        │
│                                                              │
│  mysql   :3306 (数据卷持久化)                                │
│  redis   :6379 (数据卷持久化)                                │
│  adminer :8080                                               │
└─────────────────────────────────────────────────────────────┘
```

## 文件变更清单

| 文件 | 操作 | 说明 |
|------|------|------|
| `backend/Dockerfile` | 新增 | 多阶段构建：Maven 编译 → JRE 运行 |
| `backend/.dockerignore` | 新增 | 排除 target、.git、node_modules 等 |
| `compose.yml` | 修改 | 去 admin、加 acme.sh、修正环境变量、加卷 |
| `compose.dev.yml` | 新增 | 仅 MySQL + Redis + Adminer |
| `compose.override.yml` | 删除 | 功能被 compose.dev.yml 替代 |
| `backend/nginx/nginx.conf` | 修改 | 去 admin server block、加 ACME challenge |
| `backend/nginx/nginx.dev.conf` | 修改 | 去 admin server block |
| `.env` | 修改 | 对齐 application-prod.yml 变量名 |
| `.gitignore` | 修改 | 调整卷数据路径忽略规则 |

## Backend Dockerfile（多阶段）

- **构建阶段**：`maven:3.9-eclipse-temurin-17-alpine`，利用 `mvn dependency:go-offline` 缓存依赖层，然后 `mvn package -P fat-jar -DskipTests`
- **运行阶段**：`eclipse-temurin:17-jre-alpine`，非 root `appuser:1001`，`HEALTHCHECK` 走 `/actuator/health`
- fat-jar 输出单一 `lifeassistant.jar`，比 thin-jar 分目录结构更适合容器

## acme.sh 证书管理

- 镜像：`neilpang/acme.sh:3.1.0`，`command: daemon` 后台 cron 自动续期
- 三个卷：`acme-data`（账户/配置）、`acme-webroot`（HTTP-01 验证文件）、`certs`（nginx 读证书）
- nginx 80 server 对 `/.well-known/acme-challenge/` 放行不走 301 重定向
- 用户首次部署需手动执行 `--register-account`、`--issue`、`--install-cert` 三条命令

## 环境变量重组

`application-prod.yml` 期望的变量名是 `DB_HOST`/`DB_USER`/`DB_PWD`/`REDIS_HOST`/`REDIS_PWD`，与旧 compose.yml 传的 `MYSQL_SERVER`/`REDIS_URL` 不一致。改为直接在 compose 里注入正确的变量名，不再依赖 `backend/.env` 文件。

## compose.dev.yml（开发用）

仅启动 MySQL + Redis + Adminer，暴露端口给宿主机（33062/6379/8080）。开发时 backend 和前端本地跑。

## 首次部署流程

```bash
# 1. 准备 .env（填写真实域名、密码等）
cp .env.example .env && vim .env

# 2. 构建并启动
docker compose build
docker compose up -d

# 3. acme.sh 注册 + 签发证书
docker compose exec acme.sh --register-account -m your@email.com
docker compose exec acme.sh --issue \
  -d api.yourdomain.com -d app.yourdomain.com -d adminer.yourdomain.com \
  -w /var/www/acme --server letsencrypt
docker compose exec acme.sh --install-cert -d yourdomain.com \
  --key-file /certs/privkey.pem \
  --fullchain-file /certs/fullchain.pem \
  --reloadcmd "wget -qO- --post-data='' http://nginx:80/.well-known/fake || true"

# 4. 重载 nginx 使证书生效
docker compose restart nginx
```

## 未实施部分

- admin 管理后台暂不容器化（admin_front/ 尚未完善）
- sentry/mail/sms 因基础设施上限，保留为可选配置
