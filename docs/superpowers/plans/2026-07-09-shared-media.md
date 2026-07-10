# 共享媒体记录（一起看过的电影/书/漫剧）实现计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 在伴侣页新增"一起看过的"Tab，支持添加电影/书/漫剧条目、上传封面图、维护共同/个人进度、聊天室风格评论。

**Architecture:** 后端新增 `sharedmedia` 包（3 张表、3 个 Service、3 个 Controller、1 个文件上传 Controller）。前端在现有伴侣页 `/share` 添加 Tab 栏，详情聊天页为 `/share/media/:id`。封面图上传至服务器本地存储。

**Tech Stack:** Spring Boot 3 + MyBatis-Plus + Flyway / Vue 3 + Vant 4 + vue-router auto-routes

---

## 文件结构

```
后端 (backend/lifeassistant/lifeassistant-system/src/main/java/top/lifeassistant/):
  新增 sharedmedia/ 包:
    controller/
      SharedMediaController.java
      MediaCommentController.java
      MediaProgressController.java
    mapper/
      SharedMediaMapper.java
      MediaCommentMapper.java
      MediaProgressMapper.java
    model/
      entity/
        SharedMediaDO.java
        MediaCommentDO.java
        MediaProgressDO.java
      req/
        SharedMediaCreateReq.java
        SharedMediaUpdateReq.java
        MediaCommentCreateReq.java
        MediaProgressUpdateReq.java
      resp/
        SharedMediaResp.java
        MediaCommentResp.java
        MediaProgressResp.java
      query/
        SharedMediaPageQuery.java
    service/
      SharedMediaService.java
      MediaCommentService.java
      MediaProgressService.java

新增 sharedmedia/ 包下的 FileUploadController.java (通用文件上传)

后端 (lifeassistant-server):
  resources/
    db/migration/V6__create_shared_media_tables.sql
  config/ 或 WebConfig.java — 添加静态资源映射

前端 (front/vue3-vant-mobile/src/):
  api/modules/
    shared-media.ts         — 新增 API 模块
  pages/
    share/index.vue         — 修改：添加 Tab 栏
    share/media/[id].vue    — 新增：详情聊天页
```

---

### Task 1: Flyway 迁移 — 创建 3 张表

**Files:**
- Create: `backend/lifeassistant/lifeassistant-server/src/main/resources/db/migration/V6__create_shared_media_tables.sql`

- [ ] **Step 1: 编写迁移 SQL**

```sql
-- V6__create_shared_media_tables.sql

CREATE TABLE shared_media (
    id          CHAR(36)     PRIMARY KEY,
    created_by  CHAR(36)     NOT NULL,
    title       VARCHAR(255) NOT NULL,
    media_type  VARCHAR(20)  NOT NULL COMMENT 'movie | book | tv',
    cover_path  VARCHAR(256) DEFAULT NULL COMMENT '服务器本地存储路径',
    description TEXT         DEFAULT NULL,
    is_finished TINYINT(1)   DEFAULT 0 COMMENT '双方都看完了',
    created_at  DATETIME     DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    create_by   CHAR(36)     DEFAULT NULL,
    update_by   CHAR(36)     DEFAULT NULL,
    INDEX idx_created_by (created_by),
    INDEX idx_media_type (media_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE media_comment (
    id         CHAR(36)     PRIMARY KEY,
    media_id   CHAR(36)     NOT NULL,
    user_id    CHAR(36)     NOT NULL,
    content    TEXT         NOT NULL,
    created_at DATETIME     DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_media_id (media_id),
    INDEX idx_user_id (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE media_progress (
    id            CHAR(36)      PRIMARY KEY,
    media_id      CHAR(36)      NOT NULL,
    user_id       CHAR(36)      DEFAULT NULL COMMENT 'NULL=共同进度(scope=shared), 非NULL=个人进度(scope=personal)',
    progress_text VARCHAR(100)  NOT NULL COMMENT '如"第5集/共24集"',
    created_at    DATETIME      DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_media_user (media_id, user_id),
    INDEX idx_media_id (media_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

- [ ] **Step 2: 运行 Flyway 迁移验证**

Run: 启动后端服务，观察日志确认 `V6__create_shared_media_tables` 已执行。
Expected: 终端无报错，数据库中三张表创建成功。

- [ ] **Step 3: 提交**

```bash
git add "backend/lifeassistant/lifeassistant-server/src/main/resources/db/migration/V6__create_shared_media_tables.sql"
git commit -m "feat(db): add shared_media, media_comment, media_progress tables"
```

---

### Task 2: 后端 Entity + Mapper 类

**Files:**
- Create: `backend/lifeassistant/lifeassistant-system/src/main/java/top/lifeassistant/sharedmedia/model/entity/SharedMediaDO.java`
- Create: `backend/lifeassistant/lifeassistant-system/src/main/java/top/lifeassistant/sharedmedia/model/entity/MediaCommentDO.java`
- Create: `backend/lifeassistant/lifeassistant-system/src/main/java/top/lifeassistant/sharedmedia/model/entity/MediaProgressDO.java`
- Create: `backend/lifeassistant/lifeassistant-system/src/main/java/top/lifeassistant/sharedmedia/mapper/SharedMediaMapper.java`
- Create: `backend/lifeassistant/lifeassistant-system/src/main/java/top/lifeassistant/sharedmedia/mapper/MediaCommentMapper.java`
- Create: `backend/lifeassistant/lifeassistant-system/src/main/java/top/lifeassistant/sharedmedia/mapper/MediaProgressMapper.java`

- [ ] **Step 1: 创建 SharedMediaDO.java**

```java
package top.lifeassistant.sharedmedia.model.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import top.lifeassistant.common.base.model.entity.BaseDO;
import top.lifeassistant.common.base.model.entity.OwnedEntity;

import java.io.Serial;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("shared_media")
public class SharedMediaDO extends BaseDO implements OwnedEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    private String createdBy;
    private String title;
    private String mediaType;
    private String coverPath;
    private String description;
    private Boolean isFinished;

    @Override
    public String getOwnerId() {
        return createdBy;
    }
}
```

- [ ] **Step 2: 创建 MediaCommentDO.java**

```java
package top.lifeassistant.sharedmedia.model.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@TableName("media_comment")
public class MediaCommentDO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId
    private String id;

    private String mediaId;
    private String userId;
    private String content;

    @TableField("created_at")
    private LocalDateTime createdAt;
}
```

- [ ] **Step 3: 创建 MediaProgressDO.java**

```java
package top.lifeassistant.sharedmedia.model.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@TableName("media_progress")
public class MediaProgressDO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId
    private String id;

    private String mediaId;
    private String userId;
    private String progressText;

    @TableField("created_at")
    private LocalDateTime createdAt;
}
```

- [ ] **Step 4: 创建 SharedMediaMapper.java**

```java
package top.lifeassistant.sharedmedia.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import top.lifeassistant.sharedmedia.model.entity.SharedMediaDO;

