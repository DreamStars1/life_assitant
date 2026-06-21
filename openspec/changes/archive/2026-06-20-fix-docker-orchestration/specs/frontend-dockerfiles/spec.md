## ADDED Requirements

### Requirement: Front PWA Docker 构建
系统 SHALL 为 `front/vue3-vant-mobile` 提供生产级多阶段 Dockerfile，使用 node 镜像构建静态产物，nginx:alpine 镜像托管。

#### Scenario: 构建成功
- **WHEN** 执行 `docker compose -f compose.yml build front`
- **THEN** 构建无错误完成，生成可运行的镜像

#### Scenario: 容器启动即服务
- **WHEN** nginx 反向代理向 `front:80` 发送 HTTP 请求
- **THEN** 返回 PWA 首页 HTML，状态码 200

### Requirement: Admin 前端 Docker 构建
系统 SHALL 为 `admin_front/vue3-element-admin` 提供生产级多阶段 Dockerfile，使用 node 镜像构建静态产物，nginx:alpine 镜像托管。

#### Scenario: 构建成功
- **WHEN** 执行 `docker compose -f compose.yml build admin`
- **THEN** 构建无错误完成，生成可运行的镜像

#### Scenario: 容器启动即服务
- **WHEN** nginx 反向代理向 `admin:80` 发送 HTTP 请求
- **THEN** 返回管理后台首页 HTML，状态码 200
