import { useState } from 'react';
import { Link } from 'react-router-dom';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';
import { CenteredAuthLayout } from '@/components/layout/AuthLayout';
import { Input } from '@/components/ui/Input';
import { Button } from '@/components/ui/Button';
import { Icon } from '@/components/ui/Icon';
import { authApi } from '@/services/auth';

const schema = z.object({
  email: z.string().min(1, 'Vui lòng nhập email').email('Email không hợp lệ'),
});
type FormValues = z.infer<typeof schema>;

export function ForgotPasswordPage() {
  const [sent, setSent] = useState(false);
  const { register, handleSubmit, formState: { errors, isSubmitting } } = useForm<FormValues>({
    resolver: zodResolver(schema),
  });

  const onSubmit = async (values: FormValues) => {
    // Always show success to avoid user enumeration (backend queues if exists).
    try {
      await authApi.forgotPassword(values.email);
    } finally {
      setSent(true);
    }
  };

  if (sent) {
    return (
      <CenteredAuthLayout>
        <div className="text-center">
          <span className="mx-auto mb-4 flex h-16 w-16 items-center justify-center rounded-full bg-brand-tint text-brand">
            <Icon name="mark_email_unread" className="text-[32px]" />
          </span>
          <h1 className="font-headline text-2xl font-bold text-ink">Đã gửi email!</h1>
          <p className="mt-2 text-sm text-ink-secondary">
            Nếu email tồn tại, bạn sẽ nhận được liên kết đặt lại trong vài phút. Liên kết có hiệu lực
            30 phút.
          </p>
          <a href="https://mail.google.com" target="_blank" rel="noreferrer" className="mt-6 block">
            <Button fullWidth>Mở email</Button>
          </a>
          <Link to="/login" className="mt-4 inline-block text-sm font-medium text-brand hover:underline">
            ← Quay lại đăng nhập
          </Link>
        </div>
      </CenteredAuthLayout>
    );
  }

  return (
    <CenteredAuthLayout>
      <h1 className="font-headline text-2xl font-bold text-ink">Quên mật khẩu</h1>
      <p className="mt-1 text-sm text-ink-secondary">Nhập email để nhận liên kết đặt lại mật khẩu.</p>
      <form onSubmit={handleSubmit(onSubmit)} className="mt-6 space-y-4">
        <Input label="Email" type="email" placeholder="ban@email.com" error={errors.email?.message} {...register('email')} />
        <Button type="submit" fullWidth loading={isSubmitting} size="lg">
          Gửi liên kết
        </Button>
      </form>
      <Link to="/login" className="mt-5 inline-flex items-center gap-1 text-sm text-ink-muted hover:text-ink">
        <Icon name="arrow_back" className="text-[16px]" /> Quay lại đăng nhập
      </Link>
    </CenteredAuthLayout>
  );
}
