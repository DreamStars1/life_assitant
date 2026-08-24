# 伴侣留言板 + 一起做过的事迁入看板二级页 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Tab「伴侣」改为气泡留言板（文字/图片互斥，文字可开关同步记事/待办/积分）；「一起做过的事」迁到看板「共享记录」二级页。

**Architecture:** 新域 `partner_message`；图片两步上传对齐影音评论；`POST /partner/messages` 事务编排可选创建 `shared_record` / `todo` / `partner_points`。`/shared-records` API 不动，仅前端路由迁移。

**Tech Stack:** Spring Boot 3 + MyBatis-Plus + Flyway / Vue 3 + Vant 4（`van-uploader`、`showImagePreview`）+ 文件路由

**Spec:** `docs/superpowers/specs/2026-08-24-partner-message-board-design.md`

## Global Constraints

- 文字与图片互斥；单条图最多 9 张；jpg/png/webp/gif；单张 ≤ 5MB；前缀 `/uploads/partner-messages/`
- 扩展开关仅纯文字帖；后端事务全成或全败
- 删除留言不级联删记事/待办/积分；解绑时删双方留言（对齐 shared_record）
- `todoAssignedTo`：`partner` → `assignedTo=partnerId`；`self` 与 `none` → `assignedTo=null`（当前 Todo 模型无独立「池」；UI 仍提供三选项，`self`=自己做的个人待办）
- 记事/待办标题：留言全文截断至 255 字符；待办 `priority=medium`；积分 `recordDate=今天`
- 版本：**minor** — 前端以当前 `package.json` 为准 bump（预期 `1.9.x` → `1.10.0`）；后端 `application.version` 对齐 `1.10.0-SNAPSHOT`；根 `CHANGELOG.md` + `docs/USER_CHANGELOG.md`
- PowerShell：`git commit -m "..."`，路径双引号；勿提交无关脏文件
- 实现前在干净 worktree/分支上做；勿污染 master 上其它未提交改动

## File map

| Path | Role |
|------|------|
| `.../db/migration/V20__create_partner_message.sql` | 新表 |
| `.../partner/model/entity/PartnerMessageDO.java` | 实体 |
| `.../partner/mapper/PartnerMessageMapper.java` | Mapper |
| `.../partner/model/req/PartnerMessageCreateReq.java` | 发帖请求 |
| `.../partner/model/resp/PartnerMessageResp.java` | 列表/创建响应 |
| `.../partner/model/resp/PartnerMessageImagesUploadResp.java` | 上传响应 |
| `.../partner/service/PartnerMessageImageRules.java` | 校验纯函数 |
| `.../partner/service/PartnerMessageImageRulesTest.java` | 单测 |
| `.../partner/service/PartnerMessageService.java` | 上传/列表/发帖编排/删除 |
| `.../partner/controller/PartnerMessageController.java` | API |
| `.../common/component/UploadStorage.java` | `partnerMessagesDir()` |
| `.../partner/service/PartnerPointsService.java` | `addPoints` 返回流水 id |
| `.../system/service/impl/UserServiceImpl.java` | 解绑删留言 |
| `front/.../api/modules/partner-messages.ts` | 前端 API |
| `front/.../pages/partner/dashboard/shared-records.vue` | 一起做过的事二级页 |
| `front/.../pages/share/index.vue` | 改留言板（保留绑定） |
| `front/.../pages/partner/dashboard/index.vue` | 统计卡跳转 |
| `front/.../pages/share/media/commentImageGallery.ts` | 复用预览（消息形状兼容） |
| locales / package.json / application.yml / CHANGELOGs | 文案与发版 |

---

### Task 1: Flyway + DO + Mapper + DTOs

**Files:**
- Create: `backend/lifeassistant/lifeassistant-server/src/main/resources/db/migration/V20__create_partner_message.sql`
- Create: `backend/lifeassistant/lifeassistant-system/src/main/java/top/lifeassistant/partner/model/entity/PartnerMessageDO.java`
- Create: `backend/lifeassistant/lifeassistant-system/src/main/java/top/lifeassistant/partner/mapper/PartnerMessageMapper.java`
- Create: `backend/lifeassistant/lifeassistant-system/src/main/java/top/lifeassistant/partner/model/req/PartnerMessageCreateReq.java`
- Create: `backend/lifeassistant/lifeassistant-system/src/main/java/top/lifeassistant/partner/model/resp/PartnerMessageResp.java`
- Create: `backend/lifeassistant/lifeassistant-system/src/main/java/top/lifeassistant/partner/model/resp/PartnerMessageImagesUploadResp.java`

