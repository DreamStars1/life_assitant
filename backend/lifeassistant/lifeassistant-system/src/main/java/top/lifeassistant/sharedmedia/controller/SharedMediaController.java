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
import top.lifeassistant.common.component.UploadStorage;
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
import java.util.UUID;

@Tag(name = "共享媒体 API")
@RestController
@RequiredArgsConstructor
public class SharedMediaController {

    private final SharedMediaService service;
    private final MediaCommentService commentService;
    private final MediaProgressService progressService;
    private final UploadStorage uploadStorage;

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
            @RequestParam(value = "cover", required = false) MultipartFile cover,
            @RequestParam(value = "isFinished", required = false) Boolean isFinished) throws IOException {

        SharedMediaUpdateReq req = new SharedMediaUpdateReq();
        req.setTitle(title);
        req.setMediaType(mediaType);
        req.setDescription(description);

        String coverPath = saveCover(cover);
        return ApiResponse.ok(service.update(user, id, req, coverPath, isFinished));
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
        Path dir = uploadStorage.sharedMediaDir();
        Files.createDirectories(dir);
        String filename = UUID.randomUUID() + "_" + cover.getOriginalFilename();
        Path filePath = dir.resolve(filename);
        cover.transferTo(filePath.toFile());
        return uploadStorage.sharedMediaUrl(filename);
    }
}
