# 记录 Tab 拆分 / 待办导航合并 / 私密·分类·进度时间轴 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 去掉今日 Tab；待办截止日期分色；「一起看过的」独立为「记录」Tab；支持私密、自定义分类、进度时间轴；列表按观看时间排序；现有 MCP `media` 工具保持可用。

**Architecture:** Flyway 增量加 `is_private`、`media_category`、`media_progress_event`；`SharedMediaService` 改排序并解析 `mediaTypeLabel`；前端拆 `/records`，`/share` 只留一起做过的事；`/` 重定向到 `/todos`。私密仅前端过滤。进度更新事务内追加 event，详情进独立时间轴页。

**Tech Stack:** Spring Boot 3 + MyBatis-Plus + Flyway / Vue 3 + Vant 4 / 现有 `python/agent` media 工具（不改调用面）

**Spec:** `docs/superpowers/specs/2026-07-20-records-tab-privacy-categories-design.md`

## Global Constraints

- REST 前缀保持 `/shared-media`；MCP `media` list/get/create/update 入参/路径不变；新字段未传则默认（`is_private=false`）
- 响应可增量字段；不得删除或改义现有 JSON 字段
- 私密：只藏 UI，不做服务端 404 强拦
- 自定义分类 id 用 UUID，不得与 `movie`/`book`/`tv`/`uncategorized` 碰撞
- 删分类：引用记录 `media_type` → `uncategorized`
- 时间轴只记进度更新；独立页可进入
- 排序：`ORDER BY COALESCE(last_watched_at, DATE(update_time)) DESC, update_time DESC`
- PowerShell：`git commit -m "msg"`；路径用双引号
- 下一 Flyway 版本号从 **V12** 起（V11 已占用）

---

## 文件结构

```
backend/.../db/migration/
  V12__shared_media_is_private.sql
  V13__create_media_category.sql
  V14__create_media_progress_event.sql

backend/.../sharedmedia/model/entity/SharedMediaDO.java          — +isPrivate
backend/.../sharedmedia/model/resp/SharedMediaResp.java          — +isPrivate, mediaTypeLabel
backend/.../sharedmedia/model/req/SharedMediaCreateReq.java      — +isPrivate
backend/.../sharedmedia/model/req/SharedMediaUpdateReq.java      — +isPrivate
backend/.../sharedmedia/controller/SharedMediaController.java    — +isPrivate param；删媒体时删 events
backend/.../sharedmedia/service/SharedMediaService.java          — 排序、label、isPrivate
backend/.../sharedmedia/service/MediaTypeLabels.java             — 集中解析 label（新建）
backend/.../sharedmedia/mapper/SharedMediaMapper.java            — 若仍有原生 ORDER BY 则改掉

backend/.../mediacategory/                                      — 新包（或放 sharedmedia 下）
  entity/MediaCategoryDO.java
  mapper/MediaCategoryMapper.java
  service/MediaCategoryService.java
  controller/MediaCategoryController.java
  model/req|resp/...

backend/.../sharedmedia/model/entity/MediaProgressEventDO.java
backend/.../sharedmedia/mapper/MediaProgressEventMapper.java
backend/.../sharedmedia/service/MediaProgressService.java        — 写 event
backend/.../sharedmedia/controller/MediaProgressController.java  — +list events（或挂 SharedMediaController）

front/.../components/TabBar.vue
front/.../config/routes.ts
front/.../router/index.ts
front/.../pages/login/index.vue
front/.../pages/index.vue                                       — 重定向
front/.../pages/todos/index.vue                                 — 截止日期色
front/.../pages/share/index.vue                                 — 去掉 media tab
front/.../pages/records/index.vue                               — 新建（迁媒体列表）
front/.../pages/share/media/[id].vue                            — 可迁到 records/media/[id].vue；加时间轴入口
front/.../pages/records/media/[id]/timeline.vue                — 进度时间轴页
front/.../src/api/modules/shared-media.ts
front/.../src/api/modules/media-category.ts                     — 新建
```

---

### Task 1: 导航合并 + 待办截止日期颜色

**Files:**
- Modify: `front/vue3-vant-mobile/src/components/TabBar.vue`
- Modify: `front/vue3-vant-mobile/src/config/routes.ts`
- Modify: `front/vue3-vant-mobile/src/router/index.ts`
- Modify: `front/vue3-vant-mobile/src/pages/login/index.vue`
- Modify: `front/vue3-vant-mobile/src/pages/index.vue`
- Modify: `front/vue3-vant-mobile/src/pages/todos/index.vue`