**Interfaces:**
- Produces: 表 `partner_message`；DO 字段见下；CreateReq 字段见下；Resp 含 `id, createdBy, content, imageUrls, createdAt, sharedRecordId, todoId, pointsId`

- [ ] **Step 1: 写迁移**

```sql
-- V20__create_partner_message.sql
CREATE TABLE `partner_message` (
    `id` CHAR(36) NOT NULL COMMENT 'UUID 主键',
    `created_by` CHAR(36) NOT NULL COMMENT '作者用户 ID',
    `content` TEXT NULL COMMENT '文字；纯图可空',
    `image_urls` JSON NULL COMMENT '图片相对路径数组',
    `shared_record_id` CHAR(36) NULL COMMENT '同步记事 ID',
    `todo_id` CHAR(36) NULL COMMENT '同步待办 ID',
    `points_id` CHAR(36) NULL COMMENT '同步积分流水 ID',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    KEY `idx_created_by` (`created_by`),
    KEY `idx_created_at` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='伴侣留言板';
```

- [ ] **Step 2: DO + Mapper**

```java
@Data
@TableName("partner_message")
public class PartnerMessageDO implements Serializable {
    @Serial private static final long serialVersionUID = 1L;
    @TableId private String id;
    @TableField("created_by") private String createdBy;
    private String content;
    @TableField("image_urls") private String imageUrls;
    @TableField("shared_record_id") private String sharedRecordId;
    @TableField("todo_id") private String todoId;
    @TableField("points_id") private String pointsId;
    @TableField("created_at") private LocalDateTime createdAt;
}
```

```java
@Mapper
public interface PartnerMessageMapper extends BaseMapper<PartnerMessageDO> {}
```

- [ ] **Step 3: CreateReq + UploadResp + MessageResp**

```java
@Data
@Schema(description = "创建伴侣留言")
public class PartnerMessageCreateReq {
    private String content;
    private List<String> imageUrls;
    private Boolean publishSharedRecord;
    private Boolean publishTodo;
    /** self | partner | none */
    private String todoAssignedTo;
    private Boolean publishPoints;
    private Integer pointsChange;
    private String pointsReason;
}
```

```java
@Data
@Builder
public class PartnerMessageImagesUploadResp {
    private List<String> urls;
}
```

`PartnerMessageResp`：字段 `id, createdBy, content, List<String> imageUrls, createdAt, sharedRecordId, todoId, pointsId`；静态 `from(PartnerMessageDO)`：解析 `imageUrls` JSON，失败则 `List.of()`。

- [ ] **Step 4: Commit**

```powershell
git add "backend/lifeassistant/lifeassistant-server/src/main/resources/db/migration/V20__create_partner_message.sql" "backend/lifeassistant/lifeassistant-system/src/main/java/top/lifeassistant/partner/model/" "backend/lifeassistant/lifeassistant-system/src/main/java/top/lifeassistant/partner/mapper/PartnerMessageMapper.java"
git commit -m "feat: partner_message schema and DTOs"
```

---

### Task 2: ImageRules + UploadStorage + 单测

**Files:**
- Create: `backend/lifeassistant/lifeassistant-system/src/main/java/top/lifeassistant/partner/service/PartnerMessageImageRules.java`
- Create: `backend/lifeassistant/lifeassistant-system/src/test/java/top/lifeassistant/partner/service/PartnerMessageImageRulesTest.java`
- Modify: `backend/lifeassistant/lifeassistant-common/src/main/java/top/lifeassistant/common/component/UploadStorage.java`

**Interfaces:**
- Produces: `PartnerMessageImageRules` 公开常量 `MESSAGE_IMAGE_PREFIX="/uploads/partner-messages/"`、`MAX_IMAGES=9`；方法 `validateCreatePayload`、`validateUploadFiles`、`validatePublishFlags`、`safeFilename`、`resolveContentForSave`、`serializeImageUrls`、`truncateTitle(String, int)`
- Produces: `UploadStorage.partnerMessagesDir()` / `partnerMessageUrl(filename)`

