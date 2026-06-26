# 域名迁移设计：life-assitant.top（仅 app + api）

## 背景

当前 Life Assistant 项目以 **IP 直连模式** 部署在腾讯云服务器 `134.175.67.104` 上，使用 `compose.ip.yml` 覆盖主 `compose.yml`，禁用 nginx 反向代理、acme.sh SSL 证书和 adminer 服务，前端直接将端口 80 暴露到宿主机。

项目已有自有域名 `life-assitant.top`，且 `.env` 中 `DOMAIN=life-assitant.top` 已配置。主 `compose.yml` 已内置完整的域名 + HTTPS + ACME 自动证书架构，仅因当前使用了 IP 模式覆盖而未生效。

此设计将迁移到域名模式，初期仅暴露两个子域名：`app.life-assitant.top`（用户前端）和 `api.life-assitant.top`（后端 API）。`adminer.${DOMAIN}` 暂不对外暴露。

## 架构

```
用户
 │
 ├── https://app.life-assitant.top ──→ nginx ──→ front:80 (PWA 静态文件)
 │                                     │
 │                                     └──→ backend:8000 (/api/)
 │
 └── https://api.life-assitant.top ──→ nginx ──→ backend:8000
 │
 nginx 同时处理：
   ─ HTTP 80 → 301 重定向到 HTTPS
   ─ /.well-known/acme-challenge/ → 放行给 acme.sh（webroot 验证）
```

## 变更清单

### 1. Nginx 配置 (`backend/nginx/nginx.conf`)

- HTTP 80 block：`server_name` 中去掉 `adminer.${DOMAIN}`
- 删除整个 adminer 的 HTTPS server block
- 其余不动（api 和 app 的配置直接可用）

### 2. `.env` 文件

`DOMAIN=life-assitant.top` 和 `FIRST_SUPERUSER=admin@life-assitant.top` 已正确，无需再改。
新增 `DOCKER_IMAGE_NGINX=ccr.ccs.tencentyun.com/life-assistant/lifeassistant-nginx`。

### 3. `compose.yml`

- nginx 服务新增 `image: '${DOCKER_IMAGE_NGINX:-nginx}:${TAG:-latest}'`
- adminer 服务新增 `profiles: ["disabled"]`，默认不启动
- nginx 的 `depends_on` 去掉 adminer

### 4. `build-and-push.bat`

新增 nginx 构建推送步骤（3/6 build + 6/6 push），一次 `build-and-push.bat` 现在推送三个镜像：backend、frontend、nginx。

### 5. DNS 配置（腾讯云）

| 记录类型 | 主机记录 | 记录值 |
|---------|---------|-------|
| A | `api` | `134.175.67.104` |
| A | `app` | `134.175.67.104` |

已就绪。

### 6. `.env` 文件

无需改动。`DOMAIN=life-assitant.top` 已是正确值。

### 7. 部署命令

```
# 之前（IP 模式）
docker compose -f compose.yml -f compose.ip.yml up -d

# 之后（域名模式）
docker compose up -d
```

### 5. 首次 SSL 证书签发自举

nginx 的 HTTPS server block 引用了 `fullchain.pem` 和 `privkey.pem`，但第一次启动时这些文件不存在，nginx 会启动失败。acme.sh 需要 nginx 运行才能通过 webroot 模式完成验证。此为 **鸡和蛋问题**，需手动解决首次签发：

**自举步骤**：

```bash
# 1. 用临时容器往 certs volume 写入自签名证书，让 nginx 能启动
# 注意：docker compose 会自动将项目名做前缀，volume 名为 life_assistant_certs
docker run --rm -v life_assistant_certs:/certs alpine:3.19 sh -c "
  apk add --no-cache openssl
  openssl req -x509 -nodes -days 1 -newkey rsa:2048 \
    -keyout /certs/privkey.pem \
    -out /certs/fullchain.pem \
    -subj '/CN=life-assitant.top'
"
# ponytail: 自签名仅用于临时启动 nginx，不会被浏览器信任，但 acme.sh 需要 nginx 能响应 challenge

# 2. 启动所有服务
docker compose up -d

# 3. 等 nginx 就绪后，手动触发 acme.sh 签发正式证书
docker exec acme.sh acme.sh --issue \
  -d api.life-assitant.top \
  -d app.life-assitant.top \
  --webroot /var/www/acme \
  --cert-home /certs \
  --key-file /certs/privkey.pem \
  --fullchain-file /certs/fullchain.pem \
  --reloadcmd "nginx -s reload"

# 证书签发后 nginx 自动重载，后续 acme.sh daemon 自动续签
```

> `life_assistant_certs` 是 Docker 自动生成的 volume 名（`{compose_project}_{volume_name}`）。如果项目名不同，先用 `docker volume ls | grep certs` 确认正确名称。

> 首次签发后，acme.sh daemon 会负责后续自动续签，无需再次手动干预。

## 迁移步骤

1. 修改 `backend/nginx/nginx.conf`（去掉 adminer）
2. 本地执行 `build-and-push.bat`，将 backend + frontend + nginx 三个镜像推送到 TCR
3. 在服务器上：
   `docker compose -f compose.yml -f compose.ip.yml down` 停掉当前 IP 模式
4. 生成临时自签名证书（见上）
5. `docker compose pull nginx && docker compose up -d` 拉取新镜像并启动域名模式
6. 手动触发 acme.sh 签发正式证书（见上）
7. 验证：
   - `https://app.life-assitant.top` — 前端正常加载
   - `https://api.life-assitant.top/actuator/health` — 后端健康检查
   - nginx 日志无证书报错

## 回滚方案

```bash
docker compose down
docker compose -f compose.yml -f compose.ip.yml up -d
```

30 秒内切回 IP 模式，数据无影响。

## 风险

- **acme.sh 首次申请失败**：DNS 可能尚未生效（TTL 缓存），或端口 80/443 被占用。按步骤先 `down` 再 `up -d` 可避免端口冲突。
- **Let's Encrypt 速率限制**：同一域名每周最多 5 次重复签发。首次失败后不要频繁重试。
- **迁移窗口的短暂不可用**：`down` 到 `up -d` 之间有几十秒间隙，PWA 离线缓存可缓解用户体验。
