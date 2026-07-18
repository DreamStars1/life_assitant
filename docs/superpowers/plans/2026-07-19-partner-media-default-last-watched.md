# 伴侣页默认「一起看过的」+ 可选「上次一起看」日期 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 伴侣页默认打开「一起看过的」；「上次一起看」改为独立可选日期字段，编辑可选手填/清空，更新共同进度时强制刷成当天。

**Architecture:** Flyway 给 `shared_media` 加 `last_watched_at DATE`；CRUD 透传该字段（PATCH 用「缺省=不改 / 空串=清空 / 有值=设置」三态）；`MediaProgressService` 在 `scope=shared` 时写当天。前端默认 Tab 改 `media`，添加/编辑弹窗用 `van-calendar`。

**Tech Stack:** Spring Boot 3 + MyBatis-Plus + Flyway / Vue 3 + Vant 4

## Global Constraints

- 日期精度只要 `DATE`（`yyyy-MM-dd`），不要时分秒
- 更新共同进度总是刷新 `last_watched_at` 为当天；个人进度 / 评论 / 改标题封面不改该字段
- 新建未传日期时默认当天；可清空
- 列表展示读 `lastWatchedAt`，不再用 `updateTime`
- `touchActivity` 继续只碰 `updateTime`，与「上次一起看」解耦
- 不改排序、不按该字段筛选、不加观看历史表
- PowerShell 环境：git commit 用 `-m "msg"`，路径用双引号

---

## 文件结构

```
backend/lifeassistant/lifeassistant-server/src/main/resources/db/migration/
  V10__add_shared_media_last_watched_at.sql          — 新增列

backend/.../sharedmedia/model/entity/SharedMediaDO.java
backend/.../sharedmedia/model/req/SharedMediaCreateReq.java
backend/.../sharedmedia/model/req/SharedMediaUpdateReq.java
backend/.../sharedmedia/model/resp/SharedMediaResp.java
backend/.../sharedmedia/controller/SharedMediaController.java
backend/.../sharedmedia/service/SharedMediaService.java
backend/.../sharedmedia/service/MediaProgressService.java

front/vue3-vant-mobile/src/api/modules/shared-media.ts
front/vue3-vant-mobile/src/pages/share/index.vue
```

---

### Task 1: Flyway + Entity/Resp 字段

**Files:**
- Create: `backend/lifeassistant/lifeassistant-server/src/main/resources/db/migration/V10__add_shared_media_last_watched_at.sql`
- Modify: `backend/lifeassistant/lifeassistant-system/src/main/java/top/lifeassistant/sharedmedia/model/entity/SharedMediaDO.java`
- Modify: `backend/lifeassistant/lifeassistant-system/src/main/java/top/lifeassistant/sharedmedia/model/resp/SharedMediaResp.java`

**Interfaces:**
- Produces: `SharedMediaDO.lastWatchedAt: LocalDate`；`SharedMediaResp.lastWatchedAt: LocalDate`（JSON 序列化为 `yyyy-MM-dd`）

- [ ] **Step 1: 写迁移**

```sql
ALTER TABLE shared_media
    ADD COLUMN last_watched_at DATE DEFAULT NULL COMMENT '上次一起看日期' AFTER finished_at;
```

- [ ] **Step 2: Entity 增加字段**

在 `SharedMediaDO` 的 `finishedAt` 后增加：

```java
/** 上次一起看日期 */
private java.time.LocalDate lastWatchedAt;
```

（或顶部已有 `import java.time.LocalDate;` 后写 `private LocalDate lastWatchedAt;`）

- [ ] **Step 3: Resp 映射**

`SharedMediaResp`：
- 增加 `private LocalDate lastWatchedAt;`（`import java.time.LocalDate;`）
- `from()` 增加 `.lastWatchedAt(media.getLastWatchedAt())`

- [ ] **Step 4: Commit**

```powershell
git add "backend/lifeassistant/lifeassistant-server/src/main/resources/db/migration/V10__add_shared_media_last_watched_at.sql" "backend/lifeassistant/lifeassistant-system/src/main/java/top/lifeassistant/sharedmedia/model/entity/SharedMediaDO.java" "backend/lifeassistant/lifeassistant-system/src/main/java/top/lifeassistant/sharedmedia/model/resp/SharedMediaResp.java"
git commit -m "feat: add shared_media.last_watched_at column and response field"
```

