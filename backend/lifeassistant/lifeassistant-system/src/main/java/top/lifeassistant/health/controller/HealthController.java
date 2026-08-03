package top.lifeassistant.health.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;
import top.lifeassistant.common.annotation.CurrentUser;
import top.lifeassistant.common.base.model.query.PageResult;
import top.lifeassistant.common.base.model.resp.ApiResponse;
import top.lifeassistant.health.model.query.HealthPageQuery;
import top.lifeassistant.health.model.req.HealthDailyUpsertReq;
import top.lifeassistant.health.model.req.HealthMealUpsertReq;
import top.lifeassistant.health.model.req.HealthMemoryCreateReq;
import top.lifeassistant.health.model.req.HealthMemoryPatchReq;
import top.lifeassistant.health.model.req.HealthProfileUpsertReq;
import top.lifeassistant.health.model.req.HealthToleranceCreateReq;
import top.lifeassistant.health.model.req.HealthTolerancePatchReq;
import top.lifeassistant.health.model.req.HealthTriggerCreateReq;
import top.lifeassistant.health.model.req.HealthTriggerPatchReq;
import top.lifeassistant.health.model.req.HealthWeightCreateReq;
import top.lifeassistant.health.model.resp.HealthDailyResp;
import top.lifeassistant.health.model.resp.HealthMealResp;
import top.lifeassistant.health.model.resp.HealthMemoryResp;
import top.lifeassistant.health.model.resp.HealthProfileResp;
import top.lifeassistant.health.model.resp.HealthSummaryResp;
import top.lifeassistant.health.model.resp.HealthToleranceResp;
import top.lifeassistant.health.model.resp.HealthTriggerResp;
import top.lifeassistant.health.model.resp.HealthWeightResp;
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

    @Operation(summary = "查询体重记录")
    @GetMapping("/weights")
    public ApiResponse<List<HealthWeightResp>> listWeights(@CurrentUser UserDO user,
            @RequestParam(required = false) @DateTimeFormat(iso = DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DATE) LocalDate to,
            @RequestParam(required = false, defaultValue = "all") String type) {
        return ApiResponse.ok(healthService.listWeights(user.getId(), from, to, type));
    }

    @Operation(summary = "新增体重记录")
    @PostMapping("/weights")
    public ApiResponse<HealthWeightResp> createWeight(@CurrentUser UserDO user,
            @Valid @RequestBody HealthWeightCreateReq req) {
        return ApiResponse.ok(healthService.createWeight(user.getId(), req));
    }

    @Operation(summary = "删除体重记录")
    @DeleteMapping("/weights/{id}")
    public ApiResponse<Void> deleteWeight(@CurrentUser UserDO user, @PathVariable String id) {
        healthService.deleteWeight(user.getId(), id);
        return ApiResponse.ok();
    }

    @Operation(summary = "分页查询健康规律")
    @GetMapping("/memories")
    public ApiResponse<PageResult<HealthMemoryResp>> listMemories(@CurrentUser UserDO user,
            @Valid HealthPageQuery query) {
        return ApiResponse.ok(healthService.listMemories(user.getId(), query));
    }

    @Operation(summary = "新增健康规律")
    @PostMapping("/memories")
    public ApiResponse<HealthMemoryResp> createMemory(@CurrentUser UserDO user,
            @Valid @RequestBody HealthMemoryCreateReq req) {
        return ApiResponse.ok(healthService.createMemory(user.getId(), req));
    }

    @Operation(summary = "更新健康规律")
    @PatchMapping("/memories/{id}")
    public ApiResponse<HealthMemoryResp> patchMemory(@CurrentUser UserDO user, @PathVariable String id,
            @Valid @RequestBody HealthMemoryPatchReq req) {
        return ApiResponse.ok(healthService.patchMemory(user.getId(), id, req));
    }

    @Operation(summary = "删除健康规律")
    @DeleteMapping("/memories/{id}")
    public ApiResponse<Void> deleteMemory(@CurrentUser UserDO user, @PathVariable String id) {
        healthService.deleteMemory(user.getId(), id);
        return ApiResponse.ok();
    }

    @Operation(summary = "分页查询耐受记录")
    @GetMapping("/tolerances")
    public ApiResponse<PageResult<HealthToleranceResp>> listTolerances(@CurrentUser UserDO user,
            @Valid HealthPageQuery query) {
        return ApiResponse.ok(healthService.listTolerances(user.getId(), query));
    }

    @Operation(summary = "新增耐受记录")
    @PostMapping("/tolerances")
    public ApiResponse<HealthToleranceResp> createTolerance(@CurrentUser UserDO user,
            @Valid @RequestBody HealthToleranceCreateReq req) {
        return ApiResponse.ok(healthService.createTolerance(user.getId(), req));
    }

    @Operation(summary = "更新耐受记录")
    @PatchMapping("/tolerances/{id}")
    public ApiResponse<HealthToleranceResp> patchTolerance(@CurrentUser UserDO user, @PathVariable String id,
            @Valid @RequestBody HealthTolerancePatchReq req) {
        return ApiResponse.ok(healthService.patchTolerance(user.getId(), id, req));
    }

    @Operation(summary = "删除耐受记录")
    @DeleteMapping("/tolerances/{id}")
    public ApiResponse<Void> deleteTolerance(@CurrentUser UserDO user, @PathVariable String id) {
        healthService.deleteTolerance(user.getId(), id);
        return ApiResponse.ok();
    }

    @Operation(summary = "分页查询触发因素")
    @GetMapping("/triggers")
    public ApiResponse<PageResult<HealthTriggerResp>> listTriggers(@CurrentUser UserDO user,
            @Valid HealthPageQuery query) {
        return ApiResponse.ok(healthService.listTriggers(user.getId(), query));
    }

    @Operation(summary = "新增触发因素")
    @PostMapping("/triggers")
    public ApiResponse<HealthTriggerResp> createTrigger(@CurrentUser UserDO user,
            @Valid @RequestBody HealthTriggerCreateReq req) {
        return ApiResponse.ok(healthService.createTrigger(user.getId(), req));
    }

    @Operation(summary = "更新触发因素")
    @PatchMapping("/triggers/{id}")
    public ApiResponse<HealthTriggerResp> patchTrigger(@CurrentUser UserDO user, @PathVariable String id,
            @Valid @RequestBody HealthTriggerPatchReq req) {
        return ApiResponse.ok(healthService.patchTrigger(user.getId(), id, req));
    }

    @Operation(summary = "删除触发因素")
    @DeleteMapping("/triggers/{id}")
    public ApiResponse<Void> deleteTrigger(@CurrentUser UserDO user, @PathVariable String id) {
        healthService.deleteTrigger(user.getId(), id);
        return ApiResponse.ok();
    }

    @Operation(summary = "健康概览")
    @GetMapping("/summary")
    public ApiResponse<HealthSummaryResp> getSummary(@CurrentUser UserDO user) {
        return ApiResponse.ok(healthService.getSummary(user.getId()));
    }
}
