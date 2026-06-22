import { api } from '@/lib/api';
import type { Role } from '@/types/api';

export interface LoginResponse {
  accessToken: string;
  refreshToken: string;
  email: string;
  role: Role;
}

export const authApi = {
  register: (email: string, password: string, fullName: string) =>
    api.post<string>('/users/register', { email, password, fullName }),

  verifyEmail: (token: string) => api.post<void>('/users/verify-email', { token }),

  resendVerification: (email: string) => api.post<void>('/users/resend-verification', { email }),

  forgotPassword: (email: string) => api.post<void>('/users/forgot-password', { email }),

  resetPassword: (token: string, newPassword: string) =>
    api.post<void>('/users/reset-password', { token, newPassword }),

  // OAuth (Google)
  oauthAuthorize: (provider = 'google') =>
    api.get<{ authorizationUrl: string; state: string }>(`/oauth/${provider}/authorize`),

  oauthCallback: (provider: string, code: string, state: string) =>
    api.get<LoginResponse>(`/oauth/${provider}/callback`, { params: { code, state } }),
};
