# Records Add MediaType From Filter Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 记录页打开/重置「添加一起看过的」时，类型跟随当前 `mediaTypeFilter`（无筛选则 `movie`）。

**Architecture:** 仅改 `openAddMedia` 与 `onAddMedia` 成功重置两处赋值，无新抽象、无后端改动。

**Tech Stack:** Vue 3 + Vant（`records/index.vue`）

**Spec:** `docs/superpowers/specs/2026-08-08-records-add-media-type-from-filter-design.md`

## Global Constraints

- 只改添加流程的 `mediaType` 预填/重置；不改编辑弹窗、自定义分类、后端、筛选列表
- 默认回退值仍为 `'movie'`
- 版本 bump：前端 `1.8.1` → `1.8.2`；后端 `1.5.1-SNAPSHOT` → `1.5.2-SNAPSHOT`；根 `CHANGELOG.md` 新增 `v1.8.2`
- PowerShell：`git commit -m "..."`，路径双引号
- 无单测；以手工验证为准

## File map

| Path | Role |
|------|------|
| `front/vue3-vant-mobile/src/pages/records/index.vue` | 预填/重置 `mediaType` |
| `front/vue3-vant-mobile/package.json` | version |
| `backend/.../config/application.yml` | `application.version` |
| `CHANGELOG.md` | `v1.8.2` |

---

### Task 1: 添加表单类型跟随筛选

**Files:**
- Modify: `front/vue3-vant-mobile/src/pages/records/index.vue`（`openAddMedia`、`onAddMedia` 成功重置）

**Interfaces:**
- Consumes: 已有 `mediaTypeFilter` ref、`addMediaForm.mediaType`
- Produces: 打开与成功重置时 `addMediaForm.mediaType === (mediaTypeFilter.value || 'movie')`

- [ ] **Step 1: 改 `openAddMedia`**

将：

```ts
function openAddMedia() {
  addMediaForm.lastWatchedAt = toLocalDateStr(new Date())
  showAddMedia.value = true
}
```

改为：

```ts
function openAddMedia() {
  addMediaForm.mediaType = mediaTypeFilter.value || 'movie'
  addMediaForm.lastWatchedAt = toLocalDateStr(new Date())
  showAddMedia.value = true
}
```

- [ ] **Step 2: 改 `onAddMedia` 成功重置**

在成功分支里，把：

```ts
addMediaForm.mediaType = 'movie'
```

改为：

```ts
addMediaForm.mediaType = mediaTypeFilter.value || 'movie'
```

不要改同分支其它重置字段。

- [ ] **Step 3: 静态核对**

确认编辑弹窗/`editMediaForm` 无改动；全文件仅上述两处 `mediaType` 赋值逻辑变更（外加打开时多一行赋值）。

- [ ] **Step 4: Commit**

```powershell
git add "front/vue3-vant-mobile/src/pages/records/index.vue"
git commit -m "fix(front): default add-media type from current filter"
```

---

### Task 2: 版本号与 CHANGELOG

**Files:**
- Modify: `front/vue3-vant-mobile/package.json`
- Modify: `backend/lifeassistant/lifeassistant-server/src/main/resources/config/application.yml`
- Modify: `CHANGELOG.md`

**Interfaces:**
- Produces: 前端 `1.8.2`；后端 `1.5.2-SNAPSHOT`；`## v1.8.2 (2026-08-08)`

- [ ] **Step 1: bump 前端**

`"version": "1.8.2"`

- [ ] **Step 2: bump 后端**

`application.version: 1.5.2-SNAPSHOT`（勿改 pom 父版本）

- [ ] **Step 3: CHANGELOG**

在 `v1.8.1` 之前插入：

```markdown
## v1.8.2 (2026-08-08)

### ✨ 新特性

- **记录添加类型跟随筛选**：从书籍等类型筛选进入添加时，默认选中当前筛选类型；连续添加保持同一默认

### 🖥 后端

- 版本号: `1.5.1-SNAPSHOT` → `1.5.2-SNAPSHOT`

### 📱 前端

- `openAddMedia` / 添加成功重置使用 `mediaTypeFilter || 'movie'`
- 版本号: `1.8.1` → `1.8.2`

---
```

- [ ] **Step 4: Commit**

```powershell
git add "front/vue3-vant-mobile/package.json" "backend/lifeassistant/lifeassistant-server/src/main/resources/config/application.yml" "CHANGELOG.md"
git commit -m "chore: bump app to v1.8.2 for add-media type from filter"
```

---

## Spec coverage

| Spec | Task |
|------|------|
| open 时预填筛选类型 | Task 1 Step 1 |
| 成功重置跟随筛选 | Task 1 Step 2 |
| patch + CHANGELOG | Task 2 |
| 不改编辑/分类/后端 | Global Constraints |
