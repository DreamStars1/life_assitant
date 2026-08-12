# 影音评论：跨消息图片滑动预览

## 背景

影音详情页（`share/media/[id].vue`）评论支持多图：同一条消息内的多张图可用 `showImagePreview` 左右滑。分多条消息发出的图（每条一张或多张）只能在本条内滑动，无法串到其它评论的图。用户需要：保留「一次多选合成一条多图消息」，同时预览时可在本影音下**全部评论图片**之间左右滑。

## 目标

- 点击任意评论图片时，预览相册 = 当前页所有评论中的图片，按评论列表顺序、同条内按 `imageUrls` 顺序展平
- `startPosition` 对准被点击的那一张（全局下标）
- 总数 > 1 时显示指示器；发送/合成多图行为不变

## 非目标

- 不改记录列表封面预览
- 不改上传/评论 API、不改多选合并发送逻辑
- 不做跨影音相册、不做打开后实时同步增删

## 行为

1. 用户点击某评论气泡中的第 `k` 张图（该评论在 `comments` 中，其 `imageUrls[k]` 有效）
2. 遍历 `comments`（已有时间/列表顺序）：对每条的 `imageUrls`（过滤空串）依次追加到 `images[]`
3. 在展平过程中记录被点图的全局下标 `startPosition`
4. 调用现有 `showImagePreview({ images, startPosition, showIndicators: images.length > 1, closeable: true, teleport: 'body' })`

若全局只有一张图，行为与现网单张预览一致（无指示器）。

## 实现要点

- 文件：`front/vue3-vant-mobile/src/pages/share/media/[id].vue`
- 调整 `previewCommentImages`（或等价）：入参改为能定位到「哪条评论 + 局部下标」，或传入 `commentId`/`comment` + `imgIndex`，在函数内展平；模板 `@click` 相应改传参
- 纯前端；无需后端、无需 DB、无需 bump 若仅交互修正——**用户可感知行为变更**：跨消息可滑，按 `app-version-bump` 做 **patch** 发版（前端 `package.json`、后端 `application.version` 对齐、根 `CHANGELOG.md`）

## 测试要点

- 一条多图消息：仍可从任一张滑到同条其它张
- 多条各一张：从任一条滑到前后其它评论的图
- 多条混合（单图 + 多图）：顺序与气泡出现顺序一致
- 仅一张图：可打开预览，无多余指示器
- 发送多选仍合成一条（回归）