---

### Task 2: 创建/更新透传 lastWatchedAt

**Files:**
- Modify: `.../model/req/SharedMediaCreateReq.java`
- Modify: `.../model/req/SharedMediaUpdateReq.java`
- Modify: `.../controller/SharedMediaController.java`
- Modify: `.../service/SharedMediaService.java`

**Interfaces:**
- Consumes: Task 1 的 `lastWatchedAt` 字段
- Produces:
  - Create：未传/空 → `LocalDate.now()`；有值 → parse
  - Update 三态：`lastWatchedAt` 参数 `null`=不改；`""`=清空；`"yyyy-MM-dd"`=设置
  - 清空必须用 `LambdaUpdateWrapper.set(..., null)`（MyBatis-Plus 默认跳过 null，`updateById` 清不掉）

- [ ] **Step 1: Req 加字段**

`SharedMediaCreateReq` / `SharedMediaUpdateReq` 各加：

```java
@Schema(description = "上次一起看日期 yyyy-MM-dd，可空")
private String lastWatchedAt;
```

- [ ] **Step 2: Controller 接参并塞进 Req**

`create` 方法增加：

```java
@RequestParam(value = "lastWatchedAt", required = false) String lastWatchedAt
```

在组装 `SharedMediaCreateReq` 后：`req.setLastWatchedAt(lastWatchedAt);`

`update` 方法同样增加该 `@RequestParam`，并 `req.setLastWatchedAt(lastWatchedAt);`

注意：multipart 不传该键时 Spring 给 `null`；前端传空串时是 `""`。不要把 `""` 转成 `null` 再塞进 UpdateReq，否则丢失「清空」语义。CreateReq 可以原样传入。

- [ ] **Step 3: Service create 默认今天**

在 `SharedMediaService.create`，`mapper.insert` 前：

```java
if (req.getLastWatchedAt() == null || req.getLastWatchedAt().isBlank()) {
    media.setLastWatchedAt(java.time.LocalDate.now());
} else {
    media.setLastWatchedAt(java.time.LocalDate.parse(req.getLastWatchedAt().trim()));
}
```

- [ ] **Step 4: Service update 三态**

在 `SharedMediaService.update`，现有字段赋值之后、`mapper.updateById(media)` 之前：

```java
String lw = req.getLastWatchedAt();
if (lw != null) {
    if (lw.isBlank()) {
        mapper.update(null, new com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<SharedMediaDO>()
            .eq(SharedMediaDO::getId, id)
            .set(SharedMediaDO::getLastWatchedAt, null));
        // 同步内存对象，避免后续 from() 读到旧值；最终仍以 selectById 为准
        media.setLastWatchedAt(null);
    } else {
        media.setLastWatchedAt(java.time.LocalDate.parse(lw.trim()));
    }
}
```

然后照常 `mapper.updateById(media); return SharedMediaResp.from(mapper.selectById(id));`

若清空走了单独 `update`，紧接着的 `updateById` 仍可更新其他字段；`lastWatchedAt` 已被置 null 且 `updateById` 因 FieldStrategy 不会把它写回有值，安全。

- [ ] **Step 5: 加 `markLastWatchedToday` 供进度用**

在 `SharedMediaService` 增加：

```java
/** 共同进度更新时刷新「上次一起看」为当天 */
public void markLastWatchedToday(String mediaId) {
    SharedMediaDO media = new SharedMediaDO();
    media.setId(mediaId);
    media.setLastWatchedAt(java.time.LocalDate.now());
    mapper.updateById(media);
}
```

- [ ] **Step 6: 手动冒烟（后端起来后）**

1. POST `/shared-media` 不带 `lastWatchedAt` → 响应 `lastWatchedAt` 为今天
2. PATCH 带 `lastWatchedAt=`（空）→ 响应 `lastWatchedAt: null`
3. PATCH 带 `lastWatchedAt=2026-07-01` → 响应对应日期
4. PATCH 只带 `isFinished`、不带 `lastWatchedAt` → 日期不变

- [ ] **Step 7: Commit**

