## ADDED Requirements

### Requirement: 排除版本控制目录
`.dockerignore` SHALL 排除 `.git/` 目录，防止 Git 历史进入 Docker 构建上下文和镜像。

#### Scenario: .git 不在构建上下文中
- **WHEN** 在 backend 目录执行 `docker build`
- **THEN** `.git` 目录不被发送到 Docker daemon，镜像中不含 Git 历史

### Requirement: 排除敏感环境文件
`.dockerignore` SHALL 排除 `.env` 和 `*.env` 文件，防止敏感配置（密钥、密码）进入镜像。

#### Scenario: 环境文件不在镜像中
- **WHEN** 构建后端镜像
- **THEN** `.env` 文件不存在于镜像的任何层中

### Requirement: 排除 Docker 相关文件
`.dockerignore` SHALL 排除 `Dockerfile*` 和 `docker-compose*` 文件，减少构建上下文体积。

#### Scenario: Docker 文件不在构建上下文中
- **WHEN** 执行 `docker build` 在 backend 目录
- **THEN** `Dockerfile` 和 `docker-compose*` 文件不被发送到构建上下文