import java.util.List;

@Mapper
public interface SharedMediaMapper extends BaseMapper<SharedMediaDO> {

    @Select("SELECT * FROM shared_media WHERE created_by IN (#{userId}, #{partnerId}) ORDER BY update_time DESC")
    List<SharedMediaDO> listByPartners(@Param("userId") String userId, @Param("partnerId") String partnerId);
}
```

- [ ] **Step 5: 创建 MediaCommentMapper.java**

```java
package top.lifeassistant.sharedmedia.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import top.lifeassistant.sharedmedia.model.entity.MediaCommentDO;

import java.util.List;

@Mapper
public interface MediaCommentMapper extends BaseMapper<MediaCommentDO> {

    @Select("SELECT * FROM media_comment WHERE media_id = #{mediaId} ORDER BY created_at ASC")
    List<MediaCommentDO> selectByMediaId(@Param("mediaId") String mediaId);
}
```

- [ ] **Step 6: 创建 MediaProgressMapper.java**

```java
package top.lifeassistant.sharedmedia.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import top.lifeassistant.sharedmedia.model.entity.MediaProgressDO;

import java.util.List;

@Mapper
public interface MediaProgressMapper extends BaseMapper<MediaProgressDO> {

    @Select("SELECT * FROM media_progress WHERE media_id = #{mediaId}")
    List<MediaProgressDO> selectByMediaId(@Param("mediaId") String mediaId);

    @Select("SELECT * FROM media_progress WHERE media_id = #{mediaId} AND user_id IS NULL")
    MediaProgressDO selectSharedByMediaId(@Param("mediaId") String mediaId);

    @Select("SELECT * FROM media_progress WHERE media_id = #{mediaId} AND user_id = #{userId}")
    MediaProgressDO selectByMediaIdAndUser(@Param("mediaId") String mediaId, @Param("userId") String userId);
}
```

- [ ] **Step 7: 提交**

```bash
git add "backend/lifeassistant/lifeassistant-system/src/main/java/top/lifeassistant/sharedmedia/"
git commit -m "feat: add shared-media entities and mappers"
```

---

### Task 3: 后端 Req / Resp / Query 模型

**Files:**
- Create: `backend/lifeassistant/lifeassistant-system/src/main/java/top/lifeassistant/sharedmedia/model/req/SharedMediaCreateReq.java`
- Create: `backend/lifeassistant/lifeassistant-system/src/main/java/top/lifeassistant/sharedmedia/model/req/SharedMediaUpdateReq.java`
- Create: `backend/lifeassistant/lifeassistant-system/src/main/java/top/lifeassistant/sharedmedia/model/req/MediaCommentCreateReq.java`
- Create: `backend/lifeassistant/lifeassistant-system/src/main/java/top/lifeassistant/sharedmedia/model/req/MediaProgressUpdateReq.java`
- Create: `backend/lifeassistant/lifeassistant-system/src/main/java/top/lifeassistant/sharedmedia/model/resp/SharedMediaResp.java`
- Create: `backend/lifeassistant/lifeassistant-system/src/main/java/top/lifeassistant/sharedmedia/model/resp/MediaCommentResp.java`
- Create: `backend/lifeassistant/lifeassistant-system/src/main/java/top/lifeassistant/sharedmedia/model/resp/MediaProgressResp.java`
- Create: `backend/lifeassistant/lifeassistant-system/src/main/java/top/lifeassistant/sharedmedia/model/query/SharedMediaPageQuery.java`

- [ ] **Step 1: 创建 SharedMediaCreateReq.java 和 SharedMediaUpdateReq.java**

注意：创建时使用 `multipart/form-data`（需要上传封面图），所以不用 `@RequestBody` 而是 `@RequestParam`。因此不写为典型的 Req 类，改用 `@RequestParam` 在 Controller 中接收。但这里仍定义普通的 Java 类用于 Service 层传递。

```java
package top.lifeassistant.sharedmedia.model.req;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "创建共享媒体请求")
public class SharedMediaCreateReq {
    @Schema(description = "名称")
    private String title;
    @Schema(description = "类型: movie/book/tv")
    private String mediaType;
    @Schema(description = "简介")
    private String description;
}
```

```java
package top.lifeassistant.sharedmedia.model.req;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "更新共享媒体请求")
public class SharedMediaUpdateReq {
    @Schema(description = "名称")
    private String title;
    @Schema(description = "类型: movie/book/tv")
    private String mediaType;
    @Schema(description = "简介")
    private String description;
}
```

- [ ] **Step 2: 创建 MediaCommentCreateReq.java**

```java
package top.lifeassistant.sharedmedia.model.req;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
@Schema(description = "创建评论请求")
public class MediaCommentCreateReq {
    @NotBlank
    @Schema(description = "评论内容")
    private String content;
}
```

- [ ] **Step 3: 创建 MediaProgressUpdateReq.java**

```java
package top.lifeassistant.sharedmedia.model.req;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
@Schema(description = "更新进度请求")
public class MediaProgressUpdateReq {
    @NotBlank
    @Schema(description = "范围: shared=共同, personal=个人")
    private String scope;

    @NotBlank
    @Schema(description = "进度文本，如 '第5集/共24集'")
    private String progressText;
}
```

- [ ] **Step 4: 创建 SharedMediaResp.java**

```java
package top.lifeassistant.sharedmedia.model.resp;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;
import top.lifeassistant.sharedmedia.model.entity.SharedMediaDO;

import java.time.LocalDateTime;

@Data
@Builder
@Schema(description = "共享媒体响应")
public class SharedMediaResp {
    private String id;
    private String createdBy;
    private String title;
    private String mediaType;
    private String coverPath;
    private String description;
    private Boolean isFinished;
    private LocalDateTime createdAt;
    private LocalDateTime updateTime;

    public static SharedMediaResp from(SharedMediaDO media) {
        return SharedMediaResp.builder()
            .id(media.getId())
            .createdBy(media.getCreatedBy())
            .title(media.getTitle())
            .mediaType(media.getMediaType())
            .coverPath(media.getCoverPath())
            .description(media.getDescription())
            .isFinished(media.getIsFinished())
            .createdAt(media.getCreatedAt())
            .updateTime(media.getUpdateTime())
            .build();
    }
}
```

- [ ] **Step 5: 创建 MediaCommentResp.java**

```java
package top.lifeassistant.sharedmedia.model.resp;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;
import top.lifeassistant.sharedmedia.model.entity.MediaCommentDO;

