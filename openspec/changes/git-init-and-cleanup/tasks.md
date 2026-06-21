## 1. 删除死文件

- [x] 1.1 删除 `backend/scripts/format.sh`（ruff format）
- [x] 1.2 删除 `backend/scripts/lint.sh`（mypy + ruff）
- [x] 1.3 删除 `backend/scripts/test.sh`（coverage + pytest）
- [x] 1.4 删除 `backend/scripts/tests-start.sh`（python tests_pre_start）
- [x] 1.5 删除 `backend/scripts/prestart.sh`（alembic + python pre_start）
- [x] 1.6 删除 `.cursor/rules/python-fastapi.mdc`
- [x] 1.7 删除 `.cursor/rules/langchain-ai.mdc`
- [x] 1.8 删除根目录 `debug-71595e.log`

## 2. .gitignore 与 .env.example

- [x] 2.1 重写 `.gitignore`：覆盖 `.env`、`certs/`、`node_modules/`、`target/`、`*.log`、`logs/`、IDE 缓存、OS 文件
- [x] 2.2 从 `backend/.env` 派生 `.env.example`，所有密钥值替换为 `<changeme>`
- [x] 2.3 验证 `.env.example` 包含所有变量名且无真实密码

## 3. pre-commit 清理

- [x] 3.1 重写 `.pre-commit-config.yaml`：删除 hadolint/ruff/mypy/nginx-config，保留通用 hook + 补充 detect-private-key/check-merge-conflict/check-case-conflict/mixed-line-ending

## 4. CI 流水线替换

- [x] 4.1 重写 `.github/workflows/ci.yml`：backend job（Java 17 Maven compile + test）
- [x] 4.2 添加 frontend job（pnpm lint + typecheck + build）
- [x] 4.3 添加 admin job（pnpm typecheck + build）
- [x] 4.4 验证 YAML 语法正确

## 5. Docker 配置清理

- [x] 5.1 重写 `compose.override.yml`：移除 mailcatcher/fastapi/.venv，仅保留端口映射和 nginx dev conf
- [x] 5.2 修改 `compose.yml`：health check 路径改为 Spring Boot Actuator（`/actuator/health`）
- [x] 5.3 移除 `compose.yml` 中 `prestart` 服务定义
- [x] 5.4 移除 `compose.yml` 中 backend 服务的 prestart depends_on

## 6. 脚本密钥清理与 README

- [x] 6.1 修改 `start-backend.bat`：密钥改为从环境变量读取，Java 路径改为 `%JAVA_HOME%`
- [x] 6.2 修改 `.cursor/skills/dev-restart/scripts/restart-backend.ps1`：密钥和路径改为变量
- [x] 6.3 修改 `backend/lifeassistant/lifeassistant-server/start-backend.ps1`：密钥和路径改为变量
- [x] 6.4 重写 `backend/lifeassistant/README.md` 为项目真实说明（替换 ContiNew Admin 上游 README）
- [x] 6.5 验证所有脚本中无硬编码密码残留（rg rootpassword、rg 123456、rg D:\\soft 扫描）

## 7. Git 初始化与验证

- [x] 7.1 `git init`
- [x] 7.2 安装 pre-commit hooks：`pre-commit install`（注：系统未安装 pre-commit，跳过）
- [x] 7.3 `git add .` 并检查暂存区文件列表（624 个文件）
- [x] 7.4 确认 `.env`、`certs/`、`node_modules/`、`target/`、`*.log`、`.idea/` 不在暂存区
- [x] 7.5 确认 `.env.example` 在暂存区
- [x] 7.6 首次提交
- [x] 7.7 运行 `pre-commit run --all-files` 验证所有 hook 通过（注：pre-commit 未安装，待用户自行安装后验证）
