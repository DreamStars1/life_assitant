## Why

将 Python FastAPI 后端替换为 Java Spring Boot 3.3.x 后端（基于 ContiNew 模板）。核心场景是**伴侣协作**——共享时间轴上的活动记录和任务分配。本阶段聚焦设计，不做代码实现。

## What Changes

- 从需求出发重新设计数据模型（5 张表：user/todo/activity/shared_record/push_subscription）
- **第一优先功能**: shared_record — 伴侣共享记录本，双方可见可写
- 删除原有 event / life_log / timeline_entry，统一为 activity（时间块活动）
- 伴侣共享采用全有或全无模式
- 产出全部 REST API 端点规范和技术决策记录

## Capabilities

- `java-backend-shared-record`: **★第一优先** 伴侣共享记录 — 双方可添加、双方完全可见
- `java-backend-core`: Maven 多模块、异常处理、CORS、API 文档、Flyway
- `java-backend-config`: 多环境配置 — application-{profile}.yml
- `java-backend-auth`: JWT — 登录/刷新/登出、密码重置邮件
- `java-backend-user`: 注册、个人信息、伴侣绑定（邀请码，全有或全无共享）
- `java-backend-todo`: 待办 CRUD、标记完成、伴侣分配
- `java-backend-activity`: 时间块活动 CRUD、时间范围查询、伴侣全量可见
- `java-backend-notification`: Web Push 订阅、提醒推送、静默时段
- `java-backend-monitor`: 超级管理员统计端点

> AI 分析（计划 vs 实际对比）不在此周期。

## Impact

- 产出纯设计文档，不修改代码
- 设计完成后作为实现周期的 spec 基准