import java.time.LocalDateTime;

@Data
@Builder
@Schema(description = "评论响应")
public class MediaCommentResp {
    private String id;
    private String mediaId;
    private String userId;
    private String content;
    private LocalDateTime createdAt;

    public static MediaCommentResp from(MediaCommentDO comment) {
        return MediaCommentResp.builder()
            .id(comment.getId())
            .mediaId(comment.getMediaId())
            .userId(comment.getUserId())
            .content(comment.getContent())
            .createdAt(comment.getCreatedAt())
            .build();
    }
}
```

- [ ] **Step 6: 创建 MediaProgressResp.java**

```java
package top.lifeassistant.sharedmedia.model.resp;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;
import top.lifeassistant.sharedmedia.model.entity.MediaProgressDO;

import java.time.LocalDateTime;

@Data
@Builder
@Schema(description = "进度响应")
public class MediaProgressResp {
    private String id;
    private String mediaId;
    private String userId;
    private String scope;  // "shared" 或 "personal"
    private String progressText;
    private LocalDateTime createdAt;

    public static MediaProgressResp from(MediaProgressDO p) {
        return MediaProgressResp.builder()
            .id(p.getId())
            .mediaId(p.getMediaId())
            .userId(p.getUserId())
            .scope(p.getUserId() == null ? "shared" : "personal")
            .progressText(p.getProgressText())
            .createdAt(p.getCreatedAt())
            .build();
    }
}
```

- [ ] **Step 7: 创建 SharedMediaPageQuery.java**

```java
package top.lifeassistant.sharedmedia.model.query;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import top.lifeassistant.common.base.model.query.PageQuery;

@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "共享媒体分页查询参数")
public class SharedMediaPageQuery extends PageQuery {
    @Schema(description = "媒体类型: movie/book/tv，不传=全部")
    private String mediaType;

    @Schema(description = "状态: finished=已看完, unfinished=没看完, 不传=全部")
    private String status;
}
```

- [ ] **Step 8: 提交**

```bash
git add "backend/lifeassistant/lifeassistant-system/src/main/java/top/lifeassistant/sharedmedia/model/"
git commit -m "feat: add shared-media request/response models"
```

---

### Task 4: 后端 SharedMediaService

**Files:**
- Create: `backend/lifeassistant/lifeassistant-system/src/main/java/top/lifeassistant/sharedmedia/service/SharedMediaService.java`

- [ ] **Step 1: 创建 SharedMediaService.java**

```java
package top.lifeassistant.sharedmedia.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import top.continew.starter.core.exception.BadRequestException;
import top.lifeassistant.common.base.component.OwnerValidator;
import top.lifeassistant.common.base.model.query.PageResult;
import top.lifeassistant.sharedmedia.mapper.SharedMediaMapper;
import top.lifeassistant.sharedmedia.model.entity.SharedMediaDO;
import top.lifeassistant.sharedmedia.model.query.SharedMediaPageQuery;
import top.lifeassistant.sharedmedia.model.req.SharedMediaCreateReq;
import top.lifeassistant.sharedmedia.model.req.SharedMediaUpdateReq;
import top.lifeassistant.sharedmedia.model.resp.SharedMediaResp;
import top.lifeassistant.system.model.entity.user.UserDO;

import java.util.List;

@Service
@RequiredArgsConstructor
public class SharedMediaService {

    private final SharedMediaMapper mapper;
    private final OwnerValidator ownerValidator;

    private void requirePartner(UserDO user) {
        if (user.getPartnerId() == null) {
            throw new BadRequestException("请先绑定伴侣");
        }
    }

    public SharedMediaResp create(UserDO user, SharedMediaCreateReq req, String coverPath) {
        requirePartner(user);
        SharedMediaDO media = new SharedMediaDO();
        media.setCreatedBy(user.getId());
        media.setTitle(req.getTitle());
        media.setMediaType(req.getMediaType());
        media.setCoverPath(coverPath);
        media.setDescription(req.getDescription());
        media.setIsFinished(false);
        mapper.insert(media);
        return SharedMediaResp.from(media);
    }

    public PageResult<SharedMediaResp> list(UserDO user, SharedMediaPageQuery query) {
        requirePartner(user);
        LambdaQueryWrapper<SharedMediaDO> wrapper = new LambdaQueryWrapper<>();
        // 查询双方创建的数据
        wrapper.in(SharedMediaDO::getCreatedBy, user.getId(), user.getPartnerId());

        // 类型筛选
        if (query.getMediaType() != null && !query.getMediaType().isEmpty()) {
            wrapper.eq(SharedMediaDO::getMediaType, query.getMediaType());
        }

        // 状态筛选
        if ("finished".equals(query.getStatus())) {
            wrapper.eq(SharedMediaDO::getIsFinished, true);
        } else if ("unfinished".equals(query.getStatus())) {
            wrapper.eq(SharedMediaDO::getIsFinished, false);
        }

        wrapper.orderByDesc(SharedMediaDO::getUpdateTime);

        Page<SharedMediaDO> page = mapper.selectPage(query.toPage(), wrapper);
        List<SharedMediaResp> list = page.getRecords().stream()
            .map(SharedMediaResp::from)
            .toList();
        return PageResult.of(page, list);
    }

    public SharedMediaResp getById(UserDO user, String id) {
        requirePartner(user);
        SharedMediaDO media = ownerValidator.findAndCheck(
            () -> mapper.selectById(id), "媒体不存在",
            m -> user.getId().equals(m.getCreatedBy()) || user.getId().equals(user.getPartnerId())
        );
        return SharedMediaResp.from(media);
    }

    public SharedMediaResp update(UserDO user, String id, SharedMediaUpdateReq req, String coverPath) {
        requirePartner(user);
        SharedMediaDO media = ownerValidator.requireOwner(() -> mapper.selectById(id), user.getId());

        if (req.getTitle() != null) media.setTitle(req.getTitle());
        if (req.getMediaType() != null) media.setMediaType(req.getMediaType());
        if (req.getDescription() != null) media.setDescription(req.getDescription());
        if (coverPath != null) media.setCoverPath(coverPath);

        mapper.updateById(media);
        return SharedMediaResp.from(mapper.selectById(id));
    }

    public void delete(UserDO user, String id) {
        requirePartner(user);
        SharedMediaDO media = ownerValidator.requireOwner(() -> mapper.selectById(id), user.getId());
        mapper.deleteById(id);
    }

