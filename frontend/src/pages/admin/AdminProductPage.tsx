import { useState } from 'react';
import { Link } from 'react-router-dom';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { adminProductApi } from '@/services/admin';
import { catalogApi } from '@/services/catalog';
import { Button } from '@/components/ui/Button';
import { Badge } from '@/components/ui/StatusChip';
import { Pagination } from '@/components/ui/Pagination';
import { ConfirmDialog } from '@/components/ui/Modal';
import { EmptyState } from '@/components/ui/States';
import { Skeleton } from '@/components/ui/Skeleton';
import { Icon } from '@/components/ui/Icon';
import { useToast } from '@/components/ui/Toast';
import { formatVnd } from '@/lib/format';
import { cn } from '@/lib/cn';
import { ApiError } from '@/types/api';
import type { ProductSummary } from '@/types/catalog';

export function AdminProductPage() {
  const qc = useQueryClient();
  const toast = useToast();
  const [page, setPage] = useState(0);
  const [q, setQ] = useState('');
  const [deleteTarget, setDeleteTarget] = useState<ProductSummary | null>(null);

  const { data, isLoading } = useQuery({
    queryKey: ['admin-products', page, q],
    queryFn: () => adminProductApi.list({ page, q: q || undefined, limit: 20 }),
  });
  const { data: categories } = useQuery({ queryKey: ['categories'], queryFn: catalogApi.getCategories });

  const catName = (id: string | null): string => {
    if (!id || !categories) return '—';
    const search = (list: typeof categories, parentName?: string): string | null => {
      for (const c of list) {
        const full = parentName ? `${parentName} › ${c.name}` : c.name;
        if (c.id === id) return full;
        if (c.children?.length) {
          const found = search(c.children, c.name);
          if (found) return found;
        }
      }
      return null;
    };
    return search(categories) ?? '—';
  };

  const deleteMut = useMutation({
    mutationFn: (id: string) => adminProductApi.remove(id),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['admin-products'] });
      toast.success('Đã ẩn sản phẩm');
      setDeleteTarget(null);
    },
    onError: (e) => { toast.error(e instanceof ApiError ? e.message : 'Xóa thất bại'); setDeleteTarget(null); },
  });

  const products = data?.items ?? [];

  return (
    <div className="space-y-5">
      <div className="flex items-center justify-between">
        <h1 className="font-headline text-2xl font-bold text-ink">Sản phẩm</h1>
        <Link to="/admin/products/new"><Button leadingIcon="add">Thêm sản phẩm</Button></Link>
      </div>

      <div className="flex items-center gap-3 rounded-lg border border-border bg-white p-4">
        <div className="relative flex-1 max-w-sm">
          <Icon name="search" className="absolute left-3 top-1/2 -translate-y-1/2 text-[20px] text-ink-muted" />
          <input
            value={q}
            onChange={(e) => { setQ(e.target.value); setPage(0); }}
            placeholder="Tìm theo tên / SKU..."
            className="h-10 w-full rounded border border-border pl-10 pr-3 text-sm focus:border-brand focus:outline-none"
          />
        </div>
        <span className="ml-auto text-sm text-ink-muted">{data?.totalItems ?? 0} sản phẩm</span>
      </div>

      <div className="overflow-hidden rounded-lg border border-border bg-white">
        <div className="overflow-x-auto">
          <table className="w-full text-sm">
            <thead>
              <tr className="border-b border-divider bg-page text-left text-xs uppercase text-ink-muted">
                <th className="px-4 py-3 font-semibold">Ảnh</th>
                <th className="px-4 py-3 font-semibold">Tên sản phẩm</th>
                <th className="px-4 py-3 font-semibold">Danh mục</th>
                <th className="px-4 py-3 text-right font-semibold">Giá</th>
                <th className="px-4 py-3 text-center font-semibold">Tồn kho</th>
                <th className="px-4 py-3 font-semibold">Trạng thái</th>
                <th className="px-4 py-3" />
              </tr>
            </thead>
            <tbody>
              {isLoading ? (
                Array.from({ length: 6 }).map((_, i) => (
                  <tr key={i} className="border-b border-divider"><td colSpan={7} className="px-4 py-3"><Skeleton className="h-8 w-full" /></td></tr>
                ))
              ) : products.length === 0 ? (
                <tr><td colSpan={7} className="px-4 py-10"><EmptyState icon="inventory_2" title="Chưa có sản phẩm" action={{ label: 'Thêm sản phẩm', onClick: () => (window.location.href = '/admin/products/new') }} /></td></tr>
              ) : (
                products.map((p) => (
                  <tr key={p.id} className="border-b border-divider last:border-0 hover:bg-page">
                    <td className="px-4 py-2">
                      <img src={p.primaryImage || 'https://placehold.co/48'} alt="" className="h-10 w-10 rounded object-cover" />
                    </td>
                    <td className="px-4 py-2">
                      <p className="font-medium text-ink">{p.name}</p>
                      <p className="text-xs text-ink-muted">{p.slug}</p>
                    </td>
                    <td className="px-4 py-2 text-ink-secondary">{catName(p.categoryId)}</td>
                    <td className="px-4 py-2 text-right tabular-nums">{formatVnd(p.basePrice)}</td>
                    <td className="px-4 py-2 text-center font-medium tabular-nums">
                      {p.totalStock !== undefined ? (
                        <span className={cn(p.totalStock > 0 ? 'text-ink' : 'text-danger font-bold')}>
                          {p.totalStock} sp
                        </span>
                      ) : (
                        p.inStock ? 'Còn hàng' : 'Hết hàng'
                      )}
                    </td>
                    <td className="px-4 py-2">
                      {p.active ? <Badge tone="success">Đang bán</Badge> : <Badge tone="muted">Ẩn</Badge>}
                      {!p.inStock && <span className="ml-1" title="Hết hàng / thiếu biến thể"><Icon name="warning" className="text-[16px] text-warning" /></span>}
                    </td>
                    <td className="px-4 py-2">
                      <div className="flex justify-end gap-1">
                        <Link to={`/admin/products/${p.id}/edit`} className="rounded p-1.5 text-ink-secondary hover:bg-page hover:text-brand" title="Sửa">
                          <Icon name="edit" className="text-[18px]" />
                        </Link>
                        <button onClick={() => setDeleteTarget(p)} className="rounded p-1.5 text-ink-secondary hover:bg-page hover:text-danger" title="Ẩn">
                          <Icon name="delete" className="text-[18px]" />
                        </button>
                      </div>
                    </td>
                  </tr>
                ))
              )}
            </tbody>
          </table>
        </div>
      </div>

      {data && data.totalPages > 1 && <Pagination page={data.page} totalPages={data.totalPages} onChange={setPage} />}

      <ConfirmDialog
        open={!!deleteTarget}
        onClose={() => setDeleteTarget(null)}
        onConfirm={() => deleteTarget && deleteMut.mutate(deleteTarget.id)}
        title="Ẩn sản phẩm này khỏi cửa hàng?"
        message={`"${deleteTarget?.name}" sẽ được ẩn (soft-delete) khỏi catalog công khai.`}
        confirmLabel="Ẩn sản phẩm"
        danger
        loading={deleteMut.isPending}
      />
    </div>
  );
}
