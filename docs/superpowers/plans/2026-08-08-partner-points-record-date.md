# Partner Points Record Date Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 新建加减分可选记录日期（日精度），双方可改已有积分流水的日期；复用 `created_at`，不影响余额。

**Architecture:** 抽出纯函数 `PartnerPointsRules.resolveCreatedAt`（今天→`now`，过去→`startOfDay`，未来→400）。扩展 `POST /partner/points` 可选 `recordDate`；新增 `PATCH /partner/points/{id}` 只改日期。前端积分页表单选日 + 点流水改日；MCP 本版不动。

**Tech Stack:** Spring Boot 3 + MyBatis-Plus / Vue 3 + Vant + i18n

**Spec:** `docs/superpowers/specs/2026-08-08-partner-points-record-date-design.md`

## Global Constraints

- 复用 `partner_points.created_at`，无 DB 迁移、无新列
- 日精度；未来日期拒绝；改日期不碰余额 / `pointsChange` / `reason`
- 任一方可改该对任意流水日期（`createdBy` ∈ {自己, 伴侣}）
- 仪表盘快捷加减分不加日期 UI；本版不改 MCP
- 版本 bump：前端 `1.7.0` → `1.8.0`；后端 `1.4.0-SNAPSHOT` → `1.5.0-SNAPSHOT`；写根 `CHANGELOG.md`
- PowerShell：`git commit -m "..."`，路径双引号；勿提交无关脏文件
- Java 纯逻辑测对齐 `PartnerInfoRulesTest`（无 Spring）

## File map

| Path | Role |
|------|------|
| `.../partner/service/PartnerPointsRules.java` | 解析 `recordDate` → `createdAt` |
| `.../partner/service/PartnerPointsRulesTest.java` | 单元测试 |
| `.../partner/service/PartnerPointsService.java` | `addPoints` 接日期；新增 `updateRecordDate` |
| `.../partner/controller/PartnerPointsController.java` | POST 扩展；PATCH |
| `front/.../api/modules/partner-points.ts` | `addPoints` 可选日期；`updatePointsRecordDate` |
| `front/.../pages/partner/dashboard/points.vue` | 选日 + 改日 UI |
| `front/.../locales/zh-CN.json` / `en-US.json` | 文案 |
| `front/.../package.json` | version |
| `backend/.../config/application.yml` | `application.version` |
| `CHANGELOG.md` | v1.8.0 |

---

### Task 1: `PartnerPointsRules` + 单元测试

**Files:**
- Create: `backend/lifeassistant/lifeassistant-system/src/main/java/top/lifeassistant/partner/service/PartnerPointsRules.java`
- Create: `backend/lifeassistant/lifeassistant-system/src/test/java/top/lifeassistant/partner/service/PartnerPointsRulesTest.java`

**Interfaces:**
- Produces: `PartnerPointsRules.resolveCreatedAt(LocalDate recordDate, LocalDate today, LocalDateTime now) → LocalDateTime`
  - `recordDate == null` 或 `recordDate.equals(today)` → 返回 `now`
  - `recordDate.isBefore(today)` → `recordDate.atStartOfDay()`
  - `recordDate.isAfter(today)` → `BadRequestException("记录日期不能晚于今天")`

- [ ] **Step 1: 写失败测试**

```java
package top.lifeassistant.partner.service;

import org.junit.jupiter.api.Test;
import top.continew.starter.core.exception.BadRequestException;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class PartnerPointsRulesTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 8, 8);
    private static final LocalDateTime NOW = LocalDateTime.of(2026, 8, 8, 14, 30, 0);

    @Test
    void nullDate_returnsNow() {
        assertEquals(NOW, PartnerPointsRules.resolveCreatedAt(null, TODAY, NOW));
    }

    @Test
    void today_returnsNow() {
        assertEquals(NOW, PartnerPointsRules.resolveCreatedAt(TODAY, TODAY, NOW));
    }

    @Test
    void pastDate_returnsStartOfDay() {
        LocalDate past = LocalDate.of(2026, 8, 5);
        assertEquals(past.atStartOfDay(), PartnerPointsRules.resolveCreatedAt(past, TODAY, NOW));
    }

    @Test
    void futureDate_throws() {
        assertThrows(BadRequestException.class,
            () -> PartnerPointsRules.resolveCreatedAt(TODAY.plusDays(1), TODAY, NOW));
    }
}
```

