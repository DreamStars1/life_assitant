package top.lifeassistant.auth.service;

import top.lifeassistant.auth.model.req.LoginReq;
import top.lifeassistant.auth.model.resp.LoginResp;

public interface AuthService {

    /** 登录，返回 access_token + refresh_token */
    LoginResp login(LoginReq req);

    /** 刷新 token */
    LoginResp refreshToken(String refreshToken);

    /** 登出 */
    void logout();

    /** 密码重置 — 发送邮件 */
    void sendPasswordRecovery(String email);

    /** 密码重置 — 执行 */
    void resetPassword(String token, String newPassword);
}
