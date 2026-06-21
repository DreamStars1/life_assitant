## Context

后端当前使用 PostgreSQL 18，通过 `psycopg[binary]` 驱动连接。项目使用 SQLModel（基于 SQLAlchemy）作为 ORM，Alembic 管理迁移。Docker Compose 编排了包括 `db`（PostgreSQL 18）在内的 4 个服务。

按项目规划（`docs/step-by-step-guide.md`），数据库应使用 MySQL。目前处于早期开发阶段，无生产数据，是切换的最佳时机。

## Goals / Non-Goals

**Goals：**
- 将数据库引擎从 PostgreSQL 18 切换为 MySQL 8.4
- Docker 内部服务名保持为 `db`，避免服务间依赖关系改动
- 主机暴露端口改为 `33061`（避开 `3306`/`5432` 等常用端口）
- 使用纯 Python MySQL 驱动，最小化容器体积
- 清理并重置旧迁移文件，生成 MySQL 兼容的初始迁移

**Non-Goals：**
- 不迁移现有 PostgreSQL 数据（当前无生产数据，dev 数据可重新创建）
- 不改动应用层数据模型（SQLModel 模型与数据库无关）
- 不引入异步数据库驱动（项目当前使用同步 Session）

## Decisions

### 1. 数据库镜像：`mysql:8.4`

- MySQL 8.4 是 MySQL 8.x 系列的最新 LTS 版本（8.4.x），支持到 2032 年
- Docker 官方镜像成熟稳定
- `ponytail:` 使用 `mysql:8.4` 而非 `mysql:8` tag，锁定大版本避免意外升级

### 2. 主机端口：`33061`

- `3306`（MySQL 默认）和 `5432`（PostgreSQL 默认）均属常用端口，易冲突
- `33061` 在 IANA 注册端口范围之外，极少被占用
- 容器内部仍使用 `3306`，对外映射为 `33061`
- 其他服务通过 Docker 内部网络直连 `db:3306`，不受端口映射影响

### 3. Python 驱动：`pymysql[rsa]`

- `pymysql` 是纯 Python 实现的 MySQL 驱动，无需编译 C 扩展
- MySQL 8.4 默认认证插件为 `caching_sha2_password`，`[rsa]` extra 提供 RSA 公钥交换支持
- SQLAlchemy 原生支持 `mysql+pymysql://` scheme
- 替代方案 `mysql-connector-python` 更重量级，容器体积更大，无优势

### 4. 环境变量重命名：`POSTGRES_*` → `MYSQL_*`

- 保持命名含义清晰，移除 PostgreSQL 痕迹
- `POSTGRES_SERVER` → `MYSQL_SERVER`
- `POSTGRES_PORT` → `MYSQL_PORT`
- `POSTGRES_DB` → `MYSQL_DATABASE`
- `POSTGRES_USER` → `MYSQL_USER`
- `POSTGRES_PASSWORD` → `MYSQL_PASSWORD`
- `ponytail:` 将 `POSTGRES_DB` 改为 `MYSQL_DATABASE`（MySQL 惯例用 `DATABASE` 而非 `DB`）

### 5. 数据持久化卷名：`app-db-data`

保持不变，节省 one rename 的工作量，卷名不暴露数据库类型。

### 6. MySQL 健康检查命令

PostgreSQL 使用 `pg_isready`，MySQL 改用 `mysqladmin ping -u${MYSQL_USER} -p${MYSQL_PASSWORD}`。

### 7. 迁移文件重置

- 删除 `app/alembic/versions/` 下现有的 5 个 PostgreSQL 迁移文件
- 基于当前 SQLModel 模型自动生成一个新的初始迁移
- `env.py` 无需改动（已通过 `settings.SQLALCHEMY_DATABASE_URI` 动态获取 URL）

### 8. MySQL 8.4 的 Docker 环境变量

MySQL Docker 镜像使用与 PostgreSQL 不同的环境变量名：
- `MYSQL_ROOT_PASSWORD`（必须，root 用户密码）
- `MYSQL_DATABASE`（启动时创建的数据库名）
- `MYSQL_USER`（创建的普通用户）
- `MYSQL_PASSWORD`（普通用户密码）
- `MYSQL_ROOT_HOST`（可选，允许 root 远程连接，默认禁止）

## Risks / Trade-offs

| 风险 | 缓解措施 |
|------|---------|
| MySQL 8.4 默认 `sql_mode` 包含 `ONLY_FULL_GROUP_BY`，可能影响查询 | 应用层查询使用 SQLAlchemy ORM，不依赖 MySQL 特定 SQL 模式；如遇问题可在镜像 `command` 中设置 `--sql-mode` |
| `pymysql` 不支持异步，未来若切异步驱动需替换 | 当前无需异步；升级路径为替换 `pymysql` 为 `asyncmy` 并修改引擎创建方式 |
| UUID 字段在 MySQL 中无原生 UUID 类型，SQLModel 默认映射为 `CHAR(32)` 或 `BINARY(16)` | SQLModel/SQLAlchemy 会自动处理映射，行为透明 |
| `DateTime(timezone=True)` 在 MySQL 中对应 `DATETIME` 而非 `TIMESTAMPTZ`，时区信息可能以字符串存储 | 当前数据通过 Pydantic 序列化，行为一致；`ponytail:` 已知差异但当前无实际影响 |
