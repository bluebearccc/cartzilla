import { useState } from 'react';
import { Link, useParams } from 'react-router-dom';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { orderApi } from '@/services/order';
import { OrderStatusChip, PaymentStatusChip } from '@/components/ui/StatusChip';
import { Card } from '@/components/ui/Card';
import { Button } from '@/components/ui/Button';
import { Modal } from '@/components/ui/Modal';
import { Textarea } from '@/components/ui/Input';
import { Skeleton } from '@/components/ui/Skeleton';
import { Icon } from '@/components/ui/Icon';
import { useToast } from '@/components/ui/Toast';
import { formatVnd, formatDateTime, shortCode } from '@/lib/format';
import { parseShipping, TIMELINE, ORDER_STATUS_LABELS } from '@/lib/order';
import { ApiError } from '@/types/api';
import { cn } from '@/lib/cn';

export function OrderDetailPage() {
  const { id = '' } = useParams();
  const qc = useQueryClient();
  const toast = useToast();
  const [cancelOpen, setCancelOpen] = useState(false);
  const [reason, setReason] = useState('');
  const [reasonError, setReasonError] = useState(false);

  const { data: order, isLoading, isError } = useQuery({
    queryKey: ['order', id],
    queryFn: () => orderApi.getOrder(id),
    enabled: !!id,
  });

  const cancelMut = useMutation({
    mutationFn: () => orderApi.cancel(id, reason),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['order', id] });
      qc.invalidateQueries({ queryKey: ['orders'] });
      toast.success('Đã hủy đơn hàng');
      setCancelOpen(false);
    },
    onError: (e) => toast.error(e instanceof ApiError ? e.message : 'Hủy đơn thất bại'),
  });

  if (isLoading) return <Skeleton className="h-96 w-full" />;
  if (isError || !order) {
    return (
      <div className="py-16 text-center">
        <Icon name="search_off" className="text-[40px] text-ink-muted" />
        <h1 className="mt-2 font-headline text-xl font-bold text-ink">Không tìm thấy đơn hàng</h1>
        <Link to="/orders" className="mt-4 inline-block text-brand hover:underline">← Về danh sách đơn</Link>
      </div>
    );
  }

  const ship = parseShipping(order.shippingAddress);
  const cancelled = order.status === 'CANCELLED';
  const currentStepIdx = TIMELINE.findIndex((t) => t.status === order.status);
  // Đơn VNPay đặt xong nhưng khách chưa/bỏ dở thanh toán → cho thanh toán lại.
  const awaitingVnpay =
    order.status === 'PENDING' && order.paymentMethod === 'VNPAY' && order.paymentStatus === 'PENDING';

  const submitCancel = () => {
    if (!reason.trim()) {
      setReasonError(true);
      return;
    }
    cancelMut.mutate();
  };

  return (
    <div className="space-y-6">
      <nav className="flex items-center gap-1.5 text-sm text-ink-muted">
        <Link to="/orders" className="hover:text-brand">Đơn hàng</Link>
        <Icon name="chevron_right" className="text-[16px]" />
        <span className="text-ink">{shortCode(order.id)}</span>
      </nav>

      <div className="flex flex-wrap items-center justify-between gap-3">
        <div className="flex items-center gap-3">
          <h1 className="font-headline text-2xl font-bold text-ink">Đơn {shortCode(order.id)}</h1>
          <OrderStatusChip status={order.status} />
          <span className="text-sm text-ink-muted">{formatDateTime(order.createdAt)}</span>
        </div>
        <div className="flex items-center gap-2">
          {awaitingVnpay && (
            <Link to={`/checkout/payment?orderId=${order.id}`}>
              <Button>Thanh toán VNPay</Button>
            </Link>
          )}
          {order.status === 'PENDING' && (
            <Button variant="danger" onClick={() => setCancelOpen(true)}>Hủy đơn</Button>
          )}
        </div>
      </div>

      {awaitingVnpay && (
        <div className="flex items-center gap-3 rounded-lg bg-warning-tint px-4 py-3 text-sm text-warning">
          <Icon name="schedule" />
          <p>Đơn hàng đang chờ thanh toán VNPay. Vui lòng hoàn tất thanh toán để đơn được xác nhận.</p>
        </div>
      )}

      {/* Timeline */}
      <Card>
        {cancelled ? (
          <div className="flex items-center gap-3 rounded-lg bg-danger-tint px-4 py-3 text-danger">
            <Icon name="cancel" />
            <div>
              <p className="font-semibold">Đơn hàng đã hủy</p>
              {order.cancelledReason && <p className="text-sm">Lý do: {order.cancelledReason}</p>}
            </div>
          </div>
        ) : (
          <div className="flex items-center justify-between">
            {TIMELINE.map((step, i) => {
              const done = i <= currentStepIdx;
              const current = i === currentStepIdx;
              return (
                <div key={step.status} className="flex flex-1 flex-col items-center">
                  <div className="flex w-full items-center">
                    {i > 0 && <div className={cn('h-0.5 flex-1', i <= currentStepIdx ? 'bg-brand' : 'bg-border')} />}
                    <span
                      className={cn(
                        'flex h-9 w-9 items-center justify-center rounded-full border-2',
                        done ? 'border-brand bg-brand text-white' : 'border-border bg-white text-ink-muted',
                        current && 'ring-4 ring-brand/20',
                      )}
                    >
                      <Icon name={done ? 'check' : 'circle'} className="text-[18px]" />
                    </span>
                    {i < TIMELINE.length - 1 && <div className={cn('h-0.5 flex-1', i < currentStepIdx ? 'bg-brand' : 'bg-border')} />}
                  </div>
                  <span className={cn('mt-2 text-xs', done ? 'font-medium text-ink' : 'text-ink-muted')}>{step.label}</span>
                </div>
              );
            })}
          </div>
        )}
      </Card>

      <div className="grid gap-6 lg:grid-cols-[1fr_340px]">
        <div className="space-y-6">
          <Card title="Sản phẩm">
            <div className="space-y-4">
              {order.items.map((it) => (
                <div key={it.id} className="flex items-center gap-3">
                  <img src={it.image || ''} alt="" className="h-16 w-16 rounded object-cover" />
                  <div className="min-w-0 flex-1">
                    <p className="line-clamp-1 font-medium text-ink">{it.name}</p>
                    <p className="text-xs text-ink-muted">
                      {[it.size && `Size ${it.size}`, it.color, `SKU: ${it.sku}`].filter(Boolean).join(' · ')}
                    </p>
                    <p className="text-sm text-ink-secondary">{formatVnd(it.unitPrice)} × {it.quantity}</p>
                  </div>
                  <p className="font-semibold tabular-nums text-ink">{formatVnd(it.subtotal)}</p>
                </div>
              ))}
            </div>
          </Card>

          <Card title="Lịch sử trạng thái">
            <ol className="space-y-4">
              {order.statusLogs.length === 0 && <p className="text-sm text-ink-muted">Chưa có lịch sử.</p>}
              {order.statusLogs.map((log) => (
                <li key={log.id} className="flex gap-3">
                  <span className="mt-1 h-2.5 w-2.5 shrink-0 rounded-full bg-brand" />
                  <div>
                    <p className="text-sm font-medium text-ink">
                      {log.fromStatus ? `${ORDER_STATUS_LABELS[log.fromStatus]} → ` : ''}
                      {ORDER_STATUS_LABELS[log.toStatus]}
                    </p>
                    <p className="text-xs text-ink-muted">{formatDateTime(log.createdAt)}</p>
                    {log.note && <p className="text-xs text-ink-secondary">{log.note}</p>}
                  </div>
                </li>
              ))}
            </ol>
          </Card>
        </div>

        <div className="space-y-6">
          <Card title="Địa chỉ giao hàng">
            {ship ? (
              <div className="text-sm">
                <p className="font-medium text-ink">{ship.fullName}</p>
                <p className="text-ink-secondary">{ship.phone}</p>
                <p className="mt-1 text-ink-secondary">{ship.street}, {ship.district}, {ship.city}</p>
              </div>
            ) : (
              <p className="text-sm text-ink-secondary">{order.shippingAddress}</p>
            )}
          </Card>

          <Card title="Thanh toán">
            <div className="space-y-2 text-sm">
              <div className="flex justify-between"><span className="text-ink-secondary">Phương thức</span><span className="font-medium text-ink">{order.paymentMethod}</span></div>
              <div className="flex items-center justify-between"><span className="text-ink-secondary">Trạng thái</span><PaymentStatusChip status={order.paymentStatus} /></div>
            </div>
          </Card>

          <Card title="Tóm tắt">
            <div className="space-y-2 text-sm">
              <div className="flex justify-between"><span className="text-ink-secondary">Tạm tính</span><span className="tabular-nums">{formatVnd(order.subtotal)}</span></div>
              {order.discount > 0 && (
                <div className="flex justify-between text-success">
                  <span>Giảm giá{order.voucherCode ? ` (${order.voucherCode})` : ''}</span>
                  <span className="tabular-nums">−{formatVnd(order.discount)}</span>
                </div>
              )}
              <div className="flex justify-between border-t border-divider pt-2 font-semibold text-ink">
                <span>Tổng cộng</span>
                <span className="font-headline text-lg tabular-nums text-brand">{formatVnd(order.totalAmount)}</span>
              </div>
            </div>
          </Card>
        </div>
      </div>

      <Modal open={cancelOpen} onClose={() => setCancelOpen(false)} title="Hủy đơn hàng">
        <Textarea
          label="Lý do hủy"
          required
          placeholder="Nhập lý do hủy đơn..."
          value={reason}
          onChange={(e) => { setReason(e.target.value); setReasonError(false); }}
          error={reasonError ? 'Vui lòng nhập lý do hủy' : undefined}
        />
        <div className="mt-4 flex justify-end gap-2">
          <Button variant="secondary" onClick={() => setCancelOpen(false)}>Đóng</Button>
          <Button variant="danger" loading={cancelMut.isPending} onClick={submitCancel}>Xác nhận hủy</Button>
        </div>
      </Modal>
    </div>
  );
}
