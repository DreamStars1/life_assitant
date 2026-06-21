import axios, { type InternalAxiosRequestConfig, type AxiosResponse } from "axios";
import qs from "qs";
import { useUserStoreHook } from "@/stores/user";
import { AuthStorage, redirectToLogin } from "@/utils/auth";

// 记录已重试的请求，防止无限循环
const retriedConfigs = new WeakSet<InternalAxiosRequestConfig>();

// HTTP 请求实例
const http = axios.create({
  baseURL: import.meta.env.VITE_APP_BASE_API,
  timeout: 50000,
  headers: { "Content-Type": "application/json;charset=utf-8" },
  paramsSerializer: (params) => qs.stringify(params, { arrayFormat: "repeat" }),
});

// 请求拦截器
http.interceptors.request.use(
  (config: InternalAxiosRequestConfig) => {
    const token = AuthStorage.getAccessToken();

    if (config.headers.Authorization === "no-auth") {
      delete config.headers.Authorization;
    } else if (token) {
      config.headers.Authorization = `Bearer ${token}`;
    }

    return config;
  },
  (error) => Promise.reject(error)
);

// 响应拦截器 — FastAPI 直接响应格式（无 {code, data, msg} 包装）
http.interceptors.response.use(
  (response: AxiosResponse): any => {
    const { responseType } = response.config;
    if (responseType === "blob" || responseType === "arraybuffer") {
      return response;
    }
    return response.data;
  },

  async (error) => {
    const { config, response } = error;

    if (!response) {
      ElMessage.error("网络连接失败");
      return Promise.reject(error);
    }

    const status = response.status;
    const detail = response.data?.detail || "请求失败";

    // 401/403 — 尝试 token 刷新后自动重试一次
    if (status === 401 || status === 403) {
      if (!config || retriedConfigs.has(config)) {
        await redirectToLogin(detail);
        return Promise.reject(new Error(detail));
      }

      retriedConfigs.add(config);

      try {
        const userStore = useUserStoreHook();
        await userStore.refreshTokenOnce();

        const token = AuthStorage.getAccessToken();
        if (token) {
          config.headers.set("Authorization", `Bearer ${token}`);
        }

        return http(config);
      } catch {
        await redirectToLogin(detail);
        return Promise.reject(new Error("Token refresh failed"));
      }
    }

    ElMessage.error(detail);
    return Promise.reject(new Error(detail));
  }
);

export default http;
