import { useMemo, useState } from 'react';
import { Link, useNavigate, useParams } from 'react-router-dom';
import { useQuery } from '@tanstack/react-query';
import { catalogApi } from '@/services/catalog';
import { Button } from '@/components/ui/Button';
import { Icon } from '@/components/ui/Icon';
import { Skeleton } from '@/components/ui/Skeleton';
import { useToast } from '@/components/ui/Toast';
import { useAuth } from '@/store/auth';
import { useCart } from '@/store/cart';
import { formatVnd } from '@/lib/format';
import { ApiError } from '@/types/api';
import { cn } from '@/lib/cn';

const PLACEHOLDER = 'https://placehold.co/800x800/EEF2FF/4F46E5?text=Cartzilla';

export function ProductDetailPage() {
  const { id = '' } = useParams();
  const navigate = useNavigate();
  const toast = useToast();
  const { isAuthenticated, hasRole } = useAuth();
  const { addItem } = useCart();

  const { data: product, isLoading, isError } = useQuery({
    queryKey: ['product', id],
    queryFn: () => catalogApi.getProduct(id),
    enabled: !!id,
  });

  const [size, setSize] = useState<string | null>(null);
  const [color, setColor] = useState<string | null>(null);
  const [qty, setQty] = useState(1);
  const [imgIdx, setImgIdx] = useState(0);
  const [adding, setAdding] = useState(false);

  const sizes = useMemo(
    () => Array.from(new Set((product?.variants ?? []).map((v) => v.size).filter(Boolean))) as string[],
    [product],
  );
  const colors = useMemo(
    () =>
      Array.from(
        new Map(
          (product?.variants ?? [])
            .filter((v) => v.color)
            .map((v) => [v.color, { color: v.color!, hex: v.colorHex }]),
        ).values(),
      ),
    [product],
  );

  // Resolve the variant matching the current selection (size/color may be optional).
  const selectedVariant = useMemo(() => {
    if (!product) return null;
    return (
      product.variants.find(
        (v) => (!sizes.length || v.size === size) && (!colors.length || v.color === color),
      ) ?? null
    );
  }, [product, size, color, sizes.length, colors.length]);

  const needsSelection = (sizes.length > 0 && !size) || (colors.length > 0 && !color);
  const stock = selectedVariant?.stock ?? 0;
  const price = selectedVariant?.price ?? product?.basePrice ?? 0;

  const onAdd = async () => {
    if (!isAuthenticated || !hasRole('CUSTOMER')) {
      navigate('/login', { state: { from: `/products/${id}` } });
      return;
    }
    if (needsSelection || !selectedVariant) {
      toast.error('Vui lòng chọn kích cỡ và màu sắc');
      return;
    }
    if (stock <= 0) {
      toast.error('Sản phẩm đã hết hàng');
      return;
    }
    setAdding(true);
    try {
      await addItem(selectedVariant.sku, qty);
      toast.success('Đã thêm vào giỏ');
    } catch (err) {
      toast.error(err instanceof ApiError ? err.message : 'Không thêm được vào giỏ');
    } finally {
      setAdding(false);
    }
  };

  const onBuyNow = async () => {
    await onAdd();
    if (isAuthenticated && hasRole('CUSTOMER') && selectedVariant && stock > 0) navigate('/cart');
  };

  if (isLoading) {
    return (
      <div className="mx-auto grid max-w-7xl gap-8 px-4 py-8 sm:px-6 md:grid-cols-2">
        <Skeleton className="aspect-square w-full" />
        <div className="space-y-4">
          <Skeleton className="h-4 w-24" />
          <Skeleton className="h-8 w-3/4" />
          <Skeleton className="h-10 w-40" />
          <Skeleton className="h-24 w-full" />
        </div>
      </div>
    );
  }

  if (isError || !product) {
    return (
      <div className="flex min-h-[50vh] flex-col items-center justify-center px-6 text-center">
        <Icon name="search_off" className="mb-3 text-[40px] text-ink-muted" />
        <h1 className="font-headline text-xl font-bold text-ink">Sản phẩm không tồn tại hoặc đã ẩn</h1>
        <Link to="/products" className="mt-4 rounded bg-brand px-4 py-2 text-sm font-medium text-white">
          Về danh sách
        </Link>
      </div>
    );
  }

  const images = product.images.length ? product.images : [{ id: 'ph', imageUrl: PLACEHOLDER, altText: product.name, isPrimary: true, sortOrder: 0 }];

  return (
    <div className="mx-auto max-w-7xl px-4 py-6 sm:px-6">
      <nav className="mb-4 flex items-center gap-1.5 text-sm text-ink-muted">
        <Link to="/" className="hover:text-brand">Trang chủ</Link>
        <Icon name="chevron_right" className="text-[16px]" />
        <Link to="/products" className="hover:text-brand">Sản phẩm</Link>
        <Icon name="chevron_right" className="text-[16px]" />
        <span className="line-clamp-1 text-ink">{product.name}</span>
      </nav>

      <div className="grid gap-8 md:grid-cols-2">
        {/* Gallery */}
        <div>
          <div className="aspect-square overflow-hidden rounded-lg border border-border bg-page">
            <img
              src={images[imgIdx]?.imageUrl || PLACEHOLDER}
              alt={product.name}
              className="h-full w-full object-cover"
              onError={(e) => ((e.target as HTMLImageElement).src = PLACEHOLDER)}
            />
          </div>
          {images.length > 1 && (
            <div className="mt-3 flex gap-2 overflow-x-auto">
              {images.map((img, i) => (
                <button
                  key={img.id}
                  onClick={() => setImgIdx(i)}
                  className={cn(
                    'h-20 w-20 shrink-0 overflow-hidden rounded border-2',
                    i === imgIdx ? 'border-brand' : 'border-border',
                  )}
                >
                  <img src={img.imageUrl} alt="" className="h-full w-full object-cover" />
                </button>
              ))}
            </div>
          )}
        </div>

        {/* Buy box */}
        <div>
          <div className="flex items-center gap-2">
            <span
              className={cn(
                'rounded-full px-2.5 py-1 text-xs font-medium',
                product.sellable ? 'bg-success-tint text-success' : 'bg-danger-tint text-danger',
              )}
            >
              {product.sellable ? 'Còn hàng' : 'Hết hàng'}
            </span>
          </div>
          <h1 className="mt-2 font-headline text-2xl font-bold text-ink sm:text-3xl">{product.name}</h1>
          <div className="mt-3 flex items-center gap-1 text-warning">
            {Array.from({ length: 5 }).map((_, i) => (
              <Icon key={i} name="star" filled className="text-[18px]" />
            ))}
            <span className="ml-1 text-sm text-ink-muted">(Chưa có đánh giá)</span>
          </div>

          <p className="mt-4 font-headline text-3xl font-extrabold tabular-nums text-brand">
            {formatVnd(price)}
          </p>

          {sizes.length > 0 && (
            <div className="mt-6">
              <p className="mb-2 text-sm font-medium text-ink">Kích cỡ</p>
              <div className="flex flex-wrap gap-2">
                {sizes.map((s) => (
                  <button
                    key={s}
                    onClick={() => setSize(s)}
                    className={cn(
                      'flex h-10 min-w-10 items-center justify-center rounded border px-3 text-sm',
                      size === s ? 'border-brand bg-brand text-white' : 'border-border hover:border-brand',
                    )}
                  >
                    {s}
                  </button>
                ))}
              </div>
            </div>
          )}

          {colors.length > 0 && (
            <div className="mt-5">
              <p className="mb-2 text-sm font-medium text-ink">Màu sắc{color ? `: ${color}` : ''}</p>
              <div className="flex flex-wrap gap-2">
                {colors.map((c) => (
                  <button
                    key={c.color}
                    onClick={() => setColor(c.color)}
                    title={c.color}
                    className={cn(
                      'h-9 w-9 rounded-full border-2',
                      color === c.color ? 'border-brand ring-2 ring-brand/30' : 'border-border',
                    )}
                    style={{ backgroundColor: c.hex || '#e2e8f0' }}
                  />
                ))}
              </div>
            </div>
          )}

          {selectedVariant && (
            <p className="mt-5 text-sm text-ink-secondary">
              SKU: <span className="font-medium text-ink">{selectedVariant.sku}</span>
              <span className="mx-2 text-ink-muted">·</span>
              {stock > 0 ? `Còn ${stock} sản phẩm` : 'Hết hàng'}
            </p>
          )}

          {/* Quantity */}
          <div className="mt-5 flex items-center gap-4">
            <div className="flex items-center rounded border border-border">
              <button
                onClick={() => setQty((q) => Math.max(1, q - 1))}
                className="flex h-10 w-10 items-center justify-center text-ink-secondary hover:bg-page"
              >
                <Icon name="remove" className="text-[18px]" />
              </button>
              <span className="w-12 text-center text-sm tabular-nums">{qty}</span>
              <button
                onClick={() => setQty((q) => Math.min(stock || 99, q + 1))}
                className="flex h-10 w-10 items-center justify-center text-ink-secondary hover:bg-page"
              >
                <Icon name="add" className="text-[18px]" />
              </button>
            </div>
          </div>

          <div className="mt-6 flex gap-3">
            <Button onClick={onAdd} loading={adding} fullWidth leadingIcon="add_shopping_cart" disabled={!product.sellable}>
              Thêm vào giỏ
            </Button>
            <Button onClick={onBuyNow} variant="secondary" disabled={!product.sellable}>
              Mua ngay
            </Button>
          </div>

          <div className="mt-6 flex flex-wrap gap-4 border-t border-divider pt-5 text-xs text-ink-secondary">
            <span className="flex items-center gap-1.5"><Icon name="local_shipping" className="text-[18px]" /> Giao nhanh</span>
            <span className="flex items-center gap-1.5"><Icon name="autorenew" className="text-[18px]" /> Đổi trả 7 ngày</span>
            <span className="flex items-center gap-1.5"><Icon name="payments" className="text-[18px]" /> COD / VNPay</span>
          </div>
        </div>
      </div>

      {/* Description */}
      {product.description && (
        <div className="mt-12 rounded-lg border border-border bg-white p-6">
          <h2 className="mb-3 font-headline text-lg font-bold text-ink">Mô tả sản phẩm</h2>
          <p className="whitespace-pre-line text-sm leading-relaxed text-ink-secondary">{product.description}</p>
        </div>
      )}

      <RelatedProducts categoryId={product.categoryId} excludeId={product.id} />
    </div>
  );
}

