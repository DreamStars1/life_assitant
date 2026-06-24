package top.lifeassistant.todo.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import top.lifeassistant.common.annotation.CurrentUser;
import top.lifeassistant.common.base.model.resp.ApiResponse;
import top.lifeassistant.system.model.entity.user.UserDO;
import top.lifeassistant.todo.model.req.TodoAckTemplateCreateReq;
import top.lifeassistant.todo.model.req.TodoAckTemplateReorderReq;
import top.lifeassistant.todo.model.req.TodoAckTemplateUpdateReq;
import top.lifeassistant.todo.model.resp.TodoAckTemplateResp;
import top.lifeassistant.todo.service.TodoAckTemplateService;

import java.util.List;

/**
 * 确认文案模板 API
 *
 * @author DreamStars1
 * @since 2026/6/24
 */
@Tag(name = "确认文案模板 API")
@RestController
@RequiredArgsConstructor
public class TodoAckTemplateController {

    private final TodoAckTemplateService service;

    @Operation(summary = "获取模板列表")
    @GetMapping("/ack-templates")
    public ApiResponse<List<TodoAckTemplateResp>> list(@CurrentUser UserDO user) {
        return ApiResponse.ok(service.listByUser(user));
    }

    @Operation(summary = "添加模板")
    @PostMapping("/ack-templates")
    public ApiResponse<TodoAckTemplateResp> create(@CurrentUser UserDO user,
                                                    @Valid @RequestBody TodoAckTemplateCreateReq req) {
        return ApiResponse.ok(service.create(user, req.getContent()));
    }

    @Operation(summary = "编辑模板")
    @PutMapping("/ack-templates/{id}")
    public ApiResponse<TodoAckTemplateResp> update(@CurrentUser UserDO user, @PathVariable String id,
                                                    @Valid @RequestBody TodoAckTemplateUpdateReq req) {
        return ApiResponse.ok(service.update(user, id, req.getContent()));
    }

    @Operation(summary = "删除模板")
    @DeleteMapping("/ack-templates/{id}")
    public ApiResponse<Void> delete(@CurrentUser UserDO user, @PathVariable String id) {
        service.delete(user, id);
        return ApiResponse.ok();
    }

    @Operation(summary = "排序模板")
    @PutMapping("/ack-templates/reorder")
    public ApiResponse<Void> reorder(@CurrentUser UserDO user,
                                      @Valid @RequestBody TodoAckTemplateReorderReq req) {
        service.reorder(user, req.getIds());
        return ApiResponse.ok();
    }
}
