## Context

当前 compose.yml 定义了 8 个服务（db, redis, adminer, prestart, backend, front, admin, nginx），但存在多个导致构建失败或运行时崩溃的问题。项目使用 `compose.override.yml` 覆盖开发环境配置。CI 在 GitHub Actions 上运行，依赖 compose 启动服务做集成测试。nginx 作为唯一的用户入口反向代理所有 HTTP/HTTPS 流量。

## Goals / Non-Goals

**Goals:**
- 所有 `docker compose build` 命令无错误完成
- `docker compose up` 后 nginx 能正常启动并转发流量
- CI 流水线可正常运行
- Adminer 不再通过公网域名暴露
- compose 环境变量无需额外手动设置即可完成插值

**Non-Goals:**
- 不改动应用层代码（FastAPI、Vue 前端逻辑）
- 不引入新的基础设施（不换反代、不加 Traefik/Caddy）
- 不处理生产环境证书的自动签发（ACME 等）
- 不修改数据库/Redis 的版本或配置

## Decisions

### 1. 前端 Dockerfile：多阶段构建，nginx 静态托管

**选择**: 使用 `node` 镜像构建 + `nginx:alpine` 镜像托管静态文件的多阶段构建。

**备选**: 单阶段从 node 镜像用 `serve` / `http-server` 托管，或基于 `node:alpine` 运行 dev server。

**理由**: 生产环境不需要 Node.js 运行时；nginx 静态托管体积小（~20MB）、性能好。前端代码全在 `dist/` 目录中，不依赖 SSR。

### 2. TLS 证书：自签名证书 + 卷挂载

**选择**: 在 compose 中挂载 `./certs:/etc/nginx/certs:ro` 目录；若无外部证书，nginx Dockerfile 新增 `gen-certs.sh` 入口脚本在启动时检测并自动生成自签名证书。

**备选**: 移除 TLS，nginx 只用 HTTP/80；或用 compose `secrets` 管理。

**理由**: 移除 TLS 不符合安全要求。自签名证书允许开发/内网环境直接运行，生产环境只需将真实证书放入 `./certs` 目录即可。entrypoint 脚本只在证书缺失时生成，不覆盖已有文件。

### 3. Adminer：仅在内网/dev 暴露

**选择**: 生产 nginx.conf 中移除 Adminer server 块；在 `compose.override.yml` 的开发配置中单独暴露 8080 端口。

**理由**: Adminer 已经在 compose.override.yml 中做了 `ports: "8080:8080"` 暴露，开发可直接访问。生产环境不需要数据库 Web 面板，也不应该在公网出现。

### 4. CI 修复：使用 compose 默认配置合并

**选择**: 将 CI 命令从 `docker compose -f ../compose.yml up -d db mailcatcher` 改为 `docker compose --profile ci up -d`，将 `mailcatcher` 移到 `compose.yml` 并配 `profiles: [ci]`，或直接在 override 里用并让 compose 自动合并。

**实际选择**: 将 mailcatcher 移入 compose.yml 的 ci profile（不改变其行为和端口映射），移除 CI 中的 `-f ../compose.yml` 参数，让 compose 自动加载 compose.yml + compose.override.yml。同时需要先 `docker compose down` 再 `up`。

### 5. 根目录 .env：从 backend/.env 提取 compose 插值变量

**选择**: 根目录 `.env` 只包含 compose 文件 `${VARIABLE}` 语法所需的变量（DOMAIN, TAG, MYSQL_*, SECRET_KEY 等），不重复 backend 应用级变量（SMTP_*, SENTRY_DSN 等由 backend/.env 通过 env_file 注入）。

**理由**: compose 变量插值在解析阶段发生，需要变量在 shell 环境或项目根 .env 中。分离关注点：根 .env 管 compose，backend/.env 管容器内应用。

### 6. YAML Anchor 消除环境变量重复

**选择**: 在 compose.yml 顶部定义 `x-backend-env: &backend-env` YAML anchor，prestart 和 backend 通过 `<<: *backend-env` 引用。

**理由**: 当前两个服务有 ~20 行完全相同的 environment 块，anchor 是 YAML 原生功能，不引入新工具，diff 清晰。

### 7. nginx depends_on 添加健康条件

**选择**: backend 添加 `condition: service_healthy`（已有 healthcheck），front/admin 添加 `condition: service_started`。

**理由**: front/admin 是静态 nginx 镜像，无 healthcheck 端点，`service_started` 足够（nginx 启动后立即可用）。backend 有 `/api/v1/utils/health-check/` healthcheck，用 healthy 更可靠。

## Risks / Trade-offs

- **自签名证书** → 浏览器会显示警告，开发需手动接受。生产需替换为真实证书。入口脚本不会覆盖已有文件，安全。
- **Adminer 从 nginx 移除** → 开发者需直接用 `localhost:8080` 访问，不再是 `adminer.localhost`。已在 override 中暴露端口，不影响开发体验。
- **根 .env 新增** → 可能与 backend/.env 变量值不一致。两个文件的变量不应重叠（根 .env 只管 compose 插值），需在实现时确保这一点。
- **mailcatcher 移入 compose.yml** → 通过 `profiles` 控制只在 CI 显式激活时启动，不影响本地开发。本地开发仍通过 compose.override.yml 的默认包含启动。
