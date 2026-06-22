import { useState } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { adminVoucherApi } from '@/services/admin';
import { Button } from '@/components/ui/Button';
import { Input, Select } from '@/components/ui/Input';
import { Badge } from '@/components/ui/StatusChip';
import { Modal, ConfirmDialog } from '@/components/ui/Modal';
import { EmptyState } from '@/components/ui/States';
import { Skeleton } from '@/components/ui/Skeleton';
import { Icon } from '@/components/ui/Icon';
import { useToast } from '@/components/ui/Toast';
import { formatVnd, formatDate } from '@/lib/format';
import { ApiError, type AudienceType, type DiscountType } from '@/types/api';
import type { Voucher, VoucherInput } from '@/types/admin';

const AUDIENCE_LABEL: Record<AudienceType, string> = {
  ALL_USERS: 'Tất cả',
  NEW_CUSTOMER: 'Khách mới',
  LOYAL_CUSTOMER: 'Khách thân thiết',
  SPECIFIC_USERS: 'Chỉ định',
};

function toInputDate(iso: string | null): string {
  if (!iso) return '';
  return iso.slice(0, 16); // yyyy-MM-ddTHH:mm
}
function toIso(local: string): string | null {
  return local ? new Date(local).toISOString() : null;
}

const emptyForm: VoucherInput = {
  code: '', discountType: 'PERCENTAGE', discountValue: 10, maxDiscountAmount: 150000, minOrderAmount: 0,
  maxUses: 100, minAccountAgeDays: 0, perUserLimit: 1, audienceType: 'ALL_USERS',
  firstOrderOnly: false, minCompletedOrders: 0, minTotalSpent: 0, startsAt: null, expiresAt: null, active: true,
};

