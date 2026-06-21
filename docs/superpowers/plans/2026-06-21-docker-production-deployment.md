# Docker 生产级部署实现计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 为 Life Assistant 项目创建生产级 Docker 部署方案，包含 backend multi-stage Dockerfile、acme.sh 自动 SSL、nginx 反向代理、开发环境 compose 文件。

**Architecture:** 8 个文件变更——2 个新增、4 个修改、1 个新增、1 个删除、1 个 gitignore 调整。所有变更不涉及应用代码。

**Tech Stack:** Docker Compose v3, nginx:alpine, neilpang/acme.sh:3.1.0, maven:3.9-eclipse-temurin-17-alpine, eclipse-temurin:17-jre-alpine

---

### Task 1: 创建 backend/.dockerignore

**Files:**
- Create: `backend/.dockerignore`

- [ ] **Step 1: 写入 .dockerignore**

```dockerignore
# Maven 构建产物
target/
*.jar
*.war

# IDE
.idea/
*.iml
.vscode/

# Git
.git/
.gitignore

# 文档
*.md
docs/

# 系统
.DS_Store
Thumbs.db

# 环境变量（通过 compose 注入，不打包进镜像）
.env
.env.*
```

- [ ] **Step 2: 验证** — `ls backend/.dockerignore` 确认文件存在

---

### Task 2: 创建 backend/Dockerfile（多阶段 Maven 构建）

**Files:**
- Create: `backend/Dockerfile`

- [ ] **Step 1: 确认 Dockerfile 上下文路径**

compose.yml 中的 `build.context: backend`，意味着 Dockerfile 内的路径是相对于 `backend/` 的。Maven 父 POM 在 `backend/lifeassistant/pom.xml`。

- [ ] **Step 2: 写入 Dockerfile**

```dockerfile
# ===== 构建阶段 =====
FROM maven:3.9-eclipse-temurin-17-alpine AS build

WORKDIR /build

# 先复制全量 pom.xml，利用 Docker 层缓存（依赖不常变）
COPY lifeassistant/pom.xml ./lifeassistant/
COPY lifeassistant/lifeassistant-common/pom.xml ./lifeassistant/lifeassistant-common/
COPY lifeassistant/lifeassistant-system/pom.xml ./lifeassistant/lifeassistant-system/
COPY lifeassistant/lifeassistant-server/pom.xml ./lifeassistant/lifeassistant-server/

# 下载依赖
RUN cd lifeassistant && mvn dependency:go-offline -B -q

# 复制源码
COPY lifeassistant/ ./lifeassistant/

# fat-jar 打包（跳过测试以加速）
RUN cd lifeassistant && mvn package -P fat-jar -DskipTests -B -q && \
    find lifeassistant-server/target -name 'lifeassistant.jar' -exec cp {} /build/app.jar \;

# ===== 运行阶段 =====
FROM eclipse-temurin:17-jre-alpine

RUN addgroup -g 1001 appgroup && \
    adduser -u 1001 -G appgroup -s /bin/sh -D appuser

WORKDIR /app
COPY --from=build /build/app.jar ./app.jar

RUN mkdir -p /app/logs && chown -R appuser:appgroup /app

USER appuser

EXPOSE 8000

HEALTHCHECK --interval=15s --timeout=5s --retries=3 --start-period=60s \
    CMD wget -qO- http://localhost:8000/actuator/health || exit 1

ENTRYPOINT ["java", "-jar", "app.jar"]
```

- [ ] **Step 3: 验证** — `ls backend/Dockerfile` 确认文件存在

---

### Task 3: 修改 compose.yml（去 admin、加 acme.sh、修正 backend 环境变量、去重 env_file）

**Files:**
- Modify: `compose.yml`

- [ ] **Step 1: 替换整个 compose.yml**

