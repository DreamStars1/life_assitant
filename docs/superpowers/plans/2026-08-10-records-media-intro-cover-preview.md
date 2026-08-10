# Records Media Intro & Cover Preview Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 记录列表展示简介预览并支持封面放大；添加/编辑弹窗把封面移到表头并预览（含原封面）。

**Architecture:** 仅改 `records/index.vue`：纯函数截断简介、统一 `showImagePreview` 预览封面；列表卡片加预览行与封面点击；添加/编辑 Dialog 字段顺序调整，表头用 uploader 本地预览或已有 `coverPath`。无后端改动。

**Tech Stack:** Vue 3 + Vant 4（`showImagePreview`、`van-uploader`、`UploaderFileListItem`）

**Spec:** `docs/superpowers/specs/2026-08-10-records-media-intro-cover-preview-design.md`

## Global Constraints

- 只改 `front/vue3-vant-mobile/src/pages/records/index.vue` 的列表与添加/编辑 Dialog；不改详情页布局、API、数据模型
- 不做长按简介弹全文 Dialog；点简介与点卡片一样进 `/share/media/:id`
- 列表截断：`PREVIEW_LEN = 40`（JS 字符串长度）；空/仅空白不展示；≤40 全显；>40 则 `slice(0,40)+'…'`
- 封面预览：`showImagePreview({ images: [url], startPosition: 0, closeable: true, teleport: 'body' })`
- 版本 bump：前端 `1.8.2` → `1.8.3`；后端 `1.5.2-SNAPSHOT` → `1.5.3-SNAPSHOT`；根 `CHANGELOG.md` 新增 `v1.8.3 (2026-08-10)`
- PowerShell：`git commit -m "..."`，路径双引号；无单测，以手工验证为准

## File map

| Path | Role |
|------|------|
| `front/vue3-vant-mobile/src/pages/records/index.vue` | 简介预览、封面放大、Dialog 表头封面 |
| `front/vue3-vant-mobile/package.json` | version |
| `backend/lifeassistant/lifeassistant-server/src/main/resources/config/application.yml` | `application.version` |
| `CHANGELOG.md` | `v1.8.3` |

---

### Task 1: 辅助函数与编辑原封面 ref

**Files:**
- Modify: `front/vue3-vant-mobile/src/pages/records/index.vue`（`<script setup>`）

**Interfaces:**
- Consumes: 已有 `mediaCoverUrl`、`openEditMedia`、`addMediaCoverList`、`editMediaCoverList`
- Produces:
  - `PREVIEW_LEN = 40`
  - `previewDescription(desc: string | null | undefined): string`
  - `uploaderPreviewUrl(list: UploaderFileListItem[]): string`
  - `previewCover(url: string): void`
  - `editMediaExistingCoverPath: Ref<string | null>`
  - `addDialogCoverSrc` / `editDialogCoverSrc` computed
  - cover list 类型改为 `UploaderFileListItem[]`

- [ ] **Step 1: 扩展 import**

将：

```ts
import { showConfirmDialog, showToast } from 'vant'
```

改为：

```ts
import { showConfirmDialog, showImagePreview, showToast } from 'vant'
import type { UploaderFileListItem } from 'vant'
```

- [ ] **Step 2: 改 cover list 类型并加原封面 ref**

将：

```ts
const addMediaCoverList = ref<{ file?: File }[]>([])
```

与：

```ts
const editMediaCoverList = ref<{ file?: File }[]>([])
```

改为：

```ts
const addMediaCoverList = ref<UploaderFileListItem[]>([])
const editMediaCoverList = ref<UploaderFileListItem[]>([])
const editMediaExistingCoverPath = ref<string | null>(null)
```

（两处 `ref` 定义可能不在相邻行，改类型即可；`editMediaExistingCoverPath` 放在 `editMediaCoverList` 旁。）

- [ ] **Step 3: 在 `mediaCoverUrl` 附近加入辅助函数与 computed**

```ts
const PREVIEW_LEN = 40

function previewDescription(desc: string | null | undefined): string {
  if (desc == null)
    return ''
  if (!desc.trim())
    return ''
  if (desc.length <= PREVIEW_LEN)
    return desc
  return `${desc.slice(0, PREVIEW_LEN)}…`
}

function uploaderPreviewUrl(list: UploaderFileListItem[]): string {
  const item = list[0]
  if (!item)
    return ''
  return item.objectUrl || item.content || item.url || ''
}

function previewCover(url: string) {
  if (!url)
    return
  showImagePreview({
    images: [url],
    startPosition: 0,
    closeable: true,
    teleport: 'body',
  })
}

const addDialogCoverSrc = computed(() => uploaderPreviewUrl(addMediaCoverList.value))

const editDialogCoverSrc = computed(() => {
  const fromUpload = uploaderPreviewUrl(editMediaCoverList.value)
  if (fromUpload)
    return fromUpload
  return mediaCoverUrl(editMediaExistingCoverPath.value)
})
```

