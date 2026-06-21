## 1. 依赖更新

- [x] 1.1 在 `backend/pyproject.toml` 中移除 `psycopg[binary]`，添加 `pymysql[rsa]`
- [x] 1.2 运行 `uv lock` 更新锁定文件

## 2. 环境变量与配置

- [x] 2.1 将 `backend/.env` 中的 `POSTGRES_*` 变量重命名为 `MYSQL_*`：
      `POSTGRES_SERVER` → `MYSQL_SERVER`、`POSTGRES_PORT` → `MYSQL_PORT`、
      `POSTGRES_DB` → `MYSQL_DATABASE`、`POSTGRES_USER` → `MYSQL_USER`、
      `POSTGRES_PASSWORD` → `MYSQL_PASSWORD`
- [x] 2.2 修改 `backend/app/core/config.py`：将 `POSTGRES_*` 字段重命名为 `MYSQL_*`，
      将 `PostgresDsn` 替换为通用字符串，
      scheme 从 `postgresql+psycopg` 改为 `mysql+pymysql`

## 3. Docker Compose 配置

- [x] 3.1 修改 `backend/compose.yml`：`db` 服务镜像从 `postgres:18` 改为 `mysql:8.4`，
      更新健康检查命令为 `mysqladmin ping`，更新环境变量为 MySQL 格式，
      更新 `prestart` 和 `backend` 服务传入的环境变量
- [x] 3.2 修改 `backend/compose.override.yml`：`db` 服务端口映射从 `5432:5432` 改为 `33061:3306`

## 4. 迁移重置

- [x] 4.1 删除 `backend/app/alembic/versions/` 下所有现有迁移文件
- [x] 4.2 手动编写初始迁移文件 `a001_init_mysql_tables.py`（Docker 不可用时生成）
- [ ] 4.3 启动 MySQL 容器后运行 `alembic upgrade head` 验证迁移执行

## 5. CI 更新

- [x] 5.1 CI 通过 `.env` 文件读取环境变量，已随 `.env` 更新自动适配 MySQL

## 6. 验证（需 Docker 环境）

- [ ] 6.1 启动完整服务栈：`docker compose up -d`
- [ ] 6.2 确认健康检查 `/api/v1/utils/health-check/` 返回 200
- [ ] 6.3 确认可正常创建用户和条目
- [ ] 6.4 运行 `uv run pytest` 确认测试全部通过
