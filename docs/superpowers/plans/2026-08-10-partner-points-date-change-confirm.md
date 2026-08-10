# Partner Points Date-Change Confirm Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 非 cc 改积分流水日期需对方确认；cc 发起仍即时生效；支持同意/拒绝/一键全部确认；积分页小红点。

**Architecture:** `partner_points` 增 pending 三列；纯函数扩展 `PartnerPointsRules`（cc 判定、24h 过期、清 pending）。`PATCH` 分支写 pending 或直接改 `created_at`；新增 approve / reject / approve-all。History 返回带 pending 字段 + `pendingConfirmCount`。前端积分页展示确认 UI；看板入口红点。

**Tech Stack:** Spring Boot 3 + MyBatis-Plus + Flyway / Vue 3 + Vant + i18n

**Spec:** `docs/superpowers/specs/2026-08-10-partner-points-date-change-confirm-design.md`

## Global Constraints

- pending 存 MySQL 列，不用 Redis；TTL 24h 懒清理
- 仅 `PATCH` 改已有日期走确认；新建加减分不确认
- 发起人 `UserDO.fullName` 精确等于 `"cc"` 才跳过确认
- 确认方 = pending 发起人的伴侣；禁止自己批自己
- 改日期 / 确认不碰 `points_balance` / `pointsChange` / `reason`
- 不做推送、一键拒绝、发起人撤回、MCP
- 版本：前端 `1.8.3` → `1.9.0`；后端 `1.5.3-SNAPSHOT` → `1.6.0-SNAPSHOT`；写根 `CHANGELOG.md`
- PowerShell：`git commit -m "..."`，路径双引号；勿提交无关脏文件
- 清 pending 的 NULL 字段必须用 `LambdaUpdateWrapper.set(..., null)`（MP 默认不更新 null）

## File map

| Path | Role |
|------|------|
| `.../db/migration/V19__partner_points_date_change_pending.sql` | 三列迁移 |
| `.../partner/model/entity/PartnerPointsDO.java` | pending 字段 |
| `.../partner/service/PartnerPointsRules.java` | cc / 过期 / clear 辅助 |
| `.../partner/service/PartnerPointsRulesTest.java` | 纯函数单测 |
| `.../partner/model/resp/PartnerPointsHistoryResp.java` | history + count |
| `.../partner/model/resp/PointsDateChangeResult.java` | PATCH 结果 status |
| `.../partner/model/resp/PointsDateApproveAllResult.java` | approve-all 条数 |
| `.../partner/service/PartnerPointsService.java` | pending 流程 |
| `.../partner/controller/PartnerPointsController.java` | 新路由 + history 形状 |
| `front/.../api/modules/partner-points.ts` | 类型与 API |
| `front/.../pages/partner/dashboard/points.vue` | 确认 UI + 全部确认 |
| `front/.../pages/partner/dashboard/index.vue` | 入口小红点 |
| `front/.../locales/zh-CN.json` / `en-US.json` | 文案 |
| `front/.../package.json` / `application.yml` / `CHANGELOG.md` | 发版 |

---

### Task 1: Flyway + DO 字段

**Files:**
- Create: `backend/lifeassistant/lifeassistant-server/src/main/resources/db/migration/V19__partner_points_date_change_pending.sql`
- Modify: `backend/lifeassistant/lifeassistant-system/src/main/java/top/lifeassistant/partner/model/entity/PartnerPointsDO.java`

**Interfaces:**
- Produces: DO 字段 `LocalDate pendingRecordDate`、`String pendingRequestedBy`、`LocalDateTime pendingRequestedAt`（`@TableField` 映射 snake_case）

- [ ] **Step 1: 写迁移**

```sql
ALTER TABLE partner_points
    ADD COLUMN pending_record_date DATE NULL COMMENT '待确认的目标业务日' AFTER created_at,
    ADD COLUMN pending_requested_by CHAR(36) NULL COMMENT '改日期发起人' AFTER pending_record_date,
    ADD COLUMN pending_requested_at DATETIME NULL COMMENT '发起时间(24h过期)' AFTER pending_requested_by;
```

- [ ] **Step 2: 扩展 DO**

在 `PartnerPointsDO` 的 `createdAt` 后增加：

