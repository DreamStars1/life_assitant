## ADDED Requirements

### Requirement: 根目录 .env 提供 compose 变量插值
项目根目录 SHALL 包含 `.env` 文件，提供 compose.yml 中 `${VARIABLE}` 语法所需的所有变量。

#### Scenario: compose 变量插值成功
- **WHEN** 执行 `docker compose config`（在 compose 文件所在目录）
- **THEN** 所有 `${VARIABLE}` 被替换为实际值，无 "variable not set" 错误

### Requirement: YAML Anchor 消除环境变量重复
compose.yml SHALL 使用 YAML anchor (`x-backend-env: &backend-env`) 定义共享环境变量，prestart 和 backend 服务通过 `<<: *backend-env` 引用。

#### Scenario: prestart 和 backend 环境变量一致
- **WHEN** 执行 `docker compose config`
- **THEN** prestart 和 backend 的 environment 块内容完全一致（共用同一 anchor 定义）

### Requirement: mailcatcher 保持在 compose.override.yml 中
mailcatcher 服务 SHALL 保持在 `compose.override.yml` 中定义，供开发和 CI 使用。Docker Compose v2 自动合 override 文件，CI 通过 `-f ../compose.yml` 引用基文件即可自动包含 mailcatcher。

#### Scenario: 开发环境自动包含 mailcatcher
- **WHEN** 执行 `docker compose up -d`（开发环境，override 自动合并）
- **THEN** mailcatcher 服务随 compose 启动

#### Scenario: CI 自动包含 mailcatcher
- **WHEN** GitHub Actions 执行 `docker compose -f ../compose.yml up -d db mailcatcher`
- **THEN** compose.override.yml 自动合并，mailcatcher 服务正常启动

### Requirement: CI compose 命令使用 -f 指定基文件
`.github/workflows/ci.yml` SHALL 使用 `docker compose -f ../compose.yml`（因 working-directory 为 backend/），compose 自动合并同目录下的 compose.override.yml。

#### Scenario: CI pipeline 通过
- **WHEN** GitHub Actions 执行 CI workflow 的 docker compose 步骤
- **THEN** db 和 mailcatcher 服务正常启动，测试可执行