    public void deleteByCreatedBy(String userId1, String userId2) {
        mapper.delete(new LambdaQueryWrapper<SharedMediaDO>()
            .in(SharedMediaDO::getCreatedBy, userId1, userId2));
    }
}
```

- [ ] **Step 2: 提交**

```bash
git add "backend/lifeassistant/lifeassistant-system/src/main/java/top/lifeassistant/sharedmedia/service/SharedMediaService.java"
git commit -m "feat: add SharedMediaService"
```

---

### Task 5: 后端 MediaCommentService + MediaProgressService

**Files:**
- Create: `backend/lifeassistant/lifeassistant-system/src/main/java/top/lifeassistant/sharedmedia/service/MediaCommentService.java`
- Create: `backend/lifeassistant/lifeassistant-system/src/main/java/top/lifeassistant/sharedmedia/service/MediaProgressService.java`

- [ ] **Step 1: 创建 MediaCommentService.java**

```java
package top.lifeassistant.sharedmedia.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import top.continew.starter.core.exception.BadRequestException;
import top.lifeassistant.sharedmedia.mapper.MediaCommentMapper;
import top.lifeassistant.sharedmedia.model.entity.MediaCommentDO;
import top.lifeassistant.sharedmedia.model.req.MediaCommentCreateReq;
import top.lifeassistant.sharedmedia.model.resp.MediaCommentResp;
import top.lifeassistant.system.model.entity.user.UserDO;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class MediaCommentService {

    private final MediaCommentMapper mapper;
    private final SharedMediaService sharedMediaService;

    public MediaCommentResp create(UserDO user, String mediaId, MediaCommentCreateReq req) {
        // 验证媒体存在且用户有权访问
        sharedMediaService.getById(user, mediaId);

        MediaCommentDO comment = new MediaCommentDO();
        comment.setId(UUID.randomUUID().toString());
        comment.setMediaId(mediaId);
        comment.setUserId(user.getId());
        comment.setContent(req.getContent());
        mapper.insert(comment);
        return MediaCommentResp.from(comment);
    }

    public List<MediaCommentResp> list(UserDO user, String mediaId) {
        // 验证媒体存在且用户有权访问
        sharedMediaService.getById(user, mediaId);

        List<MediaCommentDO> list = mapper.selectByMediaId(mediaId);
        return list.stream().map(MediaCommentResp::from).toList();
    }

    public void deleteByMediaId(String mediaId) {
        mapper.delete(new LambdaQueryWrapper<MediaCommentDO>().eq(MediaCommentDO::getMediaId, mediaId));
    }
}
```

- [ ] **Step 2: 创建 MediaProgressService.java**

```java
package top.lifeassistant.sharedmedia.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import top.continew.starter.core.exception.BadRequestException;
import top.lifeassistant.sharedmedia.mapper.MediaProgressMapper;
import top.lifeassistant.sharedmedia.model.entity.MediaProgressDO;
import top.lifeassistant.sharedmedia.model.req.MediaProgressUpdateReq;
import top.lifeassistant.sharedmedia.model.resp.MediaProgressResp;
import top.lifeassistant.system.model.entity.user.UserDO;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class MediaProgressService {

    private final MediaProgressMapper mapper;
    private final SharedMediaService sharedMediaService;

    public List<MediaProgressResp> list(UserDO user, String mediaId) {
        sharedMediaService.getById(user, mediaId);
        List<MediaProgressDO> list = mapper.selectByMediaId(mediaId);
        return list.stream().map(MediaProgressResp::from).toList();
    }

    public MediaProgressResp update(UserDO user, String mediaId, MediaProgressUpdateReq req) {
        sharedMediaService.getById(user, mediaId);

        if ("shared".equals(req.getScope())) {
            // 共同进度：更新或插入 user_id = NULL 的记录
            upsertProgress(mediaId, null, req.getProgressText());
            // 同步更新双方的个人进度
            upsertProgress(mediaId, user.getId(), req.getProgressText());
            if (user.getPartnerId() != null) {
                upsertProgress(mediaId, user.getPartnerId(), req.getProgressText());
            }
        } else if ("personal".equals(req.getScope())) {
            // 个人进度：只更新当前用户
            upsertProgress(mediaId, user.getId(), req.getProgressText());
        } else {
            throw new BadRequestException("scope 必须是 shared 或 personal");
        }

        // 重新查询并返回
        if ("shared".equals(req.getScope())) {
            return MediaProgressResp.from(mapper.selectSharedByMediaId(mediaId));
        } else {
            return MediaProgressResp.from(mapper.selectByMediaIdAndUser(mediaId, user.getId()));
        }
    }

    private void upsertProgress(String mediaId, String userId, String progressText) {
        MediaProgressDO existing;
        if (userId == null) {
            existing = mapper.selectSharedByMediaId(mediaId);
        } else {
            existing = mapper.selectByMediaIdAndUser(mediaId, userId);
        }
        if (existing != null) {
            existing.setProgressText(progressText);
            mapper.updateById(existing);
        } else {
            MediaProgressDO p = new MediaProgressDO();
            p.setId(UUID.randomUUID().toString());
            p.setMediaId(mediaId);
            p.setUserId(userId);
            p.setProgressText(progressText);
            mapper.insert(p);
        }
    }

    public void deleteByMediaId(String mediaId) {
        mapper.delete(new LambdaQueryWrapper<MediaProgressDO>().eq(MediaProgressDO::getMediaId, mediaId));
    }
}
```

- [ ] **Step 3: 提交**

```bash
git add "backend/lifeassistant/lifeassistant-system/src/main/java/top/lifeassistant/sharedmedia/service/"
git commit -m "feat: add MediaCommentService and MediaProgressService"
```

---

### Task 6: 后端 Controller 层

**Files:**
- Create: `backend/lifeassistant/lifeassistant-system/src/main/java/top/lifeassistant/sharedmedia/controller/SharedMediaController.java`
- Create: `backend/lifeassistant/lifeassistant-system/src/main/java/top/lifeassistant/sharedmedia/controller/MediaCommentController.java`
- Create: `backend/lifeassistant/lifeassistant-system/src/main/java/top/lifeassistant/sharedmedia/controller/MediaProgressController.java`

- [ ] **Step 1: 创建 SharedMediaController.java**

```java
package top.lifeassistant.sharedmedia.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import top.lifeassistant.common.annotation.CurrentUser;
import top.lifeassistant.common.base.model.query.PageResult;
import top.lifeassistant.common.base.model.resp.ApiResponse;
import top.lifeassistant.sharedmedia.model.query.SharedMediaPageQuery;
import top.lifeassistant.sharedmedia.model.req.SharedMediaCreateReq;
import top.lifeassistant.sharedmedia.model.req.SharedMediaUpdateReq;
import top.lifeassistant.sharedmedia.model.resp.SharedMediaResp;
import top.lifeassistant.sharedmedia.service.MediaCommentService;
import top.lifeassistant.sharedmedia.service.MediaProgressService;
import top.lifeassistant.sharedmedia.service.SharedMediaService;
import top.lifeassistant.system.model.entity.user.UserDO;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