```java
@TableField("pending_record_date")
private LocalDate pendingRecordDate;

@TableField("pending_requested_by")
private String pendingRequestedBy;

@TableField("pending_requested_at")
private LocalDateTime pendingRequestedAt;
```

（补 `import java.time.LocalDate;`）

- [ ] **Step 3: Commit**

```powershell
git add "backend/lifeassistant/lifeassistant-server/src/main/resources/db/migration/V19__partner_points_date_change_pending.sql" "backend/lifeassistant/lifeassistant-system/src/main/java/top/lifeassistant/partner/model/entity/PartnerPointsDO.java"
git commit -m "feat(partner): add pending date-change columns on partner_points"
```

---

### Task 2: `PartnerPointsRules` 扩展 + 单测

**Files:**
- Modify: `backend/lifeassistant/lifeassistant-system/src/main/java/top/lifeassistant/partner/service/PartnerPointsRules.java`
- Modify: `backend/lifeassistant/lifeassistant-system/src/test/java/top/lifeassistant/partner/service/PartnerPointsRulesTest.java`

**Interfaces:**
- Produces:
  - `boolean isCcSkipConfirm(String fullName)` — `"cc".equals(fullName)`
  - `boolean isPendingExpired(LocalDateTime requestedAt, LocalDateTime now)` — `requestedAt == null` 或 `!requestedAt.plusHours(24).isAfter(now)` 视为过期（即 `now >= requestedAt+24h`）
  - `void clearPendingFields(PartnerPointsDO record)` — 三列置 null（内存）
  - 保留既有 `resolveCreatedAt`

- [ ] **Step 1: 写失败测试（追加到现有 Test 类）**

```java
@Test
void isCcSkipConfirm_exactMatchOnly() {
    assertTrue(PartnerPointsRules.isCcSkipConfirm("cc"));
    assertFalse(PartnerPointsRules.isCcSkipConfirm("CC"));
    assertFalse(PartnerPointsRules.isCcSkipConfirm("cc "));
    assertFalse(PartnerPointsRules.isCcSkipConfirm(null));
    assertFalse(PartnerPointsRules.isCcSkipConfirm("小星露"));
}

@Test
void isPendingExpired_nullOrOver24h() {
    LocalDateTime t0 = LocalDateTime.of(2026, 8, 10, 12, 0, 0);
    assertTrue(PartnerPointsRules.isPendingExpired(null, t0));
    assertFalse(PartnerPointsRules.isPendingExpired(t0, t0.plusHours(23)));
    assertTrue(PartnerPointsRules.isPendingExpired(t0, t0.plusHours(24)));
    assertTrue(PartnerPointsRules.isPendingExpired(t0, t0.plusHours(25)));
}

@Test
void clearPendingFields_nullsAllThree() {
    PartnerPointsDO r = new PartnerPointsDO();
    r.setPendingRecordDate(LocalDate.of(2026, 8, 1));
    r.setPendingRequestedBy("u1");
    r.setPendingRequestedAt(LocalDateTime.now());
    PartnerPointsRules.clearPendingFields(r);
    assertNull(r.getPendingRecordDate());
    assertNull(r.getPendingRequestedBy());
    assertNull(r.getPendingRequestedAt());
}
```

（补 `import`：`PartnerPointsDO`、`LocalDate` 若尚未导入）

- [ ] **Step 2: 跑测确认失败**

```powershell
cd "D:\life_assistant\backend\lifeassistant"
mvn -pl lifeassistant-system -am test "-Dtest=PartnerPointsRulesTest" -q
```

Expected: FAIL（方法不存在）

- [ ] **Step 3: 最小实现**

在 `PartnerPointsRules` 增加：

```java
public static final int PENDING_TTL_HOURS = 24;

public static boolean isCcSkipConfirm(String fullName) {
    return "cc".equals(fullName);
}

public static boolean isPendingExpired(LocalDateTime requestedAt, LocalDateTime now) {
    if (requestedAt == null) {
        return true;
    }
    return !requestedAt.plusHours(PENDING_TTL_HOURS).isAfter(now);
}

public static void clearPendingFields(PartnerPointsDO record) {
    record.setPendingRecordDate(null);
    record.setPendingRequestedBy(null);
    record.setPendingRequestedAt(null);
}
```