- [ ] **Step 4: 在 `openEditMedia` 里保存原封面**

在现有赋值中增加一行（建议在 `editMediaCoverList.value = []` 之前）：

```ts
editMediaExistingCoverPath.value = item.coverPath
```

- [ ] **Step 5: 静态核对**

确认 `onAddMedia` / `onEditMedia` 仍用 `addMediaCoverList.value[0]?.file` / `editMediaCoverList.value[0]?.file` 提交，逻辑不变。

- [ ] **Step 6: Commit**

```powershell
git add "front/vue3-vant-mobile/src/pages/records/index.vue"
git commit -m "feat(front): add records intro/cover preview helpers"
```

---

### Task 2: 列表卡片 — 简介预览与封面点击放大

**Files:**
- Modify: `front/vue3-vant-mobile/src/pages/records/index.vue`（列表模板内 `media-card`）

**Interfaces:**
- Consumes: `previewDescription`、`previewCover`、`mediaCoverUrl`
- Produces: 有简介时显示预览行；有封面时可点放大且不进详情

- [ ] **Step 1: 封面加点击预览**

将列表中：

```vue
<img
  v-if="item.coverPath"
  :src="mediaCoverUrl(item.coverPath)"
  alt=""
  class="media-cover flex-shrink-0"
>
```

改为：

```vue
<img
  v-if="item.coverPath"
  :src="mediaCoverUrl(item.coverPath)"
  alt=""
  class="media-cover flex-shrink-0"
  @click.stop="previewCover(mediaCoverUrl(item.coverPath))"
>
```

占位封面（`v-else` 的 `media-cover-placeholder`）不加点击预览。

- [ ] **Step 2: 类型与状态之间插入简介预览**

在：

```vue
<div class="text-xs text-gray-500 mt-1">
  {{ formatMediaType(item.mediaType, item.mediaTypeLabel) }}
</div>
```

与：

```vue
<div class="text-xs mt-1">
  <van-tag :type="item.isFinished ? 'success' : 'warning'">
```

之间插入：

```vue
<div
  v-if="previewDescription(item.description)"
  class="text-xs text-gray-500 mt-1"
>
  {{ previewDescription(item.description) }}
</div>
```

不要给该行单独 `@click`（冒泡到卡片即可进详情）。不要加 `@longpress`。

- [ ] **Step 3: 手工验证**

1. 有长简介条目：列表显示前 40 字 + `…`
2. 短简介（≤40）：全显
3. 无简介：无预览行
4. 点简介/标题 → 进详情；点封面 → 图片预览，关闭后仍在列表

- [ ] **Step 4: Commit**

```powershell
git add "front/vue3-vant-mobile/src/pages/records/index.vue"
git commit -m "feat(front): show media intro preview and cover zoom on records list"
```

---

### Task 3: 添加/编辑 Dialog — 封面移到表头

**Files:**
- Modify: `front/vue3-vant-mobile/src/pages/records/index.vue`（两个 `van-dialog` 内容区 + 可选 scoped 样式）

**Interfaces:**
- Consumes: `addDialogCoverSrc`、`editDialogCoverSrc`、`previewCover`、`addMediaCoverList`、`editMediaCoverList`
- Produces: 添加/编辑弹窗字段顺序为 封面区 → 标题 → 类型 → 简介 → …；有图可点放大

- [ ] **Step 1: 改添加 Dialog 内容顺序**

将 `showAddMedia` 的内层：

```vue
<div class="px-4 py-3 space-y-3">
  <van-field v-model="addMediaForm.title" ...
  ...
  <div class="text-sm text-gray-500 mb-1">
    封面图
  </div>
  <van-uploader v-model="addMediaCoverList" accept="image/*" max-count="1" />
</div>
```

改为（其余 field 原样保留，仅把封面块移到最前）：

```vue
<div class="px-4 py-3 space-y-3">
  <div class="dialog-cover-block">
    <img
      v-if="addDialogCoverSrc"
      :src="addDialogCoverSrc"
      alt=""
      class="media-cover dialog-cover"
      @click="previewCover(addDialogCoverSrc)"
    >
    <div v-else class="media-cover-placeholder dialog-cover">
      <van-icon name="photo-o" size="24" />
    </div>
    <div class="text-sm text-gray-500 mt-2 mb-1">
      封面图
    </div>
    <van-uploader v-model="addMediaCoverList" accept="image/*" :max-count="1" />
  </div>
  <van-field v-model="addMediaForm.title" placeholder="名称（如：盗梦空间）" clearable />
  <!-- 类型、简介、日期、仅自己可见：保持改前内容与顺序，不要重复封面 uploader -->
</div>
```

