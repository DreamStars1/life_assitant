# Glances + Dozzle 部署实施计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 在生产部署中新增 Glances（系统监控）和 Dozzle（Docker 日志）容器，通过子域名 + Nginx Basic Auth 安全访问。

**架构：** 现有 Nginx 反向代理新增两个 server block（`monitor.*`、`logs.*`），auth_basic 保护，共用 htpasswd 文件。Glances 和 Dozzle 容器加入 compose，不暴露端口，仅通过 Nginx 网络访问。

**Tech Stack:** Docker Compose, Nginx, Glances, Dozzle

---

## 文件变更清单

| 文件 | 操作 | 说明 |
|------|------|------|
| `backend/nginx/Dockerfile` | 修改 | 安装 `apache2-utils`；entrypoint 增加 htpasswd 生成 |
| `backend/nginx/nginx.conf` | 修改 | 新增 monitor.* 和 logs.* server block；HTTP 80 放行 |
| `compose.yml` | 修改 | 新增 glances、dozzle services；nginx 注入 MONITOR_USERNAME/PASSWORD |
| `compose.ip.yml` | 修改 | 增加 IP 模式的 monitor/logs 反代 |
| `.env` | 修改 | 新增 MONITOR_USERNAME、MONITOR_PASSWORD |

---

### Task 1: Nginx Dockerfile — 安装 htpasswd + entrypoint 改造

**Files:**
- Modify: `backend/nginx/Dockerfile`

- [ ] **Step 1: 在 apk 安装列表中增加 apache2-utils**

```dockerfile
# 修改前
RUN apk add --no-cache gettext openssl && \
    rm -f /etc/nginx/conf.d/default.conf
```

```dockerfile
# 修改后
RUN apk add --no-cache gettext openssl apache2-utils && \
    rm -f /etc/nginx/conf.d/default.conf
```

- [ ] **Step 2: entrypoint CMD 增加 htpasswd 生成逻辑**

当前 CMD 的结尾（证书 bootstrap → envsubst → nginx 启动），在 `exec nginx` 之前插入 htpasswd 生成：

```dockerfile
# 修改后 CMD（在 exec nginx 之前插入）
CMD ["/bin/sh", "-c", "\
if [ ! -f /etc/nginx/certs/fullchain.pem ]; then \
  mkdir -p /etc/nginx/certs && \
  openssl req -x509 -nodes -days 365 -newkey rsa:2048 \
    -keyout /etc/nginx/certs/privkey.pem \
    -out /etc/nginx/certs/fullchain.pem \
    -subj '/CN=bootstrap' && \
  echo 'Generated self-signed bootstrap cert'; \
fi && \
if [ -s /etc/nginx/conf.d/default.conf ]; then \
  echo 'Using existing default.conf'; \
else \
  envsubst '${DOMAIN}' < /etc/nginx/conf.d/default.conf.template > /etc/nginx/conf.d/default.conf; \
fi && \
if [ ! -f /etc/nginx/conf.d/.htpasswd ] && [ -n \"${MONITOR_USERNAME}\" ] && [ -n \"${MONITOR_PASSWORD}\" ]; then \
  htpasswd -cb /etc/nginx/conf.d/.htpasswd \"${MONITOR_USERNAME}\" \"${MONITOR_PASSWORD}\" && \
  echo 'Generated htpasswd for monitor/logs'; \
fi && \
exec nginx -g 'daemon off;'"]
```

- [ ] **Step 3: 验证 Dockerfile 语法正确**

```bash
cd backend/nginx && docker build -f Dockerfile -t nginx-test .
docker run --rm nginx-test sh -c "which htpasswd && echo 'OK'"
```

预期输出：`/usr/bin/htpasswd` 和 `OK`

---

### Task 2: Nginx 配置 — 新增 monitor.* 和 logs.* server block

**Files:**
- Modify: `backend/nginx/nginx.conf`

- [ ] **Step 1: HTTP 80 块新增 server_name**

```nginx
# 修改前
server_name api.${DOMAIN} app.${DOMAIN};
```

```nginx
# 修改后
server_name api.${DOMAIN} app.${DOMAIN} monitor.${DOMAIN} logs.${DOMAIN};
```

- [ ] **Step 2: 在文件末尾追加 monitor.* server block**

