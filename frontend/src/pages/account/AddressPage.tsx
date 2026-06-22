import { useState } from 'react';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { Button } from '@/components/ui/Button';
import { Input } from '@/components/ui/Input';
import { Modal, ConfirmDialog } from '@/components/ui/Modal';
import { Badge } from '@/components/ui/StatusChip';
import { EmptyState } from '@/components/ui/States';
import { Skeleton } from '@/components/ui/Skeleton';
import { Icon } from '@/components/ui/Icon';
import { useToast } from '@/components/ui/Toast';
import { userApi } from '@/services/user';
import { ApiError } from '@/types/api';
import type { Address } from '@/types/user';

const schema = z.object({
  fullName: z.string().min(1, 'Nhập họ tên'),
  phone: z.string().min(1, 'Nhập số điện thoại'),
  street: z.string().min(1, 'Nhập địa chỉ'),
  district: z.string().min(1, 'Nhập quận/huyện'),
  city: z.string().min(1, 'Nhập tỉnh/thành phố'),
  defaultAddress: z.boolean(),
});
type FormValues = z.infer<typeof schema>;

export function AddressPage() {
  const toast = useToast();
  const qc = useQueryClient();
  const { data: addresses, isLoading } = useQuery({ queryKey: ['addresses'], queryFn: userApi.listAddresses });
  const [editing, setEditing] = useState<Address | null>(null);
  const [modalOpen, setModalOpen] = useState(false);
  const [deleteTarget, setDeleteTarget] = useState<Address | null>(null);

  const form = useForm<FormValues>({ resolver: zodResolver(schema) });
  const invalidate = () => qc.invalidateQueries({ queryKey: ['addresses'] });

  const saveMut = useMutation({
    mutationFn: (data: FormValues) =>
      editing ? userApi.updateAddress(editing.id, data) : userApi.createAddress(data),
    onSuccess: () => {
      invalidate();
      toast.success(editing ? 'Đã cập nhật địa chỉ' : 'Đã thêm địa chỉ');
      setModalOpen(false);
    },
    onError: (e) => toast.error(e instanceof ApiError ? e.message : 'Lưu thất bại'),
  });

  const setDefaultMut = useMutation({
    mutationFn: (id: string) => userApi.setDefaultAddress(id),
    onSuccess: () => { invalidate(); toast.success('Đã đặt làm mặc định'); },
    onError: (e) => toast.error(e instanceof ApiError ? e.message : 'Thất bại'),
  });

  const deleteMut = useMutation({
    mutationFn: (id: string) => userApi.deleteAddress(id),
    onSuccess: () => { invalidate(); toast.success('Đã xóa địa chỉ'); setDeleteTarget(null); },
    onError: (e) => {
      toast.error(e instanceof ApiError ? e.message : 'Xóa thất bại');
      setDeleteTarget(null);
    },
  });

  const openAdd = () => {
    setEditing(null);
    form.reset({ fullName: '', phone: '', street: '', district: '', city: '', defaultAddress: !addresses?.length });
    setModalOpen(true);
  };
  const openEdit = (a: Address) => {
    setEditing(a);
    form.reset({ ...a });
    setModalOpen(true);
  };

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <h1 className="font-headline text-2xl font-bold text-ink">Địa chỉ giao hàng</h1>
        <Button leadingIcon="add" onClick={openAdd}>Thêm địa chỉ</Button>
      </div>

      {isLoading ? (
        <div className="grid gap-4 sm:grid-cols-2">
          <Skeleton className="h-40" />
          <Skeleton className="h-40" />
        </div>
      ) : !addresses?.length ? (
        <EmptyState icon="location_off" title="Bạn chưa có địa chỉ nào" subtitle="Thêm địa chỉ để thuận tiện khi thanh toán." action={{ label: 'Thêm địa chỉ', onClick: openAdd }} />
      ) : (
        <div className="grid gap-4 sm:grid-cols-2">
          {addresses.map((a) => (
            <div key={a.id} className="rounded-lg border border-border bg-white p-4">
              <div className="flex items-start justify-between">
                <div>
                  <p className="font-semibold text-ink">{a.fullName}</p>
                  <p className="text-sm text-ink-secondary">{a.phone}</p>
                </div>
                {a.defaultAddress && <Badge tone="indigo">Mặc định</Badge>}
              </div>
              <p className="mt-2 text-sm text-ink-secondary">
                {a.street}, {a.district}, {a.city}
              </p>
              <div className="mt-4 flex gap-3 border-t border-divider pt-3 text-sm">
                <button onClick={() => openEdit(a)} className="font-medium text-brand hover:underline">Sửa</button>
                {!a.defaultAddress && (
                  <button onClick={() => setDefaultMut.mutate(a.id)} className="font-medium text-ink-secondary hover:text-ink">
                    Đặt làm mặc định
                  </button>
                )}
                <button onClick={() => setDeleteTarget(a)} className="ml-auto font-medium text-danger hover:underline">Xóa</button>
              </div>
            </div>
          ))}
        </div>
      )}

      <Modal
        open={modalOpen}
        onClose={() => setModalOpen(false)}
        title={editing ? 'Sửa địa chỉ' : 'Thêm địa chỉ'}
      >
        <form onSubmit={form.handleSubmit((d) => saveMut.mutate(d))} className="space-y-4">
          <Input label="Họ và tên" error={form.formState.errors.fullName?.message} {...form.register('fullName')} />
          <Input label="Số điện thoại" error={form.formState.errors.phone?.message} {...form.register('phone')} />
          <Input label="Địa chỉ (đường/số nhà)" error={form.formState.errors.street?.message} {...form.register('street')} />
          <div className="grid grid-cols-2 gap-3">
            <Input label="Quận/Huyện" error={form.formState.errors.district?.message} {...form.register('district')} />
            <Input label="Tỉnh/Thành phố" error={form.formState.errors.city?.message} {...form.register('city')} />
          </div>
          <label className="flex items-center gap-2 text-sm text-ink-secondary">
            <input type="checkbox" className="h-4 w-4 rounded border-border text-brand" {...form.register('defaultAddress')} />
            Đặt làm địa chỉ mặc định
          </label>
          <div className="flex justify-end gap-2 pt-2">
            <Button type="button" variant="secondary" onClick={() => setModalOpen(false)}>Hủy</Button>
            <Button type="submit" loading={saveMut.isPending}>Lưu</Button>
          </div>
        </form>
      </Modal>

      <ConfirmDialog
        open={!!deleteTarget}
        onClose={() => setDeleteTarget(null)}
        onConfirm={() => deleteTarget && deleteMut.mutate(deleteTarget.id)}
        title="Xóa địa chỉ này?"
        message={
          deleteTarget?.defaultAddress
            ? 'Đây là địa chỉ mặc định. Hãy chọn địa chỉ mặc định khác trước khi xóa.'
            : 'Bạn có chắc muốn xóa địa chỉ này?'
        }
        confirmLabel="Xóa"
        danger
        loading={deleteMut.isPending}
      />

      <p className="flex items-center gap-1 text-xs text-ink-muted">
        <Icon name="info" className="text-[16px]" />
        Địa chỉ đầu tiên tự động là mặc định. Mỗi tài khoản chỉ có một địa chỉ mặc định.
      </p>
    </div>
  );
}
