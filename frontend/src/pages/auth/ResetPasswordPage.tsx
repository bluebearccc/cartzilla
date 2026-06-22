import { useState } from 'react';
import { Link, useSearchParams } from 'react-router-dom';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';
import { CenteredAuthLayout } from '@/components/layout/AuthLayout';
import { Input } from '@/components/ui/Input';
import { Button } from '@/components/ui/Button';
import { Icon } from '@/components/ui/Icon';
import { InlineMessage } from '@/components/ui/States';
import { authApi } from '@/services/auth';
import { ApiError } from '@/types/api';

const schema = z
  .object({
    password: z.string().min(8, 'Tối thiểu 8 ký tự').regex(/[0-9]/, 'Cần có chữ số'),
    confirm: z.string(),
  })
  .refine((d) => d.password === d.confirm, { path: ['confirm'], message: 'Mật khẩu xác nhận không khớp' });
type FormValues = z.infer<typeof schema>;

export function ResetPasswordPage() {
  const [params] = useSearchParams();
  const token = params.get('token');
  const [done, setDone] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const { register, handleSubmit, formState: { errors, isSubmitting } } = useForm<FormValues>({
    resolver: zodResolver(schema),
  });

  const onSubmit = async (values: FormValues) => {
    if (!token) return;
    setError(null);
    try {
      await authApi.resetPassword(token, values.password);
      setDone(true);
    } catch (err) {
      setError(err instanceof ApiError ? err.message : 'Đặt lại mật khẩu thất bại');
    }
  };

  if (!token) {
    return (
      <CenteredAuthLayout>
        <InlineMessage>
          Liên kết đặt lại không hợp lệ hoặc đã hết hạn.{' '}
          <Link to="/forgot-password" className="font-medium underline">Yêu cầu liên kết mới</Link>
        </InlineMessage>
      </CenteredAuthLayout>
    );
  }

  if (done) {
    return (
      <CenteredAuthLayout>
        <div className="text-center">
          <span className="mx-auto mb-4 flex h-16 w-16 items-center justify-center rounded-full bg-success-tint text-success">
            <Icon name="check_circle" className="text-[32px]" />
          </span>
          <h1 className="font-headline text-2xl font-bold text-ink">Đổi mật khẩu thành công!</h1>
          <Link to="/login" className="mt-6 block">
            <Button fullWidth>Đăng nhập</Button>
          </Link>
        </div>
      </CenteredAuthLayout>
    );
  }

  return (
    <CenteredAuthLayout>
      <h1 className="font-headline text-2xl font-bold text-ink">Đặt lại mật khẩu</h1>
      {error && <div className="mt-4"><InlineMessage>{error}</InlineMessage></div>}
      <form onSubmit={handleSubmit(onSubmit)} className="mt-6 space-y-4">
        <Input label="Mật khẩu mới" type="password" helper="Tối thiểu 8 ký tự, gồm chữ và số" error={errors.password?.message} {...register('password')} />
        <Input label="Xác nhận mật khẩu mới" type="password" error={errors.confirm?.message} {...register('confirm')} />
        <Button type="submit" fullWidth loading={isSubmitting} size="lg">
          Đặt lại mật khẩu
        </Button>
      </form>
    </CenteredAuthLayout>
  );
}
