# 一起看过 · 评论纯图消息 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 在「一起看过」媒体详情评论区支持发送纯图片消息（单条 1～9 张），两步上传后落库，气泡九宫格 + 全屏预览。

**Architecture:** 先 `POST .../comment-images` 落本地 `uploads/media-comments/` 返回相对路径，再 `POST .../comments` 带 `imageUrls` 创建评论；`content` 可空；业务只存路径字符串，预留换 OSS。

**Tech Stack:** Spring Boot 3 + MyBatis-Plus + Flyway / Vue 3 + Vant 4（`van-uploader`、`showImagePreview`）

## Global Constraints

- 纯图消息：可不带文字；本轮不做图文同条
- 单条最多 9 张；格式 jpg/png/webp/gif；单张 ≤ 5MB
- `imageUrls` 必须以前缀 `/uploads/media-comments/` 开头
- 每条评论：非空文字 **或** ≥1 张图，否则 400（`BadRequestException`）
- 上传目录与封面分离：`media-comments/` vs `shared-media/`
- 不做评论真删除、单图删除、OSS 实装
- PowerShell 环境：`git commit -m "..."`（勿用 bash heredoc）；路径用双引号
- 实现与提交在 feature 分支 / worktree，勿直接改脏的 master 工作区无关文件

## File Structure

```
backend/lifeassistant/lifeassistant-server/src/main/resources/db/migration/
  V15__media_comment_images.sql

backend/lifeassistant/lifeassistant-common/.../UploadStorage.java
  + mediaCommentsDir() / mediaCommentUrl(filename)

backend/lifeassistant/lifeassistant-system/.../sharedmedia/
  model/entity/MediaCommentDO.java          + imageUrls (String JSON)
  model/req/MediaCommentCreateReq.java      content 可选 + imageUrls
  model/resp/MediaCommentResp.java          + imageUrls List
  model/resp/CommentImagesUploadResp.java   NEW { urls }
  controller/MediaCommentController.java    + uploadCommentImages
  service/MediaCommentService.java          upload + create 校验

front/vue3-vant-mobile/src/api/modules/shared-media.ts
front/vue3-vant-mobile/src/pages/share/media/[id].vue
```

---

### Task 1: Flyway + DO/Req/Resp

**Files:**
- Create: `backend/lifeassistant/lifeassistant-server/src/main/resources/db/migration/V15__media_comment_images.sql`
- Modify: `.../model/entity/MediaCommentDO.java`
- Modify: `.../model/req/MediaCommentCreateReq.java`
- Modify: `.../model/resp/MediaCommentResp.java`
- Create: `.../model/resp/CommentImagesUploadResp.java`

**Interfaces:**
- Produces: DO 字段 `imageUrls`（DB 列 `image_urls`，Java `String` 存 JSON 文本）；Req `content` 可空 + `List<String> imageUrls`；Resp `List<String> imageUrls`（null→空列表）；`CommentImagesUploadResp(List<String> urls)`

- [ ] **Step 1: 写迁移**

```sql
-- V15__media_comment_images.sql
ALTER TABLE media_comment
    MODIFY COLUMN content TEXT NULL,
    ADD COLUMN image_urls JSON NULL COMMENT '评论图片相对路径数组' AFTER content;
```

- [ ] **Step 2: 更新 DO**

在 `MediaCommentDO` 增加：

```java
/** 图片相对路径 JSON 数组文本，如 ["/uploads/media-comments/a.jpg"] */
@TableField("image_urls")
private String imageUrls;
```

`content` 注释改为可空。

- [ ] **Step 3: 更新 CreateReq**

```java
@Data
@Schema(description = "创建评论请求")
public class MediaCommentCreateReq {
    @Schema(description = "评论文字；纯图消息可空")
    private String content;

    @Schema(description = "图片相对路径列表，最多 9 张")
    private List<String> imageUrls;
}
```

移除 `@NotBlank` on content。

- [ ] **Step 4: 更新 Resp + 新建 UploadResp**

`MediaCommentResp` 增加 `List<String> imageUrls`。`from()` 用 Jackson/`ObjectMapper` 或手写简单解析：`imageUrls` 列为空 → `List.of()`；非法 JSON → `List.of()`（勿抛导致列表挂掉）。

```java
@Data
@Builder
public class CommentImagesUploadResp {
    private List<String> urls;
}
```

- [ ] **Step 5: Commit**

