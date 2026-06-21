# 技术决策与文件清单

## 决策记录 (ADR)

| 编号 | 决策 | 选择 | 理由 |
|------|------|------|------|
| D0 | 包名 | `top.lifeassistant` | 替换 ContiNew 的 `top.continew.admin` |
| D1 | 主键 | String UUID CHAR(36), ASSIGN_UUID | 前端已有 UUID，分布式友好 |
| D2 | 密码 | BCrypt | Spring Security 默认 |
| D2.1 | 邮件模板 | Thymeleaf | Spring Boot 原生集成 |
| D3 | ORM | MyBatis-Plus 3.5+ | ContiNew 内置，LambdaWrapper 灵活 |
| D4 | 响应 | 裸 JSON，错误 `{"detail":"..."}` | 不用 ContiNew 的 R 包装 |
| D5 | 迁移 | Flyway 单次 V1 建 5 张表 | 替换 Liquibase |
| D6 | 认证 | Sa-Token JWT HS256 | ContiNew 内置 |
| D7 | Entity | 按业务包分散 | 每个模块自包含 |
| D8 | 审计 | `created_at + update_time` | 不用 ContiNew BaseDO |
| D9 | 伴侣共享 | 全有或全无，绑定即共享 | 简化，Phase 2 可加 flag |
| D10 | shared_record 查询 | `WHERE created_by IN (a,b)` | 单次查询返回两人记录，无需关联表 |

### ContiNew 功能处置

**禁用的功能**: 多租户、限流、验证码、JustAuth、SnailJob、WebSocket、文件存储、`@EnableGlobalResponse`
**删除的模块**: `continew-plugin/`, `continew-extension/`, `.github/`, `.image/`, `docker/`

---

## 重写文件清单

| 文件 | 操作 | 说明 |
|------|------|------|
| 根 `pom.xml` | 修改 | `<groupId>` 改为 `top.lifeassistant`，删除 plugin/extension 模块引用，删除 spotless/sonar |
| `continew-server/pom.xml` | 修改 | 添加 flyway，排除 liquibase，移除 plugin/extension 引用 |
| `continew-server/.../ContiNewAdminApplication.java` | 重命名 | → `LifeAssistantApplication.java`，包名改为 `top.lifeassistant` |
| `continew-server/.../config/application.yml` | 修改 | `application.base-package: top.lifeassistant`，关闭多租户/限流/验证码，`id-type: ASSIGN_UUID`，删除 cosid |
| `continew-server/.../config/application-dev.yml` | 修改 | 关闭 SnailJob/JustAuth/Liquibase，开启 Flyway |
| `continew-server/.../config/` | 新增 | `GlobalExceptionHandler.java`、`CorsConfig.java`、`WebMvcConfig.java`、`MyBatisPlusConfig.java` |
| `continew-system/.../system/model/entity/` | 全部删除 | 除 `user/UserDO.java` 外的所有 entity |
| `continew-system/.../entity/user/UserDO.java` | 重写 | `@TableName("user")`，包名 `top.lifeassistant.system.model.entity.user` |
| `continew-common/.../base/model/entity/BaseDO.java` | 修改 | id 字段改为 String |
| `continew-common/.../` 全部包名 | 批量修改 | `top.continew.admin` → `top.lifeassistant` |
| `continew-system/.../` 全部包名 | 批量修改 | `top.continew.admin` → `top.lifeassistant` |
| `lombok.config` | 保留 | 沿用 ContiNew 的代码风格管理 |
| `continew-server/src/main/resources/db/migration/V1__init_all_tables.sql` | 新增 | 5 张表 DDL |

> 所有 Entity 按业务包分散创建（`todo/model/entity/TodoDO.java` 等），不放入 `system/model/entity/`。

---

## 已决议

1. **parent POM**: 保留 `continew-starter:2.15.0`，继续使用 ContiNew 的依赖管理和自动配置
2. **邮件模板**: Thymeleaf，Spring Boot 原生集成
3. **lombok.config / .style / .github**: 保留，沿用 ContiNew 的代码风格管理
4. **活动提醒机制**: Phase 2 再做，Phase 1 不做，不在 Activity 加 `remind_at` 字段
