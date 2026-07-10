# 共享媒体记录（一起看过的电影/书/漫剧）

## 背景

目前伴侣模块已有"一起做过的事"（`shared_record`），以纯文本标题+描述的方式记录共同活动。用户希望扩展一个专门记录"一起看过的电影、书和漫剧"的功能，支持：

- 添加媒体条目（名称、封面图、类型）
- 分别维护共同进度和双方个人进度
- 点进详情后以聊天室时间线方式添加评论
- 封面图上传到服务器本地存储

## 方案

在伴侣页（`/share`）"一起做过的事"旁边新增一个"一起看过的"Tab，复用现有伴侣绑定逻辑和数据可见范围。新建 3 张数据库表、10 个 API，前端不新增独立页面，以 Tab 形式嵌入现有伴侣页。

### 数据库设计

#### `shared_media` — 媒体主表

| 字段 | 类型 | 说明 |
|------|------|------|
| `id` | CHAR(36) PK | UUID |
| `created_by` | CHAR(36) FK → `user.id` | 添加者 |
| `title` | VARCHAR(255) | 名称 |
| `media_type` | VARCHAR(20) | `movie` / `book` / `tv` |
| `cover_path` | VARCHAR(256) | 服务器本地存储路径 |
| `description` | TEXT | 简介/备注 |
| `is_finished` | TINYINT(1) | 双方都看完了（快捷标记） |
| `created_at` | DATETIME | |
| `updated_at` | DATETIME | |

**关系**：`shared_media` 1 ── N `media_comment`，`shared_media` 1 ── N `media_progress`

#### `media_comment` — 聊天室评论

| 字段 | 类型 | 说明 |
|------|------|------|
| `id` | CHAR(36) PK | UUID |
| `media_id` | CHAR(36) FK → `shared_media.id` | 所属媒体 |
| `user_id` | CHAR(36) FK → `user.id` | 谁发的 |
| `content` | TEXT | 评论内容 |
| `created_at` | DATETIME | |

评论按 `created_at` 正序排列，形成时间线风格的聊天记录。

#### `media_progress` — 进度记录

| 字段 | 类型 | 说明 |
|------|------|------|
| `id` | CHAR(36) PK | UUID |
| `media_id` | CHAR(36) FK → `shared_media.id` | 所属媒体 |
| `user_id` | CHAR(36) | `NULL`=共同进度（scope=shared），`非NULL`=该用户的个人进度（scope=personal） |
| `progress_text` | VARCHAR(100) | 如"第 5 集/共 24 集"、"120 页/共 300 页" |
| `created_at` | DATETIME | |

每对 `(media_id, user_id)` 只保留一条最新记录，更新时 upsert。`user_id IS NULL` 为共同进度，`user_id` 为具体值代表某方的个人进度。双方均可看到所有进度。

**进度同步规则**：共同进度是基准线。更新共同进度时，自动将双方的个人进度也更新为相同的值。个人进度可单独覆盖（如一方先追了几集），此时不受共同进度影响。

### 后端 API

#### 媒体 CRUD

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/shared-media` | 列表（分页 + 按 media_type 筛选 + 按 is_finished 状态筛选） |
| POST | `/shared-media` | 添加媒体（multipart/form-data，含封面图上传） |
| GET | `/shared-media/{id}` | 媒体详情（含所有进度） |
| PATCH | `/shared-media/{id}` | 更新媒体信息（可替换封面图） |
| DELETE | `/shared-media/{id}` | 删除（级联删除评论和进度） |

#### 评论

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/shared-media/{id}/comments` | 评论列表（正序分页） |
| POST | `/shared-media/{id}/comments` | 发送评论 |

#### 进度

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/shared-media/{id}/progress` | 获取所有进度（共同 + 双方个人） |
| PUT | `/shared-media/{id}/progress` | 更新进度（body 传 `scope: "shared"|"personal"` 区分） |

#### 文件上传

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/upload` | 通用文件上传，返回服务端存储路径 |

### 数据可见性规则

与现有 `shared_record` 逻辑一致：查询时 union 双方 `created_by` 的数据，任何一方都能看到所有媒体条目。评论和进度同理，伴侣双方完全共享。

### 前端页面

不新增独立路由页面。在现有伴侣页（`/share` 路由，对应 `src/pages/share/index.vue`）绑定伴侣后的区域内，添加一个 Tab 栏：

- **Tab 1：一起做过的事**（现有内容）
- **Tab 2：一起看过的**（新增内容，本功能的媒体列表）

"一起看过的"Tab 内包含：
- 媒体类型筛选行：全部 / 电影 / 书籍 / 漫剧
- 状态筛选行：没看完（默认）/ 全部 / 已看完
- 添加按钮（悬浮圆形 + 图标）
- 媒体列表卡片（封面缩略图 + 名称 + 类型标签 + 共同进度 + 双方头像）

点击媒体卡片跳转详情聊天页（独立路由，如 `/share/media/{id}`），展示：
- 顶部区域：共同进度 + 你 + 伴侣 三方进度并列
- 中间区域：时间线风格评论列表（系统进度更新消息 + 用户聊天气泡）
- 底部固定输入框：发送评论

### 封面图存储

上传至服务器本地 `uploads/shared-media/` 目录，数据库存相对路径。Spring Boot 配置静态资源映射 `/uploads/**` 指向该目录。Nginx 直接代理静态文件。

## 变更清单

| 文件 | 变更类型 |
|------|----------|
| `backend/.../db/migration/V6__create_shared_media_tables.sql` | **新增** Flyway 迁移 |
| `backend/.../sharedrecord/` 改为 `sharedmedia/` 或新建包 | **新增** Controller / Service / Mapper / Entity |
| `backend/.../model/entity/SharedMediaDO.java` | **新增** |
| `backend/.../model/entity/MediaCommentDO.java` | **新增** |
| `backend/.../model/entity/MediaProgressDO.java` | **新增** |
| `backend/.../mapper/SharedMediaMapper.java` | **新增** |
| `backend/.../mapper/MediaCommentMapper.java` | **新增** |
| `backend/.../mapper/MediaProgressMapper.java` | **新增** |
| `backend/.../service/SharedMediaService.java` + `Impl` | **新增** |
| `backend/.../service/MediaCommentService.java` + `Impl` | **新增** |
| `backend/.../service/MediaProgressService.java` + `Impl` | **新增** |
| `backend/.../controller/SharedMediaController.java` | **新增** |
| `backend/.../controller/MediaCommentController.java` | **新增** |
| `backend/.../controller/MediaProgressController.java` | **新增** |
| `backend/.../controller/FileUploadController.java` | **新增** |
| `backend/.../server/.../WebConfig.java` | 修改 — 添加静态资源映射 |
| `front/.../src/api/modules/shared-media.ts` | **新增** API 模块 |
| `front/.../src/pages/share/index.vue` | 修改 — 添加 Tab 栏 + "一起看过的"内容 |
| `front/.../src/pages/share/media/[id].vue` | **新增** 详情聊天页（auto-route → `/share/media/:id`） |

## 不改动事项

| 事项 | 理由 |
|------|------|
| 第三方封面搜索 | 第一期纯手动上传，后续再考虑接入 |
| 评分/星级评价 | 未在需求范围内，YAGNI |
| 豆瓣等平台数据导入 | 独立功能，非本期目标 |
