import { Navigate, Outlet, useLocation } from 'react-router-dom';
import { useAuth } from '@/store/auth';
import type { Role } from '@/types/api';

/** Requires an authenticated session; otherwise redirect to /login (remember target). */
export function ProtectedRoute() {
  const { isAuthenticated } = useAuth();
  const location = useLocation();
  if (!isAuthenticated) {
    return <Navigate to="/login" replace state={{ from: location.pathname + location.search }} />;
  }
  return <Outlet />;
}

/** Requires one of the given roles; otherwise show a 403-style notice. */
export function RoleRoute({ roles }: { roles: Role[] }) {
  const { isAuthenticated, hasRole } = useAuth();
  const location = useLocation();
  if (!isAuthenticated) {
    return <Navigate to="/login" replace state={{ from: location.pathname + location.search }} />;
  }
  if (!hasRole(...roles)) {
    return (
      <div className="flex min-h-[60vh] flex-col items-center justify-center px-6 text-center">
        <h1 className="font-headline text-2xl font-bold text-ink">Không có quyền truy cập</h1>
        <p className="mt-2 text-ink-secondary">Bạn không có quyền xem trang này.</p>
        <a href="/" className="mt-4 rounded bg-brand px-4 py-2 text-sm font-medium text-white">
          Về trang chủ
        </a>
      </div>
    );
  }
  return <Outlet />;
}
