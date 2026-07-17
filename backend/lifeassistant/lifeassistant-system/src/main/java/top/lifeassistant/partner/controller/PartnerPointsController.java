package top.lifeassistant.partner.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
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
import top.lifeassistant.partner.model.entity.PartnerPointsDO;
import top.lifeassistant.partner.service.PartnerPointsService;
import top.lifeassistant.system.model.entity.user.UserDO;

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
    public ApiResponse<Page<PartnerPointsDO>> getHistory(
            @CurrentUser UserDO user,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.ok(service.getHistory(user.getId(), page, size));
    }

    @Operation(summary = "记录积分变动")
    @PostMapping("/partner/points")
    public ApiResponse<Void> addPoints(@CurrentUser UserDO user, @Valid @RequestBody PointsChangeRequest req) {
        service.addPoints(user.getId(), req.getPointsChange(), req.getReason());
        return ApiResponse.ok();
    }

    @Data
    public static class PointsChangeRequest {
        @NotNull
        private Integer pointsChange;
        @NotBlank
        private String reason;
    }
}