```yaml
x-backend-env: &backend-env
  PROFILES_ACTIVE: prod
  DB_HOST: db
  DB_PORT: "3306"
  DB_NAME: ${MYSQL_DATABASE}
  DB_USER: ${MYSQL_USER}
  DB_PWD: ${MYSQL_PASSWORD}
  REDIS_HOST: redis
  REDIS_PORT: "6379"
  REDIS_PWD: ""
  REDIS_DB: "0"
  DOMAIN: ${DOMAIN}
  FRONTEND_HOST: https://app.${DOMAIN}
  BACKEND_CORS_ORIGINS: https://app.${DOMAIN},https://api.${DOMAIN}
  SECRET_KEY: ${SECRET_KEY?Variable not set}
  FIRST_SUPERUSER: ${FIRST_SUPERUSER?Variable not set}
  FIRST_SUPERUSER_PASSWORD: ${FIRST_SUPERUSER_PASSWORD?Variable not set}
  SMTP_HOST: ${SMTP_HOST:-}
  SMTP_USER: ${SMTP_USER:-}
  SMTP_PASSWORD: ${SMTP_PASSWORD:-}
  EMAILS_FROM_EMAIL: ${EMAILS_FROM_EMAIL:-}
  SENTRY_DSN: ${SENTRY_DSN:-}

services:

  db:
    image: mysql:8.4
    restart: unless-stopped
    healthcheck:
      test: ["CMD", "mysqladmin", "ping", "-h", "localhost"]
      interval: 10s
      retries: 5
      start_period: 30s
      timeout: 10s
    volumes:
      - app-db-data:/var/lib/mysql
    environment:
      MYSQL_ROOT_PASSWORD: ${MYSQL_ROOT_PASSWORD?Variable not set}
      MYSQL_ROOT_HOST: "%"
      MYSQL_DATABASE: ${MYSQL_DATABASE?Variable not set}
      MYSQL_USER: ${MYSQL_USER?Variable not set}
      MYSQL_PASSWORD: ${MYSQL_PASSWORD?Variable not set}
    networks:
      - default

  redis:
    image: redis:7
    restart: unless-stopped
    healthcheck:
      test: ["CMD", "redis-cli", "ping"]
      interval: 10s
      retries: 5
      timeout: 5s
    volumes:
      - app-redis-data:/data
    networks:
      - default

  adminer:
    image: adminer
    restart: unless-stopped
    depends_on:
      - db
    environment:
      ADMINER_DESIGN: pepa-linha-dark
    networks:
      - default

  backend:
    image: '${DOCKER_IMAGE_BACKEND:-backend}:${TAG:-latest}'
    restart: unless-stopped
    depends_on:
      db:
        condition: service_healthy
        restart: true
      redis:
        condition: service_healthy
        restart: true
    environment:
      <<: *backend-env
    healthcheck:
      test: ["CMD", "wget", "-qO-", "http://localhost:8000/actuator/health"]
      interval: 15s
      timeout: 5s
      retries: 5
      start_period: 60s
    build:
      context: backend
      dockerfile: Dockerfile
    networks:
      - default

  front:
    image: '${DOCKER_IMAGE_FRONT:-front}:${TAG:-latest}'
    build:
      context: front/vue3-vant-mobile
      dockerfile: Dockerfile
    restart: unless-stopped
    networks:
      - default

  nginx:
    build:
      context: backend/nginx
      dockerfile: Dockerfile
    restart: unless-stopped
    ports:
      - "80:80"
      - "443:443"
    volumes:
      - certs:/etc/nginx/certs:ro
      - acme-webroot:/var/www/acme:ro
    environment:
      DOMAIN: ${DOMAIN?Variable not set}
    depends_on:
      backend:
        condition: service_healthy
      front:
        condition: service_started
      adminer:
        condition: service_started
    networks:
      - default

  acme.sh:
    image: neilpang/acme.sh:3.1.0
    container_name: acme.sh
    volumes:
      - acme-data:/acme.sh
      - acme-webroot:/var/www/acme
      - certs:/certs
    environment:
      AUTO_UPGRADE: "1"
    command: daemon
    restart: unless-stopped
    networks:
      - default

volumes:
  app-db-data:
  app-redis-data:
  acme-data:
  acme-webroot:
  certs:

networks:
  default:
    driver: bridge
```

- [ ] **Step 2: 验证** — `docker compose config` 确认语法正确（如果有 docker 环境）

---

### Task 4: 创建 compose.dev.yml（仅基础设施）

**Files:**
- Create: `compose.dev.yml`

- [ ] **Step 1: 写入 compose.dev.yml**

```yaml
# 开发环境：仅启动基础设施（MySQL + Redis + Adminer），应用本地跑
services:
  db:
    image: mysql:8.4
    restart: "no"
    ports:
      - "33062:3306"
    volumes:
      - app-db-data:/var/lib/mysql
    environment:
      MYSQL_ROOT_PASSWORD: rootpassword
      MYSQL_ROOT_HOST: "%"
      MYSQL_DATABASE: app
      MYSQL_USER: app
      MYSQL_PASSWORD: changethis
    healthcheck:
      test: ["CMD", "mysqladmin", "ping", "-h", "localhost"]
      interval: 10s
      retries: 5
      start_period: 30s
      timeout: 10s

  redis:
    image: redis:7
    restart: "no"
    ports:
      - "6379:6379"
    volumes:
      - app-redis-data:/data

  adminer:
    image: adminer
    restart: "no"
    ports:
      - "8080:8080"
    environment:
      ADMINER_DESIGN: pepa-linha-dark
    depends_on:
      - db

volumes:
  app-db-data:
  app-redis-data:
```

