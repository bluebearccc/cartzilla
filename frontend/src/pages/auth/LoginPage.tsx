import { useState } from 'react';
import { Link, useLocation, useNavigate } from 'react-router-dom';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';
import { AuthLayout } from '@/components/layout/AuthLayout';
import { Input } from '@/components/ui/Input';
import { Button } from '@/components/ui/Button';
import { Icon } from '@/components/ui/Icon';
import { InlineMessage } from '@/components/ui/States';
import { useAuth } from '@/store/auth';
import { authApi } from '@/services/auth';
import { useToast } from '@/components/ui/Toast';
import { ApiError } from '@/types/api';

const schema = z.object({
  email: z.string().min(1, 'Vui lòng nhập email').email('Email không hợp lệ'),
  password: z.string().min(1, 'Vui lòng nhập mật khẩu'),
  remember: z.boolean().optional(),
});
type FormValues = z.infer<typeof schema>;

export function LoginPage() {
  const { login } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();
  const toast = useToast();
  const [formError, setFormError] = useState<string | null>(null);
  const [unverified, setUnverified] = useState<string | null>(null);

  const { register, handleSubmit, formState: { errors, isSubmitting } } = useForm<FormValues>({
    resolver: zodResolver(schema),
    defaultValues: { email: '', password: '', remember: true },
  });

  const from = (location.state as { from?: string } | null)?.from;

  const onSubmit = async (values: FormValues) => {
    setFormError(null);
    setUnverified(null);
    try {
      const session = await login(values.email, values.password);
      if (session.role === 'STAFF' || session.role === 'ADMIN') navigate('/staff/orders');
      else navigate(from ?? '/');
    } catch (err) {
      const message = err instanceof ApiError ? err.message : 'Đăng nhập thất bại';
      if (/xác minh|verif/i.test(message)) setUnverified(values.email);
      setFormError(message);
    }
  };

  const onGoogle = async () => {
    try {
      const { authorizationUrl } = await authApi.oauthAuthorize('google');
      window.location.assign(authorizationUrl);
    } catch {
      toast.error('Không khởi tạo được đăng nhập Google');
    }
  };

  const resend = async () => {
    if (!unverified) return;
    try {
      await authApi.resendVerification(unverified);
      toast.success('Đã gửi lại email xác minh');
    } catch {
      toast.error('Không gửi lại được email');
    }
  };

  return (
    <AuthLayout>
      <h1 className="font-headline text-2xl font-bold text-ink">Đăng nhập</h1>
      <p className="mt-1 text-sm text-ink-secondary">Chào mừng bạn quay lại Cartzilla.</p>

      {formError && (
        <div className="mt-4">
          <InlineMessage tone={unverified ? 'warning' : 'danger'}>
            {formError}
            {unverified && (
              <button onClick={resend} className="ml-1 font-medium underline">
                Gửi lại email xác minh
              </button>
            )}
          </InlineMessage>
        </div>
      )}

      <form onSubmit={handleSubmit(onSubmit)} className="mt-6 space-y-4">
        <Input label="Email" type="email" placeholder="ban@email.com" error={errors.email?.message} {...register('email')} />
        <Input label="Mật khẩu" type="password" placeholder="••••••••" error={errors.password?.message} {...register('password')} />
        <div className="flex items-center justify-between">
          <label className="flex items-center gap-2 text-sm text-ink-secondary">
            <input type="checkbox" className="h-4 w-4 rounded border-border text-brand" {...register('remember')} />
            Ghi nhớ đăng nhập
          </label>
          <Link to="/forgot-password" className="text-sm font-medium text-brand hover:underline">
            Quên mật khẩu?
          </Link>
        </div>
        <Button type="submit" fullWidth loading={isSubmitting} size="lg">
          Đăng nhập
        </Button>
      </form>

      <div className="my-5 flex items-center gap-3">
        <div className="h-px flex-1 bg-divider" />
        <span className="text-xs text-ink-muted">hoặc</span>
        <div className="h-px flex-1 bg-divider" />
      </div>

      <button
        onClick={onGoogle}
        className="flex h-12 w-full items-center justify-center gap-2 rounded border border-border bg-white text-sm font-medium text-ink hover:bg-page"
      >
        <img src="https://www.google.com/favicon.ico" alt="" className="h-5 w-5" />
        Đăng nhập với Google
      </button>

      <p className="mt-6 text-center text-sm text-ink-secondary">
        Chưa có tài khoản?{' '}
        <Link to="/register" className="font-medium text-brand hover:underline">
          Đăng ký
        </Link>
      </p>
      <p className="mt-4 text-center">
        <Link to="/" className="inline-flex items-center gap-1 text-sm text-ink-muted hover:text-ink">
          <Icon name="arrow_back" className="text-[16px]" /> Về trang chủ
        </Link>
      </p>
    </AuthLayout>
  );
}
