# 一起看过 · 评论纯图消息

## 背景

「一起看过」媒体详情页（`/share/media/:id`）的评论目前是纯文本聊天气泡。表 `media_comment` 仅有 `content`，创建接口只接受文字。封面图已有本地 multipart 上传（`UploadStorage` → `/uploads/shared-media/`），但评论侧不能发图。

目标：支持发送**纯图片消息**（可不带文字），单条最多 9 张；本地存储，接口形态预留日后换 OSS。

## 目标与非目标

**目标**

- 发送纯图评论（1～9 张 / 条），文字评论行为不变
- 气泡内九宫格缩略图，点击全屏预览并可左右滑同条内图片
- 上传落本地磁盘；业务只持久化路径/URL 字符串，便于日后换对象存储

**非目标（本轮不做）**

- 图文同条（一条消息既有字又有图）
- 评论真删除 API / 单图删除
- OSS/MinIO 实装
- 进度事件混入聊天气泡
- 发送前强制客户端压缩到固定尺寸

## 方案概览

采用**两步提交**：先上传图片拿相对路径列表，再创建评论写入 `imageUrls`。与日后「预签名上传 → 落库 URL」一致；文字评论 API 保持 JSON，改动面最小。

## 数据模型

### `media_comment` 变更

| 字段 | 变更 | 说明 |
|------|------|------|
| `content` | TEXT，改为可空 | 纯图消息无文字 |
| `image_urls` | 新增，`JSON` 可空 | 相对路径数组，如 `["/uploads/media-comments/xxx.jpg"]`；纯文字为 `null`（列表 API 序列化为 `[]`） |

**校验（服务端硬约束）**

- 每条评论必须满足：非空文字 **或** 至少 1 张图；两者都空 → 400
- `image_urls` 长度 ≤ 9
- 不做子表；不做单图独立删除

Flyway：新 migration 修改 `content` 可空并增加 `image_urls`。

## API

### 上传评论图

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/shared-media/{mediaId}/comment-images` | multipart，字段名 `files`，1～9 张 |

- 权限/可见性：与现有评论一致（媒体对当前用户可见）
- 格式：jpg / png / webp / gif
- 单张 ≤ 5MB
- 落盘目录：`uploads/media-comments/`（与封面 `shared-media/` 分开）
- 响应：`{ "urls": string[] }`（相对路径，经现有 `/uploads/**` 静态映射可访问）

### 创建评论（扩展）

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/shared-media/{mediaId}/comments` | body：`content?` + `imageUrls?` |

- 纯文字：仅 `content`（现有行为）
- 纯图片：仅 `imageUrls`（本需求）
- 校验 `imageUrls` 为本系统评论图路径前缀（如以 `/uploads/media-comments/` 开头），拒绝外链
- 条数 ≤ 9；再与「有字或有图」规则一并校验

### 列表（扩展）

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/shared-media/{mediaId}/comments` | 响应增加 `imageUrls`；旧数据为空数组 |

## 存储抽象

- 继续使用 `UploadStorage`，增加评论图目录方法（如 `mediaCommentsDir()`）
- 业务层与 DB 只存路径字符串
- 日后换 OSS：改上传实现返回可访问 URL，评论表仍存字符串列表；创建评论的校验改为「可信 URL 前缀/域名」即可

## 前端交互

页面：`front/vue3-vant-mobile/src/pages/share/media/[id].vue`
API 模块：`src/api/modules/shared-media.ts`

**输入区**

- 保留文字输入 + 发送
- 增加选图入口（相册/相机）；复用 `van-uploader` 或等价选择器
- 选图后底部待发预览条（最多 9，可单张移除）
- 点发送：先 `comment-images`，再 `createComment({ imageUrls })`
- 纯文字仍只调 `createComment({ content })`；本轮不把图和字绑成一条

**气泡**

- 有 `imageUrls`：己方右 / 对方左，九宫格缩略图（1 / 2 / 3～9 常见网格）
- 无图有字：现有文本气泡
- 时间戳与现有一致

**全屏预览**

- 点缩略图 → 全屏，可左右滑同条内图
- 优先复用 Vant `showImagePreview`（或项目已有封装）

**状态**

- 上传/发送中禁用重复提交；预览条可显示 loading
- 失败 Toast，保留本地所选图便于重试

## 数据流

1. 用户选 1～9 张 → 本地预览
2. 发送 → `POST .../comment-images` → `urls`
3. → `POST .../comments` `{ imageUrls: urls }` → 完整评论
4. 前端 append 列表（或轻量刷新），清空草稿
5. 进页 `GET .../comments` 渲染网格 / 文本

## 错误处理

| 场景 | 行为 |
|------|------|
| 超 9 张 / 单张 >5MB / 非法格式 | 前端先拦，后端再拦，明确错误文案 |
| 上传成功、创建评论失败 | Toast「发送失败」；已上传文件本轮可不回滚（接受孤儿文件；日后可按路径 GC） |
| 媒体不可见 | 与现有评论一致 |
| 非法 `imageUrls` | 400，不入库 |

## 测试范围（最小）

**后端**

- 纯图评论创建成功
- 无字无图拒绝
- `imageUrls` > 9 拒绝
- 列表返回 `imageUrls`

**前端（手动）**

- 发 1 张、发 9 张
- 点开全屏左右滑
- 纯文字评论仍可用

## 关键文件（实现时）

| 区域 | 路径 |
|------|------|
| 前端详情/聊天 | `front/vue3-vant-mobile/src/pages/share/media/[id].vue` |
| 前端 API | `front/vue3-vant-mobile/src/api/modules/shared-media.ts` |
| 评论 Controller/Service/Req/Resp/DO | `backend/.../sharedmedia/` |
| 上传 | `UploadStorage` + 静态映射已有 `/uploads/**` |
| Migration | `backend/.../db/migration/V*__media_comment_images.sql` |