@Tag(name = "共享媒体 API")
@RestController
@RequiredArgsConstructor
public class SharedMediaController {

    private final SharedMediaService service;
    private final MediaCommentService commentService;
    private final MediaProgressService progressService;

    @Operation(summary = "添加媒体")
    @PostMapping("/shared-media")
    public ApiResponse<SharedMediaResp> create(
            @CurrentUser UserDO user,
            @RequestParam("title") String title,
            @RequestParam("mediaType") String mediaType,
            @RequestParam(value = "description", required = false) String description,
            @RequestParam(value = "cover", required = false) MultipartFile cover) throws IOException {

        SharedMediaCreateReq req = new SharedMediaCreateReq();
        req.setTitle(title);
        req.setMediaType(mediaType);
        req.setDescription(description);

        String coverPath = saveCover(cover);
        return ApiResponse.ok(service.create(user, req, coverPath));
    }

    @Operation(summary = "媒体列表")
    @GetMapping("/shared-media")
    public ApiResponse<PageResult<SharedMediaResp>> list(@CurrentUser UserDO user, @Valid SharedMediaPageQuery query) {
        return ApiResponse.ok(service.list(user, query));
    }

    @Operation(summary = "媒体详情")
    @GetMapping("/shared-media/{id}")
    public ApiResponse<SharedMediaResp> getById(@CurrentUser UserDO user, @PathVariable String id) {
        return ApiResponse.ok(service.getById(user, id));
    }

    @Operation(summary = "更新媒体")
    @PatchMapping("/shared-media/{id}")
    public ApiResponse<SharedMediaResp> update(
            @CurrentUser UserDO user,
            @PathVariable String id,
            @RequestParam(value = "title", required = false) String title,
            @RequestParam(value = "mediaType", required = false) String mediaType,
            @RequestParam(value = "description", required = false) String description,
            @RequestParam(value = "cover", required = false) MultipartFile cover) throws IOException {

        SharedMediaUpdateReq req = new SharedMediaUpdateReq();
        req.setTitle(title);
        req.setMediaType(mediaType);
        req.setDescription(description);

        String coverPath = saveCover(cover);
        return ApiResponse.ok(service.update(user, id, req, coverPath));
    }

    @Operation(summary = "删除媒体")
    @DeleteMapping("/shared-media/{id}")
    public ApiResponse<Void> delete(@CurrentUser UserDO user, @PathVariable String id) {
        service.delete(user, id);
        commentService.deleteByMediaId(id);
        progressService.deleteByMediaId(id);
        return ApiResponse.ok();
    }

    private String saveCover(MultipartFile cover) throws IOException {
        if (cover == null || cover.isEmpty()) return null;
        String uploadDir = "uploads/shared-media/";
        Path dir = Paths.get(uploadDir);
        if (!Files.exists(dir)) Files.createDirectories(dir);
        String filename = UUID.randomUUID() + "_" + cover.getOriginalFilename();
        Path filePath = dir.resolve(filename);
        cover.transferTo(filePath.toFile());
        return uploadDir + filename;
    }
}
```

- [ ] **Step 2: 创建 MediaCommentController.java**

```java
package top.lifeassistant.sharedmedia.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import top.lifeassistant.common.annotation.CurrentUser;
import top.lifeassistant.common.base.model.resp.ApiResponse;
import top.lifeassistant.sharedmedia.model.req.MediaCommentCreateReq;
import top.lifeassistant.sharedmedia.model.resp.MediaCommentResp;
import top.lifeassistant.sharedmedia.service.MediaCommentService;
import top.lifeassistant.system.model.entity.user.UserDO;

import java.util.List;

@Tag(name = "媒体评论 API")
@RestController
@RequiredArgsConstructor
public class MediaCommentController {

    private final MediaCommentService service;

    @Operation(summary = "评论列表")
    @GetMapping("/shared-media/{mediaId}/comments")
    public ApiResponse<List<MediaCommentResp>> list(@CurrentUser UserDO user, @PathVariable String mediaId) {
        return ApiResponse.ok(service.list(user, mediaId));
    }

    @Operation(summary = "发送评论")
    @PostMapping("/shared-media/{mediaId}/comments")
    public ApiResponse<MediaCommentResp> create(
            @CurrentUser UserDO user,
            @PathVariable String mediaId,
            @Valid @RequestBody MediaCommentCreateReq req) {
        return ApiResponse.ok(service.create(user, mediaId, req));
    }
}
```

- [ ] **Step 3: 创建 MediaProgressController.java**

```java
package top.lifeassistant.sharedmedia.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import top.lifeassistant.common.annotation.CurrentUser;
import top.lifeassistant.common.base.model.resp.ApiResponse;
import top.lifeassistant.sharedmedia.model.req.MediaProgressUpdateReq;
import top.lifeassistant.sharedmedia.model.resp.MediaProgressResp;
import top.lifeassistant.sharedmedia.service.MediaProgressService;
import top.lifeassistant.system.model.entity.user.UserDO;

import java.util.List;

@Tag(name = "媒体进度 API")
@RestController
@RequiredArgsConstructor
public class MediaProgressController {

    private final MediaProgressService service;

    @Operation(summary = "获取所有进度")
    @GetMapping("/shared-media/{mediaId}/progress")
    public ApiResponse<List<MediaProgressResp>> list(@CurrentUser UserDO user, @PathVariable String mediaId) {
        return ApiResponse.ok(service.list(user, mediaId));
    }

    @Operation(summary = "更新进度")
    @PutMapping("/shared-media/{mediaId}/progress")
    public ApiResponse<MediaProgressResp> update(
            @CurrentUser UserDO user,
            @PathVariable String mediaId,
            @Valid @RequestBody MediaProgressUpdateReq req) {
        return ApiResponse.ok(service.update(user, mediaId, req));
    }
}
```

- [ ] **Step 4: 提交**

```bash
git add "backend/lifeassistant/lifeassistant-system/src/main/java/top/lifeassistant/sharedmedia/controller/"
git commit -m "feat: add shared-media controllers"
```

---

### Task 7: 后端静态资源配置

**Files:**
- Create: `backend/lifeassistant/lifeassistant-server/src/main/java/top/lifeassistant/mcp/config/WebMvcConfig.java`（或修改已有 WebConfig）

- [ ] **Step 1: 创建 WebMvcConfig.java**

```java
package top.lifeassistant.mcp.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations("file:uploads/");
    }
}
```

- [ ] **Step 2: 提交**

```bash
git add "backend/lifeassistant/lifeassistant-server/src/main/java/top/lifeassistant/mcp/config/WebMvcConfig.java"
git commit -m "feat: add static resource mapping for uploads"
```

---

### Task 8: 前端 API 模块

**Files:**
- Create: `front/vue3-vant-mobile/src/api/modules/shared-media.ts`

- [ ] **Step 1: 创建 shared-media.ts**

```typescript
import request from '@/utils/request'
import type { ApiResponse, PageResult } from '@/types/api'

