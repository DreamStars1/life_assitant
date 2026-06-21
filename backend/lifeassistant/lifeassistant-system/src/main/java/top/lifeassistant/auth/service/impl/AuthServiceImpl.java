package top.lifeassistant.auth.service.impl;

import cn.dev33.satoken.secure.BCrypt;
import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.core.lang.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import top.lifeassistant.auth.model.req.LoginReq;
import top.lifeassistant.auth.model.resp.LoginResp;
import top.lifeassistant.auth.service.AuthService;
import top.lifeassistant.system.model.entity.user.UserDO;
import top.lifeassistant.system.service.UserService;
import top.continew.starter.core.exception.BadRequestException;
import top.continew.starter.core.exception.BusinessException;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserService userService;

    @Override
    public LoginResp login(LoginReq req) {
        UserDO user = userService.getByEmail(req.getEmail());
        if (user == null) {
            throw new BadRequestException("Incorrect email or password");
        }
        if (Boolean.FALSE.equals(user.getIsActive())) {
            throw new BadRequestException("Inactive user");
        }
        if (!BCrypt.checkpw(req.getPassword(), user.getPassword())) {
            throw new BadRequestException("Incorrect email or password");
        }
        StpUtil.login(user.getId());
        String accessToken = StpUtil.getTokenValue();
        // pony: refresh token = a secondary JWT with longer TTL; reuse SaToken's createToken for now
        // ceiling: single token type; upgrade path: separate SaToken login type for refresh
        return LoginResp.builder()
            .accessToken(accessToken)
            .tokenType("bearer")
            .refreshToken(accessToken)
            .build();
    }

    @Override
    public LoginResp refreshToken(String refreshToken) {
        // pony: SaToken auto-renews via dynamic-active-timeout; refresh is a no-op
        Object loginId = StpUtil.getLoginIdByToken(refreshToken);
        if (loginId == null) {
            throw new BadRequestException("Invalid refresh token");
        }
        String newToken = StpUtil.getTokenValue();
        return LoginResp.builder()
            .accessToken(newToken)
            .tokenType("bearer")
            .refreshToken(newToken)
            .build();
    }

    @Override
    public void logout() {
        StpUtil.logout();
    }

    @Override
    public void sendPasswordRecovery(String email) {
        UserDO user = userService.getByEmail(email);
        if (user == null) return; // 统一返回成功，防止邮箱枚举
        // pony: password reset skipped - requires SMTP config. Throw if attempted without SMTP.
        throw new BusinessException("Password recovery not configured: SMTP not set");
    }

    @Override
    public void resetPassword(String token, String newPassword) {
        // pony: stub — requires SMTP + token store; implement when SMTP is configured
        throw new BusinessException("Password reset not configured");
    }
}
