package top.lifeassistant.health.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;
import top.lifeassistant.common.annotation.CurrentUser;
import top.lifeassistant.common.base.model.resp.ApiResponse;
import top.lifeassistant.health.model.req.HealthDailyUpsertReq;
import top.lifeassistant.health.model.req.HealthMealUpsertReq;
import top.lifeassistant.health.model.req.HealthProfileUpsertReq;
import top.lifeassistant.health.model.resp.HealthDailyResp;
import top.lifeassistant.health.model.resp.HealthMealResp;
import top.lifeassistant.health.model.resp.HealthProfileResp;
import top.lifeassistant.health.service.HealthService;
import top.lifeassistant.system.model.entity.user.UserDO;

import java.time.LocalDate;
import java.util.List;

import static org.springframework.format.annotation.DateTimeFormat.ISO.DATE;

@Tag(name = "健康管家 API")
@RestController
@RequestMapping("/health")
@RequiredArgsConstructor
public class HealthController {

    private final HealthService healthService;

    @Operation(summary = "获取健康档案")
    @GetMapping("/profile")
    public ApiResponse<HealthProfileResp> getProfile(@CurrentUser UserDO user) {
        return ApiResponse.ok(healthService.getProfile(user.getId()));
    }

    @Operation(summary = "更新健康档案")
    @PutMapping("/profile")
    public ApiResponse<HealthProfileResp> putProfile(@CurrentUser UserDO user,
            @Valid @RequestBody HealthProfileUpsertReq req) {
        return ApiResponse.ok(healthService.upsertProfile(user.getId(), req));
    }

    @Operation(summary = "查询某日饮食")
    @GetMapping("/meals")
    public ApiResponse<List<HealthMealResp>> listMeals(@CurrentUser UserDO user,
            @RequestParam @DateTimeFormat(iso = DATE) LocalDate date) {
        return ApiResponse.ok(healthService.listMeals(user.getId(), date));
    }

    @Operation(summary = "新增或更新饮食")
    @PutMapping("/meals")
    public ApiResponse<HealthMealResp> upsertMeal(@CurrentUser UserDO user,
            @Valid @RequestBody HealthMealUpsertReq req) {
        return ApiResponse.ok(healthService.upsertMeal(user.getId(), req));
    }

    @Operation(summary = "删除饮食")
    @DeleteMapping("/meals/{id}")
    public ApiResponse<Void> deleteMeal(@CurrentUser UserDO user, @PathVariable String id) {
        healthService.deleteMeal(user.getId(), id);
        return ApiResponse.ok();
    }

    @Operation(summary = "获取某日状态")
    @GetMapping("/daily")
    public ApiResponse<HealthDailyResp> getDaily(@CurrentUser UserDO user,
            @RequestParam @DateTimeFormat(iso = DATE) LocalDate date) {
        return ApiResponse.ok(healthService.getDaily(user.getId(), date));
    }

    @Operation(summary = "更新某日状态")
    @PutMapping("/daily")
    public ApiResponse<HealthDailyResp> putDaily(@CurrentUser UserDO user,
            @Valid @RequestBody HealthDailyUpsertReq req) {
        return ApiResponse.ok(healthService.upsertDaily(user.getId(), req));
    }
}
