# java-backend-config

## Purpose

定义多环境配置规范：使用 `application-{profile}.yml` 管理全部配置项，支持 local/staging/production 三环境。

## ADDED Requirements

### Requirement: 多环境配置

系统 SHALL 支持 `local`、`staging`、`production` 三个环境 profile，各环境配置项覆盖通用配置。

#### Scenario: 本地环境启动

- **WHEN** 以 `spring.profiles.active=local` 启动
- **THEN** 加载 `application.yml` + `application-local.yml`

#### Scenario: 生产环境启动

- **WHEN** 以 `spring.profiles.active=production` 启动
- **THEN** 生产配置强制检查 SECRET_KEY 和 MYSQL_PASSWORD 不允许使用默认值

### Requirement: 所有配置项映射

系统 SHALL 提供与本项目需求一致的配置项，通过 `@ConfigurationProperties` 或 `@Value` 注入。

配置项 SHALL 包括:
- `API_V1_STR`: `/api/v1`
- `SECRET_KEY`: JWT 签名密钥
- `ACCESS_TOKEN_EXPIRE_MINUTES`: Access Token 有效期（默认 11520 分钟）
- `REFRESH_TOKEN_EXPIRE_MINUTES`: Refresh Token 有效期（默认 10080 分钟）
- `FRONTEND_HOST`: 前端地址
- `MYSQL_*`: 数据库连接参数
- `SMTP_*`: 邮件服务参数
- `REDIS_URL`: Redis 连接地址
- `VAPID_*`: Web Push VAPID 密钥
- `ILINK_*`: iLink 微信 Bot 配置
- `DEEPSEEK_*`: DeepSeek LLM 配置
- `LLM_MONTHLY_BUDGET`: LLM 月度预算

#### Scenario: 读取数据库配置

- **WHEN** 应用启动
- **THEN** 从 `application.yml` 读取 `spring.datasource.url`（等价于原 `SQLALCHEMY_DATABASE_URI`）

### Requirement: CORS 配置

系统 SHALL 支持逗号分隔的允许域名列表，并与 FRONTEND_HOST 自动合并。

#### Scenario: CORS 域名解析

- **WHEN** 配置 `BACKEND_CORS_ORIGINS: http://a.com,http://b.com` 和 `FRONTEND_HOST: http://local:5173`
- **THEN** CORS 过滤器允许 `["http://a.com", "http://b.com", "http://local:5173"]`
