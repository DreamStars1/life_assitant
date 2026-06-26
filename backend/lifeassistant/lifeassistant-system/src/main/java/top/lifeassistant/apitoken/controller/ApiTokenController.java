package top.lifeassistant.apitoken.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import top.lifeassistant.apitoken.model.req.ApiTokenCreateReq;
import top.lifeassistant.apitoken.model.resp.ApiTokenResp;
import top.lifeassistant.apitoken.service.ApiTokenService;
import top.lifeassistant.common.annotation.CurrentUser;
import top.lifeassistant.common.base.model.resp.ApiResponse;
import top.lifeassistant.system.model.entity.user.UserDO;

import java.util.List;

@Tag(name = "API Token")
@RestController
@RequiredArgsConstructor
public class ApiTokenController {

    private final ApiTokenService service;

    @Operation(summary = "创建 API Token")
    @PostMapping("/api-tokens")
    public ApiResponse<ApiTokenResp> create(@CurrentUser UserDO user, @Valid @RequestBody ApiTokenCreateReq req) {
        return ApiResponse.ok(service.create(user, req));
    }

    @Operation(summary = "获取我的 API Token 列表")
    @GetMapping("/api-tokens")
    public ApiResponse<List<ApiTokenResp>> list(@CurrentUser UserDO user) {
        return ApiResponse.ok(service.list(user));
    }

    @Operation(summary = "撤销 API Token")
    @DeleteMapping("/api-tokens/{id}")
    public ApiResponse<Void> delete(@CurrentUser UserDO user, @PathVariable String id) {
        service.delete(user, id);
        return ApiResponse.ok();
    }
}
