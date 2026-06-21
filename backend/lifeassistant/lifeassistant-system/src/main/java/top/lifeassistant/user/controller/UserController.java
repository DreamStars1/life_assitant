package top.lifeassistant.user.controller;

import cn.dev33.satoken.annotation.SaIgnore;
import cn.dev33.satoken.secure.BCrypt;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import top.lifeassistant.common.annotation.CurrentUser;
import top.lifeassistant.common.base.model.resp.ApiResponse;
import top.lifeassistant.system.model.entity.user.UserDO;
import top.lifeassistant.system.service.UserService;
import top.lifeassistant.user.model.req.UserPasswordUpdateReq;
import top.lifeassistant.user.model.req.UserRegisterReq;
import top.lifeassistant.user.model.req.UserUpdateReq;
import top.lifeassistant.user.model.resp.UserPublicResp;
import top.continew.starter.core.exception.BadRequestException;
import top.continew.starter.core.exception.BusinessException;

import java.time.LocalDateTime;

@Tag(name = "用户 API")
@Validated
@RestController
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @SaIgnore
    @Operation(summary = "用户注册")
    @PostMapping("/users/signup")
    public ApiResponse<UserPublicResp> signup(@Valid @RequestBody UserRegisterReq req) {
        if (userService.existsByEmail(req.getEmail())) {
            throw new BadRequestException("The user with this email already exists in the system");
        }
        UserDO user = new UserDO();
        user.setEmail(req.getEmail());
        user.setPassword(BCrypt.hashpw(req.getPassword()));
        user.setFullName(req.getFullName() != null ? req.getFullName() : req.getEmail());
        user.setIsActive(true);
        user.setIsSuperuser(false);
        user.setTimezone("Asia/Shanghai");
        user.setPushEnabled(true);
        user.setCreatedAt(LocalDateTime.now());
        user.setUpdateTime(LocalDateTime.now());
        userService.create(user);
        return ApiResponse.ok(UserPublicResp.from(user));
    }

    @Operation(summary = "获取当前用户")
    @GetMapping("/users/me")
    public ApiResponse<UserPublicResp> me(@CurrentUser UserDO user) {
        return ApiResponse.ok(UserPublicResp.from(user));
    }

    @Operation(summary = "获取指定用户（公开信息）")
    @GetMapping("/users/{id}")
    public ApiResponse<UserPublicResp> getUserById(@PathVariable String id) {
        return ApiResponse.ok(UserPublicResp.from(userService.getById(id)));
    }

    @Operation(summary = "更新当前用户信息")
    @PatchMapping("/users/me")
    public ApiResponse<UserPublicResp> updateMe(@CurrentUser UserDO user, @RequestBody UserUpdateReq req) {
        if (req.getFullName() != null) user.setFullName(req.getFullName());
        if (req.getEmail() != null) {
            if (!req.getEmail().equals(user.getEmail()) && userService.existsByEmail(req.getEmail())) {
                throw new BadRequestException("User with this email already exists");
            }
            user.setEmail(req.getEmail());
        }
        userService.update(user);
        return ApiResponse.ok(UserPublicResp.from(user));
    }

    @Operation(summary = "修改密码")
    @PatchMapping("/users/me/password")
    public ApiResponse<Void> updatePassword(@CurrentUser UserDO user, @Valid @RequestBody UserPasswordUpdateReq req) {
        if (!BCrypt.checkpw(req.getCurrentPassword(), user.getPassword())) {
            throw new BadRequestException("Incorrect password");
        }
        user.setPassword(BCrypt.hashpw(req.getNewPassword()));
        userService.update(user);
        return ApiResponse.ok("Password updated successfully", null);
    }

    @Operation(summary = "删除当前用户")
    @DeleteMapping("/users/me")
    public ApiResponse<Void> deleteMe(@CurrentUser UserDO user) {
        if (Boolean.TRUE.equals(user.getIsSuperuser())) {
            throw new BusinessException("Superuser cannot delete themselves");
        }
        userService.delete(user.getId());
        return ApiResponse.ok("User deleted successfully", null);
    }
}
