## MODIFIED Requirements

### Requirement: Event 去掉 actual 字段

Event 模型 SHALL 移除 `actual_start_time`、`actual_end_time` 字段。

- 实际开始/结束时间不再由 Event 记录
- LifeLog 为"实际"的唯一数据源
- **Reason**: 计划≠实际，硬关联导致无法表达"计划跑步但实际散步"的情况
- **Migration**: 存量数据中 actual_* 字段内容迁移到 LifeLog 或丢弃（根据业务判断）

#### Scenario: 创建日程只有计划时间

- **WHEN** 用户创建一个日程
- **THEN** 日程仅包含 start_time 和 end_time（计划时间），不再有 actual 字段

### Requirement: Event 增加 status 字段

Event 模型 SHALL 增加 `status` 字段，四态：planned / completed / missed / cancelled。

- 默认值为 planned
- 用户创建日程时不需要指定 status

#### Scenario: 标记日程完成

- **WHEN** 用户将某条日程标记为"已完成"
- **THEN** 该日程的 status 变为 completed

### Requirement: Event 增加 location 字段

Event 模型 SHALL 增加可选的 `location` 字段，记录日程地点。

#### Scenario: 创建带地点的日程

- **WHEN** 用户创建日程时填写了地点
- **THEN** 该日程的 location 字段保存对应的地点信息

### Requirement: EventRecord 独立为 TimelineEntry

EventRecord 表 SHALL 移除 `event_id` 外键，重命名为 `TimelineEntry`，增加 `source_type` 字段。

- source_type SHALL 支持 event / lifelog / manual 三种来源
- source_id SHALL 可选关联到来源记录的 ID
- **Reason**: 时间线不隶属于某个 Event，多个来源的条目共建统一时间线
- **Migration**: 存量 EventRecord 迁移到 TimelineEntry，source_type='event'，source_id=原 event_id
