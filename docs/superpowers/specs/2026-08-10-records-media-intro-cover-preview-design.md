# 记录列表：简介预览与封面放大

## 背景

共享媒体（记录）的简介（`description`）可在添加/编辑弹窗写入并落库，但列表卡片与详情页均未展示，编辑后用户看不到内容。封面仅在列表以缩略图展示，无法点击放大。

## 目标

在记录列表卡片上：

1. 有简介时展示前一段预览：≤40 字全显；>40 字则前 40 字 + `…`
2. 长按简介预览行 → Dialog 展示完整简介（可滚动）
3. 点击有封面的缩略图 → `showImagePreview` 放大查看（可缩放，与详情评论图一致）

## 非目标

- 不改媒体详情页（`/share/media/:id`）的简介/封面展示
- 不改添加/编辑弹窗、后端 API、数据模型
- 不抽独立卡片组件、不加新依赖

## 布局

现有卡片左右结构不变，简介插在「类型」与「状态 tag」之间：

```
[ 封面 64×80 ]  标题（单行截断）
                类型
                简介预览（有 description 才渲染）
                状态 tag + 时间行
                [编辑图标]
```

- 样式：`text-xs text-gray-500`，弱于标题
- 无简介：不渲染该行
- 无封面：占位图标，不可点预览

## 交互

| 手势 | 目标 | 行为 |
|---|---|---|
| 单击封面 | 有 `coverPath` 的 `<img>` | `@click.stop` → `showImagePreview({ images: [coverUrl], closeable: true, teleport: 'body' })` |
| 单击占位封面 | 无封面图标区 | 不预览；点击落到卡片，进详情 |
| 长按简介预览行 | 有简介的那一行 | → `van-dialog` 展示完整 `description`；标题「简介」；内容可滚动 |
| 单击卡片其余区域 | 标题/类型/状态/空白 | 照旧进 `/share/media/:id` |
| 单击编辑 / 左滑 | 现有 | 不变 |

补充：

- 简介 ≤40 字时预览即全文，长按仍打开 Dialog（行为统一）
- 封面预览必须 `stop`，避免同时跳转详情
- 长按简介时不触发卡片单击跳转

## 截断规则

- 常量：`PREVIEW_LEN = 40`（JS 字符串长度，中英文各计 1）
- 空 / 仅空白：不展示预览行
- `length ≤ 40`：原样显示
- `length > 40`：`slice(0, 40) + '…'`
- Dialog 始终用原始 `description`

## 实现范围

文件：`front/vue3-vant-mobile/src/pages/records/index.vue`

- 列表已返回 `description`、`coverPath`，无需改 API
- 封面 URL 继续用现有 `mediaCoverUrl()`
- 超长简介 Dialog 沿用现有 `.van-dialog__content` 可滚动样式
- 图片预览对齐 `share/media/[id].vue` 的 `showImagePreview` 用法

## 交付

用户可感知的行为变更：按仓库版本规范 bump 前端 `package.json` version、后端 `application.version`，并在根 `CHANGELOG.md` 记一节（具体版本号在实现计划中对照当前版本填写）。

## 验证

1. 有简介的卡片显示前 40 字（短文全显，长文带 `…`）
2. 长按预览 → Dialog 全文可读可关
3. 点封面 → 图片预览可缩放关闭；点卡片其它区域仍进详情
4. 无简介 / 无封面行为与改前一致
