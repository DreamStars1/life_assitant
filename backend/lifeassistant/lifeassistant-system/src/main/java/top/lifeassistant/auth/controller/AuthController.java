package top.lifeassistant.auth.controller;

import cn.dev33.satoken.annotation.SaIgnore;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import top.lifeassistant.auth.model.req.LoginReq;
import top.lifeassistant.auth.model.resp.LoginResp;
import top.lifeassistant.auth.service.AuthService;
import top.lifeassistant.common.base.model.resp.ApiResponse;

@Tag(name = "认证 API")
@Validated
@RestController
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @SaIgnore
    @Operation(summary = "OAuth2 登录 (form-data)")
    @PostMapping("/login/access-token")
    public ApiResponse<LoginResp> loginAccessToken(@Valid LoginReq req) {
        return ApiResponse.ok(authService.login(req));
    }

    @SaIgnore
    @Operation(summary = "JSON 登录")
    @PostMapping("/auth/login")
    public ApiResponse<LoginResp> login(@Valid @RequestBody LoginReq req) {
        return ApiResponse.ok(authService.login(req));
    }

    @SaIgnore
    @Operation(summary = "刷新 token")
    @PostMapping("/auth/refresh-token")
    public ApiResponse<LoginResp> refreshToken(@RequestParam String refreshToken) {
        return ApiResponse.ok(authService.refreshToken(refreshToken));
    }

    @Operation(summary = "登出")
    @DeleteMapping("/auth/logout")
    public ApiResponse<Void> logout() {
        authService.logout();
        return ApiResponse.ok();
    }

    @SaIgnore
    @Operation(summary = "请求密码重置")
    @PostMapping("/password-recovery/{email}")
    public ApiResponse<Void> passwordRecovery(@PathVariable String email) {
        authService.sendPasswordRecovery(email);
        return ApiResponse.ok("If that email is registered, we sent a password recovery link", null);
    }

    @SaIgnore
    @Operation(summary = "执行密码重置")
    @PostMapping("/reset-password")
    public ApiResponse<Void> resetPassword(@RequestParam String token, @RequestParam String newPassword) {
        authService.resetPassword(token, newPassword);
        return ApiResponse.ok("Password reset successfully", null);
    }
}
