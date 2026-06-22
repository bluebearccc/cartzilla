import { useState } from 'react';
import { Link, useParams } from 'react-router-dom';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { staffOrderApi } from '@/services/staffOrder';
import { OrderStatusChip, PaymentStatusChip } from '@/components/ui/StatusChip';
import { Card } from '@/components/ui/Card';
import { Button } from '@/components/ui/Button';
import { Textarea } from '@/components/ui/Input';
import { Skeleton } from '@/components/ui/Skeleton';
import { Icon } from '@/components/ui/Icon';
import { useToast } from '@/components/ui/Toast';
import { formatVnd, formatDateTime, shortCode } from '@/lib/format';
import { parseShipping, nextStatuses, ORDER_STATUS_LABELS } from '@/lib/order';
import { ApiError, type OrderStatus } from '@/types/api';

export function StaffOrderDetailPage() {
  const { id = '' } = useParams();
  const qc = useQueryClient();
  const toast = useToast();
  const [target, setTarget] = useState<OrderStatus | null>(null);
  const [reason, setReason] = useState('');

  const { data: order, isLoading } = useQuery({
    queryKey: ['staff-order', id],
    queryFn: () => staffOrderApi.getOrder(id),
    enabled: !!id,
  });

  const mut = useMutation({
    mutationFn: () => staffOrderApi.updateStatus(id, target!, target === 'CANCELLED' ? reason : undefined),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['staff-order', id] });
      qc.invalidateQueries({ queryKey: ['staff-orders'] });
      toast.success('Đã cập nhật trạng thái đơn');
      setTarget(null);
      setReason('');
    },
    onError: (e) => toast.error(e instanceof ApiError ? e.message : 'Chuyển trạng thái không hợp lệ'),
  });

  if (isLoading) return <Skeleton className="h-96 w-full" />;
  if (!order) {
    return (
      <div className="py-16 text-center">
        <h1 className="font-headline text-xl font-bold text-ink">Không tìm thấy đơn hàng</h1>
        <Link to="/staff/orders" className="mt-4 inline-block text-brand hover:underline">← Về danh sách</Link>
      </div>
    );
  }

  const ship = parseShipping(order.shippingAddress);
  const transitions = nextStatuses(order.status);
  const submit = () => {
    if (target === 'CANCELLED' && !reason.trim()) {
      toast.error('Vui lòng nhập lý do hủy');
      return;
    }
    mut.mutate();
  };

  return (
    <div className="space-y-6">
      <nav className="flex items-center gap-1.5 text-sm text-ink-muted">
        <Link to="/staff/orders" className="hover:text-brand">Đơn hàng</Link>
        <Icon name="chevron_right" className="text-[16px]" />
        <span className="text-ink">{shortCode(order.id)}</span>
      </nav>

      <div className="flex flex-wrap items-center gap-3">
        <h1 className="font-headline text-2xl font-bold text-ink">Đơn {shortCode(order.id)}</h1>
        <OrderStatusChip status={order.status} />
        <PaymentStatusChip status={order.paymentStatus} />
        <span className="text-sm text-ink-muted">{formatDateTime(order.createdAt)}</span>
      </div>

      <div className="grid gap-6 lg:grid-cols-[1fr_340px]">
        <div className="space-y-6">
          <Card title="Sản phẩm">
            <div className="overflow-x-auto">
              <table className="w-full text-sm">
                <thead>
                  <tr className="border-b border-divider text-left text-xs uppercase text-ink-muted">
                    <th className="py-2 font-semibold">Sản phẩm</th>
                    <th className="py-2 font-semibold">SKU</th>
                    <th className="py-2 text-right font-semibold">Đơn giá</th>
                    <th className="py-2 text-center font-semibold">SL</th>
                    <th className="py-2 text-right font-semibold">Thành tiền</th>
                  </tr>
                </thead>
                <tbody>
                  {order.items.map((it) => (
                    <tr key={it.id} className="border-b border-divider last:border-0">
                      <td className="py-2">
                        <div className="flex items-center gap-2">
                          <img src={it.image || ''} alt="" className="h-10 w-10 rounded object-cover" />
                          <div>
                            <p className="line-clamp-1 font-medium text-ink">{it.name}</p>
                            <p className="text-xs text-ink-muted">{[it.size, it.color].filter(Boolean).join(' · ')}</p>
                          </div>
                        </div>
                      </td>
                      <td className="py-2 text-xs text-ink-secondary">{it.sku}</td>
                      <td className="py-2 text-right tabular-nums">{formatVnd(it.unitPrice)}</td>
                      <td className="py-2 text-center">{it.quantity}</td>
                      <td className="py-2 text-right font-medium tabular-nums">{formatVnd(it.subtotal)}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          </Card>

          <Card title="Lịch sử trạng thái">
            <ol className="space-y-4">
              {order.statusLogs.map((log) => (
                <li key={log.id} className="flex gap-3">
                  <span className="mt-1 h-2.5 w-2.5 shrink-0 rounded-full bg-brand" />
                  <div>
                    <p className="text-sm font-medium text-ink">
                      {log.fromStatus ? `${ORDER_STATUS_LABELS[log.fromStatus]} → ` : ''}{ORDER_STATUS_LABELS[log.toStatus]}
                    </p>
                    <p className="text-xs text-ink-muted">{formatDateTime(log.createdAt)}{log.changedBy ? ` · ${log.changedBy.slice(0, 8)}` : ''}</p>
                    {log.note && <p className="text-xs text-ink-secondary">{log.note}</p>}
                  </div>
                </li>
              ))}
            </ol>
          </Card>
        </div>

        <div className="space-y-6">
          {/* Action panel */}
          <Card title="Cập nhật trạng thái">
            {transitions.length === 0 ? (
              <p className="rounded bg-page px-3 py-2 text-sm text-ink-secondary">Đơn đã kết thúc (terminal).</p>
            ) : (
              <div className="space-y-3">
                <div className="flex flex-wrap gap-2">
                  {transitions.map((s) => (
                    <button
                      key={s}
                      onClick={() => setTarget(s)}
                      className={`rounded border px-3 py-2 text-sm font-medium ${
                        target === s
                          ? s === 'CANCELLED' ? 'border-danger bg-danger text-white' : 'border-brand bg-brand text-white'
                          : 'border-border hover:border-brand'
                      }`}
                    >
                      {s === 'CONFIRMED' && 'Xác nhận'}
                      {s === 'SHIPPING' && 'Giao hàng'}
                      {s === 'DELIVERED' && 'Đã giao'}
                      {s === 'CANCELLED' && 'Hủy'}
                    </button>
                  ))}
                </div>
                {target === 'CANCELLED' && (
                  <Textarea label="Lý do hủy" required value={reason} onChange={(e) => setReason(e.target.value)} placeholder="Nhập lý do hủy..." />
                )}
                {order.paymentMethod === 'COD' && (
                  <p className="text-xs text-ink-muted">Khi chuyển "Đã giao", thanh toán COD sẽ tự động chuyển "Đã thanh toán".</p>
                )}
                <Button fullWidth disabled={!target} loading={mut.isPending} onClick={submit}>Cập nhật</Button>
              </div>
            )}
          </Card>

          <Card title="Khách hàng">
            <p className="font-mono text-xs text-ink-secondary">{order.userId}</p>
          </Card>

          <Card title="Địa chỉ giao hàng">
            {ship ? (
              <div className="text-sm">
                <p className="font-medium text-ink">{ship.fullName} · {ship.phone}</p>
                <p className="text-ink-secondary">{ship.street}, {ship.district}, {ship.city}</p>
              </div>
            ) : <p className="text-sm text-ink-secondary">{order.shippingAddress}</p>}
          </Card>

          <Card title="Tóm tắt tiền">
            <div className="space-y-2 text-sm">
              <div className="flex justify-between"><span className="text-ink-secondary">Tạm tính</span><span className="tabular-nums">{formatVnd(order.subtotal)}</span></div>
              {order.discount > 0 && <div className="flex justify-between text-success"><span>Giảm giá</span><span className="tabular-nums">−{formatVnd(order.discount)}</span></div>}
              <div className="flex justify-between border-t border-divider pt-2 font-semibold text-ink"><span>Tổng cộng</span><span className="font-headline tabular-nums text-brand">{formatVnd(order.totalAmount)}</span></div>
            </div>
          </Card>
        </div>
      </div>
    </div>
  );
}
