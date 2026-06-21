import request from "@/utils/request";
import type { LoginRequest, LoginResponse, CaptchaInfo } from "./types";

const AUTH_BASE_URL = "/api/v1/auth";

/** 将 FastAPI snake_case 响应映射为管理后台 camelCase 类型 */
function mapAuthResponse(raw: Record<string, any>): LoginResponse {
  return {
    accessToken: raw.access_token,
    refreshToken: raw.refresh_token,
    tokenType: raw.token_type || "bearer",
    expiresIn: raw.expires_in || 7200,
  };
}

const AuthAPI = {
  /** 登录接口 */
  login(data: LoginRequest) {
    const payload: { username: string; password: string; tenantId?: number } = {
      username: data.username,
      password: data.password,
    };
    if (typeof data.tenantId !== "undefined") {
      payload.tenantId = data.tenantId;
    }

    return request<unknown, Record<string, any>>({
      url: `${AUTH_BASE_URL}/login`,
      method: "post",
      data: payload,
    }).then(mapAuthResponse);
  },

  /** 切换租户(平台用户) - 返回新的 token */
  switchTenant(tenantId: number) {
    return request<unknown, Record<string, any>>({
      url: `${AUTH_BASE_URL}/switch-tenant`,
      method: "post",
      params: { tenantId },
    }).then(mapAuthResponse);
  },

  /** 刷新 token 接口 */
  refreshToken(refreshToken: string) {
    return request<unknown, Record<string, any>>({
      url: `${AUTH_BASE_URL}/refresh-token`,
      method: "post",
      params: { refresh_token: refreshToken },
      headers: {
        Authorization: "no-auth",
      },
    }).then(mapAuthResponse);
  },

  /** 退出登录接口 */
  logout() {
    return request({
      url: `${AUTH_BASE_URL}/logout`,
      method: "delete",
    });
  },

  /** 获取验证码接口*/
  getCaptcha() {
    return request<unknown, CaptchaInfo>({
      url: `${AUTH_BASE_URL}/captcha`,
      method: "get",
    });
  },
};

export default AuthAPI;

// 重导出类型
export * from "./types";
