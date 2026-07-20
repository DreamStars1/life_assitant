package top.lifeassistant.mediacategory.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import top.lifeassistant.common.annotation.CurrentUser;
import top.lifeassistant.common.base.model.resp.ApiResponse;
import top.lifeassistant.mediacategory.model.req.MediaCategoryCreateReq;
import top.lifeassistant.mediacategory.model.req.MediaCategoryUpdateReq;
import top.lifeassistant.mediacategory.model.resp.MediaCategoryResp;
import top.lifeassistant.mediacategory.service.MediaCategoryService;
import top.lifeassistant.system.model.entity.user.UserDO;

import java.util.List;

@Tag(name = "媒体分类 API")
@RestController
@RequiredArgsConstructor
public class MediaCategoryController {

    private final MediaCategoryService service;

    @Operation(summary = "分类列表")
    @GetMapping("/media-categories")
    public ApiResponse<List<MediaCategoryResp>> list(@CurrentUser UserDO user) {
        return ApiResponse.ok(service.list(user));
    }

    @Operation(summary = "添加分类")
    @PostMapping("/media-categories")
    public ApiResponse<MediaCategoryResp> create(@CurrentUser UserDO user,
                                                 @Valid @RequestBody MediaCategoryCreateReq req) {
        return ApiResponse.ok(service.create(user, req.getName()));
    }

    @Operation(summary = "重命名分类")
    @PatchMapping("/media-categories/{id}")
    public ApiResponse<MediaCategoryResp> update(@CurrentUser UserDO user,
                                                 @PathVariable String id,
                                                 @Valid @RequestBody MediaCategoryUpdateReq req) {
        return ApiResponse.ok(service.rename(user, id, req.getName()));
    }

    @Operation(summary = "删除分类")
    @DeleteMapping("/media-categories/{id}")
    public ApiResponse<Void> delete(@CurrentUser UserDO user, @PathVariable String id) {
        service.delete(user, id);
        return ApiResponse.ok();
    }
}