- [ ] **Step 2: 验证** — `docker compose -f compose.dev.yml config` 确认语法正确

---

### Task 5: 删除 compose.override.yml

**Files:**
- Delete: `compose.override.yml`

- [ ] **Step 1: 删除文件**

功能已被 `compose.dev.yml` 替代。

```powershell
Remove-Item compose.override.yml
```

---

### Task 6: 修改 backend/nginx/nginx.conf（去 admin server block、加 ACME challenge）

**Files:**
- Modify: `backend/nginx/nginx.conf`

- [ ] **Step 1: 替换整个 nginx.conf**

```nginx
# 生产环境 Nginx 配置
# DOMAIN 占位符由 envsubst 在容器启动时替换

# HTTP 80 —— ACME challenge 放行，其余重定向到 HTTPS
server {
    listen 80;
    server_name api.${DOMAIN} app.${DOMAIN} adminer.${DOMAIN};

    location /.well-known/acme-challenge/ {
        root /var/www/acme;
    }

    location / {
        return 301 https://$host$request_uri;
    }
}

# API 后端
server {
    listen 443 ssl http2;
    server_name api.${DOMAIN};

    ssl_certificate     /etc/nginx/certs/fullchain.pem;
    ssl_certificate_key /etc/nginx/certs/privkey.pem;

    proxy_set_header Host               $host;
    proxy_set_header X-Real-IP          $remote_addr;
    proxy_set_header X-Forwarded-For    $proxy_add_x_forwarded_for;
    proxy_set_header X-Forwarded-Proto  $scheme;

    location / {
        proxy_pass http://backend:8000;
    }
}

# 用户端 PWA
server {
    listen 443 ssl http2;
    server_name app.${DOMAIN};

    ssl_certificate     /etc/nginx/certs/fullchain.pem;
    ssl_certificate_key /etc/nginx/certs/privkey.pem;

    proxy_set_header Host               $host;
    proxy_set_header X-Real-IP          $remote_addr;
    proxy_set_header X-Forwarded-For    $proxy_add_x_forwarded_for;
    proxy_set_header X-Forwarded-Proto  $scheme;

    location /api/ {
        proxy_pass http://backend:8000;
    }

    location / {
        proxy_pass http://front:80;
    }
}

# Adminer
server {
    listen 443 ssl http2;
    server_name adminer.${DOMAIN};

    ssl_certificate     /etc/nginx/certs/fullchain.pem;
    ssl_certificate_key /etc/nginx/certs/privkey.pem;

    proxy_set_header Host               $host;
    proxy_set_header X-Real-IP          $remote_addr;
    proxy_set_header X-Forwarded-For    $proxy_add_x_forwarded_for;
    proxy_set_header X-Forwarded-Proto  $scheme;

    location / {
        proxy_pass http://adminer:8080;
    }
}
```

---

### Task 7: 修改 backend/nginx/nginx.dev.conf（去 admin server block）

**Files:**
- Modify: `backend/nginx/nginx.dev.conf`

- [ ] **Step 1: 替换整个 nginx.dev.conf**

```nginx
# 开发环境 Nginx 配置 — 无 TLS，基于子域名路由
# 使用正则 server_name 匹配任意域名的子域名前缀

# API 后端
server {
    listen 80;
    server_name ~^api\.;

    proxy_set_header Host               $host;
    proxy_set_header X-Real-IP          $remote_addr;
    proxy_set_header X-Forwarded-For    $proxy_add_x_forwarded_for;
    proxy_set_header X-Forwarded-Proto  $scheme;

    location / {
        proxy_pass http://backend:8000;
    }
}

# 用户端 PWA
server {
    listen 80;
    server_name ~^app\.;

    proxy_set_header Host               $host;
    proxy_set_header X-Real-IP          $remote_addr;
    proxy_set_header X-Forwarded-For    $proxy_add_x_forwarded_for;
    proxy_set_header X-Forwarded-Proto  $scheme;

    location /api/ {
        proxy_pass http://backend:8000;
    }

    location / {
        proxy_pass http://front:80;
    }
}

# Adminer
server {
    listen 80;
    server_name ~^adminer\.;

    proxy_set_header Host               $host;
    proxy_set_header X-Real-IP          $remote_addr;
    proxy_set_header X-Forwarded-For    $proxy_add_x_forwarded_for;
    proxy_set_header X-Forwarded-Proto  $scheme;

    location / {
        proxy_pass http://adminer:8080;
    }
}
```

