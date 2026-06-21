## ADDED Requirements

### Requirement: Git 仓库初始化
系统 SHALL 在项目根目录初始化 Git 仓库，配置 `.gitignore` 以确保敏感文件和构建产物不被跟踪。

#### Scenario: 敏感配置不入库
- **WHEN** 执行 `git add .` 后检查暂存区
- **THEN** `.env`、`.env.*` 文件不在暂存区中
- **THEN** `.env.example` 模板文件在暂存区中

#### Scenario: 构建产物不入库
- **WHEN** 执行 `git add .` 后检查暂存区
- **THEN** `node_modules/`、`target/`、`*.class`、`*.jar` 目录/文件不在暂存区中

#### Scenario: 日志不入库
- **WHEN** 执行 `git add .` 后检查暂存区
- **THEN** `*.log` 文件及 `logs/` 目录不在暂存区中

#### Scenario: 证书和密钥不入库
- **WHEN** 执行 `git add .` 后检查暂存区
- **THEN** `certs/` 目录下所有文件不在暂存区中
- **THEN** `*.pem`、`*.key`、`*.p12`、`*.jks`、`*.keystore` 文件不在暂存区中

#### Scenario: IDE 缓存不入库
- **WHEN** 执行 `git add .` 后检查暂存区
- **THEN** `.idea/`、`.metals/`、`.bloop/`、`*.iml` 目录/文件不在暂存区中