```powershell
git add "backend/lifeassistant/lifeassistant-system/src/main/java/top/lifeassistant/sharedmedia/model/req/SharedMediaCreateReq.java" "backend/lifeassistant/lifeassistant-system/src/main/java/top/lifeassistant/sharedmedia/model/req/SharedMediaUpdateReq.java" "backend/lifeassistant/lifeassistant-system/src/main/java/top/lifeassistant/sharedmedia/controller/SharedMediaController.java" "backend/lifeassistant/lifeassistant-system/src/main/java/top/lifeassistant/sharedmedia/service/SharedMediaService.java"
git commit -m "feat: accept lastWatchedAt on shared media create/update"
```

---

### Task 3: 共同进度刷新 last_watched_at

**Files:**
- Modify: `backend/lifeassistant/lifeassistant-system/src/main/java/top/lifeassistant/sharedmedia/service/MediaProgressService.java`

**Interfaces:**
- Consumes: `SharedMediaService.markLastWatchedToday(String mediaId)`
- Produces: `scope=shared` 的 `update` 会把该媒体的 `last_watched_at` 写成当天；`personal` 不调用

- [ ] **Step 1: 在 shared 分支调用**

在 `MediaProgressService.update`，现有 shared 分支的三个 `upsertProgress` 之后（`touchActivity` 之前或之后均可）增加：

```java
if ("shared".equals(req.getScope())) {
    sharedMediaService.markLastWatchedToday(mediaId);
}
```

完整结构保持：

```java
if ("shared".equals(req.getScope())) {
    upsertProgress(mediaId, null, req.getProgressText());
    upsertProgress(mediaId, user.getId(), req.getProgressText());
    if (user.getPartnerId() != null) {
        upsertProgress(mediaId, user.getPartnerId(), req.getProgressText());
    }
    sharedMediaService.markLastWatchedToday(mediaId);
} else if ("personal".equals(req.getScope())) {
    upsertProgress(mediaId, user.getId(), req.getProgressText());
} else {
    throw new BadRequestException("scope 必须是 shared 或 personal");
}

sharedMediaService.touchActivity(mediaId);
// ... return 不变
```

- [ ] **Step 2: 冒烟**

1. 某媒体 `lastWatchedAt` 设为 `2026-01-01`
2. PUT progress `scope=personal` → 日期仍为 `2026-01-01`
3. PUT progress `scope=shared` → 日期变为今天
4. POST comment → 日期不变

- [ ] **Step 3: Commit**

```powershell
git add "backend/lifeassistant/lifeassistant-system/src/main/java/top/lifeassistant/sharedmedia/service/MediaProgressService.java"
git commit -m "feat: refresh lastWatchedAt when updating shared progress"
```

---

### Task 4: 前端默认 Tab + 日期选择 + 列表展示

**Files:**
- Modify: `front/vue3-vant-mobile/src/api/modules/shared-media.ts`
- Modify: `front/vue3-vant-mobile/src/pages/share/index.vue`

**Interfaces:**
- Consumes: API 字段 `lastWatchedAt: string | null`（`yyyy-MM-dd`）
- Produces: 默认 `activeTab='media'`；创建/编辑 FormData 带 `lastWatchedAt`（可空串）；列表用该字段格式化显示

- [ ] **Step 1: 类型**

`SharedMediaItem` 增加：

```typescript
lastWatchedAt: string | null
```

可保留 `updateTime`（其它逻辑可能仍用），但列表「上次一起看」不再读它。

- [ ] **Step 2: 默认 Tab 与加载**

```typescript
const activeTab = ref<'records' | 'media'>('media')
```

`onMounted` / `watch(partnerId)`：有伴侣时调用 `loadMedia()`（可与 `goToPage(1)` 并存，或按当前 tab 分支）。推荐：

```typescript
onMounted(async () => {
  await userStore.info()
  if (partnerId.value) {
    await loadMedia()
  }
})

watch(partnerId, async (val) => {
  if (val)
    await loadMedia()
})
```

保留 `watch(activeTab, ...)`：切到 `records` 且 records 空时再 `goToPage(1)`；切到 `media` 且 media 空时 `loadMedia()`。

- [ ] **Step 3: 表单状态**

```typescript
const addMediaForm = reactive({ title: '', mediaType: 'movie', description: '', lastWatchedAt: '' })
const editMediaForm = reactive({ title: '', mediaType: 'movie', description: '', lastWatchedAt: '' })
const showAddLastWatchedCalendar = ref(false)
const showEditLastWatchedCalendar = ref(false)
```

