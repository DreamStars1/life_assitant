## Why

后端当前使用 PostgreSQL 18 作为数据库，但项目文档 (`docs/step-by-step-guide.md`) 已有"从 MySQL 迁移"的意图。考虑到实际部署环境和生态一致性，切换为 MySQL 8.4 更符合项目需求。同时将 Docker 暴露端口改为非常用端口，避免与主机上可能运行的其他数据库服务端口冲突。

## What Changes

- **数据库引擎切换**：从 PostgreSQL 18 切换为 MySQL 8.4
- **Docker 服务镜像**：`db` 服务的镜像从 `postgres:18` 改为 `mysql:8.4`
- **数据库端口**：暴露端口从默认的 `5432`/`3306` 改为非常用端口 `33061`
- **Python 驱动**：从 `psycopg[binary]`（PostgreSQL 驱动）切换为 `pymysql` 或 `asyncmy`（MySQL 驱动）
- **连接 URI 构建**：`config.py` 中的 `SQLALCHEMY_DATABASE_URI` 从 `postgresql+psycopg://` 改为 `mysql+pymysql://`
- **环境变量**：`POSTGRES_*` 相关变量重命名为 `MYSQL_*`
- **Alembic 迁移**：基于 MySQL 8.4 重新生成初始迁移（不向后兼容，当前无生产数据）
- **BREAKING**：当前 PostgreSQL 数据库中的数据无法自动迁移，需要手动导出导入

## Capabilities

### New Capabilities
- `mysql-database`: MySQL 8.4 数据库服务的部署与连接配置

### Modified Capabilities

无（项目尚在早期阶段，无已发布的 spec）。

## Impact

- **依赖变更**：`pyproject.toml` 移除 `psycopg[binary]`，添加 `pymysql[rsa]`（MySQL 驱动）
- **配置文件**：`.env`、`compose.yml`、`compose.override.yml`、`config.py`、`db.py` 均需修改
- **Docker 端口**：本地开发时数据库暴露端口改为 `33061`
- **迁移文件**：现有 5 个 Alembic 迁移文件基于 PostgreSQL，需重置为 MySQL 版本
- **CI/CD**：GitHub Actions 中的服务容器也需调整为 MySQL
