## Why

Docker 编排存在多个阻碍部署和 CI 的问题：前端服务缺少 Dockerfile 导致构建失败；nginx 容器因 TLS 证书缺失无法启动；CI 脚本引用不存在的服务；根目录缺少 .env 导致 compose 变量插值失败。此外 Adminer 直接暴露公网存在安全隐患。

## What Changes

- 为 front 和 admin 前端服务创建多阶段构建 Dockerfile（基于 nginx 静态托管）
- 补齐 nginx 需要的 TLS 证书挂载配置，增加自签名证书回退方案用于开发/内网
- 修正 CI workflow 中的 compose 命令，正确合并 compose.override.yml
- 在项目根目录创建 .env 文件，汇总 compose 变量插值所需的环境变量
- 从生产 nginx 配置中移除 Adminer 的公开入口（**BREAKING**: adminer.${DOMAIN} 不再可用）
- 为 nginx 的 depends_on 添加健康检查条件，避免前端未就绪时的 502 错误
- 完善 .dockerignore，排除 .git、.env、Dockerfile* 等不应进镜像的文件
- 生产 nginx 配置启用 HTTP/2
- 使用 YAML anchor 消除 prestart 和 backend 服务的环境变量重复

## Capabilities

### New Capabilities
- `frontend-dockerfiles`: 为移动端 PWA 和管理后台前端提供生产级多阶段 Dockerfile
- `nginx-tls-setup`: 确保 nginx 反向代理在有无外部证书时都能正常启动
- `compose-env-config`: 统一的 compose 环境变量管理（根目录 .env + 消除重复）
- `dockerignore-hardening`: 完善镜像构建排除规则，防止敏感文件泄露和镜像臃肿

### Modified Capabilities
<!-- 无现有 spec 需要修改 -->

## Impact

- `compose.yml`: 添加 TLS 证书卷挂载、YAML anchor 去重、nginx depends_on 健康条件
- `compose.override.yml`: 无需变更（开发覆盖已正确配置）
- `backend/nginx/nginx.conf`: 移除 Adminer server 块，启用 HTTP/2
- `backend/nginx/Dockerfile`: 确保 gettext 存在（已满足）
- `backend/.dockerignore`: 扩展排除规则
- `front/vue3-vant-mobile/Dockerfile`: **新建**
- `admin_front/vue3-element-admin/Dockerfile`: **新建**
- `.env`（根目录）: **新建**，从 backend/.env 提取 compose 插值变量
- `.github/workflows/ci.yml`: 修正 compose 命令
