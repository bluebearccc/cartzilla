// Axios instance for the API gateway.
//  - attaches the Bearer access token
//  - unwraps the ApiResponse envelope (returns `data.data`)
//  - on 401, refreshes once via /users/refresh-token and retries (single-flight)
//  - normalizes backend error messages into ApiError (Vietnamese message shown to user)

import axios, {
  AxiosError,
  type AxiosInstance,
  type AxiosRequestConfig,
  type InternalAxiosRequestConfig,
} from 'axios';
import { ApiError, type ApiEnvelope } from '@/types/api';
import {
  clearSession,
  getAccessToken,
  getRefreshToken,
  updateTokens,
} from '@/lib/auth-storage';

// Vite dev proxy forwards /api -> gateway:8080. In prod, set VITE_API_BASE_URL.
const baseURL = (import.meta.env.VITE_API_BASE_URL ?? '') + '/api';

export const http: AxiosInstance = axios.create({
  baseURL,
  headers: { 'Content-Type': 'application/json' },
});

http.interceptors.request.use((config: InternalAxiosRequestConfig) => {
  const token = getAccessToken();
  if (token) config.headers.set('Authorization', `Bearer ${token}`);
  return config;
});

// --- single-flight refresh ---------------------------------------------------
let refreshing: Promise<string | null> | null = null;

async function refreshAccessToken(): Promise<string | null> {
  const refreshToken = getRefreshToken();
  if (!refreshToken) return null;
  try {
    // Bare axios (no interceptors) to avoid recursion.
    const res = await axios.post<ApiEnvelope<{ accessToken: string; refreshToken: string }>>(
      `${baseURL}/users/refresh-token`,
      { refreshToken },
    );
    const { accessToken, refreshToken: newRefresh } = res.data.data;
    updateTokens(accessToken, newRefresh);
    return accessToken;
  } catch {
    clearSession();
    return null;
  }
}

http.interceptors.response.use(
  (response) => response,
  async (error: AxiosError<ApiEnvelope<unknown>>) => {
    const original = error.config as (InternalAxiosRequestConfig & { _retried?: boolean }) | undefined;
    const status = error.response?.status ?? 0;

    // Try a one-time refresh on 401 (but not for the auth endpoints themselves).
    const isAuthCall = original?.url?.includes('/users/login') ||
      original?.url?.includes('/users/refresh-token');
    if (status === 401 && original && !original._retried && !isAuthCall) {
      original._retried = true;
      refreshing = refreshing ?? refreshAccessToken();
      const newToken = await refreshing;
      refreshing = null;
      if (newToken) {
        original.headers.set('Authorization', `Bearer ${newToken}`);
        return http(original);
      }
      // refresh failed -> bounce to login
      if (typeof window !== 'undefined' && !window.location.pathname.startsWith('/login')) {
        window.location.assign('/login?expired=1');
      }
    }

    const message =
      error.response?.data?.message ||
      (status === 0 ? 'Không thể kết nối máy chủ. Vui lòng thử lại.' : 'Đã có lỗi xảy ra.');
    return Promise.reject(new ApiError(message, status));
  },
);

// --- typed envelope-unwrapping helpers --------------------------------------
async function unwrap<T>(p: Promise<{ data: ApiEnvelope<T> }>): Promise<T> {
  const res = await p;
  return res.data.data;
}

export const api = {
  get: <T>(url: string, config?: AxiosRequestConfig) =>
    unwrap<T>(http.get<ApiEnvelope<T>>(url, config)),
  post: <T>(url: string, body?: unknown, config?: AxiosRequestConfig) =>
    unwrap<T>(http.post<ApiEnvelope<T>>(url, body, config)),
  put: <T>(url: string, body?: unknown, config?: AxiosRequestConfig) =>
    unwrap<T>(http.put<ApiEnvelope<T>>(url, body, config)),
  patch: <T>(url: string, body?: unknown, config?: AxiosRequestConfig) =>
    unwrap<T>(http.patch<ApiEnvelope<T>>(url, body, config)),
  del: <T>(url: string, config?: AxiosRequestConfig) =>
    unwrap<T>(http.delete<ApiEnvelope<T>>(url, config)),
};