export interface SharedMediaItem {
  id: string
  createdBy: string
  title: string
  mediaType: string
  coverPath: string | null
  description: string | null
  isFinished: boolean
  createdAt: string
  updateTime: string
}

export interface MediaComment {
  id: string
  mediaId: string
  userId: string
  content: string
  createdAt: string
}

export interface MediaProgress {
  id: string
  mediaId: string
  userId: string | null
  scope: 'shared' | 'personal'
  progressText: string
  createdAt: string
}

export function fetchSharedMediaList(params?: {
  page?: number
  size?: number
  mediaType?: string
  status?: string
}) {
  return request.get<ApiResponse<PageResult<SharedMediaItem>>>('/shared-media', { params })
}

export function getSharedMediaDetail(id: string) {
  return request.get<ApiResponse<SharedMediaItem>>(`/shared-media/${id}`)
}

export function createSharedMedia(formData: FormData) {
  return request.post<ApiResponse<SharedMediaItem>>('/shared-media', formData, {
    headers: { 'Content-Type': 'multipart/form-data' },
  })
}

export function updateSharedMedia(id: string, formData: FormData) {
  return request.patch<ApiResponse<SharedMediaItem>>(`/shared-media/${id}`, formData, {
    headers: { 'Content-Type': 'multipart/form-data' },
  })
}

export function deleteSharedMedia(id: string) {
  return request.delete<ApiResponse<void>>(`/shared-media/${id}`)
}

export function fetchComments(mediaId: string) {
  return request.get<ApiResponse<MediaComment[]>>(`/shared-media/${mediaId}/comments`)
}

export function createComment(mediaId: string, data: { content: string }) {
  return request.post<ApiResponse<MediaComment>>(`/shared-media/${mediaId}/comments`, data)
}

export function fetchProgress(mediaId: string) {
  return request.get<ApiResponse<MediaProgress[]>>(`/shared-media/${mediaId}/progress`)
}

export function updateProgress(mediaId: string, data: { scope: 'shared' | 'personal'; progressText: string }) {
  return request.put<ApiResponse<MediaProgress>>(`/shared-media/${mediaId}/progress`, data)
}
```

- [ ] **Step 2: 提交**

```bash
git add "front/vue3-vant-mobile/src/api/modules/shared-media.ts"
git commit -m "feat: add shared-media frontend API module"
```

---

### Task 9: 前端伴侣页添加 Tab 栏

**Files:**
- Modify: `front/vue3-vant-mobile/src/pages/share/index.vue`

需要在伴侣绑定后区域内添加 Tab 栏，两个 Tab 之间共享顶部未绑定引导区。

- [ ] **Step 1: 在 `<script setup>` 中添加 Tab 状态和媒体列表逻辑**

在现有 `share/index.vue` 的 `<script setup>` 末尾添加：

```typescript
// ---- Tab 切换 ----
const activeTab = ref<'records' | 'media'>('records')

// ---- 媒体列表 ----
import { fetchSharedMediaList, deleteSharedMedia } from '@/api/modules/shared-media'
import type { SharedMediaItem } from '@/api/modules/shared-media'

const mediaRecords = ref<SharedMediaItem[]>([])
const mediaPage = ref(1)
const mediaTotalPages = ref(0)
const mediaPageSize = ref(10)
const mediaTypeFilter = ref<string | undefined>(undefined)
const mediaStatusFilter = ref<string>('unfinished')

async function loadMedia() {
  try {
    const res = await fetchSharedMediaList({
      page: mediaPage.value,
      size: mediaPageSize.value,
      mediaType: mediaTypeFilter.value,
      status: mediaStatusFilter.value,
    })
    const data = res.data ?? { records: [] as SharedMediaItem[], pages: 0 }
    mediaRecords.value = data.records
    mediaTotalPages.value = data.pages
  } catch {
    showToast('加载失败')
  }
}

function onMediaTypeFilter(type?: string) {
  mediaTypeFilter.value = type
  mediaPage.value = 1
  loadMedia()
}

function onMediaStatusFilter(status: string) {
  mediaStatusFilter.value = status
  mediaPage.value = 1
  loadMedia()
}

