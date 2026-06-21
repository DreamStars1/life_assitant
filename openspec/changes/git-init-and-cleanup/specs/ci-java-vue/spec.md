## ADDED Requirements

### Requirement: Java Spring Boot 后端 CI
CI 流水线 SHALL 在每次 push 到 main/master 及 PR 时，对后端执行 Maven 编译和单元测试。

#### Scenario: Maven 编译通过
- **WHEN** 后端代码提交到 main 分支或创建 PR
- **THEN** CI 使用 Java 17 Temurin 执行 `mvn compile`
- **THEN** CI 使用 Java 17 Temurin 执行 `mvn test`

### Requirement: Vue 3 前端 CI
CI 流水线 SHALL 对两个前端项目分别执行 lint、类型检查和构建。

#### Scenario: 移动端前端检查
- **WHEN** 前端代码提交到 main 分支或创建 PR
- **THEN** 对 `front/vue3-vant-mobile` 执行 `pnpm lint`、`pnpm typecheck`、`pnpm build:dev`

#### Scenario: 管理后台前端检查
- **WHEN** 前端代码提交到 main 分支或创建 PR
- **THEN** 对 `admin_front/vue3-element-admin` 执行 `pnpm typecheck`、`pnpm build:dev`

### Requirement: CI 使用 pnpm 缓存
CI 流水线 SHALL 利用 GitHub Actions 的 pnpm 缓存机制加速依赖安装。

#### Scenario: 依赖缓存生效
- **WHEN** pnpm-lock.yaml 未变更
- **THEN** `pnpm install` 使用缓存，无需重新下载所有依赖
