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
