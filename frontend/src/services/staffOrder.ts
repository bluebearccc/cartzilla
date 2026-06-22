import { api } from '@/lib/api';
import type { PageResponse } from '@/types/catalog';
import type { OrderDetail, OrderSummary } from '@/types/order';

export interface StaffOrderQuery {
  status?: string;
  paymentMethod?: string;
  fromDate?: string;
  toDate?: string;
  page?: number;
  limit?: number;
}

function clean(q: StaffOrderQuery): Record<string, unknown> {
  const out: Record<string, unknown> = {};
  Object.entries(q).forEach(([k, v]) => {
    if (v !== undefined && v !== null && v !== '') out[k] = v;
  });
  return out;
}

export const staffOrderApi = {
  list: (q: StaffOrderQuery = {}) =>
    api.get<PageResponse<OrderSummary>>('/staff/orders', { params: clean(q) }),
  getOrder: (id: string) => api.get<OrderDetail>(`/staff/orders/${id}`),
  updateStatus: (id: string, status: string, reason?: string) =>
    api.put<OrderDetail>(`/staff/orders/${id}/status`, { status, reason }),
};