- [ ] **Step 1: 写失败单测（先测规则类尚不存在）**

在 `PartnerMessageImageRulesTest`：

```java
@Test
void rejectsEmpty() {
    assertThrows(BadRequestException.class,
        () -> PartnerMessageImageRules.validateCreatePayload(null, null));
}

@Test
void rejectsTextAndImagesTogether() {
    assertThrows(BadRequestException.class,
        () -> PartnerMessageImageRules.validateCreatePayload("hi",
            List.of(PartnerMessageImageRules.MESSAGE_IMAGE_PREFIX + "a.jpg")));
}

@Test
void rejectsPublishFlagsOnImageOnly() {
    assertThrows(BadRequestException.class,
        () -> PartnerMessageImageRules.validatePublishFlags(
            null, List.of(PartnerMessageImageRules.MESSAGE_IMAGE_PREFIX + "a.jpg"),
            true, false, false));
}

@Test
void truncatesTitle() {
    String t = "x".repeat(300);
    assertEquals(255, PartnerMessageImageRules.truncateTitle(t, 255).length());
}
```

- [ ] **Step 2: 运行确认失败**

```powershell
cd "D:\life_assistant\backend\lifeassistant"
.\mvnw.cmd -pl lifeassistant-system -am test "-Dtest=PartnerMessageImageRulesTest" -q
```

Expected: 编译失败或测试找不到类。

- [ ] **Step 3: 实现 Rules（对齐 MediaCommentImageRules，并加互斥与开关校验）**

```java
final class PartnerMessageImageRules {
    static final String MESSAGE_IMAGE_PREFIX = "/uploads/partner-messages/";
    static final int MAX_IMAGES = 9;
    static final long MAX_BYTES = 5L * 1024 * 1024;
    static final Set<String> ALLOWED_EXT = Set.of("jpg", "jpeg", "png", "webp", "gif");

    static void validateCreatePayload(String content, List<String> imageUrls) {
        String trimmed = content == null ? "" : content.trim();
        List<String> urls = imageUrls == null ? List.of() : imageUrls;
        if (trimmed.isEmpty() && urls.isEmpty()) {
            throw new BadRequestException("留言不能为空");
        }
        if (!trimmed.isEmpty() && !urls.isEmpty()) {
            throw new BadRequestException("文字与图片不能同时发送");
        }
        if (urls.size() > MAX_IMAGES) {
            throw new BadRequestException("最多 " + MAX_IMAGES + " 张图片");
        }
        for (String url : urls) {
            if (url == null || url.isBlank() || !url.startsWith(MESSAGE_IMAGE_PREFIX)) {
                throw new BadRequestException("非法图片路径");
            }
        }
    }

    static void validatePublishFlags(String content, List<String> imageUrls,
            boolean shared, boolean todo, boolean points) {
        List<String> urls = imageUrls == null ? List.of() : imageUrls;
        if (!urls.isEmpty() && (shared || todo || points)) {
            throw new BadRequestException("图片留言不支持同步记事/待办/积分");
        }
        if ((shared || todo || points) && (content == null || content.isBlank())) {
            throw new BadRequestException("同步操作需要文字内容");
        }
    }

    static String truncateTitle(String text, int max) {
        if (text == null) return "";
        String t = text.trim();
        return t.length() <= max ? t : t.substring(0, max);
    }
    // validateUploadFiles / safeFilename / resolveContentForSave / serializeImageUrls
    // 复制 MediaCommentImageRules 对应实现，前缀与报错文案改为留言
}
```

- [ ] **Step 4: UploadStorage**

```java
private static final String PARTNER_MESSAGES_SUBDIR = "partner-messages";

public Path partnerMessagesDir() {
    return root.resolve(PARTNER_MESSAGES_SUBDIR);
}

public String partnerMessageUrl(String filename) {
    return "/uploads/" + PARTNER_MESSAGES_SUBDIR + "/" + filename;
}
```

- [ ] **Step 5: 跑通单测**

```powershell
cd "D:\life_assistant\backend\lifeassistant"
.\mvnw.cmd -pl lifeassistant-system -am test "-Dtest=PartnerMessageImageRulesTest" -q
```

Expected: PASS

- [ ] **Step 6: Commit**

