import { useState } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { adminCategoryApi } from '@/services/admin';
import { Card } from '@/components/ui/Card';
import { Input, Select } from '@/components/ui/Input';
import { Button } from '@/components/ui/Button';
import { Badge } from '@/components/ui/StatusChip';
import { ConfirmDialog } from '@/components/ui/Modal';
import { Icon } from '@/components/ui/Icon';
import { Skeleton } from '@/components/ui/Skeleton';
import { useToast } from '@/components/ui/Toast';
import { ApiError } from '@/types/api';
import type { Category } from '@/types/catalog';
import type { CategoryInput } from '@/types/admin';

export function AdminCategoryPage() {
  const qc = useQueryClient();
  const toast = useToast();
  const { data: tree, isLoading } = useQuery({ queryKey: ['admin-categories'], queryFn: adminCategoryApi.list });

  const [editing, setEditing] = useState<Category | null>(null);
  const [form, setForm] = useState<CategoryInput>({ name: '', slug: '', parentId: null, sortOrder: 0, active: true });
  const [deleteTarget, setDeleteTarget] = useState<Category | null>(null);

  const flat = (cats: Category[] | undefined): Category[] =>
    (cats ?? []).flatMap((c) => [c, ...flat(c.children)]);
  const allFlat = flat(tree);

  const invalidate = () => qc.invalidateQueries({ queryKey: ['admin-categories'] });
  const reset = () => { setEditing(null); setForm({ name: '', slug: '', parentId: null, sortOrder: 0, active: true }); };

  const saveMut = useMutation({
    mutationFn: () => editing ? adminCategoryApi.update(editing.id, form) : adminCategoryApi.create(form),
    onSuccess: () => { invalidate(); toast.success(editing ? 'Đã cập nhật danh mục' : 'Đã thêm danh mục'); reset(); },
    onError: (e) => toast.error(e instanceof ApiError ? e.message : 'Lưu thất bại'),
  });
  const deleteMut = useMutation({
    mutationFn: (id: string) => adminCategoryApi.remove(id),
    onSuccess: () => { invalidate(); toast.success('Đã ẩn danh mục'); setDeleteTarget(null); },
    onError: (e) => { toast.error(e instanceof ApiError ? e.message : 'Không thể ẩn danh mục đang có sản phẩm đang bán'); setDeleteTarget(null); },
  });

  const startEdit = (c: Category) => {
    setEditing(c);
    setForm({ name: c.name, slug: c.slug, parentId: c.parentId, imageUrl: c.imageUrl, sortOrder: c.sortOrder, active: c.active });
  };

  const renderNode = (c: Category, depth: number) => (
    <div key={c.id}>
      <div className="flex items-center justify-between border-b border-divider px-3 py-2.5 hover:bg-page" style={{ paddingLeft: 12 + depth * 20 }}>
        <div className="flex items-center gap-2">
          {depth > 0 && <Icon name="subdirectory_arrow_right" className="text-[16px] text-ink-muted" />}
          <span className="font-medium text-ink">{c.name}</span>
          <span className="text-xs text-ink-muted">{c.slug}</span>
          {!c.active && <Badge tone="muted">Ẩn</Badge>}
        </div>
        <div className="flex gap-1">
          <button onClick={() => startEdit(c)} className="rounded p-1.5 text-ink-secondary hover:text-brand"><Icon name="edit" className="text-[18px]" /></button>
          <button onClick={() => setDeleteTarget(c)} className="rounded p-1.5 text-ink-secondary hover:text-danger"><Icon name="delete" className="text-[18px]" /></button>
        </div>
      </div>
      {c.children?.map((ch) => renderNode(ch, depth + 1))}
    </div>
  );

  return (
    <div className="space-y-5">
      <h1 className="font-headline text-2xl font-bold text-ink">Danh mục</h1>
      <div className="grid gap-5 lg:grid-cols-[1fr_360px]">
        <Card title="Cây danh mục">
          {isLoading ? <Skeleton className="h-40 w-full" /> : !tree?.length ? (
            <p className="py-8 text-center text-ink-muted">Chưa có danh mục</p>
          ) : (
            <div className="-mx-5 -mb-5">{tree.map((c) => renderNode(c, 0))}</div>
          )}
        </Card>

        <Card title={editing ? 'Sửa danh mục' : 'Thêm danh mục'}>
          <form onSubmit={(e) => { e.preventDefault(); saveMut.mutate(); }} className="space-y-4">
            <Input label="Tên danh mục" required value={form.name} onChange={(e) => setForm((f) => ({ ...f, name: e.target.value }))} />
            <Input label="Slug" value={form.slug ?? ''} onChange={(e) => setForm((f) => ({ ...f, slug: e.target.value }))} helper="Để trống sẽ tự sinh" />
            <Select label="Danh mục cha" value={form.parentId ?? ''} onChange={(e) => setForm((f) => ({ ...f, parentId: e.target.value || null }))}>
              <option value="">— Danh mục gốc —</option>
              {allFlat.filter((c) => c.id !== editing?.id).map((c) => <option key={c.id} value={c.id}>{c.name}</option>)}
            </Select>
            <Input label="Thứ tự" type="number" value={form.sortOrder} onChange={(e) => setForm((f) => ({ ...f, sortOrder: Number(e.target.value) }))} />
            <label className="flex items-center gap-2 text-sm text-ink-secondary">
              <input type="checkbox" className="h-4 w-4 rounded border-border text-brand" checked={form.active ?? true} onChange={(e) => setForm((f) => ({ ...f, active: e.target.checked }))} />
              Đang hoạt động
            </label>
            <div className="flex gap-2">
              <Button type="submit" loading={saveMut.isPending}>Lưu</Button>
              {editing && <Button type="button" variant="ghost" onClick={reset}>Hủy</Button>}
            </div>
          </form>
        </Card>
      </div>

      <ConfirmDialog
        open={!!deleteTarget}
        onClose={() => setDeleteTarget(null)}
        onConfirm={() => deleteTarget && deleteMut.mutate(deleteTarget.id)}
        title="Ẩn danh mục này?"
        message="Không thể ẩn danh mục đang có sản phẩm đang bán."
        confirmLabel="Ẩn"
        danger
        loading={deleteMut.isPending}
      />
    </div>
  );
}
