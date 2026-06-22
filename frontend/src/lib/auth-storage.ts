// Token + session storage. Access token in memory-backed localStorage; refresh
// token alongside. The gateway derives X-User-Id/X-User-Role from the JWT, so the
// frontend only needs to hold the tokens and the decoded role/email for UI gating.

import type { Role } from '@/types/api';

const ACCESS_KEY = 'cz_access';
const REFRESH_KEY = 'cz_refresh';
const SESSION_KEY = 'cz_session';

export interface Session {
  email: string;
  role: Role;
}

export function getAccessToken(): string | null {
  return localStorage.getItem(ACCESS_KEY);
}

export function getRefreshToken(): string | null {
  return localStorage.getItem(REFRESH_KEY);
}

export function getSession(): Session | null {
  const raw = localStorage.getItem(SESSION_KEY);
  if (!raw) return null;
  try {
    return JSON.parse(raw) as Session;
  } catch {
    return null;
  }
}

export function saveSession(accessToken: string, refreshToken: string, session: Session): void {
  localStorage.setItem(ACCESS_KEY, accessToken);
  localStorage.setItem(REFRESH_KEY, refreshToken);
  localStorage.setItem(SESSION_KEY, JSON.stringify(session));
}

export function updateTokens(accessToken: string, refreshToken: string): void {
  localStorage.setItem(ACCESS_KEY, accessToken);
  localStorage.setItem(REFRESH_KEY, refreshToken);
}

export function clearSession(): void {
  localStorage.removeItem(ACCESS_KEY);
  localStorage.removeItem(REFRESH_KEY);
  localStorage.removeItem(SESSION_KEY);
}

/** Current user id from the access-token `sub` claim (gateway derives X-User-Id the same way). */
export function getUserId(): string | null {
  const token = getAccessToken();
  if (!token) return null;
  const claims = decodeJwt(token);
  const sub = claims?.sub;
  return typeof sub === 'string' ? sub : null;
}

/** Decode the `sub`/`role`/`email` claims from a JWT without verifying it (UI only). */
export function decodeJwt(token: string): Record<string, unknown> | null {
  try {
    const payload = token.split('.')[1];
    const json = atob(payload.replace(/-/g, '+').replace(/_/g, '/'));
    return JSON.parse(decodeURIComponent(escape(json)));
  } catch {
    return null;
  }
}
