import type { OrderStatus } from '@/types/api';
import type { ShippingSnapshot } from '@/types/order';

/** Parse the JSON shipping snapshot stored on the order; tolerate plain strings. */
export function parseShipping(raw: string | null | undefined): ShippingSnapshot | null {
  if (!raw) return null;
  try {
    const obj = JSON.parse(raw);
    if (obj && typeof obj === 'object' && 'fullName' in obj) return obj as ShippingSnapshot;
    return null;
  } catch {
    return null;
  }
}

/** Format a shipping snapshot (or fall back to the raw string) for display. */
export function formatShipping(raw: string | null | undefined): string {
  const s = parseShipping(raw);
  if (!s) return raw ?? '—';
  return `${s.street}, ${s.district}, ${s.city}`;
}

/** Next valid status transitions for the staff state machine (BR-O09..O11). */
export function nextStatuses(status: OrderStatus): OrderStatus[] {
  switch (status) {
    case 'PENDING':
      return ['CONFIRMED', 'CANCELLED'];
    case 'CONFIRMED':
      return ['SHIPPING', 'CANCELLED'];
    case 'SHIPPING':
      return ['DELIVERED'];
    default:
      return [];
  }
}

export const ORDER_STATUS_LABELS: Record<OrderStatus, string> = {
  PENDING: 'Chờ xử lý',
  CONFIRMED: 'Đã xác nhận',
  SHIPPING: 'Đang giao',
  DELIVERED: 'Đã giao',
  CANCELLED: 'Đã hủy',
};

/** Steps for the customer order timeline. */
export const TIMELINE: { status: OrderStatus; label: string }[] = [
  { status: 'PENDING', label: 'Đặt hàng' },
  { status: 'CONFIRMED', label: 'Đã xác nhận' },
  { status: 'SHIPPING', label: 'Đang giao' },
  { status: 'DELIVERED', label: 'Đã giao' },
];
