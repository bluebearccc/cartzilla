// Auth context: holds the current session (email + role) and exposes login/logout.
// Tokens live in localStorage (auth-storage); this context is the React-facing view.

import { createContext, useCallback, useContext, useMemo, useState, type ReactNode } from 'react';
import { api } from '@/lib/api';
import {
  clearSession,
  getRefreshToken,
  getSession,
  saveSession,
  type Session,
} from '@/lib/auth-storage';
import type { Role } from '@/types/api';

interface LoginResponse {
  accessToken: string;
  refreshToken: string;
  email: string;
  role: Role;
}

interface AuthContextValue {
  session: Session | null;
  isAuthenticated: boolean;
  hasRole: (...roles: Role[]) => boolean;
  login: (email: string, password: string) => Promise<Session>;
  setSessionFromTokens: (res: LoginResponse) => Session;
  logout: () => Promise<void>;
}

const AuthContext = createContext<AuthContextValue | null>(null);

export function AuthProvider({ children }: { children: ReactNode }) {
  const [session, setSession] = useState<Session | null>(() => getSession());

  const setSessionFromTokens = useCallback((res: LoginResponse): Session => {
    const s: Session = { email: res.email, role: res.role };
    saveSession(res.accessToken, res.refreshToken, s);
    setSession(s);
    return s;
  }, []);

  const login = useCallback(
    async (email: string, password: string) => {
      const res = await api.post<LoginResponse>('/users/login', { email, password });
      return setSessionFromTokens(res);
    },
    [setSessionFromTokens],
  );

  const logout = useCallback(async () => {
    const refreshToken = getRefreshToken();
    try {
      if (refreshToken) await api.post('/users/logout', { refreshToken });
    } catch {
      /* ignore network/logout errors — clear locally regardless */
    }
    clearSession();
    setSession(null);
  }, []);

  const hasRole = useCallback(
    (...roles: Role[]) => !!session && roles.includes(session.role),
    [session],
  );

  const value = useMemo<AuthContextValue>(
    () => ({
      session,
      isAuthenticated: !!session,
      hasRole,
      login,
      setSessionFromTokens,
      logout,
    }),
    [session, hasRole, login, setSessionFromTokens, logout],
  );

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth(): AuthContextValue {
  const ctx = useContext(AuthContext);
  if (!ctx) throw new Error('useAuth must be used within <AuthProvider>');
  return ctx;
}
