import { useState } from 'react';
import { Link } from 'react-router-dom';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';
import { AuthLayout } from '@/components/layout/AuthLayout';
import { Input } from '@/components/ui/Input';
import { Button } from '@/components/ui/Button';
import { InlineMessage } from '@/components/ui/States';
import { Icon } from '@/components/ui/Icon';
import { authApi } from '@/services/auth';
import { ApiError } from '@/types/api';

const schema = z
  .object({
    fullName: z.string().min(1, 'Vui lòng nhập họ tên'),
    email: z.string().min(1, 'Vui lòng nhập email').email('Email không hợp lệ'),
    password: z
      .string()
      .min(8, 'Tối thiểu 8 ký tự')
      .regex(/[a-zA-Z]/, 'Cần có chữ cái')
      .regex(/[0-9]/, 'Cần có chữ số'),
    confirm: z.string(),
    agree: z.literal(true, { errorMap: () => ({ message: 'Bạn cần đồng ý điều khoản' }) }),
  })
  .refine((d) => d.password === d.confirm, {
    path: ['confirm'],
    message: 'Mật khẩu xác nhận không khớp',
  });
type FormValues = z.infer<typeof schema>;

function strength(pw: string): { score: number; label: string; color: string } {
  let score = 0;
  if (pw.length >= 8) score++;
  if (/[A-Z]/.test(pw) && /[a-z]/.test(pw)) score++;
  if (/[0-9]/.test(pw)) score++;
  if (/[^a-zA-Z0-9]/.test(pw)) score++;
  if (score <= 1) return { score: 1, label: 'Yếu', color: 'bg-danger' };
  if (score === 2 || score === 3) return { score: 2, label: 'Trung bình', color: 'bg-warning' };
  return { score: 3, label: 'Mạnh', color: 'bg-success' };
}

export function RegisterPage() {
  const [formError, setFormError] = useState<string | null>(null);
  const [success, setSuccess] = useState<string | null>(null);
  const [resending, setResending] = useState(false);

  const { register, handleSubmit, watch, formState: { errors, isSubmitting } } = useForm<FormValues>({
    resolver: zodResolver(schema),
    defaultValues: { fullName: '', email: '', password: '', confirm: '', agree: false as unknown as true },
  });

  const pw = watch('password') ?? '';
  const s = strength(pw);

  const onSubmit = async (values: FormValues) => {
    setFormError(null);
    try {
      await authApi.register(values.email, values.password, values.fullName);
      setSuccess(values.email);
    } catch (err) {
      setFormError(err instanceof ApiError ? err.message : 'Đăng ký thất bại');
    }
  };

  const resend = async () => {
    if (!success) return;
    setResending(true);
    try {
      await authApi.resendVerification(success);
    } finally {
      setResending(false);
    }
  };

  if (success) {
    return (
      <AuthLayout>
        <div className="text-center">
          <span className="mx-auto mb-4 flex h-16 w-16 items-center justify-center rounded-full bg-success-tint text-success">
            <Icon name="mark_email_read" className="text-[32px]" />
          </span>
          <h1 className="font-headline text-2xl font-bold text-ink">Đăng ký thành công!</h1>
          <p className="mt-2 text-sm text-ink-secondary">
            Chúng tôi đã gửi email xác minh tới <span className="font-medium text-ink">{success}</span>. Vui
            lòng kiểm tra hộp thư để kích hoạt tài khoản.
          </p>
          <div className="mt-6 flex flex-col gap-2">
            <a href="https://mail.google.com" target="_blank" rel="noreferrer">
              <Button fullWidth>Mở Gmail</Button>
            </a>
            <Button variant="secondary" fullWidth loading={resending} onClick={resend}>
              Gửi lại email
            </Button>
            <Link to="/login" className="mt-2 text-sm font-medium text-brand hover:underline">
              Về trang đăng nhập
            </Link>
          </div>
        </div>
      </AuthLayout>
    );
  }

  return (
    <AuthLayout>
      <h1 className="font-headline text-2xl font-bold text-ink">Tạo tài khoản</h1>
      <p className="mt-1 text-sm text-ink-secondary">Tham gia Cartzilla ngay hôm nay.</p>

      {formError && <div className="mt-4"><InlineMessage>{formError}</InlineMessage></div>}

      <form onSubmit={handleSubmit(onSubmit)} className="mt-6 space-y-4">
        <Input label="Họ và tên" placeholder="Nguyễn Văn A" error={errors.fullName?.message} {...register('fullName')} />
        <Input label="Email" type="email" placeholder="ban@email.com" error={errors.email?.message} {...register('email')} />
        <div>
          <Input
            label="Mật khẩu"
            type="password"
            placeholder="••••••••"
            error={errors.password?.message}
            helper="Tối thiểu 8 ký tự, gồm chữ và số"
            {...register('password')}
          />
          {pw && (
            <div className="mt-2 flex items-center gap-2">
              <div className="flex h-1.5 flex-1 gap-1">
                {[1, 2, 3].map((i) => (
                  <div key={i} className={`h-full flex-1 rounded-full ${i <= s.score ? s.color : 'bg-slate-200'}`} />
                ))}
              </div>
              <span className="text-xs text-ink-muted">{s.label}</span>
            </div>
          )}
        </div>
        <Input label="Xác nhận mật khẩu" type="password" placeholder="••••••••" error={errors.confirm?.message} {...register('confirm')} />
        <div>
          <label className="flex items-start gap-2 text-sm text-ink-secondary">
            <input type="checkbox" className="mt-0.5 h-4 w-4 rounded border-border text-brand" {...register('agree')} />
            <span>
              Tôi đồng ý với <a href="#" className="text-brand hover:underline">Điều khoản</a> &{' '}
              <a href="#" className="text-brand hover:underline">Chính sách bảo mật</a>
            </span>
          </label>
          {errors.agree && <p className="mt-1 text-xs text-danger">{errors.agree.message as string}</p>}
        </div>
        <Button type="submit" fullWidth loading={isSubmitting} size="lg">
          Đăng ký
        </Button>
      </form>

      <p className="mt-6 text-center text-sm text-ink-secondary">
        Đã có tài khoản?{' '}
        <Link to="/login" className="font-medium text-brand hover:underline">
          Đăng nhập
        </Link>
      </p>
    </AuthLayout>
  );
}
