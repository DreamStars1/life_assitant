# java-backend-core

## Purpose

建立基于 ContiNew 模板的 Spring Boot 3.x Maven 项目骨架，定义项目结构、异常处理、API 文档和数据库迁移策略。

## ADDED Requirements

### Requirement: 项目结构与模块划分

项目设计 SHALL 明确 Maven 多模块结构，`continew-server`/`continew-system`/`continew-common` 三模块，JDK 17，Spring Boot 3.3.x。

#### Scenario: 模块职责清晰

- **WHEN** 查阅设计文档
- **THEN** 每个模块的职责和依赖关系有明确描述（server: 启动+配置，common: 工具+异常+常量，system: 认证+业务模块）

### Requirement: 全局异常处理设计

系统设计 SHALL 定义统一的错误响应格式 `{"detail": "错误描述"}`，HTTP 状态码与错误语义匹配。

#### Scenario: 业务异常定义

- **WHEN** 查阅异常处理设计
- **THEN** 定义了 `BusinessException`（含 HttpStatus），以及对应的 `@RestControllerAdvice` 处理逻辑

#### Scenario: 参数校验失败定义

- **WHEN** 查阅参数校验设计
- **THEN** 定义了 `@Valid` 校验失败时返回 422 + 字段级错误信息的处理方式

### Requirement: API 文档

系统设计 SHALL 明确使用 NextDoc4j（ContiNew 内置）提供 OpenAPI 3.0 文档。

#### Scenario: 查阅 API 文档入口

- **WHEN** 查阅设计文档
- **THEN** 明确了 `/doc.html` 为 API 文档访问入口

### Requirement: Flyway 数据库迁移设计

系统设计 SHALL 使用 Flyway 管理 DDL，首个迁移脚本 `V1__init_all_tables.sql` 从零创建全部 6 张业务表及索引。

#### Scenario: 迁移脚本覆盖完整

- **WHEN** 查阅 Flyway 迁移脚本设计
- **THEN** 确认 V1 脚本覆盖 user/todo/activity/shared_record/push_subscription 五张表，CHAR(36) 主键，外键和索引正确定义

### Requirement: CORS 配置设计

系统设计 SHALL 支持跨域，允许的域名通过配置文件 `application-{profile}.yml` 管理。

#### Scenario: 查阅 CORS 设计

- **WHEN** 查阅配置设计文档
- **THEN** CORS 允许的域名、方法、头信息有明确配置规范

### Requirement: 日志配置设计

系统设计 SHALL 使用 Logback，支持控制台输出和文件滚动存储。

#### Scenario: 查阅日志设计

- **WHEN** 查阅日志配置设计
- **THEN** 日志格式、级别（dev: DEBUG, prod: INFO）、文件路径有明确规范
