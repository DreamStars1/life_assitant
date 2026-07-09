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