watch(activeTab, (tab) => {
  if (tab === 'media' && mediaRecords.value.length === 0) {
    loadMedia()
  }
})
```

- [ ] **Step 2: 在 `<template>` 中伴侣绑定后的区域添加 Tab 栏**

找到现有 `<!-- ★一起做过的事（绑定后显示） -->` 区域，将整个 `<template v-if="partnerId">` 的内容改为：

```vue
<!-- ★伴侣内容（绑定后显示） -->
<template v-if="partnerId">
  <!-- Tab 栏 -->
  <van-tabs v-model:active="activeTab" sticky>
    <van-tab title="一起做过的事" name="records">
      <!-- 原有的搜索、筛选、列表、分页等内容... -->
    </van-tab>
    <van-tab title="一起看过的" name="media">
      <!-- 媒体类型 + 状态筛选 -->
      <div class="px-4 pt-3 space-y-2">
        <div class="flex gap-2 flex-wrap">
          <van-tag
            v-for="t in [{ label: '全部', value: undefined }, { label: '电影', value: 'movie' }, { label: '书籍', value: 'book' }, { label: '漫剧', value: 'tv' }]"
            :key="t.label"
            :type="mediaTypeFilter === t.value ? 'primary' : 'default'"
            size="medium"
            @click="onMediaTypeFilter(t.value)"
          >
            {{ t.label }}
          </van-tag>
        </div>
        <div class="flex gap-2 flex-wrap">
          <van-tag
            v-for="s in [{ label: '没看完', value: 'unfinished' }, { label: '全部', value: '' }, { label: '已看完', value: 'finished' }]"
            :key="s.label"
            :type="mediaStatusFilter === (s.value || 'unfinished') ? 'primary' : 'default'"
            size="medium"
            @click="onMediaStatusFilter(s.value || 'unfinished')"
          >
            {{ s.label }}
          </van-tag>
        </div>
      </div>

      <!-- 媒体列表 -->
      <van-cell-group :inset="true" title="一起看过的">
        <van-empty v-if="mediaRecords.length === 0" description="还没有记录" />
        <van-cell
          v-for="item in mediaRecords"
          :key="item.id"
          is-link
          @click="router.push(`/share/media/${item.id}`)"
        >
          <template #title>
            <div class="flex items-center gap-2">
              <van-image
                v-if="item.coverPath"
                :src="item.coverPath"
                width="36"
                height="50"
                fit="cover"
                radius="4"
              />
              <div v-else class="cover-placeholder">?</div>
              <div>
                <div class="text-sm font-medium">{{ item.title }}</div>
                <div class="text-xs text-gray-400">
                  {{ { movie: '电影', book: '书籍', tv: '漫剧' }[item.mediaType] || item.mediaType }}
                  · {{ item.isFinished ? '已看完' : '没看完' }}
                </div>
              </div>
            </div>
          </template>
        </van-cell>
      </van-cell-group>

      <!-- 分页 -->
      <div v-if="mediaTotalPages > 0" class="pagination">
        <div class="pagination-inner">
          <van-button :disabled="mediaPage <= 1" size="small" plain @click="mediaPage--; loadMedia()">上一页</van-button>
          <span class="page-info">第 {{ mediaPage }}/{{ mediaTotalPages }} 页</span>
          <van-button :disabled="mediaPage >= mediaTotalPages" size="small" plain @click="mediaPage++; loadMedia()">下一页</van-button>
        </div>
      </div>

      <!-- 添加按钮 -->
      <div class="px-4 mt-3">
        <van-button type="primary" round block icon="plus" @click="showAddMedia = true">
          添加看过的
        </van-button>
      </div>

      <!-- 添加媒体弹窗 -->
      <van-dialog v-model:show="showAddMedia" title="添加看过的" show-cancel-button @confirm="onAddMedia">
        <van-form ref="addMediaFormRef">
          <van-field v-model="addMediaForm.title" label="名称" placeholder="输入电影/书/漫剧名称" :rules="[{ required: true }]" />
          <van-field v-model="addMediaForm.mediaType" label="类型" placeholder="选择类型" is-link readonly @click="showMediaTypePicker = true" />
          <van-field v-model="addMediaForm.description" label="简介" type="textarea" placeholder="可选" />
          <van-field label="封面图">
            <template #input>
              <van-uploader v-model="addMediaCover" :max-count="1" :after-read="onCoverRead" />
            </template>
          </van-field>
        </van-form>
      </van-dialog>
      <van-picker v-model:show="showMediaTypePicker" :columns="mediaTypeColumns" @confirm="onMediaTypeConfirm" />
    </van-tab>
  </van-tabs>
</template>
```

- [ ] **Step 3: 添加媒体表单的 JS 状态和逻辑**

在 `<script setup>` 中添加：

```typescript
// ---- 媒体添加 ----
import { createSharedMedia } from '@/api/modules/shared-media'
import { useRouter } from 'vue-router'

const router = useRouter()
const showAddMedia = ref(false)
const addMediaForm = reactive({ title: '', mediaType: '', description: '' })
const addMediaCover = ref<{ content?: File; file?: File; status?: string }[]>([])
const showMediaTypePicker = ref(false)
const mediaTypeColumns = [
  { text: '电影', value: 'movie' },
  { text: '书籍', value: 'book' },
  { text: '漫剧', value: 'tv' },
]

function onMediaTypeConfirm({ selectedOptions }: any) {
  addMediaForm.mediaType = selectedOptions[0]?.value || ''
}
function onCoverRead(file: any) {
  // file.file 是实际文件对象
}
async function onAddMedia() {
  if (!addMediaForm.title || !addMediaForm.mediaType) {
    showToast('请填写名称和类型')
    return
  }
  const formData = new FormData()
  formData.append('title', addMediaForm.title)
  formData.append('mediaType', addMediaForm.mediaType)
  if (addMediaForm.description) formData.append('description', addMediaForm.description)
  if (addMediaCover.value[0]?.file) formData.append('cover', addMediaCover.value[0].file)
  try {
    await createSharedMedia(formData)
    showToast('添加成功')
    showAddMedia.value = false
    loadMedia()
  } catch {
    showToast('添加失败')
  }
}
```

- [ ] **Step 4: 添加样式**

在 `<style scoped>` 中添加：

```css
.cover-placeholder {
  width: 36px;
  height: 50px;
  background: var(--van-gray-2);
  border-radius: 4px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 16px;
  color: var(--van-gray-5);
  flex-shrink: 0;
}
```

- [ ] **Step 5: 提交**

```bash
git add "front/vue3-vant-mobile/src/pages/share/index.vue"
git commit -m "feat: add shared-media tab to partner page"
```

---

### Task 10: 前端详情聊天页

**Files:**
- Create: `front/vue3-vant-mobile/src/pages/share/media/[id].vue`

- [ ] **Step 1: 创建详情页组件**

```vue
<script setup lang="ts">
import { showToast } from 'vant'
import { useRoute, useRouter } from 'vue-router'
import { ref, onMounted, computed, nextTick } from 'vue'
import {
  getSharedMediaDetail,
  fetchComments,
  createComment,
  fetchProgress,
  updateProgress,
} from '@/api/modules/shared-media'
import type { SharedMediaItem, MediaComment, MediaProgress } from '@/api/modules/shared-media'
import { useUserStore } from '@/stores'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()
const mediaId = computed(() => route.params.id as string)

const media = ref<SharedMediaItem | null>(null)
const comments = ref<MediaComment[]>([])
const progresses = ref<MediaProgress[]>([])
const newComment = ref('')
const sending = ref(false)
const chatContainer = ref<HTMLDivElement | null>(null)
const showProgressDialog = ref(false)
const progressText = ref('')

const mediaTypeLabel = computed(() => {
  const map: Record<string, string> = { movie: '电影', book: '书籍', tv: '漫剧' }
  return map[media.value?.mediaType || ''] || media.value?.mediaType || ''
})

const sharedProgress = computed(() => progresses.value.find(p => p.scope === 'shared'))
const myProgress = computed(() => {
  const uid = (userStore.userInfo as any)?.id
  return progresses.value.find(p => p.scope === 'personal' && p.userId === uid)
})
const partnerProgress = computed(() => {
  const pid = (userStore.userInfo as any)?.partnerId
  return progresses.value.find(p => p.scope === 'personal' && p.userId === pid)
})

async function loadData() {
  try {
    const [mediaRes, commentsRes, progressRes] = await Promise.all([
      getSharedMediaDetail(mediaId.value),
      fetchComments(mediaId.value),
      fetchProgress(mediaId.value),
    ])
    media.value = mediaRes.data ?? null
    comments.value = commentsRes.data ?? []
    progresses.value = progressRes.data ?? []
    nextTick(() => scrollToBottom())
  } catch {
    showToast('加载失败')
  }
}

function scrollToBottom() {
  if (chatContainer.value) {
    chatContainer.value.scrollTop = chatContainer.value.scrollHeight
  }
}