```nginx
# 系统监控 - Glances
server {
    listen 443 ssl;
    http2 on;
    server_name monitor.${DOMAIN};

    ssl_certificate     /etc/nginx/certs/fullchain.pem;
    ssl_certificate_key /etc/nginx/certs/privkey.pem;

    auth_basic           "Monitor";
    auth_basic_user_file /etc/nginx/conf.d/.htpasswd;

    proxy_set_header Host               $host;
    proxy_set_header X-Real-IP          $remote_addr;
    proxy_set_header X-Forwarded-For    $proxy_add_x_forwarded_for;
    proxy_set_header X-Forwarded-Proto  $scheme;

    location / {
        proxy_pass http://glances:61208;
    }
}
```

- [ ] **Step 3: 在文件末尾追加 logs.* server block**

```nginx
# 容器日志 - Dozzle
server {
    listen 443 ssl;
    http2 on;
    server_name logs.${DOMAIN};

    ssl_certificate     /etc/nginx/certs/fullchain.pem;
    ssl_certificate_key /etc/nginx/certs/privkey.pem;

    auth_basic           "Logs";
    auth_basic_user_file /etc/nginx/conf.d/.htpasswd;

    proxy_set_header Host               $host;
    proxy_set_header X-Real-IP          $remote_addr;
    proxy_set_header X-Forwarded-For    $proxy_add_x_forwarded_for;
    proxy_set_header X-Forwarded-Proto  $scheme;

    location / {
        proxy_pass http://dozzle:9999;
    }
}
```

---

### Task 3: compose.yml — 新增 glances 和 dozzle 服务

**Files:**
- Modify: `compose.yml`

- [ ] **Step 1: 在 nginx 服务中增加环境变量注入**

```yaml
# nginx 服务，environment: 块中新增
    environment:
      DOMAIN: ${DOMAIN?Variable not set}
      MONITOR_USERNAME: ${MONITOR_USERNAME?Variable not set}
      MONITOR_PASSWORD: ${MONITOR_PASSWORD?Variable not set}
```

- [ ] **Step 2: 在 services 末尾（acme.sh 之后）新增 glances 服务**

```yaml
  glances:
    image: ccr.ccs.tencentyun.com/life-assistant/glances:latest-full
    restart: unless-stopped
    pid: host
    volumes:
      - /proc:/proc:ro
      - /sys:/sys:ro
      - /etc/hostname:/etc/hostname:ro
    networks:
      - default

  dozzle:
    image: ccr.ccs.tencentyun.com/life-assistant/dozzle:v8.11.0
    restart: unless-stopped
    volumes:
      - /var/run/docker.sock:/var/run/docker.sock:ro
    networks:
      - default
```

---

### Task 4: compose.ip.yml — IP 模式反代

**Files:**
- Modify: `compose.ip.yml`

- [ ] **Step 1: 在 IP 模式的 nginx 中增加 monitor 和 logs 的反代**

IP 模式下 Nginx 直接监听 80 端口，用 location 路径区分。需要在 `compose.ip.yml` 的 nginx 覆盖中新增 `volumes` 挂载一个 IP 专用配置，或者在现有结构上用路径方式处理。

不过观察现有 `compose.ip.yml`，它只是 profile disable 了 nginx + acme.sh，然后让 front 直接暴露 80 端口。IP 模式下 nginx 是被禁用的。

因此 IP 模式的监测工具访问需要另一种方式：改由 front（或直接暴露）处理。但这会引入额外的复杂度。

更务实的做法：**IP 模式下也启用 nginx**，在 IP 模式的 nginx 配置中做路径反代。

将 `compose.ip.yml` 的 nginx `profiles: ["disabled"]` 移除，改为覆盖配置：

```yaml
  nginx:
    # 移除 profiles: ["disabled"]，改为提供 IP 模式配置
    environment:
      DOMAIN: ${SERVER_IP}
    volumes:
      - certs:/etc/nginx/certs
      - acme-webroot:/var/www/acme:ro
    # 用自定义配置覆盖默认的 default.conf
```

但这样改动太大。更简单的方式：**IP 模式下直接暴露 Dozzle 和 Glances 的端口 + basic auth**。

最务实的方案：IP 模式下 glances 和 dozzle 各自 publish 端口到不同端口，通过 IP:PORT 直接访问（也带 basic auth）。但两个容器本身不支持 basic auth。

综合考虑：**IP 模式下也启用 nginx 作为反代**，这是最一致的做法。

```yaml
  nginx:
    # 移除 profiles: ["disabled"]
    ports:
      - "80:80"
    environment:
      DOMAIN: ${SERVER_IP}
    volumes:
      - certs:/etc/nginx/certs
      - acme-webroot:/var/www/acme:ro
```

同时需要提供一个 IP 模式专用的 nginx 配置模板。但这不是最佳路径。