- [ ] **Step 4: 跑测确认通过**

同 Step 2。Expected: PASS

- [ ] **Step 5: Commit**

```powershell
git add "backend/lifeassistant/lifeassistant-system/src/main/java/top/lifeassistant/partner/service/PartnerPointsRules.java" "backend/lifeassistant/lifeassistant-system/src/test/java/top/lifeassistant/partner/service/PartnerPointsRulesTest.java"
git commit -m "feat(partner): rules for cc skip and pending expiry"
```

---

### Task 3: 响应 DTO + Service 确认流程

**Files:**
- Create: `backend/lifeassistant/lifeassistant-system/src/main/java/top/lifeassistant/partner/model/resp/PointsDateChangeResult.java`
- Create: `backend/lifeassistant/lifeassistant-system/src/main/java/top/lifeassistant/partner/model/resp/PointsDateApproveAllResult.java`
- Create: `backend/lifeassistant/lifeassistant-system/src/main/java/top/lifeassistant/partner/model/resp/PartnerPointsHistoryResp.java`
- Modify: `backend/lifeassistant/lifeassistant-system/src/main/java/top/lifeassistant/partner/service/PartnerPointsService.java`

**Interfaces:**
- Produces:
  - `PointsDateChangeResult { String status }` — `"APPLIED"` | `"PENDING"`
  - `PointsDateApproveAllResult { int approvedCount }`
  - `PartnerPointsHistoryResp { List<PartnerPointsDO> records; long total; long size; long current; long pages; int pendingConfirmCount }`
  - `PointsDateChangeResult updateRecordDate(...)`（签名从 `void` 改为返回 result）
  - `void approveDateChange(String userId, String recordId)`
  - `void rejectDateChange(String userId, String recordId)`
  - `int approveAllDateChanges(String userId)`
  - `PartnerPointsHistoryResp getHistoryResp(String userId, int page, int size)`（替换或并行于旧 `getHistory`；Controller 改用此方法）
- Consumes: `PartnerPointsRules.*`、`UserService.getById`（取 `fullName`）、`PartnerPointsMapper`

**辅助（同一 Service 内 private）：**

```java
private void persistClearPending(String recordId) {
    mapper.update(null, new LambdaUpdateWrapper<PartnerPointsDO>()
        .eq(PartnerPointsDO::getId, recordId)
        .set(PartnerPointsDO::getPendingRecordDate, null)
        .set(PartnerPointsDO::getPendingRequestedBy, null)
        .set(PartnerPointsDO::getPendingRequestedAt, null));
}

private PartnerPointsDO requirePairRecord(String userId, String partnerId, String recordId) {
    // 现有 updateRecordDate 里的存在性 + createdBy ∈ {userId, partnerId} 校验，抽成共用
}

private void requireConfirmablePending(String confirmerId, PartnerPointsDO record, LocalDateTime now) {
    if (record.getPendingRecordDate() == null || record.getPendingRequestedBy() == null) {
        throw new BadRequestException("没有待确认的日期修改");
    }
    if (PartnerPointsRules.isPendingExpired(record.getPendingRequestedAt(), now)) {
        persistClearPending(record.getId());
        throw new BadRequestException("申请已过期");
    }
    if (!confirmerId.equals(getPartnerId(record.getPendingRequestedBy()))) {
        // 确认方必须是发起人的伴侣：即 confirmerId == partner of requester
        // 实现：String requester = record.getPendingRequestedBy();
        //       if (confirmerId.equals(requester) || !confirmerId.equals(getPartnerId(requester))) ...
        throw new BadRequestException("无权确认该申请");
    }
}
```

注意：`getPartnerId(requester)` 在 requester 已解绑时会抛「请先绑定伴侣」——可接受。

- [ ] **Step 1: 建三个 resp 类**

```java
@Data
public class PointsDateChangeResult {
    private String status; // APPLIED | PENDING
    public static PointsDateChangeResult applied() {
        PointsDateChangeResult r = new PointsDateChangeResult();
        r.status = "APPLIED";
        return r;
    }
    public static PointsDateChangeResult pending() {
        PointsDateChangeResult r = new PointsDateChangeResult();
        r.status = "PENDING";
        return r;
    }
}
```