注意：删除原先底部的「封面图」文案与 `van-uploader`；`max-count` 用 `:max-count="1"`（数字绑定，与现有字符串 `"1"` 等价，二选一与文件其余 uploader 风格一致即可）。

- [ ] **Step 2: 改编辑 Dialog 内容顺序**

对 `showEditMedia` 做同样结构调整；表头预览用 `editDialogCoverSrc`；文案为「封面图（不选则保留原图）」：

```vue
<div class="dialog-cover-block">
  <img
    v-if="editDialogCoverSrc"
    :src="editDialogCoverSrc"
    alt=""
    class="media-cover dialog-cover"
    @click="previewCover(editDialogCoverSrc)"
  >
  <div v-else class="media-cover-placeholder dialog-cover">
    <van-icon name="photo-o" size="24" />
  </div>
  <div class="text-sm text-gray-500 mt-2 mb-1">
    封面图（不选则保留原图）
  </div>
  <van-uploader v-model="editMediaCoverList" accept="image/*" :max-count="1" />
</div>
```

其后仍为：标题 → 类型 → 简介 → 日期 → 仅自己可见。删除底部旧封面区块。

- [ ] **Step 3: 加表头居中样式**

在 `<style scoped>` 末尾追加：

```css
.dialog-cover-block {
  display: flex;
  flex-direction: column;
  align-items: center;
}

.dialog-cover {
  width: 96px;
  height: 120px;
}
```

（复用 `.media-cover` / `.media-cover-placeholder` 的 object-fit、圆角等；`.dialog-cover` 只放大尺寸。）

- [ ] **Step 4: 手工验证**

1. 添加：未选图见占位；选图后表头预览；点预览可放大
2. 编辑：打开即见原封面；选新图替换预览；不选新图保存后原封面仍在
3. 编辑简介字段为完整原文

- [ ] **Step 5: Commit**

```powershell
git add "front/vue3-vant-mobile/src/pages/records/index.vue"
git commit -m "feat(front): move media cover to add/edit dialog header"
```

---

### Task 4: 版本号与 CHANGELOG

**Files:**
- Modify: `front/vue3-vant-mobile/package.json`
- Modify: `backend/lifeassistant/lifeassistant-server/src/main/resources/config/application.yml`
- Modify: `CHANGELOG.md`

**Interfaces:**
- Produces: 前端 `1.8.3`；后端 `1.5.3-SNAPSHOT`；`## v1.8.3 (2026-08-10)`

- [ ] **Step 1: bump 前端**

`package.json` 的 `"version"`：`1.8.2` → `1.8.3`

- [ ] **Step 2: bump 后端**

`application.yml` 的 `application.version`：`1.5.2-SNAPSHOT` → `1.5.3-SNAPSHOT`
（勿改 `pom.xml` 父版本 `4.3.0-SNAPSHOT`。）

- [ ] **Step 3: CHANGELOG**

在文件顶部 `---` 之后、`## v1.8.2` 之前插入：

```markdown
## v1.8.3 (2026-08-10)

### ✨ 新特性

- **记录简介与封面预览**：列表展示简介前 40 字；点击封面可放大；添加/编辑弹窗封面移至表头并预览原图

### 🖥 后端

- 版本号: `1.5.2-SNAPSHOT` → `1.5.3-SNAPSHOT`

### 📱 前端

- 记录列表简介预览、封面 `showImagePreview`；添加/编辑 Dialog 表头封面
- 版本号: `1.8.2` → `1.8.3`

---
```

- [ ] **Step 4: Commit**

```powershell
git add "front/vue3-vant-mobile/package.json" "backend/lifeassistant/lifeassistant-server/src/main/resources/config/application.yml" "CHANGELOG.md"
git commit -m "chore: bump app to v1.8.3 for records intro/cover preview"
```

---

## Spec coverage

| Spec | Task |
|------|------|
| 列表 40 字简介预览 | Task 1 helpers + Task 2 |
| 点简介进详情 | Task 2（无单独 handler，冒泡） |
| 全文靠编辑弹窗 | 已有字段；Task 3 不改简介 field |
| 取消长按 Dialog | Task 2 明确不加 longpress |
| 列表点封面放大 | Task 2 |
| 添加/编辑表头封面 + 原图预览 | Task 1 ref/computed + Task 3 |
| Dialog 封面可点放大 | Task 3 |
| 版本与 CHANGELOG | Task 4 |
| 不改详情页 / API | Global Constraints |
