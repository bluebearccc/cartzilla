import { useState } from 'react';
import { useQuery } from '@tanstack/react-query';
import {
  PieChart, Pie, Cell, BarChart, Bar, XAxis, YAxis, Tooltip, ResponsiveContainer, Legend,
} from 'recharts';
import { adminReportApi } from '@/services/admin';
import { Card } from '@/components/ui/Card';
import { Button } from '@/components/ui/Button';
import { Skeleton } from '@/components/ui/Skeleton';
import { Icon } from '@/components/ui/Icon';
import { formatVnd } from '@/lib/format';

const STATUS_LABEL: Record<string, string> = {
  PENDING: 'Chờ xử lý', CONFIRMED: 'Đã xác nhận', SHIPPING: 'Đang giao', DELIVERED: 'Đã giao', CANCELLED: 'Đã hủy',
};
const STATUS_COLOR: Record<string, string> = {
  PENDING: '#F59E0B', CONFIRMED: '#0EA5E9', SHIPPING: '#4F46E5', DELIVERED: '#16A34A', CANCELLED: '#DC2626',
};
const PAY_LABEL: Record<string, string> = { PENDING: 'Chờ', PAID: 'Đã thanh toán', FAILED: 'Thất bại', REFUNDED: 'Hoàn tiền' };

function presetRange(days: number): { from: string; to: string } {
  const to = new Date();
  const from = new Date();
  from.setDate(to.getDate() - days);
  const fmt = (d: Date) => d.toISOString().slice(0, 10);
  return { from: fmt(from), to: fmt(to) };
}

