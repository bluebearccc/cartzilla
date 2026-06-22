import { useState } from 'react';
import { Link } from 'react-router-dom';
import { useQuery } from '@tanstack/react-query';
import { staffOrderApi, type StaffOrderQuery } from '@/services/staffOrder';
import { OrderStatusChip, PaymentStatusChip } from '@/components/ui/StatusChip';
import { Pagination } from '@/components/ui/Pagination';
import { EmptyState } from '@/components/ui/States';
import { Skeleton } from '@/components/ui/Skeleton';
import { Button } from '@/components/ui/Button';
import { formatVnd, formatDateTime, shortCode } from '@/lib/format';

const STATUSES = ['PENDING', 'CONFIRMED', 'SHIPPING', 'DELIVERED', 'CANCELLED'];

export function StaffOrderListPage() {
  const [filters, setFilters] = useState<StaffOrderQuery>({ page: 0, limit: 20 });

  const { data, isLoading } = useQuery({
    queryKey: ['staff-orders', filters],
    queryFn: () => staffOrderApi.list(filters),
  });

  const patch = (next: Partial<StaffOrderQuery>) => setFilters((f) => ({ ...f, ...next, page: 0 }));
  const orders = data?.items ?? [];

  return (
    <div className="space-y-5">
      <h1 className="font-headline text-2xl font-bold text-ink">Quản lý đơn hàng</h1>

      {/* Filter bar */}
      <div className="flex flex-wrap items-end gap-3 rounded-lg border border-border bg-white p-4">
        <Field label="Trạng thái">
          <select
            className="h-10 rounded border border-border px-3 text-sm focus:border-brand focus:outline-none"
            value={filters.status ?? ''}
            onChange={(e) => patch({ status: e.target.value || undefined })}
          >
            <option value="">Tất cả</option>
            {STATUSES.map((s) => <option key={s} value={s}>{s}</option>)}
          </select>
        </Field>
        <Field label="Thanh toán">
          <select
            className="h-10 rounded border border-border px-3 text-sm focus:border-brand focus:outline-none"
            value={filters.paymentMethod ?? ''}
            onChange={(e) => patch({ paymentMethod: e.target.value || undefined })}
          >
            <option value="">Tất cả</option>
            <option value="COD">COD</option>
            <option value="VNPAY">VNPay</option>
          </select>
        </Field>
        <Field label="Từ ngày">
          <input type="date" className="h-10 rounded border border-border px-3 text-sm focus:border-brand focus:outline-none" value={filters.fromDate ?? ''} onChange={(e) => patch({ fromDate: e.target.value || undefined })} />
        </Field>
        <Field label="Đến ngày">
          <input type="date" className="h-10 rounded border border-border px-3 text-sm focus:border-brand focus:outline-none" value={filters.toDate ?? ''} onChange={(e) => patch({ toDate: e.target.value || undefined })} />
        </Field>
        <button onClick={() => setFilters({ page: 0, limit: 20 })} className="h-10 text-sm font-medium text-danger hover:underline">
          Xóa lọc
        </button>
        <span className="ml-auto self-center text-sm text-ink-muted">{data?.totalItems ?? 0} đơn</span>
      </div>

      <div className="overflow-hidden rounded-lg border border-border bg-white">
        <div className="overflow-x-auto">
          <table className="w-full text-sm">
            <thead>
              <tr className="border-b border-divider bg-page text-left text-xs uppercase text-ink-muted">
                <th className="px-4 py-3 font-semibold">Mã đơn</th>
                <th className="px-4 py-3 font-semibold">Khách hàng</th>
                <th className="px-4 py-3 font-semibold">Ngày tạo</th>
                <th className="px-4 py-3 font-semibold">Phương thức</th>
                <th className="px-4 py-3 font-semibold">Trạng thái</th>
                <th className="px-4 py-3 font-semibold">Thanh toán</th>
                <th className="px-4 py-3 text-right font-semibold">Tổng tiền</th>
                <th className="px-4 py-3" />
              </tr>
            </thead>
            <tbody>
              {isLoading ? (
                Array.from({ length: 6 }).map((_, i) => (
                  <tr key={i} className="border-b border-divider"><td colSpan={8} className="px-4 py-3"><Skeleton className="h-6 w-full" /></td></tr>
                ))
              ) : orders.length === 0 ? (
                <tr><td colSpan={8} className="px-4 py-10"><EmptyState icon="inbox" title="Không có đơn hàng khớp bộ lọc" /></td></tr>
              ) : (
                orders.map((o) => (
                  <tr key={o.id} className="border-b border-divider last:border-0 hover:bg-page">
                    <td className="px-4 py-3 font-medium text-ink">{shortCode(o.id)}</td>
                    <td className="px-4 py-3 font-mono text-xs text-ink-secondary">{o.userId.slice(0, 8)}</td>
                    <td className="px-4 py-3 text-ink-secondary">{formatDateTime(o.createdAt)}</td>
                    <td className="px-4 py-3 text-ink-secondary">{o.paymentMethod}</td>
                    <td className="px-4 py-3"><OrderStatusChip status={o.status} /></td>
                    <td className="px-4 py-3"><PaymentStatusChip status={o.paymentStatus} /></td>
                    <td className="px-4 py-3 text-right font-semibold tabular-nums text-ink">{formatVnd(o.totalAmount)}</td>
                    <td className="px-4 py-3 text-right">
                      <Link to={`/staff/orders/${o.id}`}><Button variant="secondary" size="sm">Xem</Button></Link>
                    </td>
                  </tr>
                ))
              )}
            </tbody>
          </table>
        </div>
      </div>

      {data && data.totalPages > 1 && (
        <Pagination page={data.page} totalPages={data.totalPages} onChange={(p) => setFilters((f) => ({ ...f, page: p }))} />
      )}
    </div>
  );
}

function Field({ label, children }: { label: string; children: React.ReactNode }) {
  return (
    <div className="flex flex-col gap-1">
      <span className="text-xs font-medium text-ink-secondary">{label}</span>
      {children}
    </div>
  );
}