**Interfaces:**
- Produces: TabBar 无今日；`/` → `/todos`；登录落地 `Todos`；待办 due 色标 class

- [ ] **Step 1: TabBar 去掉今日，保留 待办/伴侣/看板/我的（本 Task 暂不加点「记录」，Task 2 加）**

`TabBar.vue` 删除 `to="/"` 的今日项；第一项为 `to="/todos"`。

- [ ] **Step 2: `rootRouteList` 去掉 `'Today'`**

```ts
export const rootRouteList: readonly string[] = [
  'Todos',
  'Events',
  'LifeLog',
  'Profile',
  'Share',
  'PartnerDashboard',
]
```

- [ ] **Step 3: 登录与已登录跳转改为 `Todos`**

`router/index.ts`：`return { name: 'Todos' }`（原 `Today`）。
`login/index.vue`：`name: (redirect as any) || 'Todos'`。

- [ ] **Step 4: `pages/index.vue` 改成重定向（对齐 `events/index.vue`）**

整文件替换为：

```vue
<script setup lang="ts">
import { onMounted } from 'vue'
import { useRouter } from 'vue-router'

const router = useRouter()

onMounted(() => {
  router.replace('/todos')
})
</script>

<template>
  <div class="redirect-page">
    <van-loading />
  </div>
</template>

<style scoped>
.redirect-page {
  display: flex;
  justify-content: center;
  padding: 48px;
}
</style>

<route lang="json5">
{
  name: 'Today'
}
</route>
```

（保留路由名 `Today` 减少类型文件抖动；不进 `rootRouteList`。）

- [ ] **Step 5: 待办截止日期色标**

在 `todos/index.vue` 增加：

```ts
function dueDateDiff(iso: string | null | undefined): number | null {
  if (!iso) return null
  const d = new Date(`${iso.slice(0, 10)}T00:00:00`)
  const now = new Date()
  const today = new Date(now.getFullYear(), now.getMonth(), now.getDate())
  return Math.round((d.getTime() - today.getTime()) / 86400000)
}

function dueLabelClass(iso: string | null | undefined): string {
  const diff = dueDateDiff(iso)
  if (diff == null) return ''
  if (diff < 0) return 'due-overdue'
  if (diff === 0) return 'due-today'
  if (diff === 1) return 'due-tomorrow'
  return 'due-later'
}

function formatDateLabel(iso: string | null | undefined): string {
  const diff = dueDateDiff(iso)
  if (diff == null) return ''
  if (diff < 0) return `已逾期 ${Math.abs(diff)} 天`
  if (diff === 0) return '今天'
  if (diff === 1) return '明天'
  if (diff === 2) return '后天'
  const d = new Date(`${iso!.slice(0, 10)}T00:00:00`)
  return `${d.getMonth() + 1}/${d.getDate()}`
}
```

模板日期行改为：

```vue
<span
  v-if="todo.dueDate"
  class="meta-item"
  :class="dueLabelClass(todo.dueDate)"
>
  <van-icon name="clock-o" /> {{ formatDateLabel(todo.dueDate) }}
</span>
```

样式：

```css
.meta-item.due-overdue { color: #ee0a24; font-weight: 500; }
.meta-item.due-today { color: #ff976a; font-weight: 500; }
.meta-item.due-tomorrow { color: #f2c97d; font-weight: 500; }
.meta-item.due-later { color: var(--van-gray-5); }
```

- [ ] **Step 6: 手测要点**

打开 `/` → 落到 `/todos`；TabBar 无「今日」；登录后进待办；列表逾期/今天/明天/更远颜色不同。

- [ ] **Step 7: Commit**

```powershell
git add "front/vue3-vant-mobile/src/components/TabBar.vue" "front/vue3-vant-mobile/src/config/routes.ts" "front/vue3-vant-mobile/src/router/index.ts" "front/vue3-vant-mobile/src/pages/login/index.vue" "front/vue3-vant-mobile/src/pages/index.vue" "front/vue3-vant-mobile/src/pages/todos/index.vue"
git commit -m "feat: remove today tab, redirect home to todos, colorize due dates"
```

---

### Task 2: 记录 Tab 拆出 + 列表排序

**Files:**
- Modify: `backend/.../sharedmedia/service/SharedMediaService.java`
- Modify: `backend/.../sharedmedia/mapper/SharedMediaMapper.java`（若存在原生 SQL ORDER BY）
- Create: `front/vue3-vant-mobile/src/pages/records/index.vue`
- Modify: `front/vue3-vant-mobile/src/pages/share/index.vue`
- Modify: `front/vue3-vant-mobile/src/components/TabBar.vue`
- Modify: `front/vue3-vant-mobile/src/config/routes.ts`
- Optional move: `front/.../pages/share/media/[id].vue` → `front/.../pages/records/media/[id].vue`（若移动，更新所有跳转）

