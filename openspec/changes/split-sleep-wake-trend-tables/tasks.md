## 1. Backend — weekly 支持 days

- [x] 1.1 在 `PartnerCheckinService.getWeekly` 增加 `days` 参数：仅允许 7 / 30，非法抛 `BadRequestException`；`start = end.minusDays(days - 1)`，仍调用 `findWeekly`
- [x] 1.2 在 `PartnerCheckinController` 的 `GET /partner/checkin/weekly` 增加 `@RequestParam(defaultValue = "7") int days` 并传入 service
- [x] 1.3 手动或用现有方式验证：无参返回近 7 天；`days=30` 返回近 30 天；`days=14` 返回 400

## 2. Frontend API

- [x] 2.1 更新 `partner-checkin.ts` 的 `getWeeklyCheckin(days?: 7 | 30)`，将 `days` 作为 query 传给 `/partner/checkin/weekly`

## 3. Frontend — 作息详情页分图 + 筛选

- [x] 3.1 在 `sleep.vue` 增加 `rangeDays`（默认 7）与 7/30 分段筛选 UI；切换时重新拉数
- [x] 3.2 将日期轴 / `buildChartFromWeekly` 泛化为按 `rangeDays` 生成业务日标签与数据点
- [x] 3.3 拆成两个 ECharts 实例（起床图、睡觉图）：各两条线（我/伴侣），独立 Y 轴窗口；30 天时 X 轴标签抽稀
- [x] 3.4 `onUnmounted` dispose 两个 chart；打卡成功后按当前 `rangeDays` 刷新两图
- [x] 3.5 按需补充 i18n（起床走势 / 睡觉走势 / 7天 / 30天）；看板 `index.vue` 保持无参 weekly 不改结构

## 4. 验证

- [x] 4.1 详情页默认两图 + 近 7 天；切 30 天两图同步更新；切回 7 天恢复
- [x] 4.2 确认看板主页缩略图与今日打卡仍正常