- [ ] **Step 2: 跑测试确认失败**

```powershell
cd backend\lifeassistant
mvn -pl lifeassistant-system -am test "-Dtest=PartnerPointsRulesTest" -q
```

Expected: FAIL（类不存在或编译失败）

- [ ] **Step 3: 最小实现**

```java
package top.lifeassistant.partner.service;

import top.continew.starter.core.exception.BadRequestException;

import java.time.LocalDate;
import java.time.LocalDateTime;

public final class PartnerPointsRules {

    private PartnerPointsRules() {}

    public static LocalDateTime resolveCreatedAt(LocalDate recordDate, LocalDate today, LocalDateTime now) {
        if (recordDate == null || recordDate.equals(today)) {
            return now;
        }
        if (recordDate.isAfter(today)) {
            throw new BadRequestException("记录日期不能晚于今天");
        }
        return recordDate.atStartOfDay();
    }
}
```

- [ ] **Step 4: 跑测试确认通过**

```powershell
cd backend\lifeassistant
mvn -pl lifeassistant-system -am test "-Dtest=PartnerPointsRulesTest" -q
```

Expected: PASS

- [ ] **Step 5: Commit**

```powershell
git add "backend/lifeassistant/lifeassistant-system/src/main/java/top/lifeassistant/partner/service/PartnerPointsRules.java" "backend/lifeassistant/lifeassistant-system/src/test/java/top/lifeassistant/partner/service/PartnerPointsRulesTest.java"
git commit -m "feat(partner): add PartnerPointsRules for record date resolution"
```

---

### Task 2: Service + Controller（POST 扩展 + PATCH）

**Files:**
- Modify: `backend/lifeassistant/lifeassistant-system/src/main/java/top/lifeassistant/partner/service/PartnerPointsService.java`
- Modify: `backend/lifeassistant/lifeassistant-system/src/main/java/top/lifeassistant/partner/controller/PartnerPointsController.java`

**Interfaces:**
- Consumes: `PartnerPointsRules.resolveCreatedAt`
- Produces:
  - `PartnerPointsService.addPoints(String userId, int pointsChange, String reason, LocalDate recordDate)`
  - `PartnerPointsService.updateRecordDate(String userId, String recordId, LocalDate recordDate)`
  - `POST` body 字段 `recordDate` 可选（`LocalDate`）
  - `PATCH /partner/points/{id}` body `{ "recordDate": "yyyy-MM-dd" }` 必填

- [ ] **Step 1: 改 `PartnerPointsService.addPoints` 签名并写入解析后的时间**

将现有：

```java
@Transactional
public void addPoints(String userId, int pointsChange, String reason) {
    ...
    record.setCreatedAt(LocalDateTime.now());
    ...
}
```

改为：

```java
@Transactional
public void addPoints(String userId, int pointsChange, String reason, LocalDate recordDate) {
    String partnerId = getPartnerId(userId);
    LocalDateTime now = LocalDateTime.now();
    PartnerPointsDO record = new PartnerPointsDO();
    record.setId(UUID.randomUUID().toString());
    record.setCreatedBy(userId);
    record.setPointsChange(pointsChange);
    record.setReason(reason);
    record.setCreatedAt(PartnerPointsRules.resolveCreatedAt(recordDate, now.toLocalDate(), now));
    mapper.insert(record);
    partnerInfoService.addPointsBalance(userId, partnerId, pointsChange);
}
```

（补 `import java.time.LocalDate;`）

- [ ] **Step 2: 新增 `updateRecordDate`**

```java
@Transactional
public void updateRecordDate(String userId, String recordId, LocalDate recordDate) {
    if (recordDate == null) {
        throw new BadRequestException("记录日期不能为空");
    }
    String partnerId = getPartnerId(userId);
    PartnerPointsDO record = mapper.selectById(recordId);
    if (record == null) {
        throw new BadRequestException("积分记录不存在");
    }
    String by = record.getCreatedBy();
    if (!userId.equals(by) && !partnerId.equals(by)) {
        throw new BadRequestException("无权修改该积分记录");
    }
    LocalDateTime now = LocalDateTime.now();
    record.setCreatedAt(PartnerPointsRules.resolveCreatedAt(recordDate, now.toLocalDate(), now));
    mapper.updateById(record);
}
```

- [ ] **Step 3: 改 Controller**

`PointsChangeRequest` 增加可选：

