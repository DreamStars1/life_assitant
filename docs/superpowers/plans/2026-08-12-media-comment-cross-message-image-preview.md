# Media Comment Cross-Message Image Preview Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 影音评论预览时，本影音下全部评论图片按序组成一条相册可左右滑；多图合成发送逻辑不变。

**Architecture:** 抽出纯函数 `buildCommentImageGallery(comments, activeCommentId, localIndex)` 展平所有 `imageUrls` 并算全局 `startPosition`；详情页点击改调该函数再 `showImagePreview`。发送/上传不动。

**Tech Stack:** Vue 3 + Vant `showImagePreview`

**Spec:** `docs/superpowers/specs/2026-08-12-media-comment-cross-message-image-preview-design.md`

## Global Constraints

- 仅改影音详情评论预览；不改记录列表封面、不改上传/评论 API、不改多选合并发送
- 相册顺序 = `comments` 列表顺序 × 每条 `imageUrls` 数组顺序；过滤空串
- 版本 patch：前端 `1.9.0` → `1.9.1`；后端 `1.6.0-SNAPSHOT` → `1.6.1-SNAPSHOT`；根 `CHANGELOG.md`
- PowerShell：`git commit -m "..."`，路径双引号；勿提交无关脏文件（如 records/index.vue、app.less）

## File map

| Path | Role |
|------|------|
| `front/.../pages/share/media/commentImageGallery.ts` | 纯函数展平 + startPosition |
| `front/.../pages/share/media/commentImageGallery.selfcheck.ts` | assert 自检 |
| `front/.../pages/share/media/[id].vue` | 点击预览改用全局相册 |
| `front/.../package.json` / `application.yml` / `CHANGELOG.md` | 发版 |

---

### Task 1: `buildCommentImageGallery` + 自检

**Files:**
- Create: `front/vue3-vant-mobile/src/pages/share/media/commentImageGallery.ts`
- Create: `front/vue3-vant-mobile/src/pages/share/media/commentImageGallery.selfcheck.ts`

**Interfaces:**
- Produces:
```ts
export type CommentImageSource = {
  id?: string
  imageUrls?: string[] | null
}

export function buildCommentImageGallery(
  comments: CommentImageSource[],
  activeCommentId: string | undefined,
  localIndex: number,
): { images: string[]; startPosition: number }
```
- 规则：按 `comments` 顺序；每条取 `(imageUrls ?? []).filter(Boolean)`；若 `comment.id === activeCommentId` 且 `localIndex` 落在该条过滤后数组内，则该张的全局下标为 `startPosition`；若找不到匹配（id 缺失/越界），`startPosition` 回退为 `0`（有图时）或 `0`（无图时 `images` 为空）
- 无 `id` 的评论仍贡献图片到相册，但无法用 id 锚定；本页评论均有 id 时以 id 为准

- [ ] **Step 1: 写失败自检（文件可先引用未实现函数）**

```ts
// commentImageGallery.selfcheck.ts
import assert from 'node:assert/strict'
import { buildCommentImageGallery } from './commentImageGallery'

const comments = [
  { id: 'a', imageUrls: ['u1', ''] },
  { id: 'b', imageUrls: null },
  { id: 'c', imageUrls: ['u2', 'u3'] },
  { id: 'd' },
]

let g = buildCommentImageGallery(comments, 'c', 1)
assert.deepEqual(g.images, ['u1', 'u2', 'u3'])
assert.equal(g.startPosition, 2)

g = buildCommentImageGallery(comments, 'a', 0)
assert.equal(g.startPosition, 0)

g = buildCommentImageGallery(comments, 'missing', 0)
assert.deepEqual(g.images, ['u1', 'u2', 'u3'])
assert.equal(g.startPosition, 0)

g = buildCommentImageGallery([], 'a', 0)
assert.deepEqual(g.images, [])
assert.equal(g.startPosition, 0)

console.log('commentImageGallery.selfcheck: ok')
```

- [ ] **Step 2: 跑自检确认失败**

```powershell
cd "D:\life_assistant\front\vue3-vant-mobile"
npx tsx "src/pages/share/media/commentImageGallery.selfcheck.ts"
```

Expected: FAIL（模块不存在或函数未定义）

- [ ] **Step 3: 最小实现**

