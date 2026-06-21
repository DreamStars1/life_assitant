## MODIFIED Requirements

### Requirement: LifeLog 增加 quantity/unit 字段

LifeLog 模型 SHALL 增加 `quantity`（float?）和 `unit`（str?）字段。

- quantity 记录数值，unit 记录单位（如 km、min、cal、碗）
- 适用于运动距离、饮食热量、工作时长等量化场景
- **Reason**: 用户需要记录"跑了5公里"这类带量化的数据
- **Migration**: 存量数据 quantity=null, unit=null

#### Scenario: 创建可量化的生活记录

- **WHEN** 用户记录"跑步5公里"
- **THEN** 系统创建 LifeLog，log_type='exercise'，content='跑步'，quantity=5，unit='km'

### Requirement: LifeLog 增加 mood 字段

LifeLog 模型 SHALL 增加 `mood`（int?）字段，取值范围 1-10。

- 适用于心情（mood）类型的记录
- 在其他类型中可选

#### Scenario: 记录心情

- **WHEN** 用户记录心情为8分
- **THEN** 系统创建 LifeLog，log_type='mood'，mood=8

### Requirement: LifeLog 增加 tags 字段

LifeLog 模型 SHALL 增加 `tags`（JSON array of strings?）字段。

- 用于灵活的标记分类，如 ["开心", "和朋友"]
- tags 不由后端校验，纯自由标签

#### Scenario: 带标签的记录

- **WHEN** 用户记录"中午和朋友吃火锅"时添加标签["火锅", "朋友聚餐"]
- **THEN** 系统保存 LifeLog 且 tags=["火锅", "朋友聚餐"]
