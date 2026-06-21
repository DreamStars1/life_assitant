## Why

项目尚未纳入 Git 版本控制，且代码库中存在大量从 Python FastAPI 模板残留的文件——包括写死密码的脚本、完全不可用的 CI 配置、死 Python 工具链、以及不完整的 `.gitignore`。在首次提交前必须清理这些隐患，防止敏感信息泄露到 Git 历史。

## What Changes

- 初始化 Git 仓库并配置 `.gitignore`（覆盖 `.env`、`certs/`、`node_modules/`、`target/`、日志等）
- 删除 4 个死 `.cursor/rules/` 文件（`python-fastapi.mdc`、`langchain-ai.mdc`）
- 删除 5 个死 `backend/scripts/` shell 脚本（全为 Python 工具链）
- 替换 `.github/workflows/ci.yml` 为 Java Spring Boot + Vue 3 的 CI 流水线
- 修复 `compose.override.yml`：删除 Python 专属的 mailcatcher、fastapi run 命令
- 修复 `compose.yml`：health check 路径适配 Spring Boot、移除 prestart Python 服务
- 修复 3 个脚本的硬编码密码和机器特定路径（`start-backend.bat`、`restart-backend.ps1`、`start-backend.ps1`），改为从 `.env` 读取
- 从 `backend/.env` 派生 `.env.example` 模板
- 精简 `backend/lifeassistant/README.md` 为项目真实说明
- 精简 `.pre-commit-config.yaml`：删除 3 个死 Python hook，补充 4 个有用的通用 hook

## Capabilities

### New Capabilities

- `git-repo-init`: Git 仓库初始化与 `.gitignore` 覆盖策略
- `env-template`: `.env.example` 模板生成与敏感配置外移
- `ci-java-vue`: Java Spring Boot + Vue 3 的 CI 流水线
- `scripts-secure`: 脚本中硬编码密钥清理与 `.env` 读取改造
- `docker-cleanup`: Docker Compose 配置从 Python 栈迁移到 Java 栈
- `pre-commit-cleanup`: pre-commit hooks 从 Python 栈清理适配到 Java 栈

### Modified Capabilities

<!-- 不涉及现有 spec 的需求变更 -->

## Impact

- 涉及文件：`.gitignore`、`.env.example`（新建）、`.pre-commit-config.yaml`、`.github/workflows/ci.yml`、`compose.yml`、`compose.override.yml`、`start-backend.bat`、`backend/lifeassistant/README.md`、`backend/scripts/*.sh`（删除）、`.cursor/rules/python-fastapi.mdc`（删除）、`.cursor/rules/langchain-ai.mdc`（删除）
- 不影响业务逻辑代码、API 接口、数据库
- `certs/` 整个目录加入 `.gitignore`，不再跟踪