```java
@Data
public class PointsDateApproveAllResult {
    private int approvedCount;
}
```

```java
@Data
public class PartnerPointsHistoryResp {
    private List<PartnerPointsDO> records;
    private long total;
    private long size;
    private long current;
    private long pages;
    private int pendingConfirmCount;
}
```

- [ ] **Step 2: 改写 `updateRecordDate`**

逻辑要点：

1. 校验 `recordDate`、绑定、记录归属（同现网）
2. 先 `resolveCreatedAt(recordDate, today, now)` —— 未来日尽早 400（pending 路径也拒绝未来日）
3. `UserDO me = userService.getById(userId)`；若 `isCcSkipConfirm(me.getFullName())`：
   - `record.setCreatedAt(resolved)`；`clearPendingFields`；用 UpdateWrapper 同时 set createdAt + null pending 三列（或 updateById + persistClearPending）
   - return `applied()`
4. 否则：`record.setPendingRecordDate(recordDate)`；`setPendingRequestedBy(userId)`；`setPendingRequestedAt(now)`；**不改** `createdAt`；`mapper.updateById(record)`；return `pending()`

- [ ] **Step 3: 实现 `approveDateChange` / `rejectDateChange`**

同意：

1. `requirePairRecord` + `requireConfirmablePending`
2. `LocalDateTime createdAt = resolveCreatedAt(record.getPendingRecordDate(), today, now)` —— 若抛未来日，**不**清 pending，直接抛出
3. UpdateWrapper：set `created_at` + null 三列

拒绝：`requireConfirmablePending` 后 `persistClearPending`

- [ ] **Step 4: 实现 `approveAllDateChanges`**

```java
LambdaQueryWrapper<PartnerPointsDO> q = new LambdaQueryWrapper<PartnerPointsDO>()
    .isNotNull(PartnerPointsDO::getPendingRecordDate)
    .ne(PartnerPointsDO::getPendingRequestedBy, userId); // 自己发起的不算「待我确认」
// 再在内存过滤：pendingRequestedBy 的伴侣 == userId，且未过期
```

更稳妥：查出双方流水中 `pending_requested_by = partnerId`（对方发给我的）：

```java
String partnerId = getPartnerId(userId);
List<PartnerPointsDO> list = mapper.selectList(new LambdaQueryWrapper<PartnerPointsDO>()
    .eq(PartnerPointsDO::getPendingRequestedBy, partnerId)
    .isNotNull(PartnerPointsDO::getPendingRecordDate));
int n = 0;
for (PartnerPointsDO r : list) {
    if (PartnerPointsRules.isPendingExpired(r.getPendingRequestedAt(), now)) {
        persistClearPending(r.getId());
        continue;
    }
    // 同单条同意（捕获未来日 BadRequest 则 skip 该条并保留 pending，或中止——选：单条失败 skip+continue，不中断其余）
    try {
        approveOneUnlocked(r, now); // 内部已校验确认方
        n++;
    } catch (BadRequestException ignored) {
        // 跨日未来等：保留 pending，继续下一条
    }
}
return n;
```

将单条同意核心抽成 `approveOneUnlocked` 以免重复。

- [ ] **Step 5: 实现 `getHistoryResp`**

1. 复用现有分页查询
2. 遍历当前页：过期 pending → `persistClearPending` + 内存 clear（响应里不展示）
3. `pendingConfirmCount`：查 `pending_requested_by = partnerId AND pending_record_date IS NOT NULL`，再滤未过期条数（过期的顺便清）
4. 组装 `PartnerPointsHistoryResp`

可删除或保留旧 `getHistory`；Controller 只调 `getHistoryResp`。

- [ ] **Step 6: 编译检查**

```powershell
cd "D:\life_assistant\backend\lifeassistant"
mvn -pl lifeassistant-system -am compile -q
```

Expected: SUCCESS

- [ ] **Step 7: Commit**