**Interfaces:**
- Consumes: 现有 `/shared-media` API
- Produces: 路由名 `Records`；TabBar「记录」→ `/records`；列表服务端新排序

- [ ] **Step 1: 后端排序**

`SharedMediaService.list` 中把 `wrapper.orderByDesc(SharedMediaDO::getUpdateTime)` 换成 `.last("ORDER BY COALESCE(last_watched_at, DATE(update_time)) DESC, update_time DESC")`（MyBatis-Plus `last` 防注入：字面常量即可）。

若 `SharedMediaMapper` 仍有：

```java
@Select("... ORDER BY update_time DESC")
```

改为同样 `COALESCE` 排序，或确认该查询已不被 list 使用后删掉/对齐。

- [ ] **Step 2: 从 `share/index.vue` 抽出媒体相关代码到 `records/index.vue`**

- 新建 `pages/records/index.vue`：迁入原 `activeTab === 'media'` 的脚本状态、加载、筛选、添加/编辑弹窗、列表模板。
- `share/index.vue`：删除 `van-tabs` 与 media 分支，只保留一起做过的事（原 records 内容铺满页面）。
- 路由：

```json5
{ name: 'Records' }
```

- [ ] **Step 3: TabBar + rootRouteList 增加记录**

在待办与伴侣之间（或伴侣与看板之间——**按 spec：待办、伴侣、记录、看板、我的**）：

```vue
<van-tabbar-item replace to="/share">伴侣 ...</van-tabbar-item>
<van-tabbar-item replace to="/records">
  记录
  <template #icon>
    <van-icon name="bookmark-o" />
  </template>
</van-tabbar-item>
```

`rootRouteList` 加入 `'Records'`。

- [ ] **Step 4: 详情路径**

优先保留 `/share/media/:id` 可用（少改），记录列表跳转仍指向现有详情；或迁到 `/records/media/:id` 并改所有 `router.push`。二选一写死，不要两套并存无跳转。

- [ ] **Step 5: 手测**

伴侣页无媒体 Tab；记录页有列表；新看过日期靠前；无 `last_watched_at` 的按 `update_time` 日排序。

- [ ] **Step 6: Commit**

```powershell
git add "backend/lifeassistant/lifeassistant-system/src/main/java/top/lifeassistant/sharedmedia/" "front/vue3-vant-mobile/src/pages/records/" "front/vue3-vant-mobile/src/pages/share/index.vue" "front/vue3-vant-mobile/src/components/TabBar.vue" "front/vue3-vant-mobile/src/config/routes.ts"
git commit -m "feat: split records tab from share and sort media by watch date"
```

---

### Task 3: 私密字段 + 列表文案 + 前端过滤

**Files:**
- Create: `backend/.../db/migration/V12__shared_media_is_private.sql`
- Modify: `SharedMediaDO` / `CreateReq` / `UpdateReq` / `SharedMediaResp` / `SharedMediaController` / `SharedMediaService`
- Modify: `front/.../api/modules/shared-media.ts`
- Modify: `front/.../pages/records/index.vue`
- Modify: 详情页（若编辑表单在详情）

**Interfaces:**
- Produces: `isPrivate: boolean`（JSON）；multipart 可选 `isPrivate`；未传默认 false
- 前端：过滤对方私密；私密/共享时间文案

- [ ] **Step 1: Flyway**

```sql
ALTER TABLE shared_media
    ADD COLUMN is_private TINYINT(1) NOT NULL DEFAULT 0 COMMENT '1=仅自己可见' AFTER last_watched_at;
```

- [ ] **Step 2: Java 模型与 CRUD**

- `SharedMediaDO.isPrivate`：`Boolean`
- Create：`req.getIsPrivate() == null ? false : req.getIsPrivate()`
- Update：仅当请求带了 `isPrivate` 时更新（multipart：参数缺省=不改；可传 `true`/`false`）
- Controller create/update 增加 `@RequestParam(value = "isPrivate", required = false) Boolean isPrivate`
- Resp：`isPrivate`；`from()` 映射

- [ ] **Step 3: 前端类型与表单**

`SharedMediaItem` 增加 `isPrivate: boolean`。
添加/编辑：`van-switch`「仅自己可见」，默认 false；`FormData.append('isPrivate', ...)`。

- [ ] **Step 4: 列表过滤与文案**

