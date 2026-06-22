import { useMemo } from 'react';
import { Link, useSearchParams } from 'react-router-dom';
import { useQuery } from '@tanstack/react-query';
import { catalogApi } from '@/services/catalog';
import { ProductCard } from '@/components/product/ProductCard';
import { ProductCardSkeleton } from '@/components/ui/Skeleton';
import { Pagination } from '@/components/ui/Pagination';
import { EmptyState, ErrorState } from '@/components/ui/States';
import { Icon } from '@/components/ui/Icon';
import { cn } from '@/lib/cn';
import type { ProductQuery } from '@/types/catalog';

const SIZES = ['S', 'M', 'L', 'XL', 'XXL'];
const SORTS = [
  { value: 'newest', label: 'Mới nhất' },
  { value: 'price_asc', label: 'Giá thấp → cao' },
  { value: 'price_desc', label: 'Giá cao → thấp' },
  { value: 'featured', label: 'Nổi bật' },
];

export function ProductListPage() {
  const [params, setParams] = useSearchParams();

  const query: ProductQuery = useMemo(
    () => ({
      q: params.get('q') ?? undefined,
      category: params.get('category') ?? undefined,
      size: params.get('size') ?? undefined,
      vendorId: params.get('vendorId') ?? undefined,
      inStock: params.get('inStock') === '1' ? true : undefined,
      minPrice: params.get('minPrice') ? Number(params.get('minPrice')) : undefined,
      maxPrice: params.get('maxPrice') ? Number(params.get('maxPrice')) : undefined,
      sort: (params.get('sort') as ProductQuery['sort']) ?? 'newest',
      page: params.get('page') ? Number(params.get('page')) : 0,
      limit: 12,
    }),
    [params],
  );

  const { data, isLoading, isError, refetch } = useQuery({
    queryKey: ['products', query],
    queryFn: () => catalogApi.listProducts(query),
  });
  const { data: categories } = useQuery({ queryKey: ['categories'], queryFn: catalogApi.getCategories });
  const { data: vendors } = useQuery({ queryKey: ['vendors'], queryFn: catalogApi.getVendors });

  const patch = (next: Record<string, string | null>) => {
    const p = new URLSearchParams(params);
    Object.entries(next).forEach(([k, v]) => {
      if (v === null || v === '') p.delete(k);
      else p.set(k, v);
    });
    if (!('page' in next)) p.set('page', '0');
    setParams(p);
  };

  const products = data?.items ?? [];
  const total = data?.totalItems ?? 0;
  const activeSize = params.get('size');

  return (
    <div className="mx-auto max-w-7xl px-4 py-6 sm:px-6">
      <nav className="mb-4 flex items-center gap-1.5 text-sm text-ink-muted">
        <Link to="/" className="hover:text-brand">Trang chủ</Link>
        <Icon name="chevron_right" className="text-[16px]" />
        <span className="text-ink">Sản phẩm</span>
      </nav>
      <div className="mb-6 flex items-end justify-between">
        <div>
          <h1 className="font-headline text-2xl font-bold text-ink">Sản phẩm</h1>
          <p className="mt-1 text-sm text-ink-muted">
            Hiển thị {products.length} / {total} sản phẩm
          </p>
        </div>
      </div>

      <div className="grid gap-6 md:grid-cols-[260px_1fr]">
        {/* Filters */}
        <aside className="hidden h-fit space-y-6 rounded-lg border border-border bg-white p-5 md:block">
          <div>
            <h3 className="mb-3 text-sm font-semibold text-ink">Danh mục</h3>
            <ul className="space-y-1.5">
              {(categories ?? []).map((c) => (
                <li key={c.id}>
                  <button
                    onClick={() => patch({ category: c.slug })}
                    className={cn(
                      'text-sm hover:text-brand',
                      params.get('category') === c.slug ? 'font-semibold text-brand' : 'text-ink-secondary',
                    )}
                  >
                    {c.name}
                  </button>
                </li>
              ))}
              {!categories?.length && <li className="text-sm text-ink-muted">Đang tải...</li>}
            </ul>
          </div>

          <div>
            <h3 className="mb-3 text-sm font-semibold text-ink">Kích cỡ</h3>
            <div className="flex flex-wrap gap-2">
              {SIZES.map((s) => (
                <button
                  key={s}
                  onClick={() => patch({ size: activeSize === s ? null : s })}
                  className={cn(
                    'flex h-9 min-w-9 items-center justify-center rounded border px-2 text-sm',
                    activeSize === s ? 'border-brand bg-brand text-white' : 'border-border hover:border-brand',
                  )}
                >
                  {s}
                </button>
              ))}
            </div>
          </div>

          <div>
            <h3 className="mb-3 text-sm font-semibold text-ink">Thương hiệu</h3>
            <ul className="max-h-48 space-y-1.5 overflow-y-auto">
              {(vendors ?? []).map((v) => (
                <li key={v.id}>
                  <button
                    onClick={() => patch({ vendorId: params.get('vendorId') === v.id ? null : v.id })}
                    className={cn(
                      'text-sm hover:text-brand',
                      params.get('vendorId') === v.id ? 'font-semibold text-brand' : 'text-ink-secondary',
                    )}
                  >
                    {v.name}
                  </button>
                </li>
              ))}
            </ul>
          </div>

          <div>
            <h3 className="mb-3 text-sm font-semibold text-ink">Tình trạng</h3>
            <label className="flex cursor-pointer items-center gap-2 text-sm text-ink-secondary">
              <input
                type="checkbox"
                checked={params.get('inStock') === '1'}
                onChange={(e) => patch({ inStock: e.target.checked ? '1' : null })}
                className="h-4 w-4 rounded border-border text-brand"
              />
              Chỉ hiển thị còn hàng
            </label>
          </div>

          <button
            onClick={() => setParams(new URLSearchParams())}
            className="text-sm font-medium text-danger hover:underline"
          >
            Xóa tất cả bộ lọc
          </button>
        </aside>

        {/* Results */}
        <div>
          <div className="mb-4 flex items-center justify-between rounded-lg border border-border bg-white px-4 py-3">
            <div className="flex flex-wrap items-center gap-2">
              {params.get('q') && (
                <FilterChip label={`Từ khóa: ${params.get('q')}`} onRemove={() => patch({ q: null })} />
              )}
              {params.get('category') && (
                <FilterChip label={`Danh mục: ${params.get('category')}`} onRemove={() => patch({ category: null })} />
              )}
              {activeSize && <FilterChip label={`Size: ${activeSize}`} onRemove={() => patch({ size: null })} />}
            </div>
            <select
              value={params.get('sort') ?? 'newest'}
              onChange={(e) => patch({ sort: e.target.value })}
              className="h-9 rounded border border-border px-2 text-sm focus:border-brand focus:outline-none"
            >
              {SORTS.map((s) => (
                <option key={s.value} value={s.value}>
                  Sắp xếp: {s.label}
                </option>
              ))}
            </select>
          </div>

          {isError ? (
            <ErrorState onRetry={() => refetch()} />
          ) : isLoading ? (
            <div className="grid grid-cols-2 gap-4 sm:grid-cols-3">
              {Array.from({ length: 9 }).map((_, i) => (
                <ProductCardSkeleton key={i} />
              ))}
            </div>
          ) : products.length === 0 ? (
            <EmptyState
              icon="search_off"
              title="Không tìm thấy sản phẩm phù hợp"
              subtitle="Thử xóa bớt bộ lọc hoặc tìm với từ khóa khác."
              action={{ label: 'Xóa bộ lọc', onClick: () => setParams(new URLSearchParams()) }}
            />
          ) : (
            <>
              <div className="grid grid-cols-2 gap-4 sm:grid-cols-3">
                {products.map((p) => (
                  <ProductCard key={p.id} product={p} />
                ))}
              </div>
              <div className="mt-8">
                <Pagination
                  page={data?.page ?? 0}
                  totalPages={data?.totalPages ?? 1}
                  onChange={(p) => patch({ page: String(p) })}
                />
              </div>
            </>
          )}
        </div>
      </div>
    </div>
  );
}

function FilterChip({ label, onRemove }: { label: string; onRemove: () => void }) {
  return (
    <span className="inline-flex items-center gap-1 rounded-full bg-brand-tint px-2.5 py-1 text-xs font-medium text-brand">
      {label}
      <button onClick={onRemove} aria-label="Xóa lọc">
        <Icon name="close" className="text-[14px]" />
      </button>
    </span>
  );
}