**更简洁的方案：IP 模式下直接用 nginx 容器反代，但暴露在 80 端口，用路径 `/monitor` 和 `/logs` 访问。**

在 `compose.ip.yml` 中：

```yaml
  nginx:
    profiles: ["disabled"]  # 保持禁用

  # IP 模式下用独立的轻量 nginx 做 monitor/logs 反代
  tools-proxy:
    image: nginx:alpine
    profiles: ["disabled"]
    restart: unless-stopped
    ports:
      - "8080:80"
    volumes:
      - ./backend/nginx/tools-ip.conf:/etc/nginx/conf.d/default.conf:ro
    networks:
      - default
    depends_on:
      - glances
      - dozzle
```

但这个又引入了新镜像。过于复杂。

实际思考：用户使用 IP 模式时，大概率是不方便配域名时的临时方案。而 Glances 和 Dozzle 是运维工具，在临时场景下直接用 IP 加端口访问也可以接受。

**最终决定：IP 模式下，用两个容器自带的端口暴露 + 没有认证（因为在局域网或临时环境）。** 如果用户有安全性要求，可以再加固。

等一下，让我重新思考。用户说的是"管理账号便于登录的安全性"，说明安全性是关注点。IP 模式也应该有 basic auth。

最简洁的做法：**在 `compose.ip.yml` 中启用 nginx，提供一个简化的 nginx 配置文件做路径反代 + basic auth。**

```yaml
  nginx:
    # 不 disable，而是覆盖配置
    environment:
      DOMAIN: ${SERVER_IP}
      MONITOR_USERNAME: ${MONITOR_USERNAME?Variable not set}
      MONITOR_PASSWORD: ${MONITOR_PASSWORD?Variable not set}
    ports:
      - "80:80"
      # 不暴露 443（无证书）
```

同时需要一个 IP 模式的 nginx 配置模板 `backend/nginx/nginx.ip.conf`：

```nginx
server {
    listen 80;
    server_name _;

    auth_basic           "Monitor";
    auth_basic_user_file /etc/nginx/conf.d/.htpasswd;

    location /monitor/ {
        proxy_pass http://glances:61208/;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
    }

    location /logs/ {
        proxy_pass http://dozzle:9999;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
    }
}
```

但这需要改 Dockerfile 和 nginx.conf template 逻辑来支持 IP 模式配置。或者用卷挂载覆盖。

我觉得这个复杂度超出了 YAGNI。用户明确问的是子域名方案。IP 模式如果以后需要，可以再处理。

**务实决定：IP 模式下不做 special handling。用户如果要用 IP 模式，可以手动暴露 glances 和 dozzle 端口，或者到时候再补。**

所以 compose.ip.yml 不需要变更。

---

等等，让我重新想想。用户说"组合再部署这两个容器"，说明要完整部署。IP 模式也是部署方案之一。但如果 IP 模式也要 basic auth + 反代，那复杂度确实上去了。

我决定：**compose.ip.yml 不做变更。** 需要的话用户以后再提。

---

### Task 5: .env — 新增管理账号变量

**Files:**
- Modify: `.env`

- [ ] **Step 1: 在 .env 末尾新增 MONITOR_USERNAME 和 MONITOR_PASSWORD**

```ini
# Monitor / Logs 管理账号（Nginx Basic Auth）
MONITOR_USERNAME=admin
MONITOR_PASSWORD=<your-password>
```

填入实际密码（用户需要修改 `<your-password>` 为真实密码）。

---

### Task 6: 重新构建并推送 nginx 镜像

- [ ] **Step 1: 构建新的 nginx 镜像**

```bash
docker build -t ccr.ccs.tencentyun.com/life-assistant/lifeassistant-nginx:latest -f backend/nginx/Dockerfile backend/nginx/
```

- [ ] **Step 2: 推送 nginx 镜像**

```bash
docker push ccr.ccs.tencentyun.com/life-assistant/lifeassistant-nginx:latest
```

- [ ] **Step 3: 在服务器上部署**

```bash
# 服务器上拉取新镜像并重启
docker compose -f compose.yml pull nginx
docker compose -f compose.yml up -d nginx glances dozzle
```

- [ ] **Step 4: 验证访问**

```bash
# 验证 Glances
curl -u admin:<password> -o /dev/null -w '%{http_code}' https://monitor.life-assitant.top/
# 预期: 200

# 验证 Dozzle
curl -u admin:<password> -o /dev/null -w '%{http_code}' https://logs.life-assitant.top/
# 预期: 200

# 验证未认证被拒绝
curl -o /dev/null -w '%{http_code}' https://monitor.life-assitant.top/
# 预期: 401
```