```powershell
git add "backend/lifeassistant/lifeassistant-system/src/main/java/top/lifeassistant/partner/service/PartnerMessageImageRules.java" "backend/lifeassistant/lifeassistant-system/src/test/java/top/lifeassistant/partner/service/PartnerMessageImageRulesTest.java" "backend/lifeassistant/lifeassistant-common/src/main/java/top/lifeassistant/common/component/UploadStorage.java"
git commit -m "feat: partner message image rules and upload dir"
```

---

### Task 3: PartnerMessageService 基础 + Controller（无编排扩展）

**Files:**
- Create: `backend/lifeassistant/lifeassistant-system/src/main/java/top/lifeassistant/partner/service/PartnerMessageService.java`
- Create: `backend/lifeassistant/lifeassistant-system/src/main/java/top/lifeassistant/partner/controller/PartnerMessageController.java`

**Interfaces:**
- Consumes: Rules、UploadStorage、PartnerMessageMapper、CreateReq
- Produces:
  - `List<PartnerMessageResp> list(UserDO user, int page, int size)` — 双方 `created_by`，按 `created_at` 升序（聊天气泡）
  - `PartnerMessageImagesUploadResp uploadImages(UserDO user, MultipartFile[] files)`
  - `PartnerMessageResp create(UserDO user, PartnerMessageCreateReq req)` — 本 Task 只落留言，扩展字段若为 true 先抛 `BadRequestException("同步功能尚未启用")` **或** 直接忽略并在 Task 4 接上（推荐本 Task：`create` 已接校验，扩展开关 true 时暂不写关联，Task 4 补全）
  - `void delete(UserDO user, String id)` — 仅作者
  - `void deleteByCreatedBy(String userId1, String userId2)`

- [ ] **Step 1: Service 骨架**

```java
@Service
@RequiredArgsConstructor
public class PartnerMessageService {
    private final PartnerMessageMapper mapper;
    private final UploadStorage uploadStorage;

    private void requirePartner(UserDO user) {
        if (user.getPartnerId() == null) {
            throw new BadRequestException("请先绑定伴侣");
        }
    }

    public List<PartnerMessageResp> list(UserDO user, int page, int size) {
        requirePartner(user);
        int p = Math.max(page, 1);
        int s = Math.min(Math.max(size, 1), 100);
        Page<PartnerMessageDO> mp = new Page<>(p, s);
        LambdaQueryWrapper<PartnerMessageDO> qw = new LambdaQueryWrapper<>();
        qw.in(PartnerMessageDO::getCreatedBy, List.of(user.getId(), user.getPartnerId()))
          .orderByAsc(PartnerMessageDO::getCreatedAt);
        return mapper.selectPage(mp, qw).getRecords().stream()
            .map(PartnerMessageResp::from).toList();
    }

    public PartnerMessageImagesUploadResp uploadImages(UserDO user, MultipartFile[] files) throws IOException {
        requirePartner(user);
        PartnerMessageImageRules.validateUploadFiles(files);
        Path dir = uploadStorage.partnerMessagesDir();
        Files.createDirectories(dir);
        List<String> urls = new ArrayList<>();
        for (MultipartFile file : files) {
            String filename = UUID.randomUUID() + "_" + PartnerMessageImageRules.safeFilename(file);
            file.transferTo(dir.resolve(filename));
            urls.add(uploadStorage.partnerMessageUrl(filename));
        }
        return PartnerMessageImagesUploadResp.builder().urls(urls).build();
    }

    @Transactional
    public PartnerMessageResp create(UserDO user, PartnerMessageCreateReq req) {
        requirePartner(user);
        PartnerMessageImageRules.validateCreatePayload(req.getContent(), req.getImageUrls());
        boolean pubShared = Boolean.TRUE.equals(req.getPublishSharedRecord());
        boolean pubTodo = Boolean.TRUE.equals(req.getPublishTodo());
        boolean pubPoints = Boolean.TRUE.equals(req.getPublishPoints());
        PartnerMessageImageRules.validatePublishFlags(req.getContent(), req.getImageUrls(),
            pubShared, pubTodo, pubPoints);

        PartnerMessageDO msg = new PartnerMessageDO();
        msg.setId(UUID.randomUUID().toString());
        msg.setCreatedBy(user.getId());
        msg.setContent(PartnerMessageImageRules.resolveContentForSave(req.getContent()));
        msg.setImageUrls(PartnerMessageImageRules.serializeImageUrls(req.getImageUrls()));
        msg.setCreatedAt(LocalDateTime.now());
        mapper.insert(msg);
        // Task 4: 在此之后编排并 updateById 回写关联 id
        return PartnerMessageResp.from(msg);
    }

    public void delete(UserDO user, String id) {
        requirePartner(user);
        PartnerMessageDO msg = mapper.selectById(id);
        if (msg == null) throw new BadRequestException("留言不存在");
        if (!user.getId().equals(msg.getCreatedBy())) {
            throw new BadRequestException("只能删除自己的留言");
        }
        mapper.deleteById(id);
    }

    public void deleteByCreatedBy(String userId1, String userId2) {
        mapper.delete(new LambdaQueryWrapper<PartnerMessageDO>()
            .in(PartnerMessageDO::getCreatedBy, userId1, userId2));
    }
}
```

