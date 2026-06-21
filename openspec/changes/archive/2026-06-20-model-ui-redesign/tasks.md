## 1. 后端模型迁移

- [x] 1.1 Event 模型：移除 actual_start_time/actual_end_time，增加 status/location，字段改名 planned_start_time→start_time
- [x] 1.2 Todo 模型：status 四态改为 is_completed bool + completed_at + cancelled_at
- [x] 1.3 LifeLog 模型：增加 quantity/unit/mood/tags 字段
- [x] 1.4 EventRecord 重命名为 TimelineEntry，移除 event_id 外键，增加 source_type/source_id
- [x] 1.5 更新 Pydantic schema（Create/Update/Public）适配新模型
- [x] 1.6 生成 Alembic 迁移 + 数据迁移脚本（存量 EventRecord→TimelineEntry）
- [x] 1.7 验证 `alembic upgrade head`

## 2. 后端 Service 适配

- [x] 2.1 Event service：去掉 actual 相关逻辑，增加 status 更新、location 支持
- [x] 2.2 Todo service：状态逻辑从四态简化为 is_completed
- [x] 2.3 LifeLog service：增加 quantity/unit/mood/tags 的创建和查询
- [x] 2.4 TimelineEntry service：统一时间线查询（按用户+时间范围）
- [x] 2.5 今日时间轴 API：GET /timeline?date= 聚合当天 Event + LifeLog

## 3. 后端 API 路由适配

- [x] 3.1 Event router：适配新模型字段名和 schema
- [x] 3.2 Todo router：适配 is_completed 简化逻辑
- [x] 3.3 LifeLog router：适配新字段
- [x] 3.4 新增 GET /timeline 路由

## 4. 今日页面（时间轴重构）

- [x] 4.1 `pages/index.vue` 改造为时间轴视图：24h 纵向布局
- [x] 4.2 计划块（Event）和时间点标记（LifeLog）的渲染
- [x] 4.3 "我的/我们的"伴侣切换组件
- [x] 4.4 今日待办列表（到期提醒）
- [x] 4.5 "记一笔"快速录入入口
- [x] 4.6 点击条目展开详情

## 5. 日程页改造

- [x] 5.1 `pages/events/index.vue` 增加"计划/实际"切换按钮
- [x] 5.2 计划模式：Events 按 status 着色
- [x] 5.3 实际模式：LifeLogs 按类型图标展示
- [x] 5.4 伴侣视角切换（"我的/我们的"）

## 6. 待办页简化

- [x] 6.1 `pages/todos/index.vue` 按"今天/即将/已完成"分组
- [x] 6.2 勾选完成逻辑适配 is_completed
- [x] 6.3 创建/编辑待办适配新字段

## 7. 我的页面增加统计

- [x] 7.1 `pages/profile/index.vue` 增加本周统计卡片（计划完成率）
- [x] 7.2 调用 /admin/stats/overview？或新建用户级统计 API

## 8. 冒烟测试

- [ ] 8.1 `backend/tests/test_smoke.py` 补充新模型的测试用例
- [ ] 8.2 验证前端 4 个 Tab 页面加载正常
