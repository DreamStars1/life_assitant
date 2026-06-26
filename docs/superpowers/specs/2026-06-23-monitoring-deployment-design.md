# Glances + Dozzle 容器部署设计

## 背景

已在生产服务器上通过 Docker Compose 运行 Life Assistant（backend + front + nginx + acme.sh + MySQL + Redis），通过 Nginx 反向代理 + HTTPS（Let's Encrypt）提供服务。需要再部署 Glances（系统监控）和 Dozzle（Docker 日志查看器），作为运维辅助工具。

## 目标

1. 部署 Glances 和 Dozzle 容器
2. 通过子域名 `monitor.life-assitant.top` 和 `logs.life-assitant.top` 访问
3. 使用同一个管理账号通过 Nginx auth_basic 做登录认证
4. 继续走已有的 HTTPS 证书体系（acme.sh + Nginx）
5. IP 部署模式（`compose.ip.yml`）也需要能访问

## 架构

```
                        ┌──────────────┐
                        │   Nginx      │
                        │  (HTTPS)     │
                        │  auth_basic  │
                        └──┬───────┬───┘
                           │       │
                monitor.*  │       │  logs.*
                     ┌─────▼──┐ ┌──▼──────┐
                     │Glances │ │ Dozzle  │
                     │:61208  │ │:9999    │
                     └────────┘ └─────────┘
```

- 两个容器共享 Nginx 所在 Docker 网络（`default`）
- 不对外暴露端口，仅通过 Nginx 反代访问
- Nginx 配置 `auth_basic`，两个 server block 共用同一 htpasswd 文件

## 容器配置

### Glances

| 项目 | 值 |
|------|-----|
| 镜像 | `ccr.ccs.tencentyun.com/life-assistant/glances:latest-full` |
| 运行模式 | Web 服务器（默认 -w） |
| 网络 | `default`（与现有 nginx 同一网络） |
| 端口 | 容器内 `61208`，不 publish |
| 特殊 | `pid: host`，需访问宿主机 procfs |

Volumes:

```yaml
volumes:
  - /proc:/proc:ro
  - /sys:/sys:ro
  - /etc/hostname:/etc/hostname:ro
```

### Dozzle

| 项目 | 值 |
|------|-----|
| 镜像 | `ccr.ccs.tencentyun.com/life-assistant/dozzle:v8.11.0` |
| 网络 | `default` |
| 端口 | 容器内 `9999`，不 publish |
| 说明 | 通过 Docker socket 实时读取容器日志 |

Volumes:

```yaml
volumes:
  - /var/run/docker.sock:/var/run/docker.sock:ro
```

## Nginx 配置变更

### 新增 server block：monitor

```nginx
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

### 新增 server block：logs

```nginx
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

### HTTP 80 放行

已有的 `server { listen 80; }` 块新增 `monitor.*` 和 `logs.*` 的 ACME challenge 放行：

```nginx
server {
    listen 80;
    server_name api.${DOMAIN} app.${DOMAIN} monitor.${DOMAIN} logs.${DOMAIN};
    ...
}
```

## Docker Compose 变更

### compose.yml

新增 services:

```yaml
glances:
  image: nicolargo/glances:4.3.0.8
  restart: unless-stopped
  pid: host
  volumes:
    - /proc:/proc:ro
    - /sys:/sys:ro
    - /etc/hostname:/etc/hostname:ro
  networks:
    - default

dozzle:
  image: amir20/dozzle:v8.11.0
  restart: unless-stopped
  volumes:
    - /var/run/docker.sock:/var/run/docker.sock:ro
  networks:
    - default
```

### nginx 服务变更

- 环境变量新增 `MONITOR_USERNAME` 和 `MONITOR_PASSWORD`
- Dockerfile entrypoint 增加 htpasswd 生成逻辑

### compose.ip.yml

- Glances 和 Dozzle 不做 profile disable（默认启用）
- Nginx 在 IP 模式下增加 HTTP 反代到 `glances:61208` 和 `dozzle:9999`

## Nginx Dockerfile Entrypoint 变更

当前 entrypoint 做了两件事：自签名证书 bootstrap + envsubst 模板渲染。增加第三步：

```bash
if [ ! -f /etc/nginx/conf.d/.htpasswd ]; then \
  htpasswd -cb /etc/nginx/conf.d/.htpasswd "${MONITOR_USERNAME}" "${MONITOR_PASSWORD}" && \
  echo 'Generated htpasswd'; \
fi
```

前置条件：在 Dockerfile 中安装 `apache2-utils`（或 `apache2-utils` 在 alpine 中的等价包 `apache2-utils`，实际 alpine 是 `apache2-utils` → `apache2-utils` 在 apk 中叫 `apache2-utils`，确认可用）。

## 环境变量（.env）

新增三个变量：

```ini
# Monitor / Logs 管理账号（Nginx Basic Auth）
MONITOR_USERNAME=admin
MONITOR_PASSWORD=<your-password>
```

## IP 模式兼容

IP 模式通过 `compose.ip.yml` 覆盖，新增一个 nginx server 块监听 80 端口，用路径区分：

| 路径 | 目标 |
|------|------|
| `/monitor/` | Glances |
| `/logs/` | Dozzle |

也启用 auth_basic。

## DNS 变更

需要新增两条 A 记录指向服务器 IP：

```
monitor.life-assitant.top  A  134.175.67.104
logs.life-assitant.top     A  134.175.67.104
```

## 证书

acme.sh 已在运行，下次自动续签时会自动为新域名签发证书。也可以手动执行一次签发，或等待下次定时任务。

## 文件变更清单

| 文件 | 操作 |
|------|------|
| `compose.yml` | 新增 glances、dozzle services；nginx 注入环境变量 |
| `backend/nginx/nginx.conf` | 新增 monitor.* 和 logs.* server block；HTTP 80 放行 |
| `backend/nginx/Dockerfile` | 安装 htpasswd 工具；entrypoint 增加 htpasswd 生成 |
| `compose.ip.yml` | 新增 glances/dozzle 的 nginx 反代配置 |
| `.env` | 新增 MONITOR_USERNAME、MONITOR_PASSWORD |
| `build-and-push.bat` | nginx 镜像推送已覆盖，无需变更 |

## 未涵盖 / 后续

- Glances 的磁盘或网络接口白名单配置（默认显示全部，后续需要可加）
- Dozzle 的日志过滤或保留策略（默认无限制，Docker daemon 层面处理）
- 监控告警（Glances 有阈值告警能力，但不在本次范围）
- 管理账号更换（直接修改 .env 重新部署即可）
