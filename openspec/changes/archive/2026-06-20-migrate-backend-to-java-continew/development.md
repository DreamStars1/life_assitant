# 开发环境运行

## 前置依赖

| 工具 | 版本 | 说明 |
|------|------|------|
| JDK | 17+ | BellSoft Liberica JDK 17 或 Eclipse Temurin 17 |
| Maven | 3.9+ | 构建工具 |
| MySQL | 8.4 | 本地或 Docker |
| Redis | 7.x | 可选，开发环境可跳过（Sa-Token 使用内存模式） |
| IDE | IntelliJ IDEA | 推荐，需安装 Lombok 插件 |

## 本地开发流程

```bash
# 1. 启动 MySQL（Docker 方式）
docker run -d --name mysql-dev \
  -e MYSQL_ROOT_PASSWORD=123456 \
  -e MYSQL_DATABASE=life_assistant \
  -p 3306:3306 \
  mysql:8.4

# 2. 启动 Redis（可选，无 Redis 时 Sa-Token 使用内存模式）
docker run -d --name redis-dev -p 6379:6379 redis:7-alpine

# 3. 编译（首次）
cd backend/newbackend/continew-admin
mvn clean compile -DskipTests

# 4. 启动应用（dev profile）
cd continew-server
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

## IDEA 运行配置

```
Main class:   top.lifeassistant.LifeAssistantApplication
VM options:   -Dspring.profiles.active=dev
Environment:  DB_HOST=localhost;DB_PORT=3306;DB_NAME=life_assistant;
              DB_USER=root;DB_PWD=123456;
              SECRET_KEY=<生成随机32字节>
```

首次启动后 Flyway 自动执行 `V1__init_all_tables.sql` 创建全部 5 张表。

## 开发环境配置要点

| 配置 | dev 环境值 | 说明 |
|------|----------|------|
| 服务器端口 | `8000` | `application-dev.yml` |
| 数据库 | `life_assistant` (localhost:3306) | P6Spy 关闭，直接用 MySQL Connector |
| Sa-Token 持久层 | **内存**（无 Redis 时自动降级） | 开发环境无需 Redis |
| 日志级别 | `DEBUG` (top.lifeassistant) | 完整 SQL 日志 + 请求日志 |
| API 文档 | `http://localhost:8000/doc.html` | NextDoc4j |
| 密码加密 | BCrypt | ContiNew 的 `PasswordEncoderEncryptor` |
| Flyway | 首次启动自动建表 | 后续增量迁移文件按 V2/V3... 命名 |

## 快速验证

```bash
# 健康检查
curl http://localhost:8000/api/v1/admin/health

# 注册测试用户
curl -X POST http://localhost:8000/api/v1/users/signup \
  -H "Content-Type: application/json" \
  -d '{"email":"test@example.com","password":"12345678","full_name":"测试用户"}'

# 登录获取 Token
curl -X POST http://localhost:8000/api/v1/login/access-token \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "username=test@example.com&password=12345678"
```