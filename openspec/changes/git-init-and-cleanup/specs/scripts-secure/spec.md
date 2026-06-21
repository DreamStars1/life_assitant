## ADDED Requirements

### Requirement: 脚本禁止硬编码密钥
项目中的所有启动脚本 SHALL 从环境变量或 `.env` 文件读取数据库密码、Redis 密码等敏感配置，不得在脚本中硬编码。

#### Scenario: start-backend.bat 无硬编码密码
- **WHEN** 检查 `start-backend.bat` 内容
- **THEN** 文件中不存在 `rootpassword`、`123456` 等硬编码密码
- **THEN** 数据库和 Redis 连接参数从环境变量读取

#### Scenario: restart-backend.ps1 无硬编码密码
- **WHEN** 检查 `.cursor/skills/dev-restart/scripts/restart-backend.ps1` 内容
- **THEN** 文件中不存在硬编码的数据库密码和 Redis 密码
- **THEN** Java 路径使用 `$env:JAVA_HOME` 或相对路径，不硬编码 `D:\soft\Java\jdk-17.0.2`

#### Scenario: start-backend.ps1 无硬编码密码
- **WHEN** 检查 `backend/lifeassistant/lifeassistant-server/start-backend.ps1` 内容
- **THEN** 文件中不存在硬编码密码
- **THEN** 路径不硬编码为特定机器的绝对路径

### Requirement: 死 Python 脚本删除
项目 SHALL 删除所有 Python 工具链脚本（`backend/scripts/format.sh`、`lint.sh`、`test.sh`、`tests-start.sh`、`prestart.sh`），因为这些脚本引用不存在的 `app/`、`pyproject.toml`、`uv` 等 Python 资源。

#### Scenario: backend/scripts 目录清空
- **WHEN** 检查 `backend/scripts/` 目录
- **THEN** 所有 `.sh` 脚本已删除
