import { useEffect, useRef, useState } from 'react';
import { Link, useSearchParams } from 'react-router-dom';
import { CenteredAuthLayout } from '@/components/layout/AuthLayout';
import { Button } from '@/components/ui/Button';
import { Input } from '@/components/ui/Input';
import { Icon } from '@/components/ui/Icon';
import { Spinner } from '@/components/ui/Spinner';
import { authApi } from '@/services/auth';
import { useToast } from '@/components/ui/Toast';

type State = 'verifying' | 'success' | 'failed';

export function VerifyEmailPage() {
  const [params] = useSearchParams();
  const token = params.get('token');
  const [state, setState] = useState<State>(token ? 'verifying' : 'failed');
  const [email, setEmail] = useState('');
  const toast = useToast();
  const ran = useRef(false);

  useEffect(() => {
    if (!token) return;
    // Token là single-use. React StrictMode (dev) chạy effect 2 lần → lần 2 sẽ thấy token
    // "đã dùng" và báo lỗi sai. Guard để chỉ gọi verify đúng 1 lần.
    if (ran.current) return;
    ran.current = true;
    authApi
      .verifyEmail(token)
      .then(() => setState('success'))
      .catch(() => setState('failed'));
  }, [token]);

  const resend = async () => {
    if (!email) return;
    try {
      await authApi.resendVerification(email);
      toast.success('Đã gửi lại email xác minh');
    } catch {
      toast.error('Không gửi lại được email');
    }
  };

  return (
    <CenteredAuthLayout>
      {state === 'verifying' && (
        <div className="flex flex-col items-center text-center">
          <Spinner className="h-10 w-10 text-brand" />
          <p className="mt-4 text-ink-secondary">Đang xác minh email...</p>
        </div>
      )}
      {state === 'success' && (
        <div className="text-center">
          <span className="mx-auto mb-4 flex h-16 w-16 items-center justify-center rounded-full bg-success-tint text-success">
            <Icon name="check_circle" className="text-[32px]" />
          </span>
          <h1 className="font-headline text-2xl font-bold text-ink">Xác minh thành công!</h1>
          <p className="mt-2 text-sm text-ink-secondary">Tài khoản của bạn đã được kích hoạt.</p>
          <Link to="/login" className="mt-6 block">
            <Button fullWidth>Đăng nhập ngay</Button>
          </Link>
        </div>
      )}
      {state === 'failed' && (
        <div className="text-center">
          <span className="mx-auto mb-4 flex h-16 w-16 items-center justify-center rounded-full bg-danger-tint text-danger">
            <Icon name="cancel" className="text-[32px]" />
          </span>
          <h1 className="font-headline text-2xl font-bold text-ink">Liên kết không hợp lệ hoặc đã hết hạn</h1>
          <p className="mt-2 text-sm text-ink-secondary">Nhập email để nhận lại liên kết xác minh.</p>
          <div className="mt-5 space-y-3 text-left">
            <Input label="Email" type="email" placeholder="ban@email.com" value={email} onChange={(e) => setEmail(e.target.value)} />
            <Button fullWidth onClick={resend}>Gửi lại email xác minh</Button>
            <Link to="/login" className="block text-center text-sm font-medium text-brand hover:underline">
              Về trang đăng nhập
            </Link>
          </div>
        </div>
      )}
    </CenteredAuthLayout>
  );
}
