import { useEffect } from 'react';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { Card } from '@/components/ui/Card';
import { Input } from '@/components/ui/Input';
import { Button } from '@/components/ui/Button';
import { Badge } from '@/components/ui/StatusChip';
import { Skeleton } from '@/components/ui/Skeleton';
import { useToast } from '@/components/ui/Toast';
import { userApi } from '@/services/user';
import { authApi } from '@/services/auth';
import { ApiError } from '@/types/api';

const profileSchema = z.object({
  fullName: z.string().min(1, 'Vui lòng nhập họ tên'),
  phone: z.string().max(20).optional().or(z.literal('')),
});
type ProfileForm = z.infer<typeof profileSchema>;

const passwordSchema = z
  .object({
    currentPassword: z.string().min(1, 'Nhập mật khẩu hiện tại'),
    newPassword: z.string().min(6, 'Tối thiểu 6 ký tự'),
    confirm: z.string(),
  })
  .refine((d) => d.newPassword === d.confirm, { path: ['confirm'], message: 'Xác nhận không khớp' });
type PasswordForm = z.infer<typeof passwordSchema>;

export function ProfilePage() {
  const toast = useToast();
  const qc = useQueryClient();
  const { data: profile, isLoading } = useQuery({ queryKey: ['profile'], queryFn: userApi.getProfile });

  const profileForm = useForm<ProfileForm>({ resolver: zodResolver(profileSchema) });
  const passwordForm = useForm<PasswordForm>({ resolver: zodResolver(passwordSchema) });

  useEffect(() => {
    if (profile) profileForm.reset({ fullName: profile.fullName, phone: profile.phone ?? '' });
  }, [profile]); // eslint-disable-line react-hooks/exhaustive-deps

  const updateMut = useMutation({
    mutationFn: (data: ProfileForm) =>
      userApi.updateProfile({ fullName: data.fullName, phone: data.phone ?? '' }),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['profile'] });
      toast.success('Đã cập nhật hồ sơ');
    },
    onError: (e) => toast.error(e instanceof ApiError ? e.message : 'Cập nhật thất bại'),
  });

  const passwordMut = useMutation({
    mutationFn: (data: PasswordForm) => userApi.changePassword(data.currentPassword, data.newPassword),
    onSuccess: () => {
      toast.success('Đã đổi mật khẩu');
      passwordForm.reset({ currentPassword: '', newPassword: '', confirm: '' });
    },
    onError: (e) => toast.error(e instanceof ApiError ? e.message : 'Đổi mật khẩu thất bại'),
  });

  const resendVerify = async () => {
    if (!profile) return;
    try {
      await authApi.resendVerification(profile.email);
      toast.success('Đã gửi lại email xác minh');
    } catch {
      toast.error('Không gửi lại được email');
    }
  };

  if (isLoading) {
    return <Skeleton className="h-96 w-full" />;
  }

  return (
    <div className="space-y-6">
      <h1 className="font-headline text-2xl font-bold text-ink">Hồ sơ</h1>

      <Card title="Thông tin cá nhân">
        <form onSubmit={profileForm.handleSubmit((d) => updateMut.mutate(d))} className="space-y-4">
          <div className="flex items-center gap-4">
            <span className="flex h-16 w-16 items-center justify-center rounded-full bg-brand-tint text-2xl font-bold text-brand">
              {profile?.fullName?.[0]?.toUpperCase() ?? profile?.email[0].toUpperCase()}
            </span>
          </div>
          <Input label="Họ và tên" error={profileForm.formState.errors.fullName?.message} {...profileForm.register('fullName')} />
          <div>
            <label className="mb-1.5 block text-sm font-medium text-ink">Email</label>
            <div className="flex items-center gap-2">
              <input value={profile?.email ?? ''} disabled className="h-11 flex-1 rounded border border-border bg-page px-3 text-sm text-ink-muted" />
              {profile?.emailVerified ? (
                <Badge tone="success">Đã xác minh</Badge>
              ) : (
                <button type="button" onClick={resendVerify} className="shrink-0">
                  <Badge tone="warning">Chưa xác minh · Gửi lại</Badge>
                </button>
              )}
            </div>
          </div>
          <Input label="Số điện thoại" placeholder="09xxxxxxxx" error={profileForm.formState.errors.phone?.message} {...profileForm.register('phone')} />
          <div className="flex gap-2">
            <Button type="submit" loading={updateMut.isPending}>Lưu thay đổi</Button>
            <Button type="button" variant="ghost" onClick={() => profile && profileForm.reset({ fullName: profile.fullName, phone: profile.phone ?? '' })}>
              Hủy
            </Button>
          </div>
        </form>
      </Card>

      <Card title="Đổi mật khẩu">
        <form onSubmit={passwordForm.handleSubmit((d) => passwordMut.mutate(d))} className="space-y-4">
          <Input label="Mật khẩu hiện tại" type="password" error={passwordForm.formState.errors.currentPassword?.message} {...passwordForm.register('currentPassword')} />
          <Input label="Mật khẩu mới" type="password" error={passwordForm.formState.errors.newPassword?.message} {...passwordForm.register('newPassword')} />
          <Input label="Xác nhận mật khẩu mới" type="password" error={passwordForm.formState.errors.confirm?.message} {...passwordForm.register('confirm')} />
          <p className="text-xs text-ink-muted">Đổi mật khẩu sẽ đăng xuất các thiết bị khác.</p>
          <Button type="submit" loading={passwordMut.isPending}>Cập nhật mật khẩu</Button>
        </form>
      </Card>
    </div>
  );
}
