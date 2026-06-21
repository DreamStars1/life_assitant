## ADDED Requirements

### Requirement: MySQL 8.4 数据库服务

系统 SHALL 使用 MySQL 8.4 作为主数据库引擎，运行在 Docker 容器中。

#### Scenario: 数据库服务正常启动

- **WHEN** 执行 `docker compose up -d db`
- **THEN** `db` 服务容器基于 `mysql:8.4` 镜像启动
- **AND** 容器的健康检查通过

#### Scenario: 数据库对外端口为非常用端口

- **WHEN** 从主机（localhost）连接数据库
- **THEN** 数据库暴露在 `33061` 端口
- **AND** 容器内部仍使用标准 `3306` 端口

### Requirement: 数据库凭据配置

系统 SHALL 通过环境变量配置 MySQL 连接凭据，变量命名遵循 MySQL 惯例。

#### Scenario: 环境变量读取

- **WHEN** 设置 `MYSQL_SERVER`、`MYSQL_PORT`、`MYSQL_DATABASE`、`MYSQL_USER`、`MYSQL_PASSWORD` 环境变量
- **THEN** 后端应用通过这些变量构建正确的连接 URI

#### Scenario: Docker Compose 注入

- **WHEN** 通过 `docker compose up` 启动后端服务
- **THEN** Docker Compose 将 MySQL 环境变量注入到 `prestart` 和 `backend` 容器
- **AND** `MYSQL_SERVER` 被设置为 `db`（Docker 服务名）

### Requirement: MySQL Python 驱动

系统 SHALL 使用 `pymysql[rsa]` 作为 MySQL 数据库驱动。

#### Scenario: 连接建立

- **WHEN** 应用启动并创建数据库引擎
- **THEN** 连接 URI scheme 为 `mysql+pymysql://`
- **AND** 使用 `caching_sha2_password` 认证方式成功建立连接

### Requirement: 数据持久化

系统 SHALL 使用 Docker 命名卷持久化 MySQL 数据。

#### Scenario: 数据不因容器重启丢失

- **WHEN** `db` 容器重启或重建
- **THEN** 之前写入的数据仍然可用

### Requirement: 数据库迁移

系统 SHALL 使用 Alembic 管理 MySQL 数据库迁移。

#### Scenario: 初始迁移生成

- **WHEN** 数据库首次初始化
- **THEN** Alembic 执行初始迁移创建 `user` 和 `item` 两张表
- **AND** 所有表使用 MySQL 兼容的数据类型

#### Scenario: 迁移升级

- **WHEN** 运行 `alembic upgrade head`
- **THEN** 所有未应用的迁移按顺序执行
- **AND** 数据库 schema 与应用模型一致