```ts
const visibleMedia = computed(() =>
  mediaRecords.value.filter(
    (item) => !(item.isPrivate && item.createdBy !== userStore.userInfo.id),
  ),
)

function formatMediaTimeLine(item: SharedMediaItem): string {
  if (item.isPrivate) {
    if (item.lastWatchedAt) return `上次观看：${item.lastWatchedAt}`
    return `更新于：${item.updateTime.slice(0, 10)}`
  }
  if (item.lastWatchedAt) return `上次一起看：${item.lastWatchedAt}`
  return ''
}
```

列表绑定 `visibleMedia`，展示 `formatMediaTimeLine`。

- [ ] **Step 5: 手测**

A 设私密后，B 的记录列表看不到；A 仍可见；MCP 不传 `isPrivate` 创建仍为共享。

- [ ] **Step 6: Commit**

```powershell
git add "backend/lifeassistant/lifeassistant-server/src/main/resources/db/migration/V12__shared_media_is_private.sql" "backend/lifeassistant/lifeassistant-system/src/main/java/top/lifeassistant/sharedmedia/" "front/vue3-vant-mobile/src/api/modules/shared-media.ts" "front/vue3-vant-mobile/src/pages/records/"
git commit -m "feat: add shared_media is_private with UI filter and copy"
```

---

### Task 4: 自定义分类

**Files:**
- Create: `V13__create_media_category.sql`
- Create: category 包（entity/mapper/service/controller/req/resp）
- Create: `MediaTypeLabels.java`（或放 Service 内 private 方法，优先独立类便于复用）
- Modify: `SharedMediaResp` + list/get `from` 填充 `mediaTypeLabel`
- Create: `front/.../api/modules/media-category.ts`
- Modify: `records/index.vue` 类型选择器与分类管理 UI

**Interfaces:**
- `GET/POST /media-categories`；`PATCH/DELETE /media-categories/{id}`
- DELETE：事务内 `UPDATE shared_media SET media_type='uncategorized' WHERE created_by=? AND media_type=?` 再删分类
- `SharedMediaResp.mediaTypeLabel: String`

- [ ] **Step 1: 建表**

```sql
CREATE TABLE media_category (
    id          VARCHAR(36)  NOT NULL PRIMARY KEY,
    user_id     VARCHAR(36)  NOT NULL,
    name        VARCHAR(64)  NOT NULL,
    created_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_media_category_user (user_id),
    UNIQUE KEY uk_media_category_user_name (user_id, name)
) COMMENT '用户自定义媒体分类';
```

- [ ] **Step 2: CRUD Service**

- list：当前用户
- create：UUID id；name trim 非空；重名 → `BadRequestException("分类名已存在")`
- rename：仅本人；重名校验
- delete：`@Transactional`；回落 `uncategorized`；删行

- [ ] **Step 3: `MediaTypeLabels.resolve(mediaType, userIdOptionalForLookup)`**

```java
// 伪代码
if ("movie".equals(t)) return "电影";
if ("book".equals(t)) return "书籍";
if ("tv".equals(t)) return "漫剧";
if ("uncategorized".equals(t)) return "未分类";
// else 按 id 查 media_category.name；找不到则返回 t 或「未分类」
```

list/get 每条设置 `mediaTypeLabel`。查分类可用一次 map 批量避免 N+1。

- [ ] **Step 4: 前端**

- API 模块：`fetchCategories` / `createCategory` / `updateCategory` / `deleteCategory`
- 筛选 chips：全部 + 电影/书籍/漫剧 + 用户分类 + 未分类
- 添加/编辑 picker：`mediaTypeColumns = 默认 + 自定义`
- 简单管理弹层：输入名添加；左滑或按钮删（确认后调 DELETE）

- [ ] **Step 5: 手测**

新建分类并用于记录；删分类后记录显示「未分类」；对方共享记录能看到 `mediaTypeLabel`；MCP 仍用 `movie` 创建成功。

- [ ] **Step 6: Commit**

```powershell
git add "backend/lifeassistant/lifeassistant-server/src/main/resources/db/migration/V13__create_media_category.sql" "backend/lifeassistant/lifeassistant-system/src/main/java/top/lifeassistant/" "front/vue3-vant-mobile/src/api/modules/media-category.ts" "front/vue3-vant-mobile/src/pages/records/"
git commit -m "feat: user media categories with uncategorized fallback"
```

---

### Task 5: 进度时间轴