export function AdminVoucherPage() {
  const qc = useQueryClient();
  const toast = useToast();
  const { data: vouchers, isLoading } = useQuery({ queryKey: ['admin-vouchers'], queryFn: adminVoucherApi.list });

  const [open, setOpen] = useState(false);
  const [editing, setEditing] = useState<Voucher | null>(null);
  const [form, setForm] = useState<VoucherInput>(emptyForm);
  const [startsLocal, setStartsLocal] = useState('');
  const [expiresLocal, setExpiresLocal] = useState('');
  const [deleteTarget, setDeleteTarget] = useState<Voucher | null>(null);

  const invalidate = () => qc.invalidateQueries({ queryKey: ['admin-vouchers'] });

  const saveMut = useMutation({
    mutationFn: () => {
      const payload: VoucherInput = { ...form, startsAt: toIso(startsLocal), expiresAt: toIso(expiresLocal) };
      return editing ? adminVoucherApi.update(editing.id, payload) : adminVoucherApi.create(payload);
    },
    onSuccess: () => { invalidate(); toast.success(editing ? 'Đã cập nhật voucher' : 'Đã tạo voucher'); setOpen(false); },
    onError: (e) => toast.error(e instanceof ApiError ? e.message : 'Lưu thất bại'),
  });
  const deleteMut = useMutation({
    mutationFn: (id: string) => adminVoucherApi.remove(id),
    onSuccess: () => { invalidate(); toast.success('Đã xóa voucher'); setDeleteTarget(null); },
    onError: (e) => { toast.error(e instanceof ApiError ? e.message : 'Xóa thất bại'); setDeleteTarget(null); },
  });

  const openAdd = () => { setEditing(null); setForm(emptyForm); setStartsLocal(''); setExpiresLocal(''); setOpen(true); };
  const openEdit = (v: Voucher) => {
    setEditing(v);
    setForm({ ...v });
    setStartsLocal(toInputDate(v.startsAt));
    setExpiresLocal(toInputDate(v.expiresAt));
    setOpen(true);
  };

  const set = (patch: Partial<VoucherInput>) => setForm((f) => ({ ...f, ...patch }));

  return (
    <div className="space-y-5">
      <div className="flex items-center justify-between">
        <h1 className="font-headline text-2xl font-bold text-ink">Mã giảm giá</h1>
        <Button leadingIcon="add" onClick={openAdd}>Tạo voucher</Button>
      </div>

      <div className="overflow-hidden rounded-lg border border-border bg-white">
        <div className="overflow-x-auto">
          <table className="w-full text-sm">
            <thead>
              <tr className="border-b border-divider bg-page text-left text-xs uppercase text-ink-muted">
                <th className="px-4 py-3 font-semibold">Code</th>
                <th className="px-4 py-3 font-semibold">Giảm</th>
                <th className="px-4 py-3 font-semibold">Đã dùng</th>
                <th className="px-4 py-3 font-semibold">Hiệu lực</th>
                <th className="px-4 py-3 font-semibold">Đối tượng</th>
                <th className="px-4 py-3 font-semibold">Trạng thái</th>
                <th className="px-4 py-3" />
              </tr>
            </thead>
            <tbody>
              {isLoading ? (
                <tr><td colSpan={7} className="px-4 py-3"><Skeleton className="h-8 w-full" /></td></tr>
              ) : !vouchers?.length ? (
                <tr><td colSpan={7} className="px-4 py-10"><EmptyState icon="sell" title="Chưa có voucher" action={{ label: 'Tạo voucher', onClick: openAdd }} /></td></tr>
              ) : (
                vouchers.map((v) => (
                  <tr key={v.id} className="border-b border-divider last:border-0 hover:bg-page">
                    <td className="px-4 py-2 font-mono font-semibold text-ink">{v.code}</td>
                    <td className="px-4 py-2">{v.discountType === 'PERCENTAGE' ? `${v.discountValue}%` : formatVnd(v.discountValue)}</td>
                    <td className="px-4 py-2">
                      <div className="flex items-center gap-2">
                        <span className="tabular-nums text-ink-secondary">{v.usedCount}/{v.maxUses}</span>
                        <div className="h-1.5 w-16 overflow-hidden rounded-full bg-page">
                          <div className="h-full bg-brand" style={{ width: `${Math.min(100, (v.usedCount / v.maxUses) * 100)}%` }} />
                        </div>
                      </div>
                    </td>
                    <td className="px-4 py-2 text-xs text-ink-secondary">
                      {v.startsAt ? formatDate(v.startsAt) : '—'} → {v.expiresAt ? formatDate(v.expiresAt) : '∞'}
                    </td>
                    <td className="px-4 py-2"><Badge tone="indigo">{AUDIENCE_LABEL[v.audienceType]}</Badge></td>
                    <td className="px-4 py-2">{v.active ? <Badge tone="success">Đang chạy</Badge> : <Badge tone="muted">Tạm dừng</Badge>}</td>
                    <td className="px-4 py-2">
                      <div className="flex justify-end gap-1">
                        <button onClick={() => openEdit(v)} className="rounded p-1.5 text-ink-secondary hover:text-brand"><Icon name="edit" className="text-[18px]" /></button>
                        <button onClick={() => setDeleteTarget(v)} className="rounded p-1.5 text-ink-secondary hover:text-danger"><Icon name="delete" className="text-[18px]" /></button>
                      </div>
                    </td>
                  </tr>
                ))
              )}
            </tbody>
          </table>
        </div>
      </div>

      <Modal open={open} onClose={() => setOpen(false)} title={editing ? 'Sửa voucher' : 'Tạo voucher'} size="lg">
        <form onSubmit={(e) => { e.preventDefault(); saveMut.mutate(); }} className="space-y-4">
          <div className="grid grid-cols-2 gap-3">
            <Input label="Code" required value={form.code ?? ''} disabled={!!editing && (editing.usedCount > 0)} onChange={(e) => set({ code: e.target.value.toUpperCase() })} helper={editing && editing.usedCount > 0 ? 'Không thể đổi code đã phát sinh lượt dùng' : undefined} />
            <Select label="Loại giảm" value={form.discountType} onChange={(e) => set({ discountType: e.target.value as DiscountType })}>
              <option value="PERCENTAGE">Phần trăm (%)</option>
              <option value="FIXED_AMOUNT">Số tiền (₫)</option>
            </Select>
          </div>
          <div className="grid grid-cols-2 gap-3">
            <Input label={form.discountType === 'PERCENTAGE' ? 'Giá trị (%)' : 'Giá trị (₫)'} type="number" value={form.discountValue} onChange={(e) => set({ discountValue: Number(e.target.value) })} />
            <Input label="Giảm tối đa (₫)" type="number" value={form.maxDiscountAmount ?? 0} onChange={(e) => set({ maxDiscountAmount: Number(e.target.value) })} helper={form.discountType === 'PERCENTAGE' ? 'Bắt buộc với loại %' : undefined} />
          </div>
          <div className="grid grid-cols-3 gap-3">
            <Input label="Đơn tối thiểu (₫)" type="number" value={form.minOrderAmount ?? 0} onChange={(e) => set({ minOrderAmount: Number(e.target.value) })} />
            <Input label="Tổng lượt" type="number" value={form.maxUses} onChange={(e) => set({ maxUses: Number(e.target.value) })} />
            <Input label="Mỗi người" type="number" value={form.perUserLimit} onChange={(e) => set({ perUserLimit: Number(e.target.value) })} />
          </div>
          <div className="grid grid-cols-2 gap-3">
            <Input label="Tuổi TK tối thiểu (ngày)" type="number" value={form.minAccountAgeDays} onChange={(e) => set({ minAccountAgeDays: Number(e.target.value) })} />
            <Select label="Đối tượng" value={form.audienceType} onChange={(e) => set({ audienceType: e.target.value as AudienceType })}>
              <option value="ALL_USERS">Tất cả</option>
              <option value="NEW_CUSTOMER">Khách mới</option>
              <option value="LOYAL_CUSTOMER">Khách thân thiết</option>
              <option value="SPECIFIC_USERS">Chỉ định</option>
            </Select>
          </div>
          <div className="grid grid-cols-2 gap-3">
            <Input label="Từ ngày" type="datetime-local" value={startsLocal} onChange={(e) => setStartsLocal(e.target.value)} />
            <Input label="Đến ngày" type="datetime-local" value={expiresLocal} onChange={(e) => setExpiresLocal(e.target.value)} />
          </div>
          <div className="rounded bg-page p-3 text-sm text-ink-secondary">
            {form.discountType === 'PERCENTAGE'
              ? `Giảm ${form.discountValue}% tối đa ${formatVnd(form.maxDiscountAmount ?? 0)} cho đơn từ ${formatVnd(form.minOrderAmount ?? 0)}`
              : `Giảm ${formatVnd(form.discountValue)} cho đơn từ ${formatVnd(form.minOrderAmount ?? 0)}`}
          </div>
          <label className="flex items-center gap-2 text-sm text-ink-secondary">
            <input type="checkbox" className="h-4 w-4 rounded border-border text-brand" checked={form.active ?? true} onChange={(e) => set({ active: e.target.checked })} />
            Đang chạy
          </label>
          <div className="flex justify-end gap-2">
            <Button type="button" variant="secondary" onClick={() => setOpen(false)}>Hủy</Button>
            <Button type="submit" loading={saveMut.isPending}>Lưu</Button>
          </div>
        </form>
      </Modal>

      <ConfirmDialog
        open={!!deleteTarget}
        onClose={() => setDeleteTarget(null)}
        onConfirm={() => deleteTarget && deleteMut.mutate(deleteTarget.id)}
        title="Xóa voucher này?"
        message={`Voucher "${deleteTarget?.code}" sẽ bị xóa.`}
        confirmLabel="Xóa"
        danger
        loading={deleteMut.isPending}
      />
    </div>
  );
}
