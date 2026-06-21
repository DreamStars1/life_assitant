## ADDED Requirements

### Requirement: 删除死 Python hook
`.pre-commit-config.yaml` SHALL 删除所有 Python 专属 hook：`ruff-check`、`ruff-format`、`mypy`。

#### Scenario: ruff hook 已删除
- **WHEN** 检查 `.pre-commit-config.yaml`
- **THEN** 不存在 `ruff-check` 和 `ruff-format` hook 定义

#### Scenario: mypy hook 已删除
- **WHEN** 检查 `.pre-commit-config.yaml`
- **THEN** 不存在 `mypy` hook 定义

### Requirement: 补充通用 hook
`.pre-commit-config.yaml` SHALL 补充以下通用检查：`detect-private-key`、`check-merge-conflict`、`check-case-conflict`、`mixed-line-ending`（强制 LF）。

#### Scenario: 私钥检测启用
- **WHEN** 提交包含 `*.pem` 或 `*.key` 文件
- **THEN** `detect-private-key` hook 阻止提交

#### Scenario: 合并冲突检测
- **WHEN** 提交包含 `<<<<<<<`、`=======`、`>>>>>>>` 标记的文件
- **THEN** `check-merge-conflict` hook 阻止提交

#### Scenario: 换行符统一
- **WHEN** 提交包含 CRLF 换行的文件
- **THEN** `mixed-line-ending` hook 自动转换为 LF

### Requirement: 清理死路径引用
`end-of-file-fixer` hook 的 exclude 配置 SHALL 移除对 `backend/app/email-templates/build/` 的引用（目录不存在）。

#### Scenario: exclude 路径已清理
- **WHEN** 检查 `end-of-file-fixer` hook 的 exclude 配置
- **THEN** 不存在 `backend/app/email-templates/build/` 路径引用
