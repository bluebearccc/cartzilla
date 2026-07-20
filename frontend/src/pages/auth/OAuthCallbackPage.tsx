import { useEffect, useRef, useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { CenteredAuthLayout } from '@/components/layout/AuthLayout';
import { Spinner } from '@/components/ui/Spinner';
import { Icon } from '@/components/ui/Icon';
import { authApi } from '@/services/auth';
import { useAuth } from '@/store/auth';

export function OAuthCallbackPage() {
  const navigate = useNavigate();
  const { setSessionFromTokens } = useAuth();
  const [error, setError] = useState<string | null>(null);
  const ran = useRef(false);

  useEffect(() => {
    const searchParams = new URLSearchParams(window.location.search);
    // Backend redirects with tokens in the URL fragment (#) so they never hit server logs.
    const hashParams = new URLSearchParams(window.location.hash.replace(/^#/, ''));

    const token = hashParams.get('accessToken') || searchParams.get('accessToken');
    const refreshToken = hashParams.get('refreshToken') || searchParams.get('refreshToken');
    const email = hashParams.get('email') || searchParams.get('email');
    const role = hashParams.get('role') || searchParams.get('role');

    if (token && refreshToken && email && role) {
      if (ran.current) return;
      ran.current = true;
      const session = setSessionFromTokens({
        accessToken: token,
        refreshToken,
        email,
        role: role as any,
      });
      navigate(session.role === 'STAFF' || session.role === 'ADMIN' ? '/staff/orders' : '/', { replace: true });
      return;
    }

    // Check if we have the authorization code to exchange
    const code = searchParams.get('code');
    const state = searchParams.get('state');

    if (code && state) {
      if (ran.current) return;
      ran.current = true;
      authApi
        .oauthCallback('google', code, state)
        .then((res) => {
          const session = setSessionFromTokens(res);
          navigate(session.role === 'STAFF' || session.role === 'ADMIN' ? '/staff/orders' : '/', { replace: true });
        })
        .catch(() => setError('Đăng nhập Google thất bại. Vui lòng thử lại.'));
      return;
    }

    // Check for explicit error from Google (e.g. access_denied)
    const oauthError = searchParams.get('error');
    if (oauthError) {
      setError(`Lỗi từ Google: ${oauthError}`);
      return;
    }

    // If neither tokens (in fragment) nor code are present, it's an invalid callback request
    if (!token && !searchParams.has('code')) {
      setError('Thiếu thông tin xác thực từ Google.');
    }
  }, [navigate, setSessionFromTokens]);

  return (
    <CenteredAuthLayout>
      {error ? (
        <div className="text-center">
          <span className="mx-auto mb-4 flex h-16 w-16 items-center justify-center rounded-full bg-danger-tint text-danger">
            <Icon name="error" className="text-[32px]" />
          </span>
          <h1 className="font-headline text-xl font-bold text-ink">Đăng nhập Google thất bại</h1>
          <p className="mt-2 text-sm text-ink-secondary">{error}</p>
          <Link to="/login" className="mt-5 inline-block rounded bg-brand px-4 py-2 text-sm font-medium text-white">
            Quay lại đăng nhập
          </Link>
        </div>
      ) : (
        <div className="flex flex-col items-center text-center">
          <Spinner className="h-10 w-10 text-brand" />
          <p className="mt-4 text-ink-secondary">Đang đăng nhập với Google...</p>
        </div>
      )}
    </CenteredAuthLayout>
  );
}