```ts
// commentImageGallery.ts
export type CommentImageSource = {
  id?: string
  imageUrls?: string[] | null
}

export function buildCommentImageGallery(
  comments: CommentImageSource[],
  activeCommentId: string | undefined,
  localIndex: number,
): { images: string[]; startPosition: number } {
  const images: string[] = []
  let startPosition = 0
  let anchored = false

  for (const c of comments) {
    const urls = (c.imageUrls ?? []).filter((u): u is string => !!u)
    if (
      !anchored
      && activeCommentId
      && c.id === activeCommentId
      && localIndex >= 0
      && localIndex < urls.length
    ) {
      startPosition = images.length + localIndex
      anchored = true
    }
    images.push(...urls)
  }

  if (!anchored)
    startPosition = 0

  return { images, startPosition }
}
```

- [ ] **Step 4: 跑自检确认通过**

同 Step 2。Expected: 打印 `commentImageGallery.selfcheck: ok`，exit 0

- [ ] **Step 5: Commit**

```powershell
git add "front/vue3-vant-mobile/src/pages/share/media/commentImageGallery.ts" "front/vue3-vant-mobile/src/pages/share/media/commentImageGallery.selfcheck.ts"
git commit -m "feat(front): gallery helper for cross-comment image preview"
```

---

### Task 2: 接线详情页预览

**Files:**
- Modify: `front/vue3-vant-mobile/src/pages/share/media/[id].vue`

**Interfaces:**
- Consumes: `buildCommentImageGallery` from `./commentImageGallery`
- 模板点击：传入 `comment.id` 与 `imgIndex`（不要只传本条 `imageUrls`）

- [ ] **Step 1: 替换 `previewCommentImages`**

删除/改写现有：

```ts
function previewCommentImages(imageUrls: string[], startPosition: number) {
  ...
}
```

为：

```ts
import { buildCommentImageGallery } from './commentImageGallery'

function previewCommentImages(commentId: string | undefined, localIndex: number) {
  const { images, startPosition } = buildCommentImageGallery(
    comments.value,
    commentId,
    localIndex,
  )
  if (!images.length)
    return
  showImagePreview({
    images: [...images],
    startPosition,
    showIndicators: images.length > 1,
    closeable: true,
    teleport: 'body',
  })
}
```

- [ ] **Step 2: 改模板 `@click`**

```vue
@click.stop="previewCommentImages(comment.id, imgIndex)"
```

（原为 `previewCommentImages(comment.imageUrls, imgIndex)`）

- [ ] **Step 3: 确认发送路径未改**

`afterReadImage` / `sendImages` / `MAX_COMMENT_IMAGES` 保持原样；本 task 不改 uploader。

- [ ] **Step 4: Commit**

```powershell
git add "front/vue3-vant-mobile/src/pages/share/media/[id].vue"
git commit -m "feat(front): swipe all comment images in media detail preview"
```

---

### Task 3: 版本与 CHANGELOG

**Files:**
- Modify: `front/vue3-vant-mobile/package.json`（`1.9.0` → `1.9.1`）
- Modify: `backend/lifeassistant/lifeassistant-server/src/main/resources/config/application.yml`（`1.6.0-SNAPSHOT` → `1.6.1-SNAPSHOT`）
- Modify: `CHANGELOG.md`（顶部新节）

- [ ] **Step 1: bump + 日志**

```markdown
## v1.9.1 (2026-08-12)

### ✨ 新特性

- **影音评论图片跨消息滑动**：预览时本影音下全部评论图片按序可左右滑；多图合成发送不变

### 🖥 后端

- 版本号: `1.6.0-SNAPSHOT` → `1.6.1-SNAPSHOT`

### 📱 前端

- `buildCommentImageGallery` + 详情页预览接线
- 版本号: `1.9.0` → `1.9.1`
```

- [ ] **Step 2: Commit**

```powershell
git add "front/vue3-vant-mobile/package.json" "backend/lifeassistant/lifeassistant-server/src/main/resources/config/application.yml" "CHANGELOG.md"
git commit -m "chore: bump app to v1.9.1 for cross-message image preview"
```

---

### Task 4: 手工冒烟

- [ ] **Step 1:** 打开一条影音详情，发两条各一张图 + 一条多图；分别从中间某张点开，确认可滑到其它消息的图
- [ ] **Step 2:** 多选一次发送仍为一条多图气泡（回归）

（无需单独 commit）

---

## Self-review (plan vs spec)

| Spec | Task |
|------|------|
| 全局展平 + startPosition | 1–2 |
| 多图发送不变 | 2 Step 3、4 回归 |
| 非目标未纳入 | — |
| patch 发版 | 3 |
| 测试要点 | 1 自检 + 4 冒烟 |