---

### Task 8: 修改 .env（对齐 application-prod.yml 变量名，作为生产模板）

**Files:**
- Modify: `.env`

- [ ] **Step 1: 替换整个 .env**

```env
# === Life Assistant 生产环境变量 ===
# compose 解析 ${VARIABLE} 使用此文件
# 填入真实值后部署

# Domain
DOMAIN=yourdomain.com

# Environment: local, staging, production
ENVIRONMENT=production

# Backend
SECRET_KEY=<generate-64-char-random-string>
FIRST_SUPERUSER=admin@yourdomain.com
FIRST_SUPERUSER_PASSWORD=<strong-password>

# Emails (可选)
SMTP_HOST=
SMTP_USER=
SMTP_PASSWORD=
EMAILS_FROM_EMAIL=

# MySQL
MYSQL_DATABASE=app
MYSQL_USER=app
MYSQL_PASSWORD=<strong-password>
MYSQL_ROOT_PASSWORD=<strong-root-password>

# Sentry (可选)
SENTRY_DSN=

# Docker image tags
TAG=latest
DOCKER_IMAGE_BACKEND=backend
DOCKER_IMAGE_FRONT=front
```

---

### Task 9: 修改 .env.example（更新为与 .env 一致的模板）

**Files:**
- Modify: `.env.example`

- [ ] **Step 1: 替换整个 .env.example**

```env
# === Life Assistant 环境变量模板 ===
# 复制此文件为 .env 并填入真实值

# Domain
DOMAIN=yourdomain.com

# Environment: local, staging, production
ENVIRONMENT=production

# Backend
SECRET_KEY=<generate-64-char-random-string>
FIRST_SUPERUSER=admin@example.com
FIRST_SUPERUSER_PASSWORD=<strong-password>

# Emails (可选)
SMTP_HOST=
SMTP_USER=
SMTP_PASSWORD=
EMAILS_FROM_EMAIL=info@example.com

# MySQL
MYSQL_DATABASE=app
MYSQL_USER=app
MYSQL_PASSWORD=<strong-password>
MYSQL_ROOT_PASSWORD=<strong-root-password>

# Sentry (可选)
SENTRY_DSN=

# Docker image tags
TAG=latest
DOCKER_IMAGE_BACKEND=backend
DOCKER_IMAGE_FRONT=front
```

---

### Task 10: 修改 .gitignore（调整 Docker 相关忽略规则）

**Files:**
- Modify: `.gitignore`

- [ ] **Step 1: 在敏感配置部分，确保 certs/ 目录中 Docker 卷的空占位 .gitkeep 可以被跟踪**

当前 `.gitignore` 第 7 行 `certs/` 忽略整个目录。acme.sh 通过 Docker 卷直接写入证书，不需要宿主机的 `certs/` 目录。但 nginx 构建阶段不需要挂载——nginx 的 Dockerfile 通过 `envsubst` 生成配置，不需要预置证书。

结论：保持 `certs/` 在 `.gitignore` 中，无需修改。但需要确认 `.dockerignore` 不会误排除构建所需文件。

- [ ] **Step 2: 检查 backend/nginx/Dockerfile 是否引用了被忽略的文件**

当前 nginx Dockerfile 只复制 `nginx.conf` 和依赖 `gettext`，不依赖 `certs/`。无需修改 `.gitignore`。

- [ ] **Step 3: 添加 `compose.override.yml` 到 .gitignore**

既然要删除 `compose.override.yml`，如果用户未来重新创建也不应被跟踪。

无需额外操作——删除后自然不存在。但可以在 `.gitignore` 加一行防护：

在 `.gitignore` 的 Docker 相关区域添加：
```
# Docker compose override (per-developer, not tracked)
compose.override.yml
```

---

### 首次部署验证步骤

计划实施完成后，建议用户执行以下验证：

```bash
# 1. 确认所有文件到位
ls backend/Dockerfile backend/.dockerignore compose.yml compose.dev.yml backend/nginx/nginx.conf backend/nginx/nginx.dev.conf .env

# 2. 验证 compose 语法
docker compose config
docker compose -f compose.dev.yml config

# 3. 构建（不启动）
docker compose build

# 4. 开发环境基础设施启动
docker compose -f compose.dev.yml up -d
docker compose -f compose.dev.yml ps
docker compose -f compose.dev.yml down
```
