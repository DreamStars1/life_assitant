## Context

项目基于 ContiNew Admin（Java Spring Boot 3 + Vue 3 + TypeScript），但代码库混入了大量 Python FastAPI 模板残留（脚本、CI、Docker 配置、pre-commit hooks）。同时项目尚未纳入 Git 管理，`.gitignore` 覆盖不全，`.env` 文件包含真实密钥、脚本中硬编码了数据库密码。

## Goals / Non-Goals

**Goals:**
- 安全地将项目纳入 Git 管理，敏感信息（`.env`、`certs/`、日志）不被跟踪
- 删除所有无用的 Python 残留文件
- 所有硬编码密钥从脚本中移除，改为环境变量
- CI/CD 配置适配 Java + Vue 技术栈
- Docker Compose 配置从 Python 栈迁移到 Java 栈
- `.pre-commit-config.yaml` 精简为仅保留可用的 hook

**Non-Goals:**
- 不修改业务逻辑代码
- 不新增功能
- 不引入新的外部依赖
- 不配置分支保护规则或 GitHub 仓库设置
- 不编写 Dockerfile（已有）

## Decisions

### .gitignore 策略：显式枚举而非通配
选择显式列出每类需要忽略的路径（`.env`、`certs/`、`node_modules/`、`target/` 等），而非用 `*` 通配全部。理由：明确意图，避免意外忽略需要的文件。

### .env.example：从 backend/.env 派生
`backend/.env` 变量最全（数据库、Redis、SMTP、SENTRY 等），根目录 `.env` 只为 Docker Compose 变量插值。选择从 `backend/.env` 派生模板。

### CI：三 job 并行而非单 job
后端（Maven）、移动端前端（pnpm）、管理后台（pnpm）拆为三个独立 job。理由：并行执行更快，失败独立定位。

### Java 路径：使用 JAVA_HOME 环境变量
脚本中 `D:\soft\Java\jdk-17.0.2\bin\java.exe` 替换为 `$env:JAVA_HOME\bin\java.exe`。pom.xml 已通过 continew-starter parent 锁定 Java 版本，脚本无需指定具体 JDK 路径。

### compose.override.yml：完全重写而非修补
原文件 100% Python 配置（mailcatcher、fastapi run、.venv watch），修补意义不大。重写为仅保留开发环境端口映射和 dev nginx 配置覆盖。

### 死规则删除 vs 保留
`python-fastapi.mdc` 和 `langchain-ai.mdc` 与项目技术栈毫无关系，保留会误导 AI 编码助手。直接删除。

## Risks / Trade-offs

| Risk | Mitigation |
|------|------------|
| `.env.example` 缺少某些变量导致同事无法启动 | 从 `backend/.env` 完整派生，保留所有变量名 |
| `certs/` 加入 `.gitignore` 后，生产部署需单独管理证书 | Docker Compose 已在 `compose.yml` 中通过 volume mount 引用 `./certs`，证书管理由运维侧处理 |
| 删除 `backend/scripts/` 后 CI 中 `prestart`、`test` 步骤消失 | CI（`ci.yml`）已重写为 Maven 命令，不依赖这些脚本 |
| `compose.override.yml` 重写可能丢失开发环境配置 | 只保留端口暴露和 nginx dev conf 覆盖，这些都是明确的开发需求 |
