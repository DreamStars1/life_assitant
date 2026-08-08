## Why

作息详情页把起床与睡觉四条线画在同一张走势图里，图例拥挤、Y 轴跨度大，难以单独对比起床或睡觉规律。同时目前只固定近 7 天，无法看更长周期。需要拆成两张独立走势图，并支持 7 天 / 30 天切换。

## What Changes

- 作息详情页（`sleep.vue`）将「作息走势」拆成两张独立图表：**起床走势**、**睡觉走势**（各含我方与伴侣两条线）
- 在走势区域增加 **7 天 / 30 天** 筛选控件；切换后两张图同步更新日期轴与数据
- 看板主页近 7 天缩略图同样拆成起床 / 睡觉两张（仍无 7/30 筛选，点击进详情）
- 后端打卡查询由固定近 7 天扩展为可按天数（7 / 30）查询双方记录

## Capabilities

### New Capabilities

- `sleep-wake-trend-split`: 作息详情页将起床/睡觉走势分图展示，并支持 7/30 天筛选

### Modified Capabilities

- （无）现有 `openspec/specs/` 中无伴侣作息相关能力规格需 delta

## Impact

- 前端：`front/vue3-vant-mobile/src/pages/partner/dashboard/sleep.vue`、`api/modules/partner-checkin.ts`；可能补充 i18n 文案
- 后端：`PartnerCheckinController` / `PartnerCheckinService` / `PartnerCheckinMapper`（或等价查询）——由固定 weekly 扩展为按天数范围查询
- 不影响打卡写入、今日打卡、积分模块