打开添加弹窗时（`showAddMedia = true` 前或 `@click` 包装函数）设：

```typescript
addMediaForm.lastWatchedAt = toLocalDateStr(new Date())
```

`openEditMedia`：

```typescript
editMediaForm.lastWatchedAt = item.lastWatchedAt ? item.lastWatchedAt.slice(0, 10) : ''
```

日历 confirm：

```typescript
function onAddLastWatchedConfirm(d: Date) {
  addMediaForm.lastWatchedAt = toLocalDateStr(d)
  showAddLastWatchedCalendar.value = false
}
function onEditLastWatchedConfirm(d: Date) {
  editMediaForm.lastWatchedAt = toLocalDateStr(d)
  showEditLastWatchedCalendar.value = false
}
```

- [ ] **Step 4: 提交 FormData**

`onAddMedia` 在现有 append 后：

```typescript
fd.append('lastWatchedAt', addMediaForm.lastWatchedAt || '')
```

成功后重置时：`addMediaForm.lastWatchedAt = toLocalDateStr(new Date())`（或下次打开再设）。

`onEditMedia`：

```typescript
fd.append('lastWatchedAt', editMediaForm.lastWatchedAt || '')
```

编辑必须始终 append（含空串），才能清空。

- [ ] **Step 5: 弹窗 UI**

添加 / 编辑 dialog 内、简介字段附近增加（与 records 的日期 field 同风格）：

```vue
<van-field
  v-model="addMediaForm.lastWatchedAt"
  is-link
  readonly
  clearable
  label="上次一起看"
  placeholder="可选日期"
  @click="showAddLastWatchedCalendar = true"
  @clear="addMediaForm.lastWatchedAt = ''"
/>
<van-calendar
  v-model:show="showAddLastWatchedCalendar"
  :min-date="new Date('2020-01-01')"
  @confirm="onAddLastWatchedConfirm"
/>
```

编辑弹窗同理用 `editMediaForm` / `showEditLastWatchedCalendar` / `onEditLastWatchedConfirm`。

若 `clearable` + `readonly` 在 Vant 版本上 clear 不触发，可在 field 旁加「清空」文字按钮：`@click.stop="addMediaForm.lastWatchedAt = ''"`。

- [ ] **Step 6: 列表展示**

把：

```vue
<div v-if="item.updateTime" class="text-xs text-gray-400 mt-1">
  上次一起看：{{ formatLastWatched(item.updateTime) }}
</div>
```

改为：

```vue
<div v-if="item.lastWatchedAt" class="text-xs text-gray-400 mt-1">
  上次一起看：{{ formatLastWatchedDate(item.lastWatchedAt) }}
</div>
```

```typescript
function formatLastWatchedDate(iso: string | null): string {
  if (!iso)
    return ''
  return iso.slice(0, 10)
}
```

可删除仅被旧逻辑使用的 `formatLastWatched`（若无其它引用）。

- [ ] **Step 7: 手动验收（对照 spec）**

1. 打开 `/share` 默认「一起看过的」且有列表/空态
2. 新建：默认今天；改日期后列表显示所选；清空后不显示该行
3. 编辑可改/可清空
4. 详情更新共同进度后回列表，日期为今天；个人进度/评论不改日期

- [ ] **Step 8: Commit**

```powershell
git add "front/vue3-vant-mobile/src/api/modules/shared-media.ts" "front/vue3-vant-mobile/src/pages/share/index.vue"
git commit -m "feat: default share tab to media and editable lastWatchedAt"
```

---

## Spec coverage checklist

| Spec 要求 | Task |
|-----------|------|
| 默认 Tab = 一起看过的 + 加载媒体 | Task 4 |
| `last_watched_at` 列 | Task 1 |
| 创建默认今天 / 可选手填 | Task 2 + 4 |
| 更新可改可清空（三态） | Task 2 + 4 |
| 共同进度强制当天 | Task 3 |
| 个人进度/评论/改元数据不改日期 | Task 2/3（缺省不改 + 仅 shared 调用） |
| 列表用 `lastWatchedAt` | Task 4 |
| 不做排序/筛选/时分秒 | 全任务遵守 Global Constraints |
