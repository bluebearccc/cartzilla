import { useState } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { adminVoucherApi, adminUserApi } from '@/services/admin';
import { Button } from '@/components/ui/Button';
import { Input, Select } from '@/components/ui/Input';
import { Badge } from '@/components/ui/StatusChip';
import { ConfirmDialog } from '@/components/ui/Modal';
import { EmptyState } from '@/components/ui/States';
import { Skeleton } from '@/components/ui/Skeleton';
import { Icon } from '@/components/ui/Icon';
import { useToast } from '@/components/ui/Toast';
import { formatVnd, formatDate } from '@/lib/format';
import { ApiError, type AudienceType } from '@/types/api';
import type { Voucher, VoucherInput, VoucherAllowedUser, AdminUser } from '@/types/admin';

const AUDIENCE_LABEL: Record<AudienceType, string> = {
  ALL_USERS: 'Tất cả',
  NEW_CUSTOMER: 'Khách mới',
  LOYAL_CUSTOMER: 'Khách thân thiết',
  SPECIFIC_USERS: 'Chỉ định',
};

function toInputDate(iso: string | null): string {
  if (!iso) return '';
  return iso.slice(0, 16); // yyyy-MM-ddTHH:mm
}

function toIso(local: string): string | null {
  return local ? new Date(local).toISOString() : null;
}

const emptyForm: VoucherInput = {
  code: '',
  discountType: 'PERCENTAGE',
  discountValue: 10,
  maxDiscountAmount: 150000,
  minOrderAmount: 0,
  maxUses: 100,
  minAccountAgeDays: 0,
  perUserLimit: 1,
  audienceType: 'ALL_USERS',
  firstOrderOnly: false,
  minCompletedOrders: 0,
  minTotalSpent: 0,
  startsAt: null,
  expiresAt: null,
  active: true,
};

function getStatusDetails(v: Voucher) {
  const now = new Date();
  const isStarted = !v.startsAt || new Date(v.startsAt) <= now;
  const isExpired = v.expiresAt && new Date(v.expiresAt) < now;
  if (!v.active) {
    return { color: 'bg-warning', text: 'Tạm dừng', textColor: 'text-warning' };
  }
  if (isExpired) {
    return { color: 'bg-danger', text: 'Hết hạn', textColor: 'text-danger' };
  }
  if (!isStarted) {
    return { color: 'bg-info', text: 'Chưa bắt đầu', textColor: 'text-info' };
  }
  return { color: 'bg-success', text: 'Đang chạy', textColor: 'text-success' };
}

function getLiveSummary(form: VoucherInput) {
  const amountStr = form.discountType === 'PERCENTAGE'
    ? `${form.discountValue}% (tối đa ${formatVnd(form.maxDiscountAmount ?? 0)})`
    : formatVnd(form.discountValue);
  const minOrderStr = form.minOrderAmount ? ` cho đơn từ ${formatVnd(form.minOrderAmount)}` : '';
  const firstOrderStr = form.firstOrderOnly ? ', áp dụng cho đơn đầu tiên' : '';
  return `Giảm ${amountStr}${minOrderStr}${firstOrderStr}.`;
}

