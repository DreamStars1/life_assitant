package top.lifeassistant.identity.controller;

import cn.dev33.satoken.secure.BCrypt;
import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.jwt.JWT;
import cn.hutool.jwt.JWTUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import top.lifeassistant.common.annotation.CurrentUser;
import top.lifeassistant.common.base.model.resp.ApiResponse;
import top.lifeassistant.partner.model.req.PartnerSinceUpdateReq;
import top.lifeassistant.partner.service.PartnerInfoRules;
import top.lifeassistant.partner.service.PartnerInfoService;
import top.lifeassistant.system.model.entity.user.UserDO;
import top.lifeassistant.system.service.UserService;
import top.lifeassistant.user.model.resp.UserPublicResp;
import top.continew.starter.core.exception.BadRequestException;

import java.time.LocalDate;
import java.util.Map;

@Tag(name = "伴侣 API")
@RestController
@RequiredArgsConstructor
public class IdentityController {

    private final UserService userService;
    private final PartnerInfoService partnerInfoService;

    @Operation(summary = "生成伴侣邀请码")
    @PostMapping("/identity/invite")
    public ApiResponse<Map<String, String>> invite(@CurrentUser UserDO user) {
        // pony: JWT token encoding current user + expiry; simple HS256
        String token = JWTUtil.createToken(
            Map.of("uid", user.getId(), "email", user.getEmail()),
            "invite".getBytes()
        );
        return ApiResponse.ok(Map.of("invite_token", token));
    }

    @Operation(summary = "接受伴侣绑定")
    @PostMapping("/identity/bind-partner")
    @Transactional
    public ApiResponse<UserPublicResp> bindPartner(@CurrentUser UserDO me, @RequestParam String inviteToken) {
        if (me.getPartnerId() != null) {
            throw new BadRequestException("Already has a partner");
        }
        JWT jwt = JWTUtil.parseToken(inviteToken);
        String partnerId = (String) jwt.getPayload("uid");
        if (partnerId == null || partnerId.equals(me.getId())) {
            throw new BadRequestException("Invalid invite token");
        }
        UserDO partner = userService.getById(partnerId);
        if (partner.getPartnerId() != null) {
            throw new BadRequestException("Partner already bound");
        }
        // 双向绑定
        me.setPartnerId(partnerId);
        partner.setPartnerId(me.getId());
        userService.update(me);
        userService.update(partner);
        LocalDate since = LocalDate.now();
        partnerInfoService.createForPair(me.getId(), partnerId, since);
        return ApiResponse.ok(UserPublicResp.from(me, since));
    }

    @Operation(summary = "更新相识日期")
    @PutMapping("/identity/partner-since")
    @Transactional
    public ApiResponse<UserPublicResp> updatePartnerSince(
        @CurrentUser UserDO me, @Valid @RequestBody PartnerSinceUpdateReq req) {
        if (me.getPartnerId() == null) {
            throw new BadRequestException("尚未绑定伴侣");
        }
        PartnerInfoRules.requireNotFuture(req.getPartnerSince(), LocalDate.now());
        partnerInfoService.updatePartnerSince(me.getId(), me.getPartnerId(), req.getPartnerSince());
        return ApiResponse.ok(UserPublicResp.from(me, req.getPartnerSince()));
    }

    @Operation(summary = "解除伴侣绑定")
    @PostMapping("/identity/unbind-partner")
    public ApiResponse<Void> unbindPartner(@CurrentUser UserDO me) {
        userService.unbindPartner(me.getId());
        return ApiResponse.ok("已解除伴侣绑定", null);
    }
}
