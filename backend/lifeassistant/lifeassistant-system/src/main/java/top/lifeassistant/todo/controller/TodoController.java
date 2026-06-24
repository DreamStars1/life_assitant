package top.lifeassistant.todo.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import top.lifeassistant.common.annotation.CurrentUser;
import top.lifeassistant.common.base.model.resp.ApiResponse;
import top.lifeassistant.system.model.entity.user.UserDO;
import top.lifeassistant.todo.model.req.TodoCreateReq;
import top.lifeassistant.todo.model.req.TodoUpdateReq;
import top.lifeassistant.todo.model.resp.TodoResp;
import top.lifeassistant.todo.service.TodoService;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 待办 API
 *
 * @author DreamStars1
 * @since 2026/6/24
 */
@Tag(name = "待办 API")
@RestController
@RequiredArgsConstructor
public class TodoController {

    private final TodoService service;

    @Operation(summary = "待办列表")
    @GetMapping("/todos")
    public ApiResponse<List<TodoResp>> list(@CurrentUser UserDO user,
                                             @RequestParam(required = false) Boolean isCompleted,
                                             @RequestParam(required = false) String priority,
                                             @RequestParam(required = false) LocalDateTime startDueDate,
                                             @RequestParam(required = false) LocalDateTime endDueDate) {
        return ApiResponse.ok(service.list(user, isCompleted, priority, startDueDate, endDueDate));
    }

    @Operation(summary = "首页最近待办")
    @GetMapping("/todos/upcoming")
    public ApiResponse<List<TodoResp>> getUpcoming(@CurrentUser UserDO user) {
        return ApiResponse.ok(service.getUpcoming(user));
    }

    @Operation(summary = "待办详情")
    @GetMapping("/todos/{id}")
    public ApiResponse<TodoResp> getById(@CurrentUser UserDO user, @PathVariable String id) {
        return ApiResponse.ok(service.getById(user, id));
    }

    @Operation(summary = "创建待办")
    @PostMapping("/todos")
    public ApiResponse<TodoResp> create(@CurrentUser UserDO user, @Valid @RequestBody TodoCreateReq req) {
        return ApiResponse.ok(service.create(user, req));
    }

    @Operation(summary = "更新待办")
    @PatchMapping("/todos/{id}")
    public ApiResponse<TodoResp> update(@CurrentUser UserDO user, @PathVariable String id,
                                         @Valid @RequestBody TodoUpdateReq req) {
        return ApiResponse.ok(service.update(user, id, req));
    }

    @Operation(summary = "删除待办")
    @DeleteMapping("/todos/{id}")
    public ApiResponse<Void> delete(@CurrentUser UserDO user, @PathVariable String id) {
        service.delete(user, id);
        return ApiResponse.ok();
    }

    @Operation(summary = "切换完成状态")
    @PostMapping("/todos/{id}/toggle")
    public ApiResponse<TodoResp> toggleComplete(@CurrentUser UserDO user, @PathVariable String id) {
        return ApiResponse.ok(service.toggleComplete(user, id));
    }

    @Operation(summary = "确认收到")
    @PostMapping("/todos/{id}/acknowledge")
    public ApiResponse<TodoResp> acknowledge(@CurrentUser UserDO user, @PathVariable String id,
                                              @RequestBody Map<String, String> body) {
        return ApiResponse.ok(service.acknowledge(user, id, body.get("message")));
    }
}