- [ ] **Step 2: Controller**

```java
@Tag(name = "伴侣留言板 API")
@RestController
@RequiredArgsConstructor
public class PartnerMessageController {
    private final PartnerMessageService service;

    @GetMapping("/partner/messages")
    public ApiResponse<List<PartnerMessageResp>> list(
            @CurrentUser UserDO user,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "50") int size) {
        return ApiResponse.ok(service.list(user, page, size));
    }

    @PostMapping("/partner/messages/images")
    public ApiResponse<PartnerMessageImagesUploadResp> upload(
            @CurrentUser UserDO user,
            @RequestParam("files") MultipartFile[] files) throws IOException {
        return ApiResponse.ok(service.uploadImages(user, files));
    }

    @PostMapping("/partner/messages")
    public ApiResponse<PartnerMessageResp> create(
            @CurrentUser UserDO user,
            @RequestBody PartnerMessageCreateReq req) {
        return ApiResponse.ok(service.create(user, req));
    }

    @DeleteMapping("/partner/messages/{id}")
    public ApiResponse<Void> delete(@CurrentUser UserDO user, @PathVariable String id) {
        service.delete(user, id);
        return ApiResponse.ok();
    }
}
```

- [ ] **Step 3: 编译**

```powershell
cd "D:\life_assistant\backend\lifeassistant"
.\mvnw.cmd -pl lifeassistant-system,lifeassistant-server -am compile -q
```

Expected: BUILD SUCCESS

- [ ] **Step 4: Commit**

```powershell
git add "backend/lifeassistant/lifeassistant-system/src/main/java/top/lifeassistant/partner/service/PartnerMessageService.java" "backend/lifeassistant/lifeassistant-system/src/main/java/top/lifeassistant/partner/controller/PartnerMessageController.java"
git commit -m "feat: partner message list upload create delete APIs"
```

---

### Task 4: 发帖编排 + addPoints 返回 id + 解绑清理

**Files:**
- Modify: `PartnerMessageService.java`
- Modify: `PartnerPointsService.java` — `addPoints` 改为 `return String`（流水 id）
- Modify: `PartnerPointsController.java` — 调用处忽略返回值即可
- Modify: `UserServiceImpl.java` — 注入 `PartnerMessageService`，unbind 时 `deleteByCreatedBy`

**Interfaces:**
- Consumes: `SharedRecordService.create`、`TodoService.create`、`PartnerPointsService.addPoints`
- Produces: create 回写 `sharedRecordId` / `todoId` / `pointsId`

- [ ] **Step 1: 改 `addPoints` 返回 id**

```java
@Transactional
public String addPoints(String userId, int pointsChange, String reason, LocalDate recordDate) {
    // ... 现有逻辑 ...
    String id = UUID.randomUUID().toString();
    record.setId(id);
    // insert + balance ...
    return id;
}
```

Controller 仍 `service.addPoints(...); return ApiResponse.ok();`

- [ ] **Step 2: 在 `PartnerMessageService.create` 末尾编排**

注入 `SharedRecordService`、`TodoService`、`PartnerPointsService`。