function RelatedProducts({ categoryId, excludeId }: { categoryId: string | null; excludeId: string }) {
  const { data } = useQuery({
    queryKey: ['products', 'related', categoryId],
    queryFn: () => catalogApi.listProducts({ categoryId: categoryId ?? undefined, limit: 5 }),
    enabled: !!categoryId,
  });
  const items = (data?.items ?? []).filter((p) => p.id !== excludeId).slice(0, 4);
  if (!items.length) return null;
  return (
    <section className="mt-12">
      <h2 className="mb-5 font-headline text-xl font-bold text-ink">Sản phẩm liên quan</h2>
      <div className="grid grid-cols-2 gap-4 sm:grid-cols-4">
        {items.map((p) => (
          <Link key={p.id} to={`/products/${p.id}`} className="group rounded-lg border border-border bg-white p-3 hover:shadow-card">
            <div className="aspect-square overflow-hidden rounded bg-page">
              <img src={p.primaryImage || PLACEHOLDER} alt={p.name} className="h-full w-full object-cover" />
            </div>
            <h3 className="mt-2 line-clamp-2 text-sm font-medium text-ink">{p.name}</h3>
            <p className="mt-1 text-sm font-bold tabular-nums text-ink">{formatVnd(p.basePrice)}</p>
          </Link>
        ))}
      </div>
    </section>
  );
}
