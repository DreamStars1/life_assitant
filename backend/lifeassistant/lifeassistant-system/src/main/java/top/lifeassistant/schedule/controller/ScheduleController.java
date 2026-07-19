package top.lifeassistant.schedule.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import top.lifeassistant.common.annotation.CurrentUser;
import top.lifeassistant.common.base.model.resp.ApiResponse;
import top.lifeassistant.schedule.model.req.ScheduleEventCreateReq;
import top.lifeassistant.schedule.model.req.ScheduleEventUpdateReq;
import top.lifeassistant.schedule.model.req.ScheduleInviteAckReq;
import top.lifeassistant.schedule.model.resp.ScheduleEventResp;
import top.lifeassistant.schedule.service.ScheduleService;
import top.lifeassistant.system.model.entity.user.UserDO;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 日程 API
 */
@Tag(name = "日程 API")
@RestController
@RequiredArgsConstructor
public class ScheduleController {

    private final ScheduleService service;

    @Operation(summary = "我的日程列表")
    @GetMapping("/schedule/events")
    public ApiResponse<List<ScheduleEventResp>> listMine(
        @CurrentUser UserDO user,
        @RequestParam LocalDateTime from,
        @RequestParam LocalDateTime to
    ) {
        return ApiResponse.ok(service.listMine(user, from, to));
    }

    @Operation(summary = "伴侣日程列表")
    @GetMapping("/schedule/events/partner")
    public ApiResponse<List<ScheduleEventResp>> listPartner(
        @CurrentUser UserDO user,
        @RequestParam LocalDateTime from,
        @RequestParam LocalDateTime to
    ) {
        return ApiResponse.ok(service.listPartner(user, from, to));
    }

    @Operation(summary = "创建日程")
    @PostMapping("/schedule/events")
    public ApiResponse<ScheduleEventResp> create(
        @CurrentUser UserDO user,
        @Valid @RequestBody ScheduleEventCreateReq req
    ) {
        return ApiResponse.ok(service.create(user, req));
    }

    @Operation(summary = "更新日程")
    @PatchMapping("/schedule/events/{id}")
    public ApiResponse<ScheduleEventResp> update(
        @CurrentUser UserDO user,
        @PathVariable String id,
        @Valid @RequestBody ScheduleEventUpdateReq req
    ) {
        return ApiResponse.ok(service.update(user, id, req));
    }

    @Operation(summary = "删除日程")
    @DeleteMapping("/schedule/events/{id}")
    public ApiResponse<Void> delete(@CurrentUser UserDO user, @PathVariable String id) {
        service.delete(user, id);
        return ApiResponse.ok();
    }

    @Operation(summary = "发起共同邀约")
    @PostMapping("/schedule/events/{id}/invite")
    public ApiResponse<Void> invite(@CurrentUser UserDO user, @PathVariable String id) {
        service.invite(user, id);
        return ApiResponse.ok();
    }

    @Operation(summary = "确认邀约")
    @PostMapping("/schedule/invites/{id}/acknowledge")
    public ApiResponse<ScheduleEventResp> acknowledge(
        @CurrentUser UserDO user,
        @PathVariable String id,
        @Valid @RequestBody ScheduleInviteAckReq req
    ) {
        return ApiResponse.ok(service.acknowledge(user, id, req.getAction()));
    }
}
