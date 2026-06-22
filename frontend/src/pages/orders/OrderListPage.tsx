import { useState } from 'react';
import { Link } from 'react-router-dom';
import { useQuery } from '@tanstack/react-query';
import { orderApi } from '@/services/order';
import { OrderStatusChip, PaymentStatusChip } from '@/components/ui/StatusChip';
import { EmptyState } from '@/components/ui/States';
import { Skeleton } from '@/components/ui/Skeleton';
import { Button } from '@/components/ui/Button';
import { formatVnd, formatDateTime, shortCode } from '@/lib/format';
import { cn } from '@/lib/cn';
import type { OrderStatus } from '@/types/api';

const TABS: { value: OrderStatus | 'ALL'; label: string }[] = [
  { value: 'ALL', label: 'Tất cả' },
  { value: 'PENDING', label: 'Chờ xử lý' },
  { value: 'CONFIRMED', label: 'Đã xác nhận' },
  { value: 'SHIPPING', label: 'Đang giao' },
  { value: 'DELIVERED', label: 'Đã giao' },
  { value: 'CANCELLED', label: 'Đã hủy' },
];

export function OrderListPage() {
  const [tab, setTab] = useState<OrderStatus | 'ALL'>('ALL');
  const { data: orders, isLoading } = useQuery({ queryKey: ['orders'], queryFn: orderApi.myOrders });

  const list = (orders ?? []).filter((o) => tab === 'ALL' || o.status === tab);
  const countFor = (v: OrderStatus | 'ALL') =>
    v === 'ALL' ? orders?.length ?? 0 : (orders ?? []).filter((o) => o.status === v).length;

  return (
    <div className="space-y-5">
      <h1 className="font-headline text-2xl font-bold text-ink">Đơn hàng của tôi</h1>

      <div className="flex gap-2 overflow-x-auto pb-1">
        {TABS.map((t) => (
          <button
            key={t.value}
            onClick={() => setTab(t.value)}
            className={cn(
              'flex shrink-0 items-center gap-1.5 rounded-full px-4 py-1.5 text-sm font-medium',
              tab === t.value ? 'bg-brand text-white' : 'border border-border bg-white text-ink-secondary',
            )}
          >
            {t.label}
            <span className={cn('rounded-full px-1.5 text-xs', tab === t.value ? 'bg-white/20' : 'bg-page')}>
              {countFor(t.value)}
            </span>
          </button>
        ))}
      </div>

      {isLoading ? (
        <div className="space-y-3">
          {Array.from({ length: 3 }).map((_, i) => <Skeleton key={i} className="h-28 w-full" />)}
        </div>
      ) : list.length === 0 ? (
        <EmptyState
          icon="receipt_long"
          title="Bạn chưa có đơn hàng nào"
          subtitle="Hãy chọn cho mình những sản phẩm yêu thích."
          action={{ label: 'Mua sắm ngay', onClick: () => (window.location.href = '/products') }}
        />
      ) : (
        <div className="space-y-3">
          {list.map((o) => (
            <div key={o.id} className="rounded-lg border border-border bg-white p-4">
              <div className="flex flex-wrap items-center justify-between gap-2 border-b border-divider pb-3">
                <div className="flex items-center gap-2">
                  <span className="font-semibold text-ink">Đơn {shortCode(o.id)}</span>
                  <span className="text-sm text-ink-muted">{formatDateTime(o.createdAt)}</span>
                </div>
                <div className="flex items-center gap-2">
                  <OrderStatusChip status={o.status} />
                  <PaymentStatusChip status={o.paymentStatus} />
                </div>
              </div>
              <div className="flex flex-wrap items-center justify-between gap-3 pt-3">
                <div className="text-sm text-ink-secondary">
                  Phương thức: <span className="font-medium text-ink">{o.paymentMethod}</span>
                </div>
                <div className="flex items-center gap-4">
                  <span className="font-headline text-lg font-bold tabular-nums text-ink">{formatVnd(o.totalAmount)}</span>
                  <Link to={`/orders/${o.id}`}>
                    <Button variant="secondary" size="sm">Xem chi tiết</Button>
                  </Link>
                </div>
              </div>
            </div>
          ))}
        </div>
      )}
    </div>
  );
}
