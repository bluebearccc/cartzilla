import { Link, useNavigate } from 'react-router-dom';
import { useCart } from '@/store/cart';
import { Button } from '@/components/ui/Button';
import { EmptyState } from '@/components/ui/States';
import { Skeleton } from '@/components/ui/Skeleton';
import { Icon } from '@/components/ui/Icon';
import { useToast } from '@/components/ui/Toast';
import { formatVnd } from '@/lib/format';
import { ApiError } from '@/types/api';

const PLACEHOLDER = 'https://placehold.co/120x120/EEF2FF/4F46E5?text=%20';

export function CartPage() {
  const { cart, isLoading, updateItem, removeItem } = useCart();
  const navigate = useNavigate();
  const toast = useToast();

  const onQty = async (itemId: string, qty: number) => {
    try {
      await updateItem(itemId, qty);
    } catch (e) {
      toast.error(e instanceof ApiError ? e.message : 'Cập nhật thất bại');
    }
  };

  if (isLoading) {
    return (
      <div className="mx-auto max-w-7xl px-4 py-8 sm:px-6">
        <Skeleton className="h-8 w-40" />
        <Skeleton className="mt-6 h-64 w-full" />
      </div>
    );
  }

  const items = cart?.items ?? [];

  if (!items.length) {
    return (
      <div className="mx-auto max-w-7xl px-4 py-12 sm:px-6">
        <EmptyState
          icon="shopping_cart"
          title="Giỏ hàng của bạn đang trống"
          subtitle="Khám phá hàng ngàn sản phẩm thời trang tại Cartzilla."
          action={{ label: 'Khám phá sản phẩm', onClick: () => navigate('/products') }}
        />
      </div>
    );
  }

  return (
    <div className="mx-auto max-w-7xl px-4 py-6 sm:px-6">
      <h1 className="mb-6 font-headline text-2xl font-bold text-ink">Giỏ hàng</h1>
      <div className="grid gap-6 lg:grid-cols-[1fr_360px]">
        {/* Items */}
        <div className="rounded-lg border border-border bg-white">
          <div className="hidden grid-cols-[1fr_120px_140px_120px_40px] gap-4 border-b border-divider px-5 py-3 text-xs font-semibold uppercase text-ink-muted md:grid">
            <span>Sản phẩm</span>
            <span className="text-right">Đơn giá</span>
            <span className="text-center">Số lượng</span>
            <span className="text-right">Thành tiền</span>
            <span />
          </div>
          {items.map((it) => (
            <div
              key={it.id}
              className="grid grid-cols-1 items-center gap-4 border-b border-divider px-5 py-4 last:border-0 md:grid-cols-[1fr_120px_140px_120px_40px]"
            >
              <div className="flex items-center gap-3">
                <img
                  src={it.image || PLACEHOLDER}
                  alt={it.name}
                  className="h-16 w-16 rounded object-cover"
                  onError={(e) => ((e.target as HTMLImageElement).src = PLACEHOLDER)}
                />
                <div className="min-w-0">
                  <Link to={`/products/${it.productId}`} className="line-clamp-1 font-medium text-ink hover:text-brand">
                    {it.name}
                  </Link>
                  <p className="text-xs text-ink-muted">
                    {[it.size && `Size ${it.size}`, it.color].filter(Boolean).join(' · ')}
                  </p>
                  <p className="text-xs text-ink-muted">SKU: {it.sku}</p>
                </div>
              </div>
              <p className="text-right text-sm tabular-nums text-ink md:text-right">{formatVnd(it.price)}</p>
              <div className="flex items-center justify-center">
                <div className="flex items-center rounded border border-border">
                  <button onClick={() => onQty(it.id, it.quantity - 1)} className="flex h-9 w-9 items-center justify-center text-ink-secondary hover:bg-page">
                    <Icon name="remove" className="text-[16px]" />
                  </button>
                  <span className="w-10 text-center text-sm tabular-nums">{it.quantity}</span>
                  <button onClick={() => onQty(it.id, it.quantity + 1)} className="flex h-9 w-9 items-center justify-center text-ink-secondary hover:bg-page">
                    <Icon name="add" className="text-[16px]" />
                  </button>
                </div>
              </div>
              <p className="text-right font-semibold tabular-nums text-ink">{formatVnd(it.subtotal)}</p>
              <button onClick={() => removeItem(it.id)} className="justify-self-end text-ink-muted hover:text-danger" aria-label="Xóa">
                <Icon name="delete" />
              </button>
            </div>
          ))}
          <div className="flex items-center justify-between px-5 py-3">
            <Link to="/products" className="flex items-center gap-1 text-sm font-medium text-brand hover:underline">
              <Icon name="arrow_back" className="text-[16px]" /> Tiếp tục mua sắm
            </Link>
          </div>
        </div>

        {/* Summary */}
        <div className="h-fit rounded-lg border border-border bg-white p-5 lg:sticky lg:top-20">
          <h2 className="mb-4 font-headline text-lg font-bold text-ink">Tóm tắt đơn hàng</h2>
          <div className="space-y-2 text-sm">
            <div className="flex justify-between">
              <span className="text-ink-secondary">Tạm tính</span>
              <span className="tabular-nums">{formatVnd(cart?.subtotal ?? 0)}</span>
            </div>
            <div className="flex justify-between">
              <span className="text-ink-secondary">Phí vận chuyển</span>
              <span className="text-success">Miễn phí</span>
            </div>
          </div>
          <div className="my-4 border-t border-divider" />
          <div className="flex items-center justify-between">
            <span className="font-semibold text-ink">Tổng cộng</span>
            <span className="font-headline text-xl font-bold tabular-nums text-brand">
              {formatVnd(cart?.subtotal ?? 0)}
            </span>
          </div>
          <Button fullWidth size="lg" className="mt-5" onClick={() => navigate('/checkout')}>
            Tiến hành thanh toán
          </Button>
          <p className="mt-2 text-center text-xs text-ink-muted">Mã giảm giá được áp dụng ở bước thanh toán.</p>
        </div>
      </div>
    </div>
  );
}
