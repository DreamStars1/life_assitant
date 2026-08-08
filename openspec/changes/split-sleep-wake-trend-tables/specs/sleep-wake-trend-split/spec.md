## ADDED Requirements

### Requirement: Sleep detail shows separate wake and sleep trend charts

作息详情页 SHALL 将起床与睡觉走势拆成两张独立折线图展示，而不是在同一张图中叠加四条线。

每张图 MUST 仅包含对应打卡类型的我方与伴侣两条序列（若未绑定伴侣，则仅我方一条）。

#### Scenario: Page shows two trend charts

- **WHEN** 用户打开作息详情页
- **THEN** 页面同时展示「起床走势」与「睡觉走势」两张图
- **AND** 起床图不包含睡觉序列
- **AND** 睡觉图不包含起床序列

#### Scenario: Charts include partner series when bound

- **WHEN** 当前用户已绑定伴侣且所选范围内存在双方打卡数据
- **THEN** 起床图显示我方起床与伴侣起床
- **AND** 睡觉图显示我方睡觉与伴侣睡觉

### Requirement: User can filter trends by 7 or 30 days

作息详情页 SHALL 提供 7 天与 30 天筛选；默认 MUST 为 7 天。

切换筛选后，两张走势图 MUST 同步使用同一时间范围，并基于业务日（凌晨 4 点日界）展示连续 `N` 天（含当天）。

#### Scenario: Default range is 7 days

- **WHEN** 用户首次进入作息详情页
- **THEN** 筛选默认为 7 天
- **AND** 两张图的横轴均为近 7 个业务日

#### Scenario: Switch to 30 days refreshes both charts

- **WHEN** 用户将筛选切换为 30 天
- **THEN** 系统拉取近 30 个业务日的双方打卡数据
- **AND** 起床图与睡觉图均更新为 30 天横轴与对应数据点

#### Scenario: Switch back to 7 days

- **WHEN** 用户从 30 天切换回 7 天
- **THEN** 两张图均恢复为近 7 个业务日的数据与横轴

### Requirement: Checkin weekly API accepts days filter

`GET /partner/checkin/weekly` SHALL 接受可选查询参数 `days`，允许值为 `7` 或 `30`；省略时 MUST 默认为 `7`。

当 `days` 为其他值时，系统 MUST 返回客户端错误（400）。

返回结果 MUST 包含当前用户与其伴侣在 `[end-(days-1), end]` 业务日区间内的打卡记录（`end` 为当前业务日）。

#### Scenario: Default weekly behavior unchanged

- **WHEN** 客户端调用 `GET /partner/checkin/weekly` 且不传 `days`
- **THEN** 系统返回近 7 个业务日的双方打卡记录

#### Scenario: Request 30-day range

- **WHEN** 客户端调用 `GET /partner/checkin/weekly?days=30`
- **THEN** 系统返回近 30 个业务日的双方打卡记录

#### Scenario: Reject invalid days

- **WHEN** 客户端调用 `GET /partner/checkin/weekly?days=14`
- **THEN** 系统返回 400 错误且不返回打卡列表
