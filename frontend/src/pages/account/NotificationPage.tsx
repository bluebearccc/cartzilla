import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { notificationApi } from '@/services/notification';
import { EmptyState } from '@/components/ui/States';
import { Skeleton } from '@/components/ui/Skeleton';
import { Pagination } from '@/components/ui/Pagination';
import { Icon } from '@/components/ui/Icon';
import { relativeTime } from '@/lib/format';
import { cn } from '@/lib/cn';
import type { NotificationItem } from '@/types/notification';

const typeIcon: Record<string, { icon: string; tone: string }> = {
  ORDER_CONFIRMED: { icon: 'check_circle', tone: 'text-success bg-success-tint' },
  ORDER_SHIPPED: { icon: 'local_shipping', tone: 'text-brand bg-brand-tint' },
  ORDER_DELIVERED: { icon: 'inventory', tone: 'text-success bg-success-tint' },
  ORDER_CANCELLED: { icon: 'cancel', tone: 'text-danger bg-danger-tint' },
};

export function NotificationPage() {
  const qc = useQueryClient();
  const navigate = useNavigate();
  const [page, setPage] = useState(0);
  const [filter, setFilter] = useState<'all' | 'unread'>('all');

  const { data, isLoading } = useQuery({
    queryKey: ['notifications', page],
    queryFn: () => notificationApi.list(page, 20),
  });

  const markRead = useMutation({
    mutationFn: (id: string) => notificationApi.markRead(id),
    onSuccess: () => qc.invalidateQueries({ queryKey: ['notifications'] }),
  });

  const items = data?.items ?? [];
  const filtered = filter === 'unread' ? items.filter((n) => n.status === 'UNREAD') : items;
  const unreadCount = items.filter((n) => n.status === 'UNREAD').length;

  const onClick = async (n: NotificationItem) => {
    if (n.status === 'UNREAD') await markRead.mutateAsync(n.id);
    if (n.orderId) navigate(`/orders/${n.orderId}`);
  };

  const markAll = async () => {
    await Promise.all(items.filter((n) => n.status === 'UNREAD').map((n) => notificationApi.markRead(n.id)));
    qc.invalidateQueries({ queryKey: ['notifications'] });
  };

  return (
    <div className="space-y-5">
      <div className="flex flex-wrap items-center justify-between gap-3">
        <div className="flex items-center gap-2">
          <h1 className="font-headline text-2xl font-bold text-ink">Thông báo</h1>
          {unreadCount > 0 && (
            <span className="rounded-full bg-danger px-2 py-0.5 text-xs font-semibold text-white">
              {unreadCount} chưa đọc
            </span>
          )}
        </div>
        {unreadCount > 0 && (
          <button onClick={markAll} className="text-sm font-medium text-brand hover:underline">
            Đánh dấu tất cả đã đọc
          </button>
        )}
      </div>

      <div className="flex gap-2">
        {(['all', 'unread'] as const).map((f) => (
          <button
            key={f}
            onClick={() => setFilter(f)}
            className={cn(
              'rounded-full px-4 py-1.5 text-sm font-medium',
              filter === f ? 'bg-brand text-white' : 'bg-white text-ink-secondary border border-border',
            )}
          >
            {f === 'all' ? 'Tất cả' : 'Chưa đọc'}
          </button>
        ))}
      </div>

      {isLoading ? (
        <div className="space-y-2">
          {Array.from({ length: 4 }).map((_, i) => <Skeleton key={i} className="h-20 w-full" />)}
        </div>
      ) : filtered.length === 0 ? (
        <EmptyState icon="notifications_off" title="Bạn chưa có thông báo nào" />
      ) : (
        <div className="overflow-hidden rounded-lg border border-border bg-white">
          {filtered.map((n) => {
            const ic = typeIcon[n.type] ?? { icon: 'notifications', tone: 'text-ink-secondary bg-page' };
            return (
              <button
                key={n.id}
                onClick={() => onClick(n)}
                className={cn(
                  'flex w-full items-start gap-3 border-b border-divider px-4 py-3 text-left last:border-0 hover:bg-page',
                  n.status === 'UNREAD' && 'bg-brand-tint/40',
                )}
              >
                <span className={cn('flex h-10 w-10 shrink-0 items-center justify-center rounded-full', ic.tone)}>
                  <Icon name={ic.icon} className="text-[20px]" />
                </span>
                <div className="min-w-0 flex-1">
                  <p className="font-medium text-ink">{n.title}</p>
                  <p className="line-clamp-2 text-sm text-ink-secondary">{n.message}</p>
                </div>
                <div className="flex shrink-0 flex-col items-end gap-1">
                  <span className="text-xs text-ink-muted">{relativeTime(n.createdAt)}</span>
                  {n.status === 'UNREAD' && <span className="h-2 w-2 rounded-full bg-brand" />}
                </div>
              </button>
            );
          })}
        </div>
      )}

      {data && data.totalPages > 1 && (
        <Pagination page={page} totalPages={data.totalPages} onChange={setPage} />
      )}
    </div>
  );
}
