## ADDED Requirements

### Requirement: compose.override.yml 清理
`compose.override.yml` SHALL 删除所有 Python 专属配置，包括 `mailcatcher` 服务、`fastapi run` 命令、`.venv`/`pyproject.toml` 的 watch 路径。

#### Scenario: mailcatcher 服务已移除
- **WHEN** 检查 `compose.override.yml`
- **THEN** `mailcatcher` 服务定义不存在

#### Scenario: fastapi 命令已移除
- **WHEN** 检查 `compose.override.yml` 中 backend 服务的 command 字段
- **THEN** 不存在 `fastapi run --reload "app/main.py"`

#### Scenario: Python watch 路径已移除
- **WHEN** 检查 `compose.override.yml` 中 develop.watch.ignore
- **THEN** 不存在 `.venv` 路径引用

### Requirement: compose.yml 适配 Java 栈
`compose.yml` SHALL 将 health check 路径从 FastAPI 风格（`/api/v1/utils/health-check/`）改为 Spring Boot Actuator 风格，并移除 `prestart` Python 服务。

#### Scenario: health check 路径修正
- **WHEN** 检查 `compose.yml` backend 服务的 healthcheck
- **THEN** test 命令使用 Spring Boot Actuator 端点（如 `/actuator/health`）

#### Scenario: prestart 服务已移除
- **WHEN** 检查 `compose.yml`
- **THEN** `prestart` 服务定义不存在
