import { useState } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { adminUserApi } from '@/services/admin';
import { Badge } from '@/components/ui/StatusChip';
import { Pagination } from '@/components/ui/Pagination';
import { ConfirmDialog } from '@/components/ui/Modal';
import { EmptyState } from '@/components/ui/States';
import { Skeleton } from '@/components/ui/Skeleton';
import { Icon } from '@/components/ui/Icon';
import { useToast } from '@/components/ui/Toast';
import { ApiError } from '@/types/api';
import type { AdminUser } from '@/types/admin';

export function AdminUserPage() {
  const qc = useQueryClient();
  const toast = useToast();
  const [page, setPage] = useState(0);
  const [q, setQ] = useState('');
  const [roleFilter, setRoleFilter] = useState('');
  const [confirm, setConfirm] = useState<{ user: AdminUser; type: 'lock' | 'unlock' } | null>(null);

  const { data, isLoading } = useQuery({
    queryKey: ['admin-users', page, q, roleFilter],
    queryFn: () => adminUserApi.list({ page, q: q || undefined, role: roleFilter || undefined, limit: 20 }),
  });

  const invalidate = () => qc.invalidateQueries({ queryKey: ['admin-users'] });

  const roleMut = useMutation({
    mutationFn: ({ id, role }: { id: string; role: string }) => adminUserApi.updateRole(id, role),
    onSuccess: () => { invalidate(); toast.success('Đã cập nhật vai trò'); },
    onError: (e) => toast.error(e instanceof ApiError ? e.message : 'Không thể đổi vai trò'),
  });
  const statusMut = useMutation({
    mutationFn: ({ id, active }: { id: string; active: boolean }) => adminUserApi.updateStatus(id, active),
    onSuccess: () => { invalidate(); toast.success('Đã cập nhật trạng thái'); setConfirm(null); },
    onError: (e) => { toast.error(e instanceof ApiError ? e.message : 'Thất bại'); setConfirm(null); },
  });

  const users = data?.content ?? [];

  return (
    <div className="space-y-5">
      <h1 className="font-headline text-2xl font-bold text-ink">Người dùng</h1>

      <div className="flex flex-wrap items-center gap-3 rounded-lg border border-border bg-white p-4">
        <div className="relative max-w-sm flex-1">
          <Icon name="search" className="absolute left-3 top-1/2 -translate-y-1/2 text-[20px] text-ink-muted" />
          <input value={q} onChange={(e) => { setQ(e.target.value); setPage(0); }} placeholder="Tìm theo tên / email..." className="h-10 w-full rounded border border-border pl-10 pr-3 text-sm focus:border-brand focus:outline-none" />
        </div>
        <select value={roleFilter} onChange={(e) => { setRoleFilter(e.target.value); setPage(0); }} className="h-10 rounded border border-border px-3 text-sm focus:border-brand focus:outline-none">
          <option value="">Tất cả vai trò</option>
          <option value="CUSTOMER">CUSTOMER</option>
          <option value="STAFF">STAFF</option>
          <option value="ADMIN">ADMIN</option>
        </select>
        <span className="ml-auto text-sm text-ink-muted">{data?.totalElements ?? 0} người dùng</span>
      </div>

      <div className="overflow-hidden rounded-lg border border-border bg-white">
        <div className="overflow-x-auto">
          <table className="w-full text-sm">
            <thead>
              <tr className="border-b border-divider bg-page text-left text-xs uppercase text-ink-muted">
                <th className="px-4 py-3 font-semibold">Họ tên</th>
                <th className="px-4 py-3 font-semibold">Email</th>
                <th className="px-4 py-3 font-semibold">Vai trò</th>
                <th className="px-4 py-3 font-semibold">Trạng thái</th>
                <th className="px-4 py-3" />
              </tr>
            </thead>
            <tbody>
              {isLoading ? (
                <tr><td colSpan={5} className="px-4 py-3"><Skeleton className="h-8 w-full" /></td></tr>
              ) : users.length === 0 ? (
                <tr><td colSpan={5} className="px-4 py-10"><EmptyState icon="group" title="Không có người dùng" /></td></tr>
              ) : (
                users.map((u) => (
                  <tr key={u.id} className="border-b border-divider last:border-0 hover:bg-page">
                    <td className="px-4 py-2">
                      <div className="flex items-center gap-2">
                        <span className="flex h-8 w-8 items-center justify-center rounded-full bg-brand-tint text-xs font-semibold text-brand">
                          {(u.fullName || u.email)[0].toUpperCase()}
                        </span>
                        <span className="font-medium text-ink">{u.fullName || '—'}</span>
                      </div>
                    </td>
                    <td className="px-4 py-2">
                      <span className="text-ink-secondary">{u.email}</span>
                      {u.emailVerified ? <span className="ml-1 text-[11px] text-success">✓</span> : <span className="ml-1 text-[11px] text-warning">●</span>}
                    </td>
                    <td className="px-4 py-2">
                      <select
                        value={u.role}
                        onChange={(e) => roleMut.mutate({ id: u.id, role: e.target.value })}
                        className="rounded border border-border bg-white px-2 py-1 text-xs focus:border-brand focus:outline-none"
                      >
                        <option value="CUSTOMER">CUSTOMER</option>
                        <option value="STAFF">STAFF</option>
                        <option value="ADMIN">ADMIN</option>
                      </select>
                    </td>
                    <td className="px-4 py-2">
                      {u.active ? <Badge tone="success">Hoạt động</Badge> : <Badge tone="danger">Đã khóa</Badge>}
                    </td>
                    <td className="px-4 py-2 text-right">
                      <button
                        onClick={() => setConfirm({ user: u, type: u.active ? 'lock' : 'unlock' })}
                        className={`rounded p-1.5 hover:bg-page ${u.active ? 'text-ink-secondary hover:text-danger' : 'text-success'}`}
                        title={u.active ? 'Khóa' : 'Mở khóa'}
                      >
                        <Icon name={u.active ? 'lock_open' : 'lock'} className="text-[18px]" />
                      </button>
                    </td>
                  </tr>
                ))
              )}
            </tbody>
          </table>
        </div>
      </div>

      {data && data.totalPages > 1 && <Pagination page={page} totalPages={data.totalPages} onChange={setPage} />}

      <ConfirmDialog
        open={!!confirm}
        onClose={() => setConfirm(null)}
        onConfirm={() => confirm && statusMut.mutate({ id: confirm.user.id, active: confirm.type === 'unlock' })}
        title={confirm?.type === 'lock' ? 'Khóa tài khoản này?' : 'Mở khóa tài khoản này?'}
        message={confirm?.type === 'lock' ? 'Người dùng sẽ không thể đăng nhập/checkout.' : 'Người dùng sẽ có thể đăng nhập trở lại.'}
        confirmLabel={confirm?.type === 'lock' ? 'Khóa' : 'Mở khóa'}
        danger={confirm?.type === 'lock'}
        loading={statusMut.isPending}
      />

      <p className="text-xs text-ink-muted">Lưu ý: không thể tự hạ quyền admin cuối cùng (kiểm tra phía server).</p>
    </div>
  );
}
