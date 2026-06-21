## 1. ★第一优先: 一起做过的事

- [x] 1.1 审查 shared_record 表设计（7 字段：id/created_by/title/content/occurred_at/created_at/update_time）
- [x] 1.2 审查 shared-record 端点规范（5 端点：创建/列表/详情/更新/删除）
- [x] 1.3 文档化查询逻辑（`WHERE created_by IN (user_id, partner_id)`）
- [x] 1.4 文档化权限规则（仅创建者可改/删自己的记录）
- [x] 1.5 确认未绑定伴侣时的错误处理（400 + 403）
- [x] 1.6 确认所有表均有 `update_time` 字段

## 2. 数据模型设计审查

- [x] 2.1 审查 user 表字段完整性（13 字段，不含 wechat_id，自引用 partner_id）
- [x] 2.2 审查 todo 表字段完整性（14 字段，含 update_time）
- [x] 2.3 审查 activity 表字段完整性（10 字段，含 update_time）
- [x] 2.4 审查 push_subscription 表字段完整性（8 字段，含 update_time）
- [x] 2.5 确认 ER 关系图：user 1:N 全部 5 张子表 + user 自引用 + todo FK 分配关系
- [x] 2.6 确认索引策略（所有 FK 列 + user.email UNIQUE + shared_record.created_by）

## 3. API 端点设计审查

- [x] 3.1 审查认证端点（6 个）
- [x] 3.2 审查用户端点（10 个）
- [x] 3.3 审查伴侣端点（2 个：邀请/绑定，第一阶段不含微信）
- [x] 3.4 审查 shared-record 端点（5 个，★第一优先）
- [x] 3.5 审查待办端点（7 个）
- [x] 3.6 审查活动端点（6 个）
- [x] 3.7 审查推送端点（4 个）
- [x] 3.8 审查管理后台端点（4 个，★ superuser）
- [x] 3.9 确认错误响应统一格式

## 4. 代码架构设计文档

- [x] 4.1 产出包结构图（8 个业务包含 sharedrecord）
- [x] 4.2 文档化分层职责（Controller → Service → Mapper）
- [x] 4.3 文档化 `@CurrentUser` + `@RequireSuperuser` 认证注入
- [x] 4.4 文档化 Sa-Token 集成方案
- [x] 4.5 确认 Entity 按业务包分散

## 5. 关键业务流程设计

- [x] 5.1 文档化 shared-record 查询逻辑（`IN (a,b)` 共享空间）
- [x] 5.2 文档化 shared-record 权限（创建者改删）
- [x] 5.3 文档化伴侣绑定流程（邀请码 → 双向写入）
- [x] 5.4 文档化待办完成逻辑
- [x] 5.5 文档化伴侣共享逻辑（activity 全量、shared-record 双方互见）
- [x] 5.6 文档化推送静默时段判断

## 6. 定时任务设计

- [x] 6.1 设计待办提醒（每分钟 due_date 扫描）
- [x] 6.2 设计每日摘要（8:00 Todo 数量 + 活动摘要）
- [x] 6.3 确认 shared-record / activity 提醒为未决项（见 Open Questions #3）

## 7. ContiNew 模板裁剪方案

- [x] 7.1 列出保留/删除/禁用清单（见 design.md 技术决策表后注）
- [x] 7.2 列出重写文件清单（已补充，含包名更新）

## 8. 技术决策记录 (ADR)

- [x] 8.1 主键 UUID vs Snowflake → D1
- [x] 8.2 密码 BCrypt → D2
- [x] 8.3 ORM MyBatis-Plus → D3
- [x] 8.4 响应裸 JSON vs R 包装 → D4
- [x] 8.5 Entity 分散 vs 集中 → D7
- [x] 8.6 伴侣共享 全有或全无 vs 逐条控制 → D9
- [x] 8.7 shared_record 查询方式（IN(a,b) → D10，已补充）
- [x] 8.8 Activity 模型 单表 vs 计划/实际两表 → design Line 66 Phase 2 扩展说明

## 9. 待定问题决议

- [x] 9.1 parent POM: continew-starter vs Spring Boot 原生 → 保留 continew-starter:2.15.0
- [x] 9.2 邮件模板: Thymeleaf vs FreeMarker → Thymeleaf
- [x] 9.3 lombok/.style 去留 → 保留，沿用 ContiNew 代码风格管理
- [x] 9.4 活动/共享记录提醒机制 → Phase 2 再做，Phase 1 不加

## 10. Docker 与 Nginx 部署设计

- [x] 10.1 设计 Dockerfile（Maven 多阶段构建 → bellsoft-liberica JDK 17 + ZGC）
- [x] 10.2 确认 Nginx 零改动策略（容器名/端口不变，仅替换容器内容）
- [x] 10.3 设计 docker-compose.yml 服务配置（MySQL 8.4 + Redis 7 + backend:8000 + Nginx）
- [x] 10.4 文档化部署流程（构建→停止→替换→健康检查→回滚方案）
- [x] 10.5 文档化开发环境运行方法 → 抽取到 `development.md`

## 11. 文档拆分

- [x] 11.1 抽取 `decisions.md`：技术决策 (ADR) + 重写文件清单 + 已决议
- [x] 11.2 抽取 `deployment.md`：Docker 与 Nginx 部署设计
- [x] 11.3 抽取 `development.md`：开发环境运行方法
- [x] 11.4 精简 `design.md`：保留起点/数据模型/API/架构核心，底部关联文档索引
