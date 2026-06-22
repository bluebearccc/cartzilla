import { Link, useNavigate } from 'react-router-dom';
import { useState } from 'react';
import { Icon } from '@/components/ui/Icon';
import { formatVnd } from '@/lib/format';
import { useAuth } from '@/store/auth';
import { useCart } from '@/store/cart';
import { useToast } from '@/components/ui/Toast';
import { catalogApi } from '@/services/catalog';
import { ApiError } from '@/types/api';
import type { ProductSummary } from '@/types/catalog';

const PLACEHOLDER =
  'https://placehold.co/600x600/EEF2FF/4F46E5?text=Cartzilla';

export function ProductCard({ product }: { product: ProductSummary }) {
  const { isAuthenticated, hasRole } = useAuth();
  const { addItem } = useCart();
  const toast = useToast();
  const navigate = useNavigate();
  const [adding, setAdding] = useState(false);

  const onQuickAdd = async (e: React.MouseEvent) => {
    e.preventDefault();
    if (!isAuthenticated || !hasRole('CUSTOMER')) {
      navigate('/login');
      return;
    }
    // Quick-add needs a SKU; fetch the first in-stock variant.
    setAdding(true);
    try {
      const detail = await catalogApi.getProduct(product.id);
      const variant = detail.variants.find((v) => v.stock > 0) ?? detail.variants[0];
      if (!variant) {
        toast.error('Sản phẩm chưa có biến thể.');
        return;
      }
      await addItem(variant.sku, 1);
      toast.success('Đã thêm vào giỏ');
    } catch (err) {
      toast.error(err instanceof ApiError ? err.message : 'Không thêm được vào giỏ');
    } finally {
      setAdding(false);
    }
  };

  return (
    <Link
      to={`/products/${product.id}`}
      className="group relative flex flex-col overflow-hidden rounded-lg border border-border bg-white transition hover:shadow-card"
    >
      <div className="relative aspect-square overflow-hidden bg-page">
        <img
          src={product.primaryImage || PLACEHOLDER}
          alt={product.name}
          loading="lazy"
          className="h-full w-full object-cover transition group-hover:scale-105"
          onError={(e) => ((e.target as HTMLImageElement).src = PLACEHOLDER)}
        />
        {product.featured && (
          <span className="absolute left-2 top-2 rounded-full bg-brand px-2 py-0.5 text-[11px] font-semibold text-white">
            Nổi bật
          </span>
        )}
        {!product.inStock && (
          <div className="absolute inset-0 flex items-center justify-center bg-white/60">
            <span className="rounded-full bg-ink/80 px-3 py-1 text-xs font-semibold text-white">
              Hết hàng
            </span>
          </div>
        )}
        <button
          className="absolute right-2 top-2 flex h-8 w-8 items-center justify-center rounded-full bg-white/90 text-ink-muted opacity-0 transition group-hover:opacity-100 hover:text-danger"
          onClick={(e) => e.preventDefault()}
          aria-label="Yêu thích"
        >
          <Icon name="favorite" className="text-[18px]" />
        </button>
        {product.inStock && (
          <button
            onClick={onQuickAdd}
            disabled={adding}
            className="absolute inset-x-2 bottom-2 flex h-9 translate-y-12 items-center justify-center gap-1.5 rounded bg-brand text-sm font-medium text-white opacity-0 transition-all group-hover:translate-y-0 group-hover:opacity-100 hover:bg-brand-hover disabled:opacity-70"
          >
            <Icon name="add_shopping_cart" className="text-[18px]" />
            {adding ? 'Đang thêm...' : 'Thêm vào giỏ'}
          </button>
        )}
      </div>
      <div className="flex flex-1 flex-col p-3">
        <p className="text-xs text-ink-muted">{product.tags?.split(',')[0] || 'Cartzilla'}</p>
        <h3 className="mt-0.5 line-clamp-2 text-sm font-medium text-ink">{product.name}</h3>
        <p className="mt-auto pt-2 font-headline text-base font-bold tabular-nums text-ink">
          {formatVnd(product.basePrice)}
        </p>
      </div>
    </Link>
  );
}