```powershell
git add "backend/lifeassistant/lifeassistant-system/src/main/java/top/lifeassistant/partner/model/resp/" "backend/lifeassistant/lifeassistant-system/src/main/java/top/lifeassistant/partner/service/PartnerPointsService.java"
git commit -m "feat(partner): pending date-change service flow"
```

---

### Task 4: Controller API

**Files:**
- Modify: `backend/lifeassistant/lifeassistant-system/src/main/java/top/lifeassistant/partner/controller/PartnerPointsController.java`

**Interfaces:**
- Consumes: Service 新方法
- Produces HTTP:
  - `GET /partner/points/history` → `ApiResponse<PartnerPointsHistoryResp>`
  - `PATCH /partner/points/{id}` → `ApiResponse<PointsDateChangeResult>`
  - `POST /partner/points/{id}/date-change/approve` → `ApiResponse<Void>`
  - `POST /partner/points/{id}/date-change/reject` → `ApiResponse<Void>`
  - `POST /partner/points/date-change/approve-all` → `ApiResponse<PointsDateApproveAllResult>`

注意：`approve-all` 路径必须注册在 `/{id}/...` **之前**或使用不冲突路径（当前为 `/partner/points/date-change/approve-all`，与 `/{id}` 不冲突）。

- [ ] **Step 1: 改 history 与 PATCH 返回类型；加三个 endpoint**

```java
@GetMapping("/partner/points/history")
public ApiResponse<PartnerPointsHistoryResp> getHistory(...) {
    return ApiResponse.ok(service.getHistoryResp(user.getId(), page, size));
}

@PatchMapping("/partner/points/{id}")
public ApiResponse<PointsDateChangeResult> updateRecordDate(...) {
    return ApiResponse.ok(service.updateRecordDate(user.getId(), id, req.getRecordDate()));
}

@PostMapping("/partner/points/{id}/date-change/approve")
public ApiResponse<Void> approveDateChange(@CurrentUser UserDO user, @PathVariable String id) {
    service.approveDateChange(user.getId(), id);
    return ApiResponse.ok();
}

@PostMapping("/partner/points/{id}/date-change/reject")
public ApiResponse<Void> rejectDateChange(@CurrentUser UserDO user, @PathVariable String id) {
    service.rejectDateChange(user.getId(), id);
    return ApiResponse.ok();
}

@PostMapping("/partner/points/date-change/approve-all")
public ApiResponse<PointsDateApproveAllResult> approveAllDateChanges(@CurrentUser UserDO user) {
    PointsDateApproveAllResult r = new PointsDateApproveAllResult();
    r.setApprovedCount(service.approveAllDateChanges(user.getId()));
    return ApiResponse.ok(r);
}
```

- [ ] **Step 2: 编译**

```powershell
cd "D:\life_assistant\backend\lifeassistant"
mvn -pl lifeassistant-system -am compile -q
```

- [ ] **Step 3: Commit**

```powershell
git add "backend/lifeassistant/lifeassistant-system/src/main/java/top/lifeassistant/partner/controller/PartnerPointsController.java"
git commit -m "feat(partner): date-change confirm API endpoints"
```

---

### Task 5: 前端 API 模块

**Files:**
- Modify: `front/vue3-vant-mobile/src/api/modules/partner-points.ts`

**Interfaces:**
- Produces 更新后的类型与函数，供页面消费

- [ ] **Step 1: 更新类型与 API**

```typescript
export interface PointsRecord {
  id: string
  createdBy: string
  pointsChange: number
  reason: string
  createdAt: string
  pendingRecordDate?: string | null
  pendingRequestedBy?: string | null
  pendingRequestedAt?: string | null
}

export interface PointsHistoryResult {
  records: PointsRecord[]
  total: number
  size: number
  current: number
  pages: number
  pendingConfirmCount: number
}

export type PointsDateChangeStatus = 'APPLIED' | 'PENDING'

export function getPointsHistory(page = 1, size = 20) {
  return request.get<ApiResponse<PointsHistoryResult>>('/partner/points/history', {
    params: { page, size },
  })
}

export function updatePointsRecordDate(id: string, recordDate: string) {
  return request.patch<ApiResponse<{ status: PointsDateChangeStatus }>>(`/partner/points/${id}`, { recordDate })
}

export function approvePointsDateChange(id: string) {
  return request.post<ApiResponse<void>>(`/partner/points/${id}/date-change/approve`)
}

export function rejectPointsDateChange(id: string) {
  return request.post<ApiResponse<void>>(`/partner/points/${id}/date-change/reject`)
}

export function approveAllPointsDateChanges() {
  return request.post<ApiResponse<{ approvedCount: number }>>('/partner/points/date-change/approve-all')
}
```

