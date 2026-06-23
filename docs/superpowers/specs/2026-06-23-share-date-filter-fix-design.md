# 一起做过的事：日期筛选与编辑回填修复

## 背景

「一起做过的事」（`/share` 页面）存在两类前端日期问题：

1. **筛选/新建/编辑选日期少一天**：日历确认回调使用 `Date.toISOString().slice(0, 10)` 取日期。Vant Calendar 返回本地 0 点的 `Date`，`toISOString()` 转为 UTC 后，在东八区会偏前 8 小时，导致日期字符串比用户选择的少一天。
2. **编辑时不显示原日期**：`openEdit` 将 `form.occurredAt` 硬编码为 `''`，未从记录回填。

后端 `SharedRecordController` 接收 `LocalDateTime` 类型的 `start`/`end` 查询参数，按 `occurredAt >= start AND occurredAt <= end` 过滤，逻辑正确，无需改动。

## 根因

| 位置 | 问题代码 | 现象 |
|------|----------|------|
| `onFilterStartConfirm` / `onFilterEndConfirm` | `d.toISOString().slice(0, 10)` | 筛选框显示及 API 参数少一天 |
| `onDateConfirm` | 同上 | 新建/编辑表单日期少一天 |
| `openCreate` | `new Date().toISOString().slice(0, 10)` | 默认日期可能少一天 |
| `openEdit` | `form.occurredAt = ''` | 编辑时日期字段为空 |

项目内 `MonthGrid.vue` 已使用 `getFullYear()` / `getMonth()` / `getDate()` 拼本地日期，本页应对齐该模式。

## 方案

采用**方案 A：页面内本地日期工具函数**，仅修改 `front/vue3-vant-mobile/src/pages/share/index.vue`，不新增依赖、不抽公共 utils（当前仅此页受影响）。

### 新增函数

```typescript
function toLocalDateStr(d: Date): string {
  const y = d.getFullYear()
  const m = String(d.getMonth() + 1).padStart(2, '0')
  const day = String(d.getDate()).padStart(2, '0')
  return `${y}-${m}-${day}`
}

function apiDateToLocalStr(iso: string | null | undefined): string {
  if (!iso) return ''
  return iso.slice(0, 10)
}
```

- `toLocalDateStr`：Calendar 返回的 `Date` → `YYYY-MM-DD`（本地日历日）
- `apiDateToLocalStr`：后端 `LocalDateTime` 序列化为 `2026-06-23T00:00:00`（无时区），直接取前 10 字符

### 修改点

| 函数 | 改后 |
|------|------|
| `onFilterStartConfirm` / `onFilterEndConfirm` | `filterStart/End.value = toLocalDateStr(d)` |
| `onDateConfirm` | `form.occurredAt = toLocalDateStr(d)` |
| `openCreate` | `form.occurredAt = toLocalDateStr(new Date())` |
| `openEdit` | `form.occurredAt = apiDateToLocalStr(r.occurredAt)` |
| `formatDate` | 使用 `apiDateToLocalStr(d)` 后 `toLocaleDateString('zh-CN')`，或仅 slice 展示 |

保存逻辑不变：仍发送 `${form.occurredAt}T00:00:00` 给后端。

### 顺带清理

移除 `saveRecord` 中残留的 debug fetch 日志（`#region agent log` 块）。

## 不在范围内

- 后端 API / 数据库 schema 变更
- 抽取 `utils/date.ts`（YAGNI，仅一页有问题）
- 结束日期「含当天 23:59:59」扩展（当前记录均存 `T00:00:00`，现有 `<= end` 已足够）

## 验证清单

- [ ] 选 6 月 23 日作为开始/结束筛选 → 输入框显示 `2026-06-23`，结果与预期一致
- [ ] 新建记录，日历选今天 → 表单日期为今天
- [ ] 点击已有记录编辑 → 日期字段显示原 `occurredAt` 日期
- [ ] 编辑后保存 → 列表日期不变（除非用户改了日期）
- [ ] 列表 `formatDate` 显示与数据库日期一致