```java
if (pubShared) {
    SharedRecordCreateReq sr = new SharedRecordCreateReq();
    sr.setTitle(PartnerMessageImageRules.truncateTitle(msg.getContent(), 255));
    SharedRecordResp created = sharedRecordService.create(user, sr);
    msg.setSharedRecordId(created.getId());
}
if (pubTodo) {
    String assign = req.getTodoAssignedTo() == null ? "none" : req.getTodoAssignedTo();
    TodoCreateReq tr = new TodoCreateReq();
    tr.setTitle(PartnerMessageImageRules.truncateTitle(msg.getContent(), 255));
    tr.setPriority("medium");
    if ("partner".equals(assign)) {
        tr.setAssignedTo(user.getPartnerId());
    }
    // self / none → assignedTo 保持 null
    TodoResp created = todoService.create(user, tr);
    msg.setTodoId(created.getId());
}
if (pubPoints) {
    if (req.getPointsChange() == null || req.getPointsChange() == 0) {
        throw new BadRequestException("积分变更不能为 0");
    }
    String reason = (req.getPointsReason() == null || req.getPointsReason().isBlank())
        ? msg.getContent() : req.getPointsReason().trim();
    String pointsId = partnerPointsService.addPoints(
        user.getId(), req.getPointsChange(), reason, LocalDate.now());
    msg.setPointsId(pointsId);
}
if (pubShared || pubTodo || pubPoints) {
    mapper.updateById(msg);
}
return PartnerMessageResp.from(msg);
```

注意：整个 `create` 已有 `@Transactional`；任一步异常整单回滚（含已 insert 的留言）。

- [ ] **Step 3: 解绑清理**

在 `UserServiceImpl.unbindPartner`，`sharedRecordService.deleteByCreatedBy` 同行增加：

```java
partnerMessageService.deleteByCreatedBy(me.getId(), partner.getId());
```

- [ ] **Step 4: 编译**

```powershell
cd "D:\life_assistant\backend\lifeassistant"
.\mvnw.cmd -pl lifeassistant-system,lifeassistant-server -am compile -q
```

Expected: BUILD SUCCESS

- [ ] **Step 5: Commit**

```powershell
git add "backend/lifeassistant/lifeassistant-system/src/main/java/top/lifeassistant/partner/service/PartnerMessageService.java" "backend/lifeassistant/lifeassistant-system/src/main/java/top/lifeassistant/partner/service/PartnerPointsService.java" "backend/lifeassistant/lifeassistant-system/src/main/java/top/lifeassistant/partner/controller/PartnerPointsController.java" "backend/lifeassistant/lifeassistant-system/src/main/java/top/lifeassistant/system/service/impl/UserServiceImpl.java"
git commit -m "feat: orchestrate message publish to record todo points"
```

---

### Task 5: 前端 API 模块 +「一起做过的事」迁二级页

**Files:**
- Create: `front/vue3-vant-mobile/src/api/modules/partner-messages.ts`
- Create: `front/vue3-vant-mobile/src/pages/partner/dashboard/shared-records.vue`
- Modify: `front/vue3-vant-mobile/src/pages/partner/dashboard/index.vue` — `router.push('/partner/dashboard/shared-records')`
- Modify: `front/vue3-vant-mobile/src/pages/share/index.vue` — **本 Task 先抽离列表**：把原列表相关 script/template 迁到 `shared-records.vue`；`share/index.vue` 暂时只保留绑定引导 + 已绑定时空态「留言板即将上线」或直接空列表占位（Task 6 填满）。更干净做法：本 Task 把完整原列表迁走后，`share/index.vue` 只留绑定块。

**Interfaces:**
- Produces: `fetchPartnerMessages` / `uploadPartnerMessageImages` / `createPartnerMessage` / `deletePartnerMessage`

- [ ] **Step 1: API**

```ts
// partner-messages.ts
export interface PartnerMessage {
  id: string
  createdBy: string
  content?: string | null
  imageUrls: string[]
  createdAt: string
  sharedRecordId?: string | null
  todoId?: string | null
  pointsId?: string | null
}

export interface PartnerMessageCreateBody {
  content?: string
  imageUrls?: string[]
  publishSharedRecord?: boolean
  publishTodo?: boolean
  todoAssignedTo?: 'self' | 'partner' | 'none'
  publishPoints?: boolean
  pointsChange?: number
  pointsReason?: string
}

export function fetchPartnerMessages(params?: { page?: number, size?: number }) {
  return request.get<ApiResponse<PartnerMessage[]>>('/partner/messages', { params })
}

export function uploadPartnerMessageImages(files: File[]) {
  const formData = new FormData()
  files.forEach(f => formData.append('files', f))
  return request.post<ApiResponse<{ urls: string[] }>>('/partner/messages/images', formData, {
    headers: { 'Content-Type': 'multipart/form-data' },
  })
}

export function createPartnerMessage(data: PartnerMessageCreateBody) {
  return request.post<ApiResponse<PartnerMessage>>('/partner/messages', data)
}

export function deletePartnerMessage(id: string) {
  return request.delete<ApiResponse<void>>(`/partner/messages/${id}`)
}
```