**Files:**
- Create: `V14__create_media_progress_event.sql`
- Create: `MediaProgressEventDO` / Mapper / Resp
- Modify: `MediaProgressService.update` — 追加 event
- Modify: delete 媒体时删 events
- Add: `GET /shared-media/{id}/progress-events`
- Modify: 详情页入口
- Create: `front/.../pages/records/media/[id]/timeline.vue`（或 `share/media/...` 与详情同目录）
- Modify: `shared-media.ts` — `fetchProgressEvents`

**Interfaces:**
- Event: `{ id, mediaId, userId, scope, progressText, createdAt }`
- update progress：同一 `@Transactional` 先 upsert progress，再 insert event（`scope` 与请求一致；`userId`=操作者；shared 也记一条 event 即可，不要为每位 partner 各插一条 event）

- [ ] **Step 1: 建表**

```sql
CREATE TABLE media_progress_event (
    id            VARCHAR(36)  NOT NULL PRIMARY KEY,
    media_id      VARCHAR(36)  NOT NULL,
    user_id       VARCHAR(36)  NOT NULL,
    scope         VARCHAR(16)  NOT NULL COMMENT 'shared|personal',
    progress_text VARCHAR(512) NOT NULL,
    created_at    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_mpe_media_created (media_id, created_at)
) COMMENT '媒体进度变更历史';
```

- [ ] **Step 2: Service**

在 `MediaProgressService.update` 成功路径末尾：

```java
MediaProgressEventDO ev = new MediaProgressEventDO();
ev.setId(UUID.randomUUID().toString());
ev.setMediaId(mediaId);
ev.setUserId(user.getId());
ev.setScope(req.getScope());
ev.setProgressText(req.getProgressText());
eventMapper.insert(ev);
```

方法加 `@Transactional`。
`deleteByMediaId` 同时删 event。`SharedMediaController.delete` 调用 event 删除（或放 progressService.deleteByMediaId 内）。

- [ ] **Step 3: GET 列表**

```java
@GetMapping("/shared-media/{id}/progress-events")
public ApiResponse<List<MediaProgressEventResp>> listEvents(...) {
    sharedMediaService.getById(user, id); // 沿用现有可见性
    return ApiResponse.ok(progressService.listEvents(id));
}
```

`ORDER BY created_at DESC`。

- [ ] **Step 4: 前端独立页**

详情加 cell/按钮「进度时间轴」→ `router.push(\`/records/media/${id}/timeline\`)`。
时间轴页：`onMounted` 拉 events；列表展示时间、scope、文案、操作者（可用 userId 与自己/伴侣比对显示名）。

- [ ] **Step 5: 手测**

更新共同/个人进度 → 时间轴多一条；当前进度条与最新一致；删媒体无孤儿 event。

- [ ] **Step 6: Commit**

```powershell
git add "backend/lifeassistant/lifeassistant-server/src/main/resources/db/migration/V14__create_media_progress_event.sql" "backend/lifeassistant/lifeassistant-system/src/main/java/top/lifeassistant/sharedmedia/" "front/vue3-vant-mobile/src/api/modules/shared-media.ts" "front/vue3-vant-mobile/src/pages/"
git commit -m "feat: append media progress events and timeline page"
```

---

### Task 6: MCP 兼容验收

**Files:**
- 原则上不改 `python/agent`；若响应新增字段导致测试断言过严再放宽断言（不得删字段）

**Interfaces:**
- Consumes: 现有 `media` tool 与 `tests/test_schedule_media_http.py`

- [ ] **Step 1: 跑单测**

```powershell
cd python/agent
python -m unittest tests.test_schedule_media_http -v
```

Expected: PASS

- [ ] **Step 2: 若本地 Java 可起，冒烟**

用现有 token 调 `GET /shared-media`、`POST /shared-media`（form：title + mediaType=movie），确认 200 且旧字段仍在。

- [ ] **Step 3: Commit（仅当有测试/文档微调）**

若无代码变更可跳过 commit；有放宽断言则：

```powershell
git commit -m "test: keep media MCP http tests compatible with additive fields"
```

---

## Spec 覆盖自检

| Spec 项 | Task |
|---------|------|
| 去今日 + `/`→todos + 登录 Todos | 1 |
| 截止日期四色 | 1 |
| 伴侣/记录拆分 + TabBar | 2 |
| 排序 COALESCE | 2 |
| is_private + UI 过滤 + 文案 | 3 |
| 自定义分类 + uncategorized + mediaTypeLabel | 4 |
| 进度 event + 独立时间轴页 | 5 |
| MCP 兼容 | 6 / Global Constraints |

## 执行说明

用户已指定 **Subagent-Driven**：每 Task 一个实现 subagent → 任务级 review → 必要时 fix → 下一 Task；全部完成后做整支 review。