export function AdminReportsPage() {
  const [range, setRange] = useState<{ from?: string; to?: string }>({});

  const summary = useQuery({ queryKey: ['report-summary', range], queryFn: () => adminReportApi.summary(range.from, range.to) });
  const top = useQuery({ queryKey: ['report-top', range], queryFn: () => adminReportApi.topProducts(10, range.from, range.to) });

  const s = summary.data;
  const statusData = (s?.ordersByStatus ?? []).map((d) => ({ name: STATUS_LABEL[d.key] ?? d.key, value: d.count, key: d.key }));
  const payData = (s?.ordersByPaymentStatus ?? []).map((d) => ({ name: PAY_LABEL[d.key] ?? d.key, value: d.count }));
  const totalOrders = s?.totalOrders ?? 0;
  const delivered = s?.ordersByStatus?.find((d) => d.key === 'DELIVERED')?.count ?? 0;
  const cancelled = s?.ordersByStatus?.find((d) => d.key === 'CANCELLED')?.count ?? 0;
  const cancelRate = totalOrders ? Math.round((cancelled / totalOrders) * 100) : 0;

  const kpis = [
    { label: 'Tổng doanh thu', value: formatVnd(s?.totalRevenue ?? 0), icon: 'payments', tone: 'text-success' },
    { label: 'Tổng đơn hàng', value: String(totalOrders), icon: 'receipt_long', tone: 'text-brand' },
    { label: 'Đơn đã giao', value: String(delivered), icon: 'local_shipping', tone: 'text-info' },
    { label: 'Tỷ lệ hủy', value: `${cancelRate}%`, icon: 'cancel', tone: 'text-danger' },
  ];

  return (
    <div className="space-y-5">
      <div className="flex flex-wrap items-center justify-between gap-3">
        <h1 className="font-headline text-2xl font-bold text-ink">Báo cáo</h1>
        <div className="flex flex-wrap items-center gap-2">
          {[{ l: '7 ngày', d: 7 }, { l: '30 ngày', d: 30 }, { l: '90 ngày', d: 90 }].map((p) => (
            <Button key={p.d} size="sm" variant="secondary" onClick={() => setRange(presetRange(p.d))}>{p.l}</Button>
          ))}
          <Button size="sm" variant="ghost" onClick={() => setRange({})}>Tất cả</Button>
        </div>
      </div>

      {/* KPI cards */}
      <div className="grid grid-cols-2 gap-4 lg:grid-cols-4">
        {kpis.map((k) => (
          <div key={k.label} className="rounded-lg border border-border bg-white p-5">
            <div className="flex items-center justify-between">
              <span className="text-sm text-ink-secondary">{k.label}</span>
              <Icon name={k.icon} className={`text-[22px] ${k.tone}`} />
            </div>
            {summary.isLoading ? <Skeleton className="mt-2 h-8 w-24" /> : (
              <p className="mt-2 font-headline text-2xl font-bold tabular-nums text-ink">{k.value}</p>
            )}
          </div>
        ))}
      </div>

      {/* Charts */}
      <div className="grid gap-5 lg:grid-cols-2">
        <Card title="Đơn theo trạng thái">
          {summary.isLoading ? <Skeleton className="h-64 w-full" /> : statusData.length === 0 ? (
            <Empty />
          ) : (
            <ResponsiveContainer width="100%" height={260}>
              <PieChart>
                <Pie data={statusData} dataKey="value" nameKey="name" cx="50%" cy="50%" innerRadius={60} outerRadius={90} paddingAngle={2}>
                  {statusData.map((d) => <Cell key={d.key} fill={STATUS_COLOR[d.key] ?? '#94A3B8'} />)}
                </Pie>
                <Tooltip />
                <Legend />
              </PieChart>
            </ResponsiveContainer>
          )}
        </Card>

        <Card title="Đơn theo trạng thái thanh toán">
          {summary.isLoading ? <Skeleton className="h-64 w-full" /> : payData.length === 0 ? (
            <Empty />
          ) : (
            <ResponsiveContainer width="100%" height={260}>
              <BarChart data={payData} layout="vertical" margin={{ left: 20 }}>
                <XAxis type="number" allowDecimals={false} />
                <YAxis type="category" dataKey="name" width={90} />
                <Tooltip />
                <Bar dataKey="value" fill="#4F46E5" radius={[0, 6, 6, 0]} />
              </BarChart>
            </ResponsiveContainer>
          )}
        </Card>
      </div>

      {/* Revenue by method */}
      <Card title="Doanh thu theo phương thức">
        <div className="grid grid-cols-1 gap-4 sm:grid-cols-2">
          {(s?.revenueByMethod ?? []).map((m) => (
            <div key={m.method} className="flex items-center justify-between rounded-lg border border-border p-4">
              <div className="flex items-center gap-3">
                <span className="flex h-10 w-10 items-center justify-center rounded-full bg-brand-tint text-brand">
                  <Icon name={m.method === 'VNPAY' ? 'qr_code_2' : 'payments'} />
                </span>
                <div>
                  <p className="font-medium text-ink">{m.method}</p>
                  <p className="text-xs text-ink-muted">{m.count} đơn</p>
                </div>
              </div>
              <p className="font-headline text-lg font-bold tabular-nums text-ink">{formatVnd(m.revenue)}</p>
            </div>
          ))}
          {!s?.revenueByMethod?.length && !summary.isLoading && <Empty />}
        </div>
      </Card>

      {/* Top products */}
      <Card title="Top sản phẩm bán chạy">
        {top.isLoading ? <Skeleton className="h-40 w-full" /> : !top.data?.length ? <Empty /> : (
          <div className="overflow-x-auto">
            <table className="w-full text-sm">
              <thead>
                <tr className="border-b border-divider text-left text-xs uppercase text-ink-muted">
                  <th className="py-2 font-semibold">#</th>
                  <th className="py-2 font-semibold">Sản phẩm</th>
                  <th className="py-2 font-semibold">SKU</th>
                  <th className="py-2 text-right font-semibold">Đã bán</th>
                  <th className="py-2 text-right font-semibold">Doanh thu</th>
                </tr>
              </thead>
              <tbody>
                {top.data.map((p, i) => (
                  <tr key={p.productId + p.sku} className="border-b border-divider last:border-0">
                    <td className="py-2 text-ink-muted">{i + 1}</td>
                    <td className="py-2 font-medium text-ink">{p.name}</td>
                    <td className="py-2 text-xs text-ink-secondary">{p.sku}</td>
                    <td className="py-2 text-right tabular-nums">{p.quantitySold}</td>
                    <td className="py-2 text-right font-medium tabular-nums">{formatVnd(p.revenue)}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </Card>
    </div>
  );
}

function Empty() {
  return <p className="py-12 text-center text-sm text-ink-muted">Không có dữ liệu trong khoảng thời gian này</p>;
}