```java
private LocalDate recordDate;
```

`addPoints` 调用：

```java
service.addPoints(user.getId(), req.getPointsChange(), req.getReason(), req.getRecordDate());
```

新增：

```java
@Operation(summary = "修改积分流水记录日期")
@PatchMapping("/partner/points/{id}")
public ApiResponse<Void> updateRecordDate(
        @CurrentUser UserDO user,
        @PathVariable String id,
        @Valid @RequestBody PointsRecordDateRequest req) {
    service.updateRecordDate(user.getId(), id, req.getRecordDate());
    return ApiResponse.ok();
}

@Data
public static class PointsRecordDateRequest {
    @NotNull
    private LocalDate recordDate;
}
```

补全 import：`LocalDate`、`NotNull`（若尚未有）、`PatchMapping`。

- [ ] **Step 4: 编译 system 模块**

```powershell
cd backend\lifeassistant
mvn -pl lifeassistant-system -am compile -q
```

Expected: BUILD SUCCESS

- [ ] **Step 5: Commit**

```powershell
git add "backend/lifeassistant/lifeassistant-system/src/main/java/top/lifeassistant/partner/service/PartnerPointsService.java" "backend/lifeassistant/lifeassistant-system/src/main/java/top/lifeassistant/partner/controller/PartnerPointsController.java"
git commit -m "feat(partner): allow set and patch points record date"
```

---

### Task 3: 前端 API + 积分页 UI + i18n

**Files:**
- Modify: `front/vue3-vant-mobile/src/api/modules/partner-points.ts`
- Modify: `front/vue3-vant-mobile/src/pages/partner/dashboard/points.vue`
- Modify: `front/vue3-vant-mobile/src/locales/zh-CN.json`
- Modify: `front/vue3-vant-mobile/src/locales/en-US.json`

**Interfaces:**
- Consumes: `POST /partner/points`（可选 `recordDate`）、`PATCH /partner/points/{id}`
- Produces:
  - `addPoints(pointsChange, reason, recordDate?: string)`
  - `updatePointsRecordDate(id: string, recordDate: string)`

- [ ] **Step 1: 改 API 模块**

```ts
export function addPoints(pointsChange: number, reason: string, recordDate?: string) {
  return request.post<ApiResponse<void>>('/partner/points', {
    pointsChange,
    reason,
    ...(recordDate ? { recordDate } : {}),
  })
}

export function updatePointsRecordDate(id: string, recordDate: string) {
  return request.patch<ApiResponse<void>>(`/partner/points/${id}`, { recordDate })
}
```

- [ ] **Step 2: i18n**

`zh-CN.json` 的 `dashboard` 下增加：

```json
"recordDate": "记录日期",
"editRecordDate": "修改日期"
```

`en-US.json` 对应：

```json
"recordDate": "Record date",
"editRecordDate": "Edit date"
```

- [ ] **Step 3: `points.vue` — 新建选日**

在 script 中增加（对齐看板 `van-calendar` 用法）：

```ts
import { updatePointsRecordDate } from '@/api/modules/partner-points'

function formatYmd(d: Date): string {
  const y = d.getFullYear()
  const m = String(d.getMonth() + 1).padStart(2, '0')
  const day = String(d.getDate()).padStart(2, '0')
  return `${y}-${m}-${day}`
}

function formatDisplayDate(iso: string): string {
  const d = new Date(iso)
  return d.toLocaleDateString('zh-CN')
}

const recordDate = ref(formatYmd(new Date()))
const showCreateCalendar = ref(false)
const calendarMaxDate = new Date()
const createCalendarDefault = computed(() => {
  const [y, m, d] = recordDate.value.split('-').map(Number)
  return new Date(y, m - 1, d)
})

function onCreateDateConfirm(val: Date) {
  recordDate.value = formatYmd(val)
  showCreateCalendar.value = false
}
```

`confirmPoints` 里：

```ts
await addPoints(change, reason.value.trim(), recordDate.value)
```

成功后可将 `recordDate` 重置为今天。

模板：在 `reason` 的 `van-field` 上方加：

```vue
<van-field
  :model-value="recordDate"
  is-link
  readonly
  :label="$t('dashboard.recordDate')"
  @click="showCreateCalendar = true"
/>
```

页面底部（与其它日历同级）增加：

