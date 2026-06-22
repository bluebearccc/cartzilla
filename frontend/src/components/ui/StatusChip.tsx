import { cn } from '@/lib/cn';
import type { OrderStatus, PaymentStatus } from '@/types/api';

type Tone = 'warning' | 'info' | 'indigo' | 'success' | 'danger' | 'muted';

const toneClasses: Record<Tone, string> = {
  warning: 'bg-warning-tint text-warning',
  info: 'bg-info-tint text-info',
  indigo: 'bg-brand-tint text-brand',
  success: 'bg-success-tint text-success',
  danger: 'bg-danger-tint text-danger',
  muted: 'bg-page text-ink-secondary',
};

const orderMap: Record<OrderStatus, { label: string; tone: Tone }> = {
  PENDING: { label: 'Chờ xử lý', tone: 'warning' },
  CONFIRMED: { label: 'Đã xác nhận', tone: 'info' },
  SHIPPING: { label: 'Đang giao', tone: 'indigo' },
  DELIVERED: { label: 'Đã giao', tone: 'success' },
  CANCELLED: { label: 'Đã hủy', tone: 'danger' },
};

const paymentMap: Record<PaymentStatus, { label: string; tone: Tone }> = {
  PENDING: { label: 'Chờ thanh toán', tone: 'warning' },
  PAID: { label: 'Đã thanh toán', tone: 'success' },
  FAILED: { label: 'Thất bại', tone: 'danger' },
  REFUNDED: { label: 'Đã hoàn tiền', tone: 'muted' },
};

function Chip({ label, tone }: { label: string; tone: Tone }) {
  return (
    <span
      className={cn(
        'inline-flex items-center gap-1 rounded-full px-2.5 py-1 text-xs font-medium',
        toneClasses[tone],
      )}
    >
      {label}
    </span>
  );
}

export function OrderStatusChip({ status }: { status: OrderStatus }) {
  const m = orderMap[status] ?? { label: status, tone: 'muted' as Tone };
  return <Chip label={m.label} tone={m.tone} />;
}

export function PaymentStatusChip({ status }: { status: PaymentStatus }) {
  const m = paymentMap[status] ?? { label: status, tone: 'muted' as Tone };
  return <Chip label={m.label} tone={m.tone} />;
}

export function Badge({
  children,
  tone = 'muted',
}: {
  children: React.ReactNode;
  tone?: Tone;
}) {
  return <Chip label={children as string} tone={tone} />;
}
