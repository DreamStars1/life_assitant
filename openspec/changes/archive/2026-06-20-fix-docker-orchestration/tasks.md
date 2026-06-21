## 1. 前端 Dockerfile

- [x] 1.1 创建 `front/vue3-vant-mobile/Dockerfile`：多阶段构建（node 构建 + nginx:alpine 托管），产物输出到 `/usr/share/nginx/html`
- [x] 1.2 创建 `admin_front/vue3-element-admin/Dockerfile`：多阶段构建（node 构建 + nginx:alpine 托管），产物输出到 `/usr/share/nginx/html`
- [ ] 1.3 验证：`docker compose -f compose.yml build front admin` 无错误完成

## 2. nginx TLS 与安全加固

- [x] 2.1 创建 `backend/nginx/gen-certs.sh` entrypoint 脚本：检测 `/etc/nginx/certs/fullchain.pem` 是否存在，不存在则 `openssl req -x509 -nodes -days 365 -newkey rsa:2048` 生成自签名证书
- [x] 2.2 修改 `backend/nginx/Dockerfile`：COPY `gen-certs.sh` 并设为 entrypoint，在 `envsubst` 写配置前先执行
- [x] 2.3 修改 `compose.yml` 的 nginx 服务：添加 `volumes: - ./certs:/etc/nginx/certs:ro`
- [x] 2.4 修改 `backend/nginx/nginx.conf`：移除 `server_name adminer.${DOMAIN}` 的 server 块，所有 `listen 443 ssl` 改为 `listen 443 ssl http2`

## 3. 环境变量与 compose 配置

- [x] 3.1 创建根目录 `.env`：从 `backend/.env` 提取 compose 变量插值所需的变量，不包含应用级变量
- [x] 3.2 修改 `compose.yml`：在文件顶部添加 `x-backend-env: &backend-env` YAML anchor，prestart 和 backend 的 environment 块替换为 `<<: *backend-env`
- [x] 3.3 mailcatcher 保持在 `compose.override.yml` 中：Compose v2 自动合并 override，CI 的 `-f ../compose.yml` 即可包含 mailcatcher，无需 profiles

## 4. CI 工作流修复

- [x] 4.1 保持 `.github/workflows/ci.yml` 使用 `docker compose -f ../compose.yml`：compose v2 自动合并同目录下的 compose.override.yml，mailcatcher 可用
- [x] 4.2 确认 CI 的 down 和 up 命令使用一致的 `-f ../compose.yml` 参数

## 5. nginx depends_on 健康条件

- [x] 5.1 修改 `compose.yml` nginx 的 depends_on：backend 添加 `condition: service_healthy`，front/admin/adminer 添加 `condition: service_started`

## 6. .dockerignore 加固

- [x] 6.1 修改 `backend/.dockerignore`：追加 `.git`、`.env`、`*.env`、`Dockerfile*`、`docker-compose*`、`.cursor/`、`.github/`、`openspec/`
- [ ] 6.2 验证：`docker build -t test --no-cache backend/` 后检查镜像体积，确认 .env 等文件不在镜像中

## 7. 最终验证

- [x] 7.1 `docker compose config` 无错误（变量插值完整）
- [ ] 7.2 `docker compose build` 所有服务构建成功
- [ ] 7.3 `docker compose up -d` 所有容器启动，nginx 无 crash
- [ ] 7.4 `curl -k https://api.localhost/api/v1/utils/health-check/` 返回 200