保留 `getPointsBalance` / `addPoints`。

- [ ] **Step 2: Commit**

```powershell
git add "front/vue3-vant-mobile/src/api/modules/partner-points.ts"
git commit -m "feat(front): partner points date-change confirm API client"
```

---

### Task 6: 积分页 UI + i18n

**Files:**
- Modify: `front/vue3-vant-mobile/src/pages/partner/dashboard/points.vue`
- Modify: `front/vue3-vant-mobile/src/locales/zh-CN.json`（`dashboard` 段）
- Modify: `front/vue3-vant-mobile/src/locales/en-US.json`（`dashboard` 段）

**Interfaces:**
- Consumes: Task 5 API；`userStore.userInfo.id`（判断是否确认方）

- [ ] **Step 1: i18n 键**

`zh-CN.json` `dashboard` 内增加：

```json
"dateChangePending": "申请改为 {date} · 待确认",
"dateChangeSubmitted": "已提交，等待对方确认",
"dateChangeApproved": "已确认日期修改",
"dateChangeRejected": "已拒绝日期修改",
"approveDateChange": "同意",
"rejectDateChange": "拒绝",
"approveAllDateChanges": "全部确认",
"approveAllDone": "已确认 {n} 条"
```

`en-US.json` 对应英文。

- [ ] **Step 2: `points.vue` 状态与加载**

```typescript
import { approveAllPointsDateChanges, approvePointsDateChange, rejectPointsDateChange, ... } from '@/api/modules/partner-points'
import { useUserStore } from '@/stores/modules/user'

const userStore = useUserStore()
const pendingConfirmCount = ref(0)

// loadHistory 内：
pendingConfirmCount.value = res.data?.pendingConfirmCount ?? 0
// records 取自 res.data?.records（形状已变，不再是 PageResult 外包一层 data 的旧 page 字段名：后端 current/pages 对齐）
```

注意：旧代码 `res.data?.records` / `total` 在新 `PointsHistoryResult` 上仍成立。

- [ ] **Step 3: 改 `onEditDateConfirm`**

```typescript
const res = await updatePointsRecordDate(editingId.value, formatYmd(val))
const status = res.data?.status
showToast(status === 'PENDING'
  ? t('dashboard.dateChangeSubmitted')
  : t('dashboard.editRecordDate') /* 或单独「已更新日期」键；可用已有 toast 文案 */)
```

若现有硬编码 `'已更新日期'`，PENDING 用 i18n，APPLIED 保持「已更新日期」或抽 `dateChangeApplied`。

- [ ] **Step 4: 列表项 pending UI**

- `history-title` 旁：`pendingConfirmCount > 0` 时 `van-badge`
- `pendingConfirmCount > 0` 时标题下/旁按钮「全部确认」→ `approveAllPointsDateChanges` → toast → 刷新
- 每条若 `item.pendingRecordDate && item.pendingRequestedBy`：
  - 展示 `$t('dashboard.dateChangePending', { date: item.pendingRecordDate })`
  - 若 `item.pendingRequestedBy !== userStore.userInfo.id`（我是确认方）：同意/拒绝按钮；`@click.stop` 防止触发改日
  - 点击同意/拒绝后刷新列表
- 有 pending 的条目：点击改日仍可打开日历（再申请覆盖）；确认按钮勿冒泡

- [ ] **Step 5: 手动看一眼页面编译**

```powershell
cd "D:\life_assistant\front\vue3-vant-mobile"
npx vue-tsc --noEmit
```

若项目惯用其它检查，用现有脚本；至少确保无 TS 报错。

- [ ] **Step 6: Commit**

```powershell
git add "front/vue3-vant-mobile/src/pages/partner/dashboard/points.vue" "front/vue3-vant-mobile/src/locales/zh-CN.json" "front/vue3-vant-mobile/src/locales/en-US.json"
git commit -m "feat(front): points page date-change confirm UI"
```

