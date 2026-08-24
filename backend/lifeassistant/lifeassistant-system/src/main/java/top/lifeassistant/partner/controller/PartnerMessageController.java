package top.lifeassistant.partner.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import top.lifeassistant.common.annotation.CurrentUser;
import top.lifeassistant.common.base.model.resp.ApiResponse;
import top.lifeassistant.partner.model.req.PartnerMessageCreateReq;
import top.lifeassistant.partner.model.resp.PartnerMessageImagesUploadResp;
import top.lifeassistant.partner.model.resp.PartnerMessageResp;
import top.lifeassistant.partner.service.PartnerMessageService;
import top.lifeassistant.system.model.entity.user.UserDO;

import java.io.IOException;
import java.util.List;

@Tag(name = "伴侣留言板 API")
@RestController
@RequiredArgsConstructor
public class PartnerMessageController {

    private final PartnerMessageService service;

    @Operation(summary = "留言列表")
    @GetMapping("/partner/messages")
    public ApiResponse<List<PartnerMessageResp>> list(
            @CurrentUser UserDO user,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "50") int size) {
        return ApiResponse.ok(service.list(user, page, size));
    }

    @Operation(summary = "上传留言图片")
    @PostMapping("/partner/messages/images")
    public ApiResponse<PartnerMessageImagesUploadResp> upload(
            @CurrentUser UserDO user,
            @RequestParam("files") MultipartFile[] files) throws IOException {
        return ApiResponse.ok(service.uploadImages(user, files));
    }

    @Operation(summary = "发送留言")
    @PostMapping("/partner/messages")
    public ApiResponse<PartnerMessageResp> create(
            @CurrentUser UserDO user,
            @RequestBody PartnerMessageCreateReq req) {
        return ApiResponse.ok(service.create(user, req));
    }

    @Operation(summary = "删除留言")
    @DeleteMapping("/partner/messages/{id}")
    public ApiResponse<Void> delete(@CurrentUser UserDO user, @PathVariable String id) {
        service.delete(user, id);
        return ApiResponse.ok();
    }
}