（`ApiResponse` / `request` import 对齐 `shared-media.ts`。）

- [ ] **Step 2: 新建 `shared-records.vue`**

从当前 `share/index.vue` 复制「一起做过的事」列表/搜索/分页/新建编辑逻辑与样式；页面顶部加 `van-nav-bar` 标题「一起做过的事」`left-arrow` `@click="router.back()"`。删除该文件中的绑定引导块（绑定只留在 Tab 伴侣页）。

- [ ] **Step 3: 精简 `share/index.vue`**

保留绑定引导；已绑定分支先放简单占位（Task 6 替换）：

```vue
<div v-if="userInfo?.partnerId" class="msg-placeholder">
  <!-- Task 6 fills message board -->
</div>
```

去掉对 `shared-records` API 的 import 与列表 UI。

- [ ] **Step 4: 看板跳转**

`index.vue` 中：

```ts
@click="router.push('/partner/dashboard/shared-records')"
```

- [ ] **Step 5: 手测**

启动前后端；看板点「共享记录」应进入二级列表且 CRUD 可用；Tab「伴侣」不再显示该列表。

- [ ] **Step 6: Commit**

```powershell
git add "front/vue3-vant-mobile/src/api/modules/partner-messages.ts" "front/vue3-vant-mobile/src/pages/partner/dashboard/shared-records.vue" "front/vue3-vant-mobile/src/pages/partner/dashboard/index.vue" "front/vue3-vant-mobile/src/pages/share/index.vue"
git commit -m "feat(front): move shared records under dashboard secondary page"
```

---

### Task 6: 留言板气泡 UI（文字 + 图片 + 跨帖预览）

**Files:**
- Modify: `front/vue3-vant-mobile/src/pages/share/index.vue`
- Reuse: `front/vue3-vant-mobile/src/pages/share/media/commentImageGallery.ts`（`PartnerMessage` 已有 `id` + `imageUrls`，可直接传入）

**Interfaces:**
- Consumes: Task 5 API；影音详情页气泡样式可对照 `share/media/[id].vue` 评论区

- [ ] **Step 1: 列表 + 发文字**

已绑定后：

- `messages` ref；`loadMessages` 调 `fetchPartnerMessages({ page: 1, size: 50 })`
- 气泡：`msg.createdBy === userInfo.id` 右侧，否则左侧
- 底部：`van-field` + 发送；`createPartnerMessage({ content })` 成功后 append 并滚底
- 长按/按钮删除自己的：`deletePartnerMessage`

对照 `media/[id].vue` 的 comment 气泡 class，能抄则抄，避免重新发明样式。

- [ ] **Step 2: 发图片模式**

- 模式切换：文字 | 图片
- 图片：`van-uploader` `max-count=9`，`before-read` 校验扩展名与 5MB（复制媒体评论）；`after-read` 收集 File → `uploadPartnerMessageImages` → `createPartnerMessage({ imageUrls })`
- 切模式时：进图片清空文字；进文字清空 uploader 文件列表
- 气泡九宫格展示 + `showImagePreview`：

```ts
import { buildCommentImageGallery } from '@/pages/share/media/commentImageGallery'

function preview(msg: PartnerMessage, localIndex: number) {
  const { images, startPosition } = buildCommentImageGallery(messages.value, msg.id, localIndex)
  showImagePreview({ images, startPosition, showIndicators: images.length > 1, closeable: true, teleport: 'body' })
}
```

- [ ] **Step 3: 手测**

纯文字、纯图（1～9）、跨帖滑动预览、删除自己的留言。

- [ ] **Step 4: Commit**

```powershell
git add "front/vue3-vant-mobile/src/pages/share/index.vue"
git commit -m "feat(front): partner tab message board with text and images"
```

---

### Task 7: 文字发帖扩展开关 UI