```vue
<van-calendar
  v-model:show="showCreateCalendar"
  :min-date="new Date('2020-01-01')"
  :max-date="calendarMaxDate"
  :default-date="createCalendarDefault"
  @confirm="onCreateDateConfirm"
/>
```

- [ ] **Step 4: `points.vue` — 点流水改日**

```ts
const showEditCalendar = ref(false)
const editingId = ref<string | null>(null)
const editCalendarDefault = ref(new Date())

function openEditDate(item: PointsRecord) {
  editingId.value = item.id
  editCalendarDefault.value = new Date(item.createdAt)
  showEditCalendar.value = true
}

async function onEditDateConfirm(val: Date) {
  if (!editingId.value)
    return
  try {
    await updatePointsRecordDate(editingId.value, formatYmd(val))
    showToast('已更新日期')
    showEditCalendar.value = false
    editingId.value = null
    page.value = 1
    finished.value = false
    await loadHistory()
  }
  catch { /* interceptor */ }
}
```

历史项：

```vue
<div
  v-for="item in history"
  :key="item.id"
  class="history-item"
  @click="openEditDate(item)"
>
  ...
  <div class="hi-time">
    {{ formatDisplayDate(item.createdAt) }}
  </div>
  ...
</div>
```

第二个日历：

```vue
<van-calendar
  v-model:show="showEditCalendar"
  :min-date="new Date('2020-01-01')"
  :max-date="calendarMaxDate"
  :default-date="editCalendarDefault"
  @confirm="onEditDateConfirm"
/>
```

补 `computed` import（若尚未从 `vue` 引入）。

- [ ] **Step 5: 手动冒烟（开发服已起则测；否则记下留给联调）**

1. 新建不改日期 → 今天有记录
2. 新建选过去日 → 列表显示该日、余额变
3. 点流水改到另一天 → 日期变、余额不变
4. 日历不可选未来

- [ ] **Step 6: Commit**

```powershell
git add "front/vue3-vant-mobile/src/api/modules/partner-points.ts" "front/vue3-vant-mobile/src/pages/partner/dashboard/points.vue" "front/vue3-vant-mobile/src/locales/zh-CN.json" "front/vue3-vant-mobile/src/locales/en-US.json"
git commit -m "feat(front): pick and edit partner points record date"
```

---

### Task 4: 版本号与 CHANGELOG

**Files:**
- Modify: `front/vue3-vant-mobile/package.json`（`1.7.0` → `1.8.0`）
- Modify: `backend/lifeassistant/lifeassistant-server/src/main/resources/config/application.yml`（`1.4.0-SNAPSHOT` → `1.5.0-SNAPSHOT`）
- Modify: `CHANGELOG.md`（顶部新增 `## v1.8.0 (2026-08-08)`）

- [ ] **Step 1: bump 版本**

`package.json`：`"version": "1.8.0"`
`application.yml`：`version: 1.5.0-SNAPSHOT`

- [ ] **Step 2: CHANGELOG 顶部插入**

```markdown
## v1.8.0 (2026-08-08)

### ✨ 新特性

- **积分流水记录日期**：新建加减分可选业务日；双方可改已有流水日期（日精度，复用 `created_at`）

### 🖥 后端

- `POST /partner/points` 可选 `recordDate`；新增 `PATCH /partner/points/{id}`
- 版本号: `1.4.0-SNAPSHOT` → `1.5.0-SNAPSHOT`

### 📱 前端

- 积分页选日 / 点流水改日；列表按天展示
- 版本号: `1.7.0` → `1.8.0`

---
```

- [ ] **Step 3: Commit**

```powershell
git add "front/vue3-vant-mobile/package.json" "backend/lifeassistant/lifeassistant-server/src/main/resources/config/application.yml" "CHANGELOG.md"
git commit -m "chore: bump app to v1.8.0 for points record date"
```

---

## Spec coverage checklist

| Spec 要求 | Task |
|-----------|------|
| POST 可选 `recordDate` | 2 |
| PATCH 改日期 | 2 |
| 今天→now / 过去→00:00 / 未来拒绝 | 1, 2 |
| 双方可改、越权 400 | 2 |
| 改日期不动余额 | 2（只 `updateById` createdAt） |
| 前端新建选日 + 点流水改日 | 3 |
| 按天展示 | 3 |
| 仪表盘不加日期 | 3（不改 index） |
| 不做 MCP | —（无任务） |
| 版本 + CHANGELOG | 4 |
| 规则单测 | 1 |
