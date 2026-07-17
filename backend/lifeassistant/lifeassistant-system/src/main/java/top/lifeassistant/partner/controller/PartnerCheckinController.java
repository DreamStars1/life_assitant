package top.lifeassistant.partner.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import top.lifeassistant.common.annotation.CurrentUser;
import top.lifeassistant.common.base.model.resp.ApiResponse;
import top.lifeassistant.partner.model.entity.PartnerCheckinDO;
import top.lifeassistant.partner.service.PartnerCheckinService;
import top.lifeassistant.system.model.entity.user.UserDO;

import java.util.List;

@Tag(name = "伴侣作息打卡 API")
@RestController
@RequiredArgsConstructor
public class PartnerCheckinController {
    private final PartnerCheckinService service;

    @Operation(summary = "作息打卡")
    @PostMapping("/partner/checkin")
    public ApiResponse<Void> checkin(@CurrentUser UserDO user, @Valid @RequestBody CheckinRequest req) {
        service.checkin(user.getId(), req.getCheckinType());
        return ApiResponse.ok();
    }

    @Operation(summary = "查询今日打卡状态")
    @GetMapping("/partner/checkin/today")
    public ApiResponse<List<PartnerCheckinDO>> getToday(@CurrentUser UserDO user) {
        return ApiResponse.ok(service.getToday(user.getId()));
    }

    @Operation(summary = "查询近7天打卡数据")
    @GetMapping("/partner/checkin/weekly")
    public ApiResponse<List<PartnerCheckinDO>> getWeekly(@CurrentUser UserDO user) {
        return ApiResponse.ok(service.getWeekly(user.getId()));
    }

    @Data
    public static class CheckinRequest {
        @NotBlank
        private String checkinType;
    }
}
