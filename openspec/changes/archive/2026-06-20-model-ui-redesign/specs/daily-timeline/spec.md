## ADDED Requirements

### Requirement: 今日时间轴展示

系统 SHALL 在"今日"页面上以纵向时间轴形式聚合展示当天的 Event（计划）和 LifeLog（实际记录）。

- 时间轴 SHALL 以 24 小时为刻度，从 00:00 到 23:59
- Event SHALL 显示为蓝色时间块，占据对应的时间段
- LifeLog SHALL 显示为绿色图标标记，在对应时间点展示
- 被取消的 Event（status=cancelled）SHALL 显示为灰色虚线块
- 用户 SHALL 能点击时间轴上的条目展开详情

#### Scenario: 今日时间轴加载当天数据

- **WHEN** 用户打开"今日"页面
- **THEN** 系统加载当天所有 Event 和 LifeLog，按时间排列展示在同一条时间轴上

### Requirement: 伴侣视角切换

今日时间轴 SHALL 支持"我的/我们的"两种视角。

- "我的"模式：只显示当前用户的数据
- "我们的"模式：同时显示当前用户和伴侣的数据，双方条目以不同侧边色块区分

#### Scenario: 切换伴侣视角

- **WHEN** 用户点击顶部切换按钮从"我的"切换到"我们的"
- **THEN** 时间轴上同时展示双方今日的安排，左侧为当前用户，右侧为伴侣

### Requirement: 今日待办列表

今日页面 SHALL 在时间轴下方展示今日到期的待办事项列表。

- 待办列表 SHALL 显示标题和优先级
- 用户 SHALL 可以勾选完成待办
- 已完成的待办 SHALL 显示为删除线

#### Scenario: 勾选完成今日待办

- **WHEN** 用户在今日页面勾选一个待办
- **THEN** 该待办标记为已完成，显示删除线

### Requirement: 快速录入

今日页面 SHALL 提供"记一笔"快速录入入口。

- 用户 SHALL 可以通过一个按钮快速打开 LifeLog 录入表单
- 录入表单 SHALL 默认使用当前时间作为 logged_at

#### Scenario: 快速记录生活

- **WHEN** 用户点击"记一笔"按钮并提交
- **THEN** 系统创建一条 LifeLog 并立即在时间轴上刷新展示
