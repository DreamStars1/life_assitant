package top.lifeassistant.sharedrecord.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import top.lifeassistant.common.annotation.CurrentUser;
import top.lifeassistant.common.base.model.resp.ApiResponse;
import top.lifeassistant.common.base.model.query.PageResult;
import top.lifeassistant.sharedrecord.model.query.SharedRecordPageQuery;
import top.lifeassistant.sharedrecord.model.req.SharedRecordCreateReq;
import top.lifeassistant.sharedrecord.model.req.SharedRecordUpdateReq;
import top.lifeassistant.sharedrecord.model.resp.SharedRecordResp;
import top.lifeassistant.sharedrecord.service.SharedRecordService;
import top.lifeassistant.system.model.entity.user.UserDO;

@Tag(name = "共享记录 API")
@RestController
@RequiredArgsConstructor
public class SharedRecordController {

    private final SharedRecordService service;

    @Operation(summary = "添加记录")
    @PostMapping("/shared-records")
    public ApiResponse<SharedRecordResp> create(@CurrentUser UserDO user, @Valid @RequestBody SharedRecordCreateReq req) {
        return ApiResponse.ok(service.create(user, req));
    }

    @Operation(summary = "记录列表")
    @GetMapping("/shared-records")
    public ApiResponse<PageResult<SharedRecordResp>> list(@CurrentUser UserDO user, @Valid SharedRecordPageQuery query) {
        return ApiResponse.ok(service.list(user, query));
    }

    @Operation(summary = "记录详情")
    @GetMapping("/shared-records/{id}")
    public ApiResponse<SharedRecordResp> getById(@CurrentUser UserDO user, @PathVariable String id) {
        return ApiResponse.ok(service.getById(user, id));
    }

    @Operation(summary = "更新记录")
    @PatchMapping("/shared-records/{id}")
    public ApiResponse<SharedRecordResp> update(@CurrentUser UserDO user, @PathVariable String id,
                                                 @Valid @RequestBody SharedRecordUpdateReq req) {
        return ApiResponse.ok(service.update(user, id, req));
    }

    @Operation(summary = "删除记录")
    @DeleteMapping("/shared-records/{id}")
    public ApiResponse<Void> delete(@CurrentUser UserDO user, @PathVariable String id) {
        service.delete(user, id);
        return ApiResponse.ok();
    }
}
