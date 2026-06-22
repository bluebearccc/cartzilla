import { useState } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { adminVendorApi } from '@/services/admin';
import { Button } from '@/components/ui/Button';
import { Input, Select } from '@/components/ui/Input';
import { Badge } from '@/components/ui/StatusChip';
import { Modal, ConfirmDialog } from '@/components/ui/Modal';
import { EmptyState } from '@/components/ui/States';
import { Skeleton } from '@/components/ui/Skeleton';
import { Icon } from '@/components/ui/Icon';
import { useToast } from '@/components/ui/Toast';
import { ApiError, type VendorType } from '@/types/api';
import type { Vendor } from '@/types/catalog';
import type { VendorInput } from '@/types/admin';

const TYPE_LABEL: Record<VendorType, string> = {
  SUPPLIER: 'Nhà cung cấp',
  BRAND: 'Thương hiệu',
  MANUFACTURER: 'Nhà sản xuất',
};

const emptyForm: VendorInput = { name: '', slug: '', vendorType: 'BRAND', contactEmail: '', phone: '', website: '', logoUrl: '', active: true };

export function AdminVendorPage() {
  const qc = useQueryClient();
  const toast = useToast();
  const { data: vendors, isLoading } = useQuery({ queryKey: ['admin-vendors'], queryFn: adminVendorApi.list });

  const [open, setOpen] = useState(false);
  const [editing, setEditing] = useState<Vendor | null>(null);
  const [form, setForm] = useState<VendorInput>(emptyForm);
  const [deleteTarget, setDeleteTarget] = useState<Vendor | null>(null);

  const invalidate = () => qc.invalidateQueries({ queryKey: ['admin-vendors'] });

  const saveMut = useMutation({
    mutationFn: () => editing ? adminVendorApi.update(editing.id, form) : adminVendorApi.create(form),
    onSuccess: () => { invalidate(); toast.success(editing ? 'Đã cập nhật' : 'Đã thêm vendor'); setOpen(false); },
    onError: (e) => toast.error(e instanceof ApiError ? e.message : 'Lưu thất bại'),
  });
  const deleteMut = useMutation({
    mutationFn: (id: string) => adminVendorApi.remove(id),
    onSuccess: () => { invalidate(); toast.success('Đã ẩn vendor'); setDeleteTarget(null); },
    onError: (e) => { toast.error(e instanceof ApiError ? e.message : 'Xóa thất bại'); setDeleteTarget(null); },
  });

  const openAdd = () => { setEditing(null); setForm(emptyForm); setOpen(true); };
  const openEdit = (v: Vendor) => {
    setEditing(v);
    setForm({ name: v.name, slug: v.slug, vendorType: v.vendorType, contactEmail: v.contactEmail ?? '', phone: v.phone ?? '', website: v.website ?? '', logoUrl: v.logoUrl ?? '', active: v.active });
    setOpen(true);
  };

  return (
    <div className="space-y-5">
      <div className="flex items-center justify-between">
        <h1 className="font-headline text-2xl font-bold text-ink">Nhà cung cấp / Thương hiệu</h1>
        <Button leadingIcon="add" onClick={openAdd}>Thêm vendor</Button>
      </div>

      <div className="overflow-hidden rounded-lg border border-border bg-white">
        <table className="w-full text-sm">
          <thead>
            <tr className="border-b border-divider bg-page text-left text-xs uppercase text-ink-muted">
              <th className="px-4 py-3 font-semibold">Tên</th>
              <th className="px-4 py-3 font-semibold">Loại</th>
              <th className="px-4 py-3 font-semibold">Slug</th>
              <th className="px-4 py-3 font-semibold">Trạng thái</th>
              <th className="px-4 py-3" />
            </tr>
          </thead>
          <tbody>
            {isLoading ? (
              <tr><td colSpan={5} className="px-4 py-3"><Skeleton className="h-8 w-full" /></td></tr>
            ) : !vendors?.length ? (
              <tr><td colSpan={5} className="px-4 py-10"><EmptyState icon="store" title="Chưa có vendor" action={{ label: 'Thêm vendor', onClick: openAdd }} /></td></tr>
            ) : (
              vendors.map((v) => (
                <tr key={v.id} className="border-b border-divider last:border-0 hover:bg-page">
                  <td className="px-4 py-2">
                    <div className="flex items-center gap-2">
                      {v.logoUrl ? <img src={v.logoUrl} alt="" className="h-8 w-8 rounded object-cover" /> : <span className="flex h-8 w-8 items-center justify-center rounded bg-brand-tint text-brand"><Icon name="store" className="text-[18px]" /></span>}
                      <span className="font-medium text-ink">{v.name}</span>
                    </div>
                  </td>
                  <td className="px-4 py-2"><Badge tone="indigo">{TYPE_LABEL[v.vendorType]}</Badge></td>
                  <td className="px-4 py-2 text-ink-secondary">{v.slug}</td>
                  <td className="px-4 py-2">{v.active ? <Badge tone="success">Đang hoạt động</Badge> : <Badge tone="muted">Ẩn</Badge>}</td>
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

      <Modal open={open} onClose={() => setOpen(false)} title={editing ? 'Sửa vendor' : 'Thêm vendor'}>
        <form onSubmit={(e) => { e.preventDefault(); saveMut.mutate(); }} className="space-y-4">
          <Input label="Tên" required value={form.name} onChange={(e) => setForm((f) => ({ ...f, name: e.target.value }))} />
          <Input label="Slug" value={form.slug ?? ''} onChange={(e) => setForm((f) => ({ ...f, slug: e.target.value }))} />
          <Select label="Loại" value={form.vendorType} onChange={(e) => setForm((f) => ({ ...f, vendorType: e.target.value as VendorType }))}>
            <option value="SUPPLIER">Nhà cung cấp</option>
            <option value="BRAND">Thương hiệu</option>
            <option value="MANUFACTURER">Nhà sản xuất</option>
          </Select>
          <Input label="Email liên hệ" value={form.contactEmail ?? ''} onChange={(e) => setForm((f) => ({ ...f, contactEmail: e.target.value }))} />
          <Input label="Logo URL" value={form.logoUrl ?? ''} onChange={(e) => setForm((f) => ({ ...f, logoUrl: e.target.value }))} />
          <label className="flex items-center gap-2 text-sm text-ink-secondary">
            <input type="checkbox" className="h-4 w-4 rounded border-border text-brand" checked={form.active ?? true} onChange={(e) => setForm((f) => ({ ...f, active: e.target.checked }))} />
            Đang hoạt động
          </label>
          <p className="text-xs text-ink-muted">Vendor đang ẩn không thể gán cho sản phẩm mới.</p>
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
        title="Ẩn vendor này?"
        message={`"${deleteTarget?.name}" sẽ được ẩn khỏi danh sách.`}
        confirmLabel="Ẩn"
        danger
        loading={deleteMut.isPending}
      />
    </div>
  );
}
