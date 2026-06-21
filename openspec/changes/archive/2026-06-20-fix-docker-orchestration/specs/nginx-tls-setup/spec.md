## ADDED Requirements

### Requirement: nginx 容器启动不依赖外部证书
nginx 容器 SHALL 在 `/etc/nginx/certs/` 目录缺少 TLS 证书时自动生成自签名证书，确保容器始终可启动。

#### Scenario: 证书缺失时自动生成
- **WHEN** `/etc/nginx/certs/fullchain.pem` 不存在且容器启动
- **THEN** entrypoint 脚本自动生成自签名证书，nginx 正常启动

#### Scenario: 已有证书不被覆盖
- **WHEN** `/etc/nginx/certs/fullchain.pem` 已存在且容器启动
- **THEN** 现有证书保持不变，nginx 使用已有证书启动

### Requirement: 证书目录挂载
compose.yml SHALL 将宿主机 `./certs/` 目录挂载到 nginx 容器的 `/etc/nginx/certs/`，支持运维直接放置真实证书。

#### Scenario: 证书卷挂载生效
- **WHEN** 宿主机 `./certs/` 目录包含 `fullchain.pem` 和 `privkey.pem`
- **THEN** nginx 容器使用宿主机提供的证书启动

### Requirement: 生产环境 Adminer 不通过 nginx 公开
生产 nginx 配置 SHALL NOT 包含 `server_name adminer.${DOMAIN}` 的 server 块。

#### Scenario: 公网域名不可访问 Adminer
- **WHEN** 通过 `https://adminer.${DOMAIN}` 发送请求
- **THEN** nginx 返回 404 或不匹配任何 server 块

### Requirement: nginx 启用 HTTP/2
nginx 生产配置 SHALL 在 SSL server 块中使用 `http2` 选项。

#### Scenario: HTTP/2 可用
- **WHEN** 客户端通过 HTTPS 连接 nginx
- **THEN** 连接协商为 HTTP/2 协议