**Files:**
- Modify: `front/vue3-vant-mobile/src/pages/share/index.vue`
- Modify: `front/vue3-vant-mobile/src/locales/zh-CN.json`、`en-US.json`（留言板相关文案键，如 `share.messageBoard`、`share.publishRecord` 等）

**Interfaces:**
- Consumes: `PartnerMessageCreateBody` 开关字段

- [ ] **Step 1: 开关区（仅文字模式）**

三个 `van-checkbox`（或 switch）：

1. 记到一起做过的事 → `publishSharedRecord`
2. 建待办 → `publishTodo`；勾选后 `van-radio-group`：自己 / 对方 / 不指定 → `todoAssignedTo`
3. 改积分 → `publishPoints`；勾选后正负切换 + `van-field type=digit` 分值；可选原因字段默认留言内容

发送：

```ts
await createPartnerMessage({
  content: text.value.trim(),
  publishSharedRecord: flags.record,
  publishTodo: flags.todo,
  todoAssignedTo: flags.todo ? flags.assign : undefined,
  publishPoints: flags.points,
  pointsChange: flags.points ? (flags.pointsSign * Number(flags.pointsValue)) : undefined,
  pointsReason: flags.points ? (flags.pointsReason || text.value.trim()) : undefined,
})
```

校验：积分勾选时 `pointsChange !== 0`；对方指派时确认已绑定（页面已绑定）。

- [ ] **Step 2: 手测验收**

按 spec 验收 3–6：记事出现在二级页；待办三种指派；积分余额/流水；多开关全成。

- [ ] **Step 3: Commit**

```powershell
git add "front/vue3-vant-mobile/src/pages/share/index.vue" "front/vue3-vant-mobile/src/locales/zh-CN.json" "front/vue3-vant-mobile/src/locales/en-US.json"
git commit -m "feat(front): message compose toggles for record todo points"
```

---

### Task 8: 发版 bump + 用户向日志

**Files:**
- Modify: `front/vue3-vant-mobile/package.json` → `1.10.0`（若当前已非 1.9.x，按「当前 minor+1」）
- Modify: `backend/lifeassistant/lifeassistant-server/src/main/resources/config/application.yml` → `application.version: 1.10.0-SNAPSHOT`
- Modify: `CHANGELOG.md` — 顶部新节
- Modify: `docs/USER_CHANGELOG.md` — 用户向短句

- [ ] **Step 1: 写 CHANGELOG**

根 `CHANGELOG.md` 示例结构（对照最近一节）：

```markdown
## v1.10.0 (2026-08-24)

### 新特性
- 伴侣 Tab 改为双方气泡留言板（文字或最多 9 张图；图可跨留言预览）
- 文字留言可同步到「一起做过的事」、待办（可选指派）、加减积分
- 「一起做过的事」迁至看板「共享记录」二级页

### 后端
- 新增 `partner_message` 与 `/partner/messages` 编排 API

### 前端
- 留言板发帖区与看板二级共享记录页
```

`USER_CHANGELOG.md` 用人话写能感知到的变化，不对齐 API 名。

- [ ] **Step 2: bump 版本号文件**

- [ ] **Step 3: Commit**

```powershell
git add "front/vue3-vant-mobile/package.json" "backend/lifeassistant/lifeassistant-server/src/main/resources/config/application.yml" "CHANGELOG.md" "docs/USER_CHANGELOG.md"
git commit -m "chore: bump app to v1.10.0 for partner message board"
```

---

## Self-review (plan vs spec)

| Spec 要求 | Task |
|-----------|------|
| 一起做过的事 → 看板二级页 | Task 5 |
| Tab 伴侣 → 气泡留言板 | Task 6 |
| 文字/图互斥，最多 9，跨帖预览 | Task 2、6 |
| 文字开关：记事/待办/积分 | Task 4、7 |
| 待办指派 self/partner/none | Task 4、7 |
| 未绑定引导仍在伴侣 Tab | Task 5/6 保留绑定块 |
| 后端事务编排 | Task 4 |
| 删除不级联；解绑清留言 | Task 3 delete + Task 4 unbind |
| minor 发版 + USER_CHANGELOG | Task 8 |
| 图片规则单测 | Task 2 |

无 TBD/占位；类型名前后一致（`PartnerMessageCreateReq` / `todoAssignedTo` / `MESSAGE_IMAGE_PREFIX`）。