```powershell
git add "backend/lifeassistant/lifeassistant-server/src/main/resources/db/migration/V15__media_comment_images.sql" "backend/lifeassistant/lifeassistant-system/src/main/java/top/lifeassistant/sharedmedia/model/"
git commit -m "feat: media_comment image_urls schema and DTOs"
```

---

### Task 2: 上传 API + 创建评论校验

**Files:**
- Modify: `backend/lifeassistant/lifeassistant-common/src/main/java/top/lifeassistant/common/component/UploadStorage.java`
- Modify: `backend/lifeassistant/lifeassistant-system/src/main/java/top/lifeassistant/sharedmedia/controller/MediaCommentController.java`
- Modify: `backend/lifeassistant/lifeassistant-system/src/main/java/top/lifeassistant/sharedmedia/service/MediaCommentService.java`
- Create: `backend/lifeassistant/lifeassistant-system/src/test/java/top/lifeassistant/sharedmedia/service/MediaCommentImageRulesTest.java`（若 system 模块无 test 目录则放在 `lifeassistant-server/src/test/java/...` 并对纯函数/包可见逻辑测；优先把路径校验抽成 package-private 或 public static 方法便于测）

**Interfaces:**
- Consumes: Task 1 DTOs；`UploadStorage`；现有 `sharedMediaService.getById` 可见性校验
- Produces:
  - `UploadStorage.mediaCommentsDir(): Path`
  - `UploadStorage.mediaCommentUrl(String filename): String` → `/uploads/media-comments/{filename}`
  - `MediaCommentService.uploadImages(UserDO, String mediaId, MultipartFile[] files): CommentImagesUploadResp`
  - `MediaCommentService.create` 支持纯图 / 纯文
  - `POST /shared-media/{mediaId}/comment-images` multipart field `files`
  - 常量前缀：`/uploads/media-comments/`

- [ ] **Step 1: UploadStorage**

```java
private static final String MEDIA_COMMENTS_SUBDIR = "media-comments";

public Path mediaCommentsDir() {
    return root.resolve(MEDIA_COMMENTS_SUBDIR);
}

public String mediaCommentUrl(String filename) {
    return "/uploads/" + MEDIA_COMMENTS_SUBDIR + "/" + filename;
}
```

- [ ] **Step 2: 校验规则（可抽 static 方法）**

```java
static final String COMMENT_IMAGE_PREFIX = "/uploads/media-comments/";
static final int MAX_IMAGES = 9;
static final long MAX_BYTES = 5L * 1024 * 1024;
static final Set<String> ALLOWED_EXT = Set.of("jpg", "jpeg", "png", "webp", "gif");

// create 时：
// - trimmed content 空且 imageUrls 空/null → BadRequestException("评论不能为空")
// - imageUrls.size() > 9 → BadRequestException
// - 任一 url 为 null/blank 或不以 COMMENT_IMAGE_PREFIX 开头 → BadRequestException("非法图片路径")
// - content 非空则 trim 入库；纯图 content 存 null
// - imageUrls 序列化为 JSON 字符串写入 DO.imageUrls；纯文字写 null
```

上传时：
- `files` null/空 → 400
- 张数 1～9
- 每张：非空、size≤5MB、扩展名在 ALLOWED_EXT（从 originalFilename 取，无扩展名拒绝）
- `UUID + "_" + 安全文件名` 落盘；返回 urls 列表

- [ ] **Step 3: Controller**

在 `MediaCommentController`：

```java
@PostMapping("/{mediaId}/comment-images")
public ApiResponse<CommentImagesUploadResp> uploadImages(
    @CurrentUser UserDO user,
    @PathVariable String mediaId,
    @RequestParam("files") MultipartFile[] files) throws IOException {
    return ApiResponse.ok(service.uploadImages(user, mediaId, files));
}
```

保持现有 `POST /{mediaId}/comments`；注入 `UploadStorage` 到 Service（或 Controller 存盘后调 service——优先 Service 内完成，与封面模式接近即可）。

参考封面：`SharedMediaController.saveCover` 的 `transferTo` 模式。

- [ ] **Step 4: 最小测试**

```java
class MediaCommentImageRulesTest {
  @Test void rejectsEmptyComment() { ... }
  @Test void rejectsMoreThanNine() { ... }
  @Test void rejectsExternalUrl() { ... }
  @Test void acceptsValidPrefix() { ... }
}
```

对抽出来的校验方法断言；若不便抽，测 Service 内 package-visible helper。

Run（按模块实际测试命令调整）：

```powershell
cd backend/lifeassistant
mvn -pl lifeassistant-system -am test -Dtest=MediaCommentImageRulesTest
```

