package top.lifeassistant.partner.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import top.lifeassistant.common.annotation.CurrentUser;
import top.lifeassistant.common.base.model.resp.ApiResponse;
import top.lifeassistant.partner.model.resp.PartnerPointsHistoryResp;
import top.lifeassistant.partner.model.resp.PointsDateApproveAllResult;
import top.lifeassistant.partner.model.resp.PointsDateChangeResult;
import top.lifeassistant.partner.service.PartnerPointsService;
import top.lifeassistant.system.model.entity.user.UserDO;

import java.time.LocalDate;

@Tag(name = "伴侣积分 API")
@RestController
@RequiredArgsConstructor
public class PartnerPointsController {
    private final PartnerPointsService service;

    @Operation(summary = "查询积分余额")
    @GetMapping("/partner/points")
    public ApiResponse<Integer> getBalance(@CurrentUser UserDO user) {
        return ApiResponse.ok(service.getBalance(user.getId()));
    }

    @Operation(summary = "查询积分变动历史")
    @GetMapping("/partner/points/history")
    public ApiResponse<PartnerPointsHistoryResp> getHistory(
            @CurrentUser UserDO user,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.ok(service.getHistoryResp(user.getId(), page, size));
    }

    @Operation(summary = "记录积分变动")
    @PostMapping("/partner/points")
    public ApiResponse<Void> addPoints(@CurrentUser UserDO user, @Valid @RequestBody PointsChangeRequest req) {
        service.addPoints(user.getId(), req.getPointsChange(), req.getReason(), req.getRecordDate());
        return ApiResponse.ok();
    }

    @Operation(summary = "修改积分流水记录日期")
    @PatchMapping("/partner/points/{id}")
    public ApiResponse<PointsDateChangeResult> updateRecordDate(
            @CurrentUser UserDO user,
            @PathVariable String id,
            @Valid @RequestBody PointsRecordDateRequest req) {
        return ApiResponse.ok(service.updateRecordDate(user.getId(), id, req.getRecordDate()));
    }

    @Operation(summary = "确认伴侣发起的积分日期变更")
    @PostMapping("/partner/points/{id}/date-change/approve")
    public ApiResponse<Void> approveDateChange(@CurrentUser UserDO user, @PathVariable String id) {
        service.approveDateChange(user.getId(), id);
        return ApiResponse.ok();
    }

    @Operation(summary = "拒绝伴侣发起的积分日期变更")
    @PostMapping("/partner/points/{id}/date-change/reject")
    public ApiResponse<Void> rejectDateChange(@CurrentUser UserDO user, @PathVariable String id) {
        service.rejectDateChange(user.getId(), id);
        return ApiResponse.ok();
    }

    @Operation(summary = "批量确认伴侣发起的积分日期变更")
    @PostMapping("/partner/points/date-change/approve-all")
    public ApiResponse<PointsDateApproveAllResult> approveAllDateChanges(@CurrentUser UserDO user) {
        PointsDateApproveAllResult r = new PointsDateApproveAllResult();
        r.setApprovedCount(service.approveAllDateChanges(user.getId()));
        return ApiResponse.ok(r);
    }

    @Data
    public static class PointsChangeRequest {
        @NotNull
        private Integer pointsChange;
        @NotBlank
        private String reason;
        private LocalDate recordDate;
    }

    @Data
    public static class PointsRecordDateRequest {
        @NotNull
        private LocalDate recordDate;
    }
}