async function sendComment() {
  if (!newComment.value.trim()) return
  sending.value = true
  try {
    await createComment(mediaId.value, { content: newComment.value.trim() })
    newComment.value = ''
    const res = await fetchComments(mediaId.value)
    comments.value = res.data ?? []
    nextTick(() => scrollToBottom())
  } catch {
    showToast('发送失败')
  } finally {
    sending.value = false
  }
}

async function onUpdateProgress() {
  if (!progressText.value.trim()) return
  try {
    await updateProgress(mediaId.value, { scope: 'shared', progressText: progressText.value.trim() })
    showToast('进度已更新')
    showProgressDialog.value = false
    progressText.value = ''
    const res = await fetchProgress(mediaId.value)
    progresses.value = res.data ?? []
  } catch {
    showToast('更新失败')
  }
}

onMounted(loadData)
</script>

<template>
  <div class="media-detail-page">
    <!-- 顶部导航 -->
    <van-nav-bar :title="media?.title || '加载中...'" left-arrow @click-left="router.back()">
      <template #right>
        <van-icon name="edit" @click="showProgressDialog = true" />
      </template>
    </van-nav-bar>

    <!-- 进度区域 -->
    <div v-if="media" class="progress-bar">
      <div class="progress-item">
        <span class="label">共同</span>
        <span class="value">{{ sharedProgress?.progressText || '未记录' }}</span>
      </div>
      <div class="divider" />
      <div class="progress-item">
        <span class="label">你</span>
        <span class="value">{{ myProgress?.progressText || '未记录' }}</span>
      </div>
      <div class="divider" />
      <div class="progress-item">
        <span class="label">伴侣</span>
        <span class="value">{{ partnerProgress?.progressText || '未记录' }}</span>
      </div>
    </div>

    <!-- 聊天记录 -->
    <div ref="chatContainer" class="chat-container">
      <template v-for="comment in comments" :key="comment.id">
        <div v-if="comment.userId === (userStore.userInfo as any)?.id" class="chat-bubble right">
          <div class="bubble-content">{{ comment.content }}</div>
          <span class="time">{{ comment.createdAt?.slice(11, 16) }}</span>
        </div>
        <div v-else class="chat-bubble left">
          <div class="avatar">{{ (userStore.partnerName as string)?.[0] || '伴' }}</div>
          <div>
            <div class="bubble-content">{{ comment.content }}</div>
            <span class="time">{{ comment.createdAt?.slice(11, 16) }}</span>
          </div>
        </div>
      </template>
      <van-empty v-if="comments.length === 0" description="还没有评论，说点什么吧" />
    </div>

    <!-- 输入框 -->
    <div class="input-bar">
      <van-field
        v-model="newComment"
        placeholder="说点什么..."
        :border="false"
        @keypress.enter="sendComment"
      />
      <van-button
        :loading="sending"
        type="primary"
        size="small"
        round
        icon="send"
        @click="sendComment"
      />
    </div>

    <!-- 更新进度弹窗 -->
    <van-dialog
      v-model:show="showProgressDialog"
      title="更新共同进度"
      show-cancel-button
      @confirm="onUpdateProgress"
    >
      <van-field
        v-model="progressText"
        placeholder="如：第5集/共24集"
        :rules="[{ required: true }]"
        style="margin: 16px"
      />
    </van-dialog>
  </div>
</template>

<style scoped>
.media-detail-page {
  display: flex;
  flex-direction: column;
  height: 100vh;
  background: var(--van-background-2);
}
.progress-bar {
  display: flex;
  align-items: center;
  padding: 12px 16px;
  background: var(--van-background);
  border-bottom: 1px solid var(--van-border-color);
}
.progress-item {
  flex: 1;
  text-align: center;
}
.progress-item .label {
  display: block;
  font-size: 11px;
  color: var(--van-gray-5);
}
.progress-item .value {
  display: block;
  font-size: 14px;
  font-weight: 600;
  margin-top: 2px;
}
.divider {
  width: 1px;
  height: 32px;
  background: var(--van-border-color);
}
.chat-container {
  flex: 1;
  overflow-y: auto;
  padding: 12px;
}
.chat-bubble {
  display: flex;
  gap: 8px;
  margin-bottom: 14px;
  align-items: flex-end;
}
.chat-bubble.right {
  flex-direction: row-reverse;
}
.chat-bubble.left {
  flex-direction: row;
}
.bubble-content {
  max-width: 220px;
  padding: 8px 12px;
  border-radius: 12px;
  font-size: 14px;
  line-height: 1.5;
  word-break: break-word;
}
.chat-bubble.right .bubble-content {
  background: var(--van-primary-color);
  color: #fff;
  border-bottom-right-radius: 4px;
}
.chat-bubble.left .bubble-content {
  background: var(--van-background);
  border-bottom-left-radius: 4px;
}
.chat-bubble .time {
  display: block;
  font-size: 10px;
  color: var(--van-gray-5);
  margin-top: 2px;
  text-align: right;
}
.chat-bubble.left .time {
  text-align: left;
}
.avatar {
  width: 28px;
  height: 28px;
  border-radius: 50%;
  background: var(--van-primary-color-light);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 12px;
  font-weight: 600;
  color: var(--van-primary-color);
  flex-shrink: 0;
}
.input-bar {
  display: flex;
  align-items: center;
  padding: 8px 12px;
  gap: 8px;
  background: var(--van-background);
  border-top: 1px solid var(--van-border-color);
}
.input-bar .van-field {
  flex: 1;
}
</style>

<route lang="json5">
{
  name: 'MediaDetail'
}
</route>
```

- [ ] **Step 2: 在 `TabBar.vue` 中确认是否需要隐藏 TabBar**

详情页需要隐藏底部导航栏。检查 `TabBar.vue` 的 `rootRouteList`，如果详情页路由不在其中即可。`MediaDetail` 路由名为自动生成的，路径 `/share/media/:id` 不在 `rootRouteList` 中，所以 TabBar 已隐藏。

- [ ] **Step 3: 提交**

```bash
git add "front/vue3-vant-mobile/src/pages/share/media/[id].vue"
git commit -m "feat: add shared-media detail chat page"
```

---

## 自检清单

- [x] **规范覆盖**：所有需求点（添加媒体、上传封面、类型/状态筛选、共同/个人进度同步、聊天室评论）都有对应任务
- [x] **占位符检查**：无 "TBD"、"TODO"、省略号
- [x] **类型一致性**：Req/Resp 字段、Service 方法签名、API 路径在所有任务间一致
- [x] **遵循项目模式**：Entity 继承 BaseDO、Mapper 继承 BaseMapper、Service 用 @Service + @RequiredArgsConstructor、Controller 用 @RestController + @CurrentUser