---

### Task 7: 看板入口小红点

**Files:**
- Modify: `front/vue3-vant-mobile/src/pages/partner/dashboard/index.vue`

**Interfaces:**
- Consumes: `getPointsHistory(1, 1)` 或沿用已有 history 拉取，读 `pendingConfirmCount`

- [ ] **Step 1: 在现有 `loadHistory` / 拉取积分历史处读取 `pendingConfirmCount`**

```typescript
const pendingConfirmCount = ref(0)
// 解析 res.data.pendingConfirmCount
```

若看板仍按旧 `PageResult` 读 `records`，改为：

```typescript
const data = res.data
historyList.value = data?.records ?? []
pendingConfirmCount.value = data?.pendingConfirmCount ?? 0
```

- [ ] **Step 2: 「积分记录」wide-header 加 badge**

```vue
<div class="wide-header">
  <span class="wide-title-with-badge">
    {{ $t('dashboard.pointsHistory') }}
    <van-badge v-if="pendingConfirmCount > 0" :content="pendingConfirmCount" />
  </span>
  <van-icon name="arrow" color="#ccc" />
</div>
```

- [ ] **Step 3: Commit**

```powershell
git add "front/vue3-vant-mobile/src/pages/partner/dashboard/index.vue"
git commit -m "feat(front): badge pending date-change on dashboard points entry"
```

---

### Task 8: 版本与 CHANGELOG

**Files:**
- Modify: `front/vue3-vant-mobile/package.json`（`1.8.3` → `1.9.0`）
- Modify: `backend/lifeassistant/lifeassistant-server/src/main/resources/config/application.yml`（`1.5.3-SNAPSHOT` → `1.6.0-SNAPSHOT`）
- Modify: `CHANGELOG.md`（顶部新节）

- [ ] **Step 1: bump + 写日志**

`CHANGELOG.md` 顶部：

```markdown
## v1.9.0 (2026-08-10)

### ✨ 新特性

- **积分改日期对方确认**：非 cc 修改流水日期需伴侣同意/拒绝；支持一键全部确认；看板与积分页小红点；cc 发起仍即时生效；申请 24h 过期

### 🖥 后端

- `partner_points` pending 三列；approve/reject/approve-all；history 带 `pendingConfirmCount`
- 版本号: `1.5.3-SNAPSHOT` → `1.6.0-SNAPSHOT`

### 📱 前端

- 积分页确认 UI；看板入口红点
- 版本号: `1.8.3` → `1.9.0`
```

- [ ] **Step 2: Commit**

```powershell
git add "front/vue3-vant-mobile/package.json" "backend/lifeassistant/lifeassistant-server/src/main/resources/config/application.yml" "CHANGELOG.md"
git commit -m "chore: bump app to v1.9.0 for points date-change confirm"
```

---

### Task 9: 端到端冒烟（手工）

- [ ] **Step 1: 启动后端（确保 Flyway V19 成功）与前端**
- [ ] **Step 2: 用非 cc 账号改一条流水日期 → 日期不变、出现待确认**
- [ ] **Step 3: 用伴侣账号看红点 → 同意 → 日期变；再测拒绝、覆盖申请、全部确认**
- [ ] **Step 4: 用 `fullName=cc` 账号改日期 → 直接生效、无 pending**
- [ ] **Step 5: 确认余额数字始终不变**

（无需为手工步骤单独 commit）

---

## Self-review (plan vs spec)

| Spec 项 | Task |
|---------|------|
| MySQL pending 三列 | 1 |
| cc 精确匹配跳过 | 2, 3 |
| 24h 过期懒清理 | 2, 3 |
| PATCH APPLIED/PENDING | 3, 4 |
| approve / reject / approve-all | 3, 4 |
| history + pendingConfirmCount | 3, 4, 5 |
| 积分页 UI + 全部确认 | 6 |
| 入口小红点 | 7 |
| 非目标（推送等）未纳入 | — |
| 发版 | 8 |
| 余额不变 | 3 + 9 |

无 TBD 占位；清 NULL 用 UpdateWrapper 已写明。
