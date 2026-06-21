## ADDED Requirements

### Requirement: 环境变量模板生成
系统 SHALL 从 `backend/.env` 派生出 `.env.example` 模板文件，所有密钥值替换为 `<changeme>` 占位符。

#### Scenario: .env.example 包含所有变量名
- **WHEN** 对比 `backend/.env` 和 `.env.example`
- **THEN** `.env.example` 包含与 `backend/.env` 相同的变量名
- **THEN** 所有原本为真实密码/密钥的值替换为 `<changeme>`

#### Scenario: .env.example 可追踪
- **WHEN** 检查 `.gitignore` 规则
- **THEN** `.env.example` 被 `!.env.example` 规则豁免，允许提交到 Git