export function AdminVoucherPage() {
  const qc = useQueryClient();
  const toast = useToast();

  // Queries & States
  const { data: vouchers, isLoading } = useQuery({ queryKey: ['admin-vouchers'], queryFn: adminVoucherApi.list });
  
  const [search, setSearch] = useState('');
  const [statusFilter, setStatusFilter] = useState(''); // '', 'active', 'expired', 'paused'
  const [audienceFilter, setAudienceFilter] = useState(''); // '', AudienceType
  
  const [open, setOpen] = useState(false);
  const [editing, setEditing] = useState<Voucher | null>(null);
  const [form, setForm] = useState<VoucherInput>(emptyForm);
  const [startsLocal, setStartsLocal] = useState('');
  const [expiresLocal, setExpiresLocal] = useState('');
  const [deleteTarget, setDeleteTarget] = useState<Voucher | null>(null);

  // Specific users picker state
  const [userSearch, setUserSearch] = useState('');
  const [selectedUsers, setSelectedUsers] = useState<VoucherAllowedUser[]>([]); // Array of { userId, email, fullName }

  // Query to search users for picker
  const { data: searchUsersRes } = useQuery({
    queryKey: ['admin-users-search', userSearch],
    queryFn: () => adminUserApi.list({ q: userSearch, limit: 10 }),
    enabled: !!userSearch && form.audienceType === 'SPECIFIC_USERS',
  });
  const searchedUsers = searchUsersRes?.items || [];

  const invalidate = () => qc.invalidateQueries({ queryKey: ['admin-vouchers'] });

  // Mutations
  const saveMut = useMutation({
    mutationFn: async () => {
      const payload: VoucherInput = {
        ...form,
        startsAt: toIso(startsLocal),
        expiresAt: toIso(expiresLocal),
      };
      
      if (editing) {
        return adminVoucherApi.update(editing.id, payload);
      } else {
        const newVoucher = await adminVoucherApi.create(payload);
        // If SPECIFIC_USERS, call addAllowedUser for each selected user
        if (payload.audienceType === 'SPECIFIC_USERS' && selectedUsers.length > 0) {
          for (const u of selectedUsers) {
            await adminVoucherApi.addAllowedUser(newVoucher.id, u.userId);
          }
        }
        return newVoucher;
      }
    },
    onSuccess: () => {
      invalidate();
      toast.success(editing ? 'Đã cập nhật voucher' : 'Đã tạo voucher');
      setOpen(false);
    },
    onError: (e) => toast.error(e instanceof ApiError ? e.message : 'Lưu thất bại'),
  });

  const deleteMut = useMutation({
    mutationFn: (id: string) => adminVoucherApi.remove(id),
    onSuccess: () => { invalidate(); toast.success('Đã xóa voucher'); setDeleteTarget(null); },
    onError: (e) => { toast.error(e instanceof ApiError ? e.message : 'Xóa thất bại'); setDeleteTarget(null); },
  });

  const toggleActiveMut = useMutation({
    mutationFn: (v: Voucher) => adminVoucherApi.update(v.id, {
      discountType: v.discountType,
      discountValue: v.discountValue,
      maxDiscountAmount: v.maxDiscountAmount,
      minOrderAmount: v.minOrderAmount,
      maxUses: v.maxUses,
      startsAt: v.startsAt,
      expiresAt: v.expiresAt,
      minAccountAgeDays: v.minAccountAgeDays,
      perUserLimit: v.perUserLimit,
      audienceType: v.audienceType,
      firstOrderOnly: v.firstOrderOnly,
      minCompletedOrders: v.minCompletedOrders,
      minTotalSpent: v.minTotalSpent,
      active: !v.active,
    }),
    onSuccess: () => { invalidate(); toast.success('Đã cập nhật trạng thái hoạt động'); },
    onError: () => toast.error('Cập nhật trạng thái thất bại'),
  });

  const addAllowedMut = useMutation({
    mutationFn: (userId: string) => adminVoucherApi.addAllowedUser(editing!.id, userId),
    onSuccess: (res) => {
      setSelectedUsers((prev) => [...prev, res]);
      toast.success('Đã thêm người dùng');
    },
    onError: (e) => toast.error(e instanceof ApiError ? e.message : 'Thêm thất bại'),
  });

  const removeAllowedMut = useMutation({
    mutationFn: (userId: string) => adminVoucherApi.removeAllowedUser(editing!.id, userId),
    onSuccess: (_, userId) => {
      setSelectedUsers((prev) => prev.filter((u) => u.userId !== userId));
      toast.success('Đã xóa người dùng');
    },
    onError: (e) => toast.error(e instanceof ApiError ? e.message : 'Xóa thất bại'),
  });

  // Modal open handlers
  const openAdd = () => {
    setEditing(null);
    setForm(emptyForm);
    setStartsLocal('');
    setExpiresLocal('');
    setSelectedUsers([]);
    setUserSearch('');
    setOpen(true);
  };

  const openEdit = async (v: Voucher) => {
    setEditing(v);
    setForm({ ...v });
    setStartsLocal(toInputDate(v.startsAt));
    setExpiresLocal(toInputDate(v.expiresAt));
    setUserSearch('');
    setOpen(true);
    
    // Load allowed users if audienceType is SPECIFIC_USERS
    if (v.audienceType === 'SPECIFIC_USERS') {
      try {
        const allowed = await adminVoucherApi.listAllowedUsers(v.id);
        setSelectedUsers(allowed);
      } catch {
        setSelectedUsers([]);
      }
    } else {
      setSelectedUsers([]);
    }
  };

  const handleAddUser = (user: AdminUser) => {
    if (selectedUsers.some((u) => u.userId === user.id)) {
      toast.error('Người dùng này đã được thêm');
      return;
    }
    
    if (editing) {
      addAllowedMut.mutate(user.id);
    } else {
      setSelectedUsers((prev) => [...prev, { userId: user.id, email: user.email, fullName: user.fullName }]);
    }
    setUserSearch('');
  };

  const handleRemoveUser = (userId: string) => {
    if (editing) {
      removeAllowedMut.mutate(userId);
    } else {
      setSelectedUsers((prev) => prev.filter((u) => u.userId !== userId));
    }
  };

  const set = (patch: Partial<VoucherInput>) => setForm((f) => ({ ...f, ...patch }));

  const handleAudienceTypeChange = (type: AudienceType) => {
    setForm((prev) => {
      const patch: Partial<VoucherInput> = { audienceType: type };
      if (type === 'NEW_CUSTOMER') {
        patch.firstOrderOnly = true;
        patch.minCompletedOrders = 0;
        patch.minTotalSpent = 0;
      } else if (type === 'LOYAL_CUSTOMER') {
        patch.firstOrderOnly = false;
        if (prev.minCompletedOrders < 3) {
          patch.minCompletedOrders = 3;
        }
      }
      return { ...prev, ...patch };
    });
  };

  // Client filtering
  const filteredVouchers = vouchers?.filter((v) => {
    if (search && !v.code.toLowerCase().includes(search.toLowerCase())) {
      return false;
    }
    
    const now = new Date();
    const isStarted = !v.startsAt || new Date(v.startsAt) <= now;
    const isExpired = v.expiresAt && new Date(v.expiresAt) < now;
    const isRunning = v.active && isStarted && !isExpired;
    const isPaused = !v.active;
    
    if (statusFilter === 'active' && !isRunning) return false;
    if (statusFilter === 'expired' && !isExpired) return false;
    if (statusFilter === 'paused' && !isPaused) return false;

    if (audienceFilter && v.audienceType !== audienceFilter) return false;

    return true;
  });

  return (
    <div className="space-y-6">
      {/* Page Header */}
      <div className="flex items-center justify-between">
        <div>
          <h2 className="text-3xl font-headline font-bold text-ink tracking-tight">Mã giảm giá</h2>
          <p className="text-ink-secondary text-sm mt-1">Quản lý và theo dõi các chương trình khuyến mãi.</p>
        </div>
        <Button leadingIcon="add" onClick={openAdd}>Tạo voucher</Button>
      </div>

      {/* Toolbar: Search & Filters */}
      <div className="bg-white p-4 rounded-xl shadow-card border border-border flex flex-col md:flex-row gap-4 items-center justify-between">
        <div className="relative w-full md:w-96">
          <span className="material-symbols-outlined absolute left-3 top-1/2 -translate-y-1/2 text-ink-muted">search</span>
          <input
            className="w-full pl-10 pr-4 py-2.5 bg-page border border-border rounded-lg text-sm focus:ring-2 focus:ring-brand focus:border-brand outline-none transition-all placeholder:text-ink-muted text-ink"
            placeholder="Tìm mã..."
            type="text"
            value={search}
            onChange={(e) => setSearch(e.target.value)}
          />
        </div>
        <div className="flex gap-3 w-full md:w-auto">
          <select
            className="w-full md:w-40 py-2.5 px-4 bg-page border border-border rounded-lg text-sm text-ink-secondary focus:ring-2 focus:ring-brand outline-none cursor-pointer"
            value={statusFilter}
            onChange={(e) => setStatusFilter(e.target.value)}
          >
            <option value="">Trạng thái</option>
            <option value="active">Đang chạy</option>
            <option value="expired">Hết hạn</option>
            <option value="paused">Tạm dừng</option>
          </select>
          <select
            className="w-full md:w-48 py-2.5 px-4 bg-page border border-border rounded-lg text-sm text-ink-secondary focus:ring-2 focus:ring-brand outline-none cursor-pointer"
            value={audienceFilter}
            onChange={(e) => setAudienceFilter(e.target.value)}
          >
            <option value="">Đối tượng</option>
            <option value="ALL_USERS">Tất cả</option>
            <option value="NEW_CUSTOMER">Khách mới</option>
            <option value="LOYAL_CUSTOMER">Khách thân thiết</option>
            <option value="SPECIFIC_USERS">Chỉ định</option>
          </select>
        </div>
      </div>

      {/* Data Table Card */}
      <div className="bg-white rounded-xl shadow-card border border-border overflow-hidden">
        <div className="overflow-x-auto">
          <table className="w-full text-left text-sm whitespace-nowrap">
            <thead className="bg-page text-ink-secondary font-medium border-b border-divider">
              <tr>
                <th className="px-6 py-4">Mã</th>
                <th className="px-6 py-4">Loại &amp; Giá trị</th>
                <th className="px-6 py-4">Đã dùng / Giới hạn</th>
                <th className="px-6 py-4">Hiệu lực</th>
                <th className="px-6 py-4">Đối tượng</th>
                <th className="px-6 py-4">Trạng thái</th>
                <th className="px-6 py-4 text-right">Thao tác</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-divider">
              {isLoading ? (
                <tr>
                  <td colSpan={7} className="px-6 py-4">
                    <Skeleton className="h-8 w-full" />
                  </td>
                </tr>
              ) : !filteredVouchers?.length ? (
                <tr>
                  <td colSpan={7} className="px-6 py-10">
                    <EmptyState
                      icon="sell"
                      title="Chưa có voucher phù hợp"
                      action={{ label: 'Tạo voucher', onClick: openAdd }}
                    />
                  </td>
                </tr>
              ) : (
                filteredVouchers.map((v) => {
                  const status = getStatusDetails(v);
                  return (
                    <tr key={v.id} className="hover:bg-page transition-colors group">
                      <td className="px-6 py-4">
                        <span className="font-mono font-semibold text-ink bg-page px-2 py-1 rounded">
                          {v.code}
                        </span>
                      </td>
                      <td className="px-6 py-4">
                        <div className="flex items-center gap-2">
                          <span className={`px-2 py-0.5 rounded-full text-xs font-medium border ${
                            v.discountType === 'PERCENTAGE' 
                              ? 'bg-blue-50 text-blue-700 border-blue-200' 
                              : 'bg-emerald-50 text-emerald-700 border-emerald-200'
                          }`}>
                            {v.discountType === 'PERCENTAGE' ? 'Phần trăm' : 'Số tiền'}
                          </span>
                          <span className="font-semibold text-ink tabular-nums">
                            {v.discountType === 'PERCENTAGE' ? `${v.discountValue}%` : formatVnd(v.discountValue)}
                          </span>
                        </div>
                        {v.discountType === 'PERCENTAGE' && v.maxDiscountAmount && (
                          <div className="text-xs text-ink-muted mt-1">Tối đa {formatVnd(v.maxDiscountAmount)}</div>
                        )}
                      </td>
                      <td className="px-6 py-4">
                        <div className="flex justify-between text-xs mb-1 font-medium">
                          <span className="text-brand font-semibold">{v.usedCount}</span>
                          <span className="text-ink-secondary">{v.maxUses}</span>
                        </div>
                        <div className="w-24 h-1.5 bg-page rounded-full overflow-hidden">
                          <div
                            className="h-full bg-brand rounded-full transition-all"
                            style={{ width: `${Math.min(100, (v.usedCount / v.maxUses) * 100)}%` }}
                          />
                        </div>
                      </td>
                      <td className="px-6 py-4">
                        <div className="text-ink">{v.startsAt ? formatDate(v.startsAt) : '—'}</div>
                        <div className="text-xs text-ink-muted">đến {v.expiresAt ? formatDate(v.expiresAt) : '∞'}</div>
                      </td>
                      <td className="px-6 py-4">
                        <Badge tone="indigo">{AUDIENCE_LABEL[v.audienceType]}</Badge>
                      </td>
                      <td className="px-6 py-4">
                        <div className="flex items-center gap-1.5">
                          <span className={`w-2 h-2 rounded-full ${status.color}`} />
                          <span className={`${status.textColor} font-medium`}>{status.text}</span>
                        </div>
                      </td>
                      <td className="px-6 py-4 text-right">
                        <div className="flex items-center justify-end gap-1 opacity-0 group-hover:opacity-100 transition-opacity">
                          <button
                            onClick={() => openEdit(v)}
                            className="p-1.5 text-ink-secondary hover:text-brand hover:bg-indigo-50 rounded-lg transition-colors"
                            title="Sửa"
                          >
                            <Icon name="edit" className="text-[18px]" />
                          </button>
                          <button
                            onClick={() => toggleActiveMut.mutate(v)}
                            disabled={toggleActiveMut.isPending}
                            className={`p-1.5 rounded-lg transition-colors ${
                              v.active
                                ? 'text-ink-secondary hover:text-warning hover:bg-yellow-50'
                                : 'text-ink-secondary hover:text-success hover:bg-green-50'
                            }`}
                            title={v.active ? 'Tạm dừng' : 'Kích hoạt'}
                          >
                            <Icon name={v.active ? 'pause_circle' : 'play_circle'} className="text-[18px]" />
                          </button>
                          <button
                            onClick={() => setDeleteTarget(v)}
                            className="p-1.5 text-ink-secondary hover:text-danger hover:bg-red-50 rounded-lg transition-colors"
                            title="Xóa"
                          >
                            <Icon name="delete" className="text-[18px]" />
                          </button>
                        </div>
                      </td>
                    </tr>
                  );
                })
              )}
            </tbody>
          </table>
        </div>
      </div>

      {/* CREATE/EDIT DRAWER (Slide-in right side panel) */}
      {open && (
        <>
          {/* Backdrop overlay */}
          <div
            className="fixed inset-0 bg-ink/40 backdrop-blur-sm z-40 transition-opacity"
            onClick={() => setOpen(false)}
          />

          {/* Drawer container */}
          <div className="fixed top-0 right-0 h-full w-[480px] bg-white shadow-2xl z-50 flex flex-col border-l border-border transition-transform transform translate-x-0 duration-300">
            {/* Drawer Header */}
            <div className="px-6 py-4 border-b border-divider flex items-center justify-between bg-page">
              <div>
                <h3 className="font-headline text-lg font-bold text-ink">
                  {editing ? 'Sửa mã giảm giá' : 'Tạo mã giảm giá mới'}
                </h3>
                <p className="text-xs text-ink-secondary">Thiết lập điều kiện và giới hạn cho voucher.</p>
              </div>
              <button
                onClick={() => setOpen(false)}
                className="p-2 text-ink-muted hover:text-ink hover:bg-page-hover rounded-full transition-colors"
              >
                <Icon name="close" />
              </button>
            </div>

            {/* Drawer Body (Scrollable) */}
            <div className="flex-1 overflow-y-auto p-6 space-y-6 drawer-scroll">
              
              {/* Group 1: Đối tượng áp dụng */}
              <section className="space-y-4">
                <h4 className="text-xs font-semibold text-ink uppercase tracking-wider flex items-center gap-2">
                  <span className="w-1.5 h-4 bg-slate-800 rounded-full" />
                  Đối tượng áp dụng
                </h4>
                <div className="space-y-3">
                  <Select
                    label="Đối tượng"
                    value={form.audienceType}
                    onChange={(e) => handleAudienceTypeChange(e.target.value as AudienceType)}
                  >
                    <option value="ALL_USERS">Tất cả khách hàng</option>
                    <option value="NEW_CUSTOMER">Khách hàng mới</option>
                    <option value="LOYAL_CUSTOMER">Khách thân thiết</option>
                    <option value="SPECIFIC_USERS">Chỉ định cụ thể (SPECIFIC_USERS)</option>
                  </Select>

                  {/* Specific Users Picker */}
                  {form.audienceType === 'SPECIFIC_USERS' && (
                    <div className="border border-border rounded-xl p-3 bg-page shadow-sm space-y-3">
                      <div className="relative">
                        <span className="material-symbols-outlined absolute left-3 top-1/2 -translate-y-1/2 text-ink-muted text-[18px]">
                          search
                        </span>
                        <input
                          className="w-full pl-9 pr-4 py-2 bg-white border border-border rounded-lg text-sm focus:ring-2 focus:ring-brand outline-none"
                          placeholder="Tìm tên hoặc email người dùng..."
                          type="text"
                          value={userSearch}
                          onChange={(e) => setUserSearch(e.target.value)}
                        />
                        
                        {/* Search Results Dropdown */}
                        {userSearch && searchedUsers.length > 0 && (
                          <div className="absolute left-0 right-0 mt-1 max-h-48 overflow-y-auto bg-white border border-border rounded-lg shadow-lg z-10 divide-y divide-divider">
                            {searchedUsers.map((user) => (
                              <button
                                key={user.id}
                                type="button"
                                onClick={() => handleAddUser(user)}
                                className="w-full px-3 py-2 text-left text-xs hover:bg-page text-ink flex flex-col"
                              >
                                <span className="font-medium">{user.fullName}</span>
                                <span className="text-ink-muted">{user.email}</span>
                              </button>
                            ))}
                          </div>
                        )}
                      </div>

                      {/* Selected Users Chip List */}
                      <div className="flex flex-wrap gap-2">
                        {selectedUsers.length === 0 ? (
                          <span className="text-xs text-ink-muted italic">Chưa chọn người dùng nào</span>
                        ) : (
                          selectedUsers.map((u) => (
                            <span
                              key={u.userId}
                              className="inline-flex items-center gap-1.5 px-2.5 py-1 rounded-md bg-indigo-50 text-brand text-xs font-medium border border-indigo-100"
                            >
                              {u.fullName || u.email || 'Người dùng'}
                              <span
                                className="material-symbols-outlined text-[14px] cursor-pointer hover:text-indigo-900"
                                onClick={() => handleRemoveUser(u.userId)}
                              >
                                close
                              </span>
                            </span>
                          ))
                        )}
                      </div>
                    </div>
                  )}
                </div>
              </section>

              {/* Group 2: Mã & loại giảm */}
              <section className="space-y-4">
                <h4 className="text-xs font-semibold text-ink uppercase tracking-wider flex items-center gap-2">
                  <span className="w-1.5 h-4 bg-brand rounded-full" />
                  Mã &amp; loại giảm
                </h4>
                <div className="space-y-3">
                  <Input
                    label="Mã voucher"
                    required
                    value={form.code ?? ''}
                    disabled={!!editing && (editing.usedCount > 0)}
                    onChange={(e) => set({ code: e.target.value.toUpperCase() })}
                    helper={editing && editing.usedCount > 0 ? 'Không thể đổi code đã phát sinh lượt dùng' : undefined}
                    placeholder="VD: SUMMER24"
                  />

                  <div>
                    <label className="block text-sm font-medium text-ink-secondary mb-2">Loại giảm giá *</label>
                    <div className="grid grid-cols-2 gap-3">
                      <label className={`relative flex cursor-pointer rounded-lg border p-3 focus:outline-none transition-all ${
                        form.discountType === 'PERCENTAGE'
                          ? 'border-brand bg-indigo-50/50 ring-1 ring-brand'
                          : 'border-border bg-white hover:bg-page'
                      }`}>
                        <input
                          type="radio"
                          className="sr-only"
                          name="discountType"
                          value="PERCENTAGE"
                          checked={form.discountType === 'PERCENTAGE'}
                          onChange={() => set({ discountType: 'PERCENTAGE', maxDiscountAmount: 150000 })}
                        />
                        <div className="flex items-center gap-2">
                          <Icon name="percent" className={form.discountType === 'PERCENTAGE' ? 'text-brand' : 'text-ink-secondary'} />
                          <span className="text-sm font-medium text-ink">Phần trăm</span>
                        </div>
                      </label>
                      
                      <label className={`relative flex cursor-pointer rounded-lg border p-3 focus:outline-none transition-all ${
                        form.discountType === 'FIXED_AMOUNT'
                          ? 'border-brand bg-indigo-50/50 ring-1 ring-brand'
                          : 'border-border bg-white hover:bg-page'
                      }`}>
                        <input
                          type="radio"
                          className="sr-only"
                          name="discountType"
                          value="FIXED_AMOUNT"
                          checked={form.discountType === 'FIXED_AMOUNT'}
                          onChange={() => set({ discountType: 'FIXED_AMOUNT', maxDiscountAmount: null })}
                        />
                        <div className="flex items-center gap-2">
                          <Icon name="payments" className={form.discountType === 'FIXED_AMOUNT' ? 'text-brand' : 'text-ink-secondary'} />
                          <span className="text-sm font-medium text-ink">Số cố định</span>
                        </div>
                      </label>
                    </div>
                  </div>

                  <div className="grid grid-cols-2 gap-3">
                    <Input
                      label={form.discountType === 'PERCENTAGE' ? 'Giá trị (%) *' : 'Giá trị (₫) *'}
                      type="number"
                      required
                      value={form.discountValue}
                      onChange={(e) => set({ discountValue: Number(e.target.value) })}
                    />
                    <Input
                      label="Giảm tối đa (₫)"
                      type="number"
                      value={form.discountType === 'PERCENTAGE' ? (form.maxDiscountAmount ?? 0) : ''}
                      disabled={form.discountType === 'FIXED_AMOUNT'}
                      onChange={(e) => set({ maxDiscountAmount: e.target.value ? Number(e.target.value) : null })}
                      helper={
                        form.discountType === 'PERCENTAGE'
                          ? 'Bắt buộc với loại giảm theo %'
                          : 'Không áp dụng khi giảm theo số tiền cố định'
                      }
                    />
                  </div>
                </div>
              </section>

              {/* Group 3: Điều kiện áp dụng */}
              <section className="space-y-4">
                <h4 className="text-xs font-semibold text-ink uppercase tracking-wider flex items-center gap-2">
                  <span className="w-1.5 h-4 bg-info rounded-full" />
                  Điều kiện áp dụng
                </h4>
                <div className="space-y-3 bg-page p-4 rounded-xl border border-divider">
                  <Input
                    label="Giá trị đơn tối thiểu (₫)"
                    type="number"
                    value={form.minOrderAmount ?? 0}
                    onChange={(e) => set({ minOrderAmount: Number(e.target.value) })}
                  />
                  
                  <Input
                    label="Tuổi tài khoản (ngày)"
                    type="number"
                    value={form.minAccountAgeDays}
                    onChange={(e) => set({ minAccountAgeDays: Number(e.target.value) })}
                    placeholder="Để trống nếu không yêu cầu"
                  />

                  <div className="grid grid-cols-2 gap-3">
                    <Select
                      label="Đơn đầu tiên?"
                      value={form.firstOrderOnly ? 'true' : 'false'}
                      onChange={(e) => set({ firstOrderOnly: e.target.value === 'true' })}
                      disabled={form.audienceType === 'NEW_CUSTOMER' || form.audienceType === 'LOYAL_CUSTOMER'}
                      helper={
                        form.audienceType === 'NEW_CUSTOMER'
                          ? 'Bắt buộc cho khách mới'
                          : form.audienceType === 'LOYAL_CUSTOMER'
                          ? 'Không dùng cho khách thân thiết'
                          : undefined
                      }
                    >
                      <option value="false">Không bắt buộc</option>
                      <option value="true">Bắt buộc (First Order Only)</option>
                    </Select>
                    
                    <Input
                      label="Số đơn tối thiểu"
                      type="number"
                      value={form.minCompletedOrders}
                      min={form.audienceType === 'LOYAL_CUSTOMER' ? 3 : 0}
                      disabled={form.audienceType === 'NEW_CUSTOMER'}
                      onChange={(e) => {
                        const val = Number(e.target.value);
                        if (form.audienceType === 'LOYAL_CUSTOMER' && val < 3) {
                          set({ minCompletedOrders: 3 });
                        } else {
                          set({ minCompletedOrders: val });
                        }
                      }}
                      helper={
                        form.audienceType === 'NEW_CUSTOMER'
                          ? 'Mặc định là 0 cho khách mới'
                          : form.audienceType === 'LOYAL_CUSTOMER'
                          ? 'Yêu cầu tối thiểu là 3'
                          : undefined
                      }
                    />
                  </div>

                  <Input
                    label="Tổng chi tiêu tích lũy tối thiểu (₫)"
                    type="number"
                    value={form.minTotalSpent ?? 0}
                    disabled={form.audienceType === 'NEW_CUSTOMER'}
                    onChange={(e) => set({ minTotalSpent: Number(e.target.value) })}
                    helper={
                      form.audienceType === 'NEW_CUSTOMER'
                        ? 'Mặc định là 0 cho khách mới'
                        : undefined
                    }
                  />
                </div>
              </section>

              {/* Group 4: Giới hạn dùng */}
              <section className="space-y-4">
                <h4 className="text-xs font-semibold text-ink uppercase tracking-wider flex items-center gap-2">
                  <span className="w-1.5 h-4 bg-warning rounded-full" />
                  Giới hạn dùng
                </h4>
                <div className="grid grid-cols-2 gap-3">
                  <Input
                    label="Tổng lượt dùng"
                    type="number"
                    value={form.maxUses}
                    onChange={(e) => set({ maxUses: Number(e.target.value) })}
                  />
                  <Input
                    label="Giới hạn/người"
                    type="number"
                    value={form.perUserLimit}
                    onChange={(e) => set({ perUserLimit: Number(e.target.value) })}
                  />
                </div>
              </section>

              {/* Group 5: Thời gian hiệu lực */}
              <section className="space-y-4">
                <h4 className="text-xs font-semibold text-ink uppercase tracking-wider flex items-center gap-2">
                  <span className="w-1.5 h-4 bg-success rounded-full" />
                  Thời gian hiệu lực
                </h4>
                <div className="grid grid-cols-2 gap-3">
                  <Input
                    label="Từ ngày"
                    type="datetime-local"
                    value={startsLocal}
                    onChange={(e) => setStartsLocal(e.target.value)}
                  />
                  <Input
                    label="Đến ngày"
                    type="datetime-local"
                    value={expiresLocal}
                    onChange={(e) => setExpiresLocal(e.target.value)}
                  />
                </div>
              </section>

              {/* Status active state */}
              <label className="flex items-center gap-2 text-sm text-ink-secondary select-none">
                <input
                  type="checkbox"
                  className="h-4 w-4 rounded border-border text-brand focus:ring-brand"
                  checked={form.active ?? true}
                  onChange={(e) => set({ active: e.target.checked })}
                />
                Đang kích hoạt hoạt động
              </label>

              <div className="h-8" />
            </div>

            {/* Drawer Footer */}
            <div className="border-t border-divider bg-white p-4 shadow-[0_-4px_6px_-1px_rgba(0,0,0,0.05)]">
              
              {/* Live Summary Alert Box */}
              <div className="mb-4 px-3 py-2 bg-indigo-50/50 rounded-lg border border-indigo-100 flex items-start gap-2">
                <span className="material-symbols-outlined text-brand text-[18px] mt-0.5">info</span>
                <p className="text-sm text-indigo-900 font-medium leading-tight">
                  {getLiveSummary(form)}
                </p>
              </div>

              <div className="flex items-center justify-end gap-3">
                <Button type="button" variant="secondary" onClick={() => setOpen(false)}>
                  Hủy
                </Button>
                <Button type="button" onClick={() => saveMut.mutate()} loading={saveMut.isPending}>
                  Lưu
                </Button>
              </div>
            </div>
          </div>
        </>
      )}

      {/* Delete Confirmation */}
      <ConfirmDialog
        open={!!deleteTarget}
        onClose={() => setDeleteTarget(null)}
        onConfirm={() => deleteTarget && deleteMut.mutate(deleteTarget.id)}
        title="Xóa voucher này?"
        message={`Voucher "${deleteTarget?.code}" sẽ bị xóa.`}
        confirmLabel="Xóa"
        danger
        loading={deleteMut.isPending}
      />
    </div>
  );
}
