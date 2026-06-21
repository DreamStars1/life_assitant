## ADDED Requirements

### Requirement: 日历实际模式

日程日历页面 SHALL 支持"计划/实际"两种展示模式切换。

- "计划"模式（默认）：日历上显示 Events，按状态着色（planned=蓝、completed=绿、missed=红、cancelled=灰）
- "实际"模式：日历上显示 LifeLogs，按类型图标区分（饮食🍜、运动🏃、工作💼、心情😊、睡眠😴）

#### Scenario: 切换到实际模式

- **WHEN** 用户点击切换按钮到"实际"模式
- **THEN** 日历隐藏 Events，改为展示 LifeLogs，每日以类型图标标记

### Requirement: 实际模式点击行为

在实际模式下点击日历日期 SHALL 跳转到当天的 LifeLog 列表。

#### Scenario: 点击实际模式日期

- **WHEN** 用户在"实际"模式下点击一个日期
- **THEN** 系统展示该日期所有 LifeLog 记录的列表