若 system 无 surefire/测试源码集，把测试放到 `lifeassistant-server` 并测同包 helper，或把 helper 放到 common。以能跑通为准。

- [ ] **Step 5: Commit**

```powershell
git commit -m "feat: upload comment images and validate imageUrls on create"
```

---

### Task 3: 前端 API

**Files:**
- Modify: `front/vue3-vant-mobile/src/api/modules/shared-media.ts`

**Interfaces:**
- Consumes: 后端 Task 2 路径与字段名 `files` / `imageUrls` / `urls`
- Produces:
  - `MediaComment.content: string | null`
  - `MediaComment.imageUrls: string[]`
  - `uploadCommentImages(mediaId, files: File[]): Promise<...>`
  - `createComment(mediaId, data: { content?: string; imageUrls?: string[] })`

- [ ] **Step 1: 更新类型与函数**

```typescript
export interface MediaComment {
  id: string
  mediaId: string
  userId: string
  content: string | null
  imageUrls: string[]
  createdAt: string
}

export function uploadCommentImages(mediaId: string, files: File[]) {
  const formData = new FormData()
  for (const file of files)
    formData.append('files', file)
  return request.post<ApiResponse<{ urls: string[] }>>(
    `/shared-media/${mediaId}/comment-images`,
    formData,
    { headers: { 'Content-Type': 'multipart/form-data' } },
  )
}

export function createComment(
  mediaId: string,
  data: { content?: string, imageUrls?: string[] },
) {
  return request.post<ApiResponse<MediaComment>>(`/shared-media/${mediaId}/comments`, data)
}
```

列表解析处若依赖 `content: string`，改为容忍 null。

- [ ] **Step 2: Commit**

```powershell
git commit -m "feat: shared-media API for comment image upload"
```

---

### Task 4: 详情页发图 UI + 气泡 + 预览

**Files:**
- Modify: `front/vue3-vant-mobile/src/pages/share/media/[id].vue`

**Interfaces:**
- Consumes: `uploadCommentImages`、`createComment`、`MediaComment.imageUrls`
- Produces: 选图预览条、纯图发送、九宫格气泡、`showImagePreview`

- [ ] **Step 1: 输入区**

- 保留文字发送 `sendMessage`
- 增加 `van-uploader`：`v-model` 文件列表，`max-count=9`，`preview-full-image=false`（发送前用自己的预览条），`accept="image/*"`
- 或图标按钮 + 隐藏 uploader；选中后底部草稿条显示缩略图，可删除单张
- `sending` 期间禁用文字发送与图片发送

- [ ] **Step 2: sendImages**

```typescript
async function sendImages() {
  if (sending.value || pendingFiles.length === 0) return
  // 前端拦：>9、单张>5MB（file.size）
  sending.value = true
  try {
    const up = await uploadCommentImages(id, files)
    const urls = up.data?.urls ?? []
    if (!urls.length) throw new Error('upload empty')
    const created = await createComment(id, { imageUrls: urls })
    // append / reloadComments，清空 pending
  } catch {
    showToast('发送失败')
  } finally {
    sending.value = false
  }
}
```

纯文字路径不变（只传 `content`）。

- [ ] **Step 3: 气泡渲染**

- `comment.imageUrls?.length`：己方/对方气泡内网格
  - 1 张：单图较大
  - 2 张：一行两列
  - 3～9：三列网格
- 无图：现有文本 `comment.content`
- `@click` → `showImagePreview({ images: absoluteUrls, startPosition: index })`
- 相对路径需拼可访问 URL（与封面一致：直接用 path，开发代理 `/uploads` 已有则原样 `/uploads/...`）

- [ ] **Step 4: 手动冒烟清单（实现者在 report 写明已自检或未跑）**

- 发 1 张、发最多 9 张
- 点开全屏左右滑
- 纯文字仍可用
- 超 5MB / 超 9 张有提示

- [ ] **Step 5: Commit**

```powershell
git commit -m "feat: media comment image bubbles and send UI"
```

---

## Spec Coverage Checklist

| Spec 项 | Task |
|---------|------|
| content 可空 + image_urls JSON | 1 |
| 两步上传 API | 2 |
| 路径前缀校验、≤9、5MB、格式 | 2 |
| UploadStorage 独立目录 | 2 |
| 前端 API | 3 |
| 预览条、九宫格、全屏滑 | 4 |
| 不做图文同条 / 真删除 / OSS | 全任务遵守 |
